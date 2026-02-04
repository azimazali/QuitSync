<?php

namespace App\Http\Controllers;

use App\Models\SmokingLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use App\Services\GeofenceService;
use App\Services\SentimentService;

class SmokingLogController extends Controller
{
    protected $geofenceService;
    protected $sentimentService;

    public function __construct(GeofenceService $geofenceService, SentimentService $sentimentService)
    {
        $this->geofenceService = $geofenceService;
        $this->sentimentService = $sentimentService;
    }

    public function index()
    {
        $recentLogs = SmokingLog::where('user_id', Auth::id())
            ->latest('smoked_at')
            ->take(10)
            ->get();

        return view('smoking_logs.index', compact('recentLogs'));
    }

    public function store(Request $request)
    {
        $request->validate([
            'notes' => 'nullable|string|max:1000',
            'latitude' => 'nullable|numeric|between:-90,90',
            'longitude' => 'nullable|numeric|between:-180,180',
            'address' => 'nullable|string|max:255',
            'quantity' => 'required|integer|min:0|max:100', // Allow 0 for 'resisted'
            'type' => 'required|in:smoked,resisted',
        ]);

        // Analyze Sentiment of notes
        $sentiment = ['score' => null, 'magnitude' => null, 'risk_level' => null];
        if ($request->filled('notes')) {
            $sentiment = $this->sentimentService->analyze($request->notes);
        }

        SmokingLog::create([
            'user_id' => Auth::id(),
            'smoked_at' => now(),
            'notes' => $request->notes,
            'latitude' => $request->latitude,
            'longitude' => $request->longitude,
            'address' => $request->address,
            'quantity' => $request->type === 'resisted' ? 0 : $request->quantity,
            'type' => $request->type,
            'sentiment_score' => $sentiment['score'],
            'sentiment_magnitude' => $sentiment['magnitude'],
            'risk_level' => $sentiment['risk_level'],
        ]);

        $message = '';
        $redirect = back(); // Redirect back to wherever they came from (Dashboard or Activity)

        // Trigger dynamic geofencing logic ONLY if smoked
        if ($request->type === 'smoked') {
            $this->geofenceService->detectAndCreateAutoGeofences(Auth::user());

            // Reset Quit Date to NOW to restart savings counter
            $user = Auth::user();
            $user->quit_date = now();
            $user->save();

            $message = 'Smoking event logged. Savings counter reset.';
        } else {
            $message = 'Great job resisting!';
        }

        // Add encouragement if risk is moderate or high
        if (in_array($sentiment['risk_level'], ['moderate', 'high'])) {
            $redirect->with('warning', 'We noticed you seem to be having a tough time. Stay strong, tomorrow is a new day! You can do this.');
        }

        return $redirect->with('status', $message);
    }

    public function show(SmokingLog $smokingLog)
    {
        if ($smokingLog->user_id !== Auth::id()) {
            abort(403);
        }
        return view('smoking_logs.show', compact('smokingLog'));
    }

    public function edit(SmokingLog $smokingLog)
    {
        if ($smokingLog->user_id !== Auth::id()) {
            abort(403);
        }
        return view('smoking_logs.edit', compact('smokingLog'));
    }

    public function update(Request $request, SmokingLog $smokingLog)
    {
        if ($smokingLog->user_id !== Auth::id()) {
            abort(403);
        }

        $request->validate([
            'notes' => 'nullable|string|max:1000',
            'smoked_at' => 'required|date',
            'quantity' => 'required|integer|min:1|max:100',
        ]);

        $smokingLog->update([
            'notes' => $request->notes,
            'smoked_at' => $request->smoked_at,
            'quantity' => $request->quantity,
        ]);

        return redirect()->route('dashboard')->with('status', 'Log updated successfully.');
    }

    public function destroy(SmokingLog $smokingLog)
    {
        if ($smokingLog->user_id !== Auth::id()) {
            abort(403);
        }

        $smokingLog->delete();

        return redirect()->route('dashboard')->with('status', 'Log deleted successfully.');
    }
}
