<?php

namespace App\Services;

use App\Models\Geofence;
use App\Models\User;
use Illuminate\Support\Facades\DB;

class GeofenceService
{
    private const CLUSTER_RADIUS_METERS = 50;
    private const MIN_LOGS_FOR_CLUSTER = 5;

    /**
     * Detect and create/update auto-geofences based on smoking history.
     */
    public function detectAndCreateAutoGeofences(User $user)
    {
        $logs = $user->smokingLogs()
            ->whereNotNull('latitude')
            ->whereNotNull('longitude')
            ->get();

        // 1. Cluster Analysis (Simplified "DBSCAN-like" Approach)
        // We aren't doing full DBSCAN but rather checking 'density' around the latest log first.
        // It's efficient enough for our use case: when a new log comes in, check its vicinity.

        // Actually, to fully "detect" new zones from past history, we should iterate all logs.
        // But optimization: assuming this runs when a new log is added, we focus on that area.
        // However, the request implies a robust "learning" system.
        // Let's implement a loop that checks density for every log to find centers.

        $clusters = [];
        $processedLogIds = [];

        foreach ($logs as $pivotLog) {
            if (in_array($pivotLog->id, $processedLogIds))
                continue;

            $neighbors = [];
            foreach ($logs as $targetLog) {
                $dist = $this->calculateDistance(
                    $pivotLog->latitude,
                    $pivotLog->longitude,
                    $targetLog->latitude,
                    $targetLog->longitude
                );

                if ($dist <= self::CLUSTER_RADIUS_METERS) {
                    $neighbors[] = $targetLog;
                }
            }

            if (count($neighbors) >= self::MIN_LOGS_FOR_CLUSTER) {
                // Found a cluster!
                $clusterLogs = collect($neighbors);

                // Calculate centroid
                $avgLat = $clusterLogs->avg('latitude');
                $avgLng = $clusterLogs->avg('longitude');

                $clusters[] = [
                    'latitude' => $avgLat,
                    'longitude' => $avgLng,
                    'count' => $clusterLogs->count(),
                    'last_smoked_at' => $clusterLogs->max('smoked_at'),
                    'logs' => $clusterLogs
                ];

                // Mark these as processed to avoid duplicate clusters for same area
                foreach ($neighbors as $n) {
                    $processedLogIds[] = $n->id;
                }
            }
        }

        // 2. Process Clusters into Geofences
        foreach ($clusters as $cluster) {
            $this->syncAutoGeofence($user, $cluster);
        }
    }

    private function syncAutoGeofence(User $user, array $cluster)
    {
        // Check if a geofence (Manual OR Auto) already exists near this center
        $existing = Geofence::where('user_id', $user->id)
            ->get()
            ->filter(function ($fence) use ($cluster) {
                $dist = $this->calculateDistance(
                    $fence->latitude,
                    $fence->longitude,
                    $cluster['latitude'],
                    $cluster['longitude']
                );
                // Overlap if distance is less than sum of radii (assuming standard 50m for auto)
                return $dist < ($fence->radius + self::CLUSTER_RADIUS_METERS);
            })->first();

        if ($existing) {
            // Logic:
            // If it's a manual fence, we DO NOT overwrite it. The user deemed this area special.
            // If it's an auto fence, we update it.
            if ($existing->is_auto) {
                $this->updateRiskScore($existing, $cluster);
            }
            return;
        }

        // Create new Auto Geofence
        $geofence = Geofence::create([
            'user_id' => $user->id,
            'name' => 'High Risk Area',
            'latitude' => $cluster['latitude'],
            'longitude' => $cluster['longitude'],
            'radius' => self::CLUSTER_RADIUS_METERS,
            'is_auto' => true,
            'risk_score' => 0 // will be updated below
        ]);

        $this->updateRiskScore($geofence, $cluster);
    }

    private function updateRiskScore(Geofence $geofence, array $cluster)
    {
        // Risk Score Algorithm:
        // Base: 10 points per cigarette
        // Recency Bonus: +50 if last log was < 7 days ago

        $score = $cluster['count'] * 10;

        $daysSinceLastSmoke = now()->diffInDays($cluster['last_smoked_at']);
        if ($daysSinceLastSmoke < 7) {
            $score += 50;
        }

        $geofence->update([
            'risk_score' => $score,
            // Optionally update center slightly to keep it accurate? Let's keep it simple for now. 
            // We don't want the fence jumping around too much.
        ]);
    }

    /**
     * Haversine Formula for distance in meters
     */
    private function calculateDistance($lat1, $lon1, $lat2, $lon2)
    {
        $earthRadius = 6371000; // meters

        $lat1 = deg2rad($lat1);
        $lon1 = deg2rad($lon1);
        $lat2 = deg2rad($lat2);
        $lon2 = deg2rad($lon2);

        $latDelta = $lat2 - $lat1;
        $lonDelta = $lon2 - $lon1;

        $a = sin($latDelta / 2) * sin($latDelta / 2) +
            cos($lat1) * cos($lat2) *
            sin($lonDelta / 2) * sin($lonDelta / 2);

        $c = 2 * atan2(sqrt($a), sqrt(1 - $a));

        return $earthRadius * $c;
    }
}
