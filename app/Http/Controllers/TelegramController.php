<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Telegram\Bot\Laravel\Facades\Telegram;
use App\Models\User;
use App\Services\GeofenceService;
use Illuminate\Support\Facades\Log;

class TelegramController extends Controller
{
    protected $geofenceService;

    public function __construct(GeofenceService $geofenceService)
    {
        $this->geofenceService = $geofenceService;
    }

    public function webhook(Request $request)
    {
        try {
            $update = Telegram::getWebhookUpdates();

            // If commandsHandler returns an array of updates, use the first one or iterate.
            // Usually for webhook it processes the specific update.
            // However, we might need to manually handle non-command messages or if commandsHandler doesn't cover everything we want.

            // For now, let's assume we use commands or standard text checking.
            // We can also just get the message directly if commandsHandler isn't enough.

            $message = $update->getMessage();
            if ($message) {
                $chatId = $message->getChat()->getId();
                $userId = $message->getFrom()->getId();

                // Handle Location
                if ($message->getLocation()) {
                    $location = $message->getLocation();
                    $this->replyWithNearest($chatId, $location->getLatitude(), $location->getLongitude());
                    return response('OK', 200);
                }

                $text = $message->getText();

                // Handle /start <token>
                if (str_starts_with($text, '/start ')) {
                    $token = trim(substr($text, 7));
                    $this->linkUser($chatId, $token);
                }
                // Handle /nearest
                elseif ($text === '/nearest') {
                    $this->askForLocation($chatId);
                }
            }

            return response('OK', 200);
        } catch (\Exception $e) {
            Log::error('Telegram Webhook Error: ' . $e->getMessage());
            return response('Error', 200); // Return 200 to keep Telegram happy so it stops retrying
        }
    }

    private function linkUser($chatId, $token)
    {
        $user = User::where('telegram_verification_token', $token)->first();

        if ($user) {
            $user->update([
                'telegram_chat_id' => $chatId,
                'telegram_verification_token' => null // Invalidate token after use
            ]);
            Telegram::sendMessage([
                'chat_id' => $chatId,
                'text' => 'Successfully linked your QuitSync account! You will now receive notifications when entering high-risk zones.'
            ]);
        } else {
            Telegram::sendMessage([
                'chat_id' => $chatId,
                'text' => 'Invalid or expired verification token. Please try generating a new one from your dashboard.'
            ]);
        }
    }

    private function askForLocation($chatId)
    {
        Telegram::sendMessage([
            'chat_id' => $chatId,
            'text' => 'To find the nearest high-risk zone, please share your current location using the paperclip icon 📎 or the "Location" button.',
        ]);
    }

    private function replyWithNearest($chatId, $latitude, $longitude)
    {
        $user = User::where('telegram_chat_id', $chatId)->first();

        if (!$user) {
            Telegram::sendMessage([
                'chat_id' => $chatId,
                'text' => 'Account not linked. Use /start <token>.'
            ]);
            return;
        }

        $result = $this->geofenceService->getNearestZone($user, $latitude, $longitude);

        if ($result && isset($result['zone'])) {
            $zone = $result['zone'];
            $dist = round($result['distance']);
            Telegram::sendMessage([
                'chat_id' => $chatId,
                'text' => "📍 Nearest Zone: {$zone->name}\n📏 Distance: {$dist} meters away.\n\nStay alert!"
            ]);

            // Optionally send location of the zone
            Telegram::sendLocation([
                'chat_id' => $chatId,
                'latitude' => $zone->latitude,
                'longitude' => $zone->longitude
            ]);
        } else {
            Telegram::sendMessage([
                'chat_id' => $chatId,
                'text' => 'No zones found nearby.'
            ]);
        }
    }
}
