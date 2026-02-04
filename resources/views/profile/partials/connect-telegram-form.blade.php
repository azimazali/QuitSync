<section>
    <header>
        <h2 class="text-lg font-medium text-gray-900">
            {{ __('Connect Telegram') }}
        </h2>

        <p class="mt-1 text-sm text-gray-600">
            {{ __('Link your Telegram account to receive real-time notifications when you enter high-risk zones.') }}
        </p>
    </header>

    <div class="mt-6 space-y-6">
        @if(Auth::user()->telegram_chat_id)
            <div class="flex items-center gap-4">
                <div class="text-green-600 font-medium">
                    {{ __('✅ Connected') }}
                </div>
                <div class="text-sm text-gray-500">
                    {{ __('Chat ID: ') . Auth::user()->telegram_chat_id }}
                </div>
            </div>
        @else
            @if(Auth::user()->telegram_verification_token)
                <div class="bg-gray-50 p-4 rounded-md">
                    <p class="text-sm text-gray-700 mb-2">
                        {{ __('To connect, click the button below or send the command to our bot:') }}
                    </p>
                    <div class="font-mono bg-gray-200 p-2 rounded text-center mb-4">
                        /start {{ Auth::user()->telegram_verification_token }}
                    </div>

                    @php
                        $botUsername = env('TELEGRAM_BOT_USERNAME', 'QuitSyncBot');
                    @endphp

                    <a href="https://t.me/{{ $botUsername }}?start={{ Auth::user()->telegram_verification_token }}"
                        target="_blank"
                        class="inline-flex items-center px-4 py-2 bg-indigo-600 border border-transparent rounded-md font-semibold text-xs text-white uppercase tracking-widest hover:bg-indigo-500 focus:bg-indigo-700 active:bg-indigo-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 transition ease-in-out duration-150">
                        {{ __('Open in Telegram') }}
                    </a>
                </div>
            @endif

            <form method="post" action="{{ route('profile.telegram.generate') }}" class="mt-6">
                @csrf
                <x-primary-button>{{ __('Generate Connection Token') }}</x-primary-button>

                @if (session('status') === 'telegram-token-generated')
                    <p x-data="{ show: true }" x-show="show" x-transition x-init="setTimeout(() => show = false, 2000)"
                        class="text-sm text-gray-600 dark:text-gray-400">{{ __('Token Generated.') }}</p>
                @endif
            </form>
        @endif
    </div>
</section>