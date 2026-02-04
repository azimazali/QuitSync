<?php

namespace App\Http\Controllers;

use App\Models\Geofence;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\DB;

class GeofenceController extends Controller
{
    public function index()
    {
        $userGeofences = Auth::user()->geofences;
        $recommendedZones = $this->getRecommendedZones();

        // Filter out recommended zones that overlap with user's existing zones
        $finalRecommendations = $recommendedZones->filter(function ($rec) use ($userGeofences) {
            foreach ($userGeofences as $myFence) {
                if ($this->calculateDistance($rec->latitude, $rec->longitude, $myFence->latitude, $myFence->longitude) < 100) {
                    return false; // User already has a zone here
                }
            }
            return true;
        });

        $geofences = $userGeofences->merge($finalRecommendations);

        return view('geofences.index', compact('geofences'));
    }



    public function dismissRecommendation(Request $request)
    {
        $request->validate([
            'latitude' => 'required|numeric',
            'longitude' => 'required|numeric',
        ]);

        DB::table('dismissed_recommendations')->insert([
            'user_id' => Auth::id(),
            'latitude' => $request->latitude,
            'longitude' => $request->longitude,
            'radius' => 100, // Matching the cluster radius
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        return back()->with('status', 'Recommendation dismissed.');
    }

    private function getRecommendedZones()
    {
        // Filter out admins from user count and geofences
        $totalUsers = \App\Models\User::where('is_admin', 0)->count();

        // Ensure new users see recommendations as long as there is community data
        // If I am the only user, no recommendations. If there are >= 2 users, show.
        // Or if I am a new user, show recommendations if they exist.
        // Actually, we want to show recommendations derived from OTHER users.

        $otherUsersCount = \App\Models\User::where('is_admin', 0)
            ->where('id', '!=', Auth::id())
            ->count();

        if ($otherUsersCount < 1)
            return collect([]);

        $threshold = max(1, $otherUsersCount / 2); // At least 50% of OTHER users

        // Filter out admin geofences and my own geofences
        $allGeofences = Geofence::whereHas('user', function ($q) {
            $q->where('is_admin', 0);
        })->where('user_id', '!=', Auth::id())
            ->get();

        $clusters = [];
        $radius = 100; // Cluster radius in meters

        // Simple clustering
        foreach ($allGeofences as $fence) {
            $added = false;
            foreach ($clusters as &$cluster) {
                // Check distance to the first element of existing cluster
                if ($this->calculateDistance($fence->latitude, $fence->longitude, $cluster[0]->latitude, $cluster[0]->longitude) < $radius) {
                    $cluster[] = $fence;
                    $added = true;
                    break;
                }
            }
            if (!$added) {
                $clusters[] = [$fence];
            }
        }

        $recommendations = collect([]);

        // Load dismissed zones for this user
        $dismissed = DB::table('dismissed_recommendations')
            ->where('user_id', Auth::id())
            ->get();

        foreach ($clusters as $cluster) {
            $userIds = collect($cluster)->pluck('user_id')->unique();

            if ($userIds->count() >= $threshold) { // Changed to >= to be inclusive
                // Calculate centroid
                $latSum = 0;
                $lngSum = 0;
                foreach ($cluster as $c) {
                    $latSum += $c->latitude;
                    $lngSum += $c->longitude;
                }
                $count = count($cluster);
                $avgLat = $latSum / $count;
                $avgLng = $lngSum / $count;

                // Check if this centroid is near a dismissed zone
                $isDismissed = $dismissed->contains(function ($d) use ($avgLat, $avgLng) {
                    return $this->calculateDistance($avgLat, $avgLng, $d->latitude, $d->longitude) < ($d->radius + 50); // Small buffer
                });

                if ($isDismissed) {
                    continue;
                }

                $rec = new Geofence();
                $rec->name = "Community Hotspot";
                $rec->latitude = $avgLat;
                $rec->longitude = $avgLng;
                $rec->radius = 150;
                $rec->risk_score = 'high';
                $rec->is_recommended = true;

                $recommendations->push($rec);
            }
        }

        return $recommendations;
    }

    private function calculateDistance($lat1, $lon1, $lat2, $lon2)
    {
        $earthRadius = 6371000; // meters

        $dLat = deg2rad($lat2 - $lat1);
        $dLon = deg2rad($lon2 - $lon1);

        $a = sin($dLat / 2) * sin($dLat / 2) +
            cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
            sin($dLon / 2) * sin($dLon / 2);

        $c = 2 * atan2(sqrt($a), sqrt(1 - $a));

        return $earthRadius * $c;
    }
    public function store(Request $request)
    {
        $request->validate([
            'name' => 'required|string|max:255',
            'latitude' => 'required|numeric|between:-90,90',
            'longitude' => 'required|numeric|between:-180,180',
            'radius' => 'required|integer|min:10|max:5000',
        ]);

        Geofence::create([
            'user_id' => Auth::id(),
            'name' => $request->name,
            'latitude' => $request->latitude,
            'longitude' => $request->longitude,
            'radius' => $request->radius,
        ]);

        return back()->with('status', 'Geofence added successfully.');
    }

    public function destroy(Geofence $geofence)
    {
        if ($geofence->user_id !== Auth::id()) {
            abort(403);
        }

        $geofence->delete();

        return redirect()->route('dashboard')->with('status', 'Geofence deleted successfully!');
    }

    public function show(Geofence $geofence)
    {
        if ($geofence->user_id !== Auth::id()) {
            abort(403);
        }
        return view('geofences.show', compact('geofence'));
    }

    public function edit(Geofence $geofence)
    {
        if ($geofence->user_id !== Auth::id()) {
            abort(403);
        }
        return view('geofences.edit', compact('geofence'));
    }

    public function update(Request $request, Geofence $geofence)
    {
        if ($geofence->user_id !== Auth::id()) {
            abort(403);
        }

        $request->validate([
            'name' => 'required|string|max:255',
            'latitude' => 'required|numeric|between:-90,90',
            'longitude' => 'required|numeric|between:-180,180',
            'radius' => 'required|integer|min:10|max:5000',
        ]);

        $geofence->update($request->all());

        return redirect()->route('dashboard')->with('status', 'Geofence updated successfully.');
    }
}
