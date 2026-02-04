<?php

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;
use App\Http\Controllers\TelegramController;
use App\Http\Controllers\Api\GeofenceCheckController;

Route::post('/telegram/webhook', [TelegramController::class, 'webhook']);

Route::middleware('auth:sanctum')->group(function () {
    Route::post('/geofence/check', [GeofenceCheckController::class, 'check']);
});
