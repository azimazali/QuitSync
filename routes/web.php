<?php

use App\Http\Controllers\ProfileController;
use Illuminate\Support\Facades\Route;

Route::get('/', function () {
    return view('welcome');
});

use App\Http\Controllers\DashboardController;

Route::get('/dashboard', [DashboardController::class, 'index'])
    ->middleware(['auth', 'verified'])
    ->name('dashboard');

use App\Http\Controllers\SmokingLogController;

Route::get('/activity', [SmokingLogController::class, 'index'])
    ->middleware(['auth', 'verified'])
    ->name('activity.index');

Route::resource('smoking-log', SmokingLogController::class)
    ->only(['store', 'edit', 'update', 'destroy', 'show'])
    ->middleware(['auth', 'verified']);

use App\Http\Controllers\GeofenceController;

Route::get('/zones', [GeofenceController::class, 'index'])
    ->middleware(['auth', 'verified'])
    ->name('geofences.index');

Route::post('/geofences', [GeofenceController::class, 'store'])
    ->middleware(['auth', 'verified'])
    ->name('geofences.store');

Route::get('/geofences/{geofence}', [GeofenceController::class, 'show'])
    ->middleware(['auth', 'verified'])
    ->name('geofences.show');

Route::get('/geofences/{geofence}/edit', [GeofenceController::class, 'edit'])
    ->middleware(['auth', 'verified'])
    ->name('geofences.edit');

Route::put('/geofences/{geofence}', [GeofenceController::class, 'update'])
    ->middleware(['auth', 'verified'])
    ->name('geofences.update');

Route::post('/geofences/recommendations/dismiss', [GeofenceController::class, 'dismissRecommendation'])
    ->middleware(['auth', 'verified'])
    ->name('geofences.recommendations.dismiss');

Route::delete('/geofences/{geofence}', [GeofenceController::class, 'destroy'])
    ->middleware(['auth', 'verified'])
    ->name('geofences.destroy');

use App\Http\Controllers\ForumController;
use App\Http\Controllers\CommentController;

Route::middleware(['auth', 'verified'])->group(function () {
    Route::get('/forum', [ForumController::class, 'index'])->name('forum.index');
    Route::get('/forum/create', [ForumController::class, 'create'])->name('forum.create');
    Route::post('/forum', [ForumController::class, 'store'])->name('forum.store');
    Route::get('/forum/{post}', [ForumController::class, 'show'])->name('forum.show');
    Route::get('/forum/{post}/edit', [ForumController::class, 'edit'])->name('forum.edit');
    Route::put('/forum/{post}', [ForumController::class, 'update'])->name('forum.update');
    Route::delete('/forum/{post}', [ForumController::class, 'destroy'])->name('forum.destroy');
    Route::post('/forum/{post}/lock', [ForumController::class, 'toggleLock'])->name('forum.lock');

    Route::post('/forum/{post}/comments', [CommentController::class, 'store'])->name('comments.store');
    Route::delete('/comments/{comment}', [CommentController::class, 'destroy'])->name('comments.destroy');
});

Route::middleware('auth')->group(function () {
    Route::get('/profile', [ProfileController::class, 'edit'])->name('profile.edit');
    Route::patch('/profile', [ProfileController::class, 'update'])->name('profile.update');
    Route::patch('/profile/smoking', [ProfileController::class, 'updateSmoking'])->name('profile.smoking.update');
    Route::delete('/profile', [ProfileController::class, 'destroy'])->name('profile.destroy');
});

use App\Http\Controllers\AdminController;

Route::middleware(['auth', 'admin'])->prefix('admin')->name('admin.')->group(function () {
    Route::resource('users', AdminController::class);
});

Route::get('/admin/login', [\App\Http\Controllers\Auth\AdminAuthenticatedSessionController::class, 'create'])->middleware('guest')->name('admin.login');
Route::post('/admin/login', [\App\Http\Controllers\Auth\AdminAuthenticatedSessionController::class, 'store'])->middleware('guest')->name('admin.login.store');

require __DIR__ . '/auth.php';
