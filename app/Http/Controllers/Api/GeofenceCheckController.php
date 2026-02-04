<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use App\Services\GeofenceService;
use Illuminate\Support\Facades\Auth;

class GeofenceCheckController extends Controller
{
    protected $geofenceService;

    public function __construct(GeofenceService $geofenceService)
    {
        $this->geofenceService = $geofenceService;
    }

    public function check(Request $request)
    {
        $request->validate([
            'latitude' => 'required|numeric',
            'longitude' => 'required|numeric',
        ]);

        $user = Auth::user();

        // This method should check boundaries and send notification if needed
        $result = $this->geofenceService->checkAndNotify(
            $user,
            $request->latitude,
            $request->longitude
        );

        return response()->json([
            'status' => 'success',
            'in_zone' => $result['in_zone'],
            'zone' => $result['zone'] ?? null,
            'notified' => $result['notified']
        ]);
    }
}
