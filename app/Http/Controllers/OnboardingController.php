<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Http\RedirectResponse;
use Illuminate\View\View;
use Illuminate\Support\Facades\Auth;

class OnboardingController extends Controller
{
    /**
     * Display the onboarding view.
     */
    public function create(): View
    {
        return view('onboarding', [
            'user' => Auth::user(),
        ]);
    }

    /**
     * Handle the incoming onboarding request.
     */
    public function store(Request $request): RedirectResponse
    {
        $request->validate([
            'cigarettes_per_day' => ['required', 'integer', 'min:0'],
            'pack_price' => ['required', 'numeric', 'min:0'],
            'quit_date' => ['nullable', 'date'],
        ]);

        $user = Auth::user();

        // Update user profile with smoking habits
        $user->forceFill([
            'cigarettes_per_day' => $request->cigarettes_per_day,
            'pack_price' => $request->pack_price,
            'quit_date' => $request->quit_date,
        ])->save();

        return redirect()->route('dashboard')->with('status', 'profile-updated');
    }
}
