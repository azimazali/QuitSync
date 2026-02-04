<?php

namespace App\Http\Controllers;

use App\Models\SmokingLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Carbon\Carbon;

class DashboardController extends Controller
{
    public function index()
    {
        $user = Auth::user();

        // Check if user smoked today
        $smokedToday = SmokingLog::where('user_id', $user->id)
            ->whereDate('smoked_at', Carbon::today())
            ->where('type', 'smoked')
            ->exists();

        // Calculate Penalty (Total Cost of Smoked Cigarettes SINCE quit date)
        $totalPenalty = 0;
        if ($user->quit_date) {
            $totalSmoked = SmokingLog::where('user_id', $user->id)
                ->where('smoked_at', '>', $user->quit_date)
                ->sum('quantity');
            $costPerCig = ($user->pack_price ?? 10) / 20;
            $totalPenalty = $totalSmoked * $costPerCig;
        }

        // Calendar Logic
        $startOfMonth = Carbon::now()->startOfMonth();
        $endOfMonth = Carbon::now()->endOfMonth();
        $today = Carbon::today();

        $monthlyLogs = SmokingLog::where('user_id', $user->id)
            ->whereBetween('smoked_at', [$startOfMonth, $endOfMonth])
            ->where('type', 'smoked') // Only count actual smoking, not resisted
            ->get()
            ->groupBy(function ($date) {
                return Carbon::parse($date->smoked_at)->format('Y-m-d');
            });

        $calendar = [];
        $currentDate = $startOfMonth->copy();

        while ($currentDate <= $endOfMonth) {
            $dateString = $currentDate->format('Y-m-d');
            $dayNumber = $currentDate->day;

            if ($currentDate->isFuture()) {
                $status = 'future';
            } elseif ($monthlyLogs->has($dateString)) {
                $status = 'smoked';
            } else {
                // Check if before user joined? Optional, but simpler to just say CLEAN if they didn't log.
                // Or maybe check quit_date if available. 
                // Let's assume if it's in the past and no log, it's clean.
                $status = 'clean';
            }

            $calendar[] = [
                'day' => $dayNumber,
                'date' => $dateString,
                'status' => $status,
                'is_today' => $currentDate->isToday(),
            ];

            $currentDate->addDay();
        }

        // Top Topics Analysis (NLP Tags)
        $posts = \App\Models\Post::where('user_id', $user->id)->whereNotNull('tags')->get();
        $tagCounts = [];
        foreach ($posts as $post) {
            if ($post->tags) {
                foreach ($post->tags as $tag) {
                    if (!isset($tagCounts[$tag]))
                        $tagCounts[$tag] = 0;
                    $tagCounts[$tag]++;
                }
            }
        }
        arsort($tagCounts);
        arsort($tagCounts);
        $topTopics = array_slice($tagCounts, 0, 5, true);

        // Leaderboard Logic
        $users = \App\Models\User::where('is_admin', 0)->get();
        $leaderboard = $users->map(function ($u) {
            if (!$u->quit_date) {
                return ['name' => $u->name, 'days' => 0];
            }

            $lastSmoke = SmokingLog::where('user_id', $u->id)
                ->where('type', 'smoked')
                ->max('smoked_at');

            $startDate = $u->quit_date;

            if ($lastSmoke) {
                $lastSmokeDate = Carbon::parse($lastSmoke);
                if ($lastSmokeDate->gt($startDate)) {
                    $startDate = $lastSmokeDate;
                }
            }

            $days = (int) $startDate->diffInDays(Carbon::now());

            return [
                'name' => $u->name,
                'days' => $days,
                // Mask name for privacy if needed, e.g., "Azim A."
                'display_name' => $u->name
            ];
        })->filter(function ($item) {
            return $item['days'] > 0; // Only show positive streaks
        })->sortByDesc('days')->values()->take(5);

        return view('dashboard', compact('smokedToday', 'totalPenalty', 'calendar', 'topTopics', 'leaderboard'));
    }
}

