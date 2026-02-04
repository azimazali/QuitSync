<?php

use App\Models\User;
use App\Models\SmokingLog;
use Carbon\Carbon;

// Mock data check or just query existing users
$users = User::where('is_admin', 0)->get();

$leaderboard = $users->map(function ($user) {
    if (!$user->quit_date) {
        return ['name' => $user->name, 'days' => 0];
    }

    $lastSmoke = SmokingLog::where('user_id', $user->id)
        ->where('type', 'smoked')
        ->max('smoked_at');

    $startDate = $user->quit_date;

    if ($lastSmoke) {
        $lastSmokeDate = Carbon::parse($lastSmoke);
        if ($lastSmokeDate->gt($startDate)) {
            $startDate = $lastSmokeDate;
        }
    }

    $days = (int) $startDate->diffInDays(Carbon::now()); // Floor

    return [
        'name' => $user->name,
        'days' => $days
    ];
})->sortByDesc('days')->values()->take(5);

print_r($leaderboard);
