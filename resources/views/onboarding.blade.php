<x-app-layout>
    <div class="py-12">
        <div class="max-w-xl mx-auto sm:px-6 lg:px-8">
            <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg p-6">
                <header>
                    <h2 class="text-xl font-bold text-gray-900">
                        {{ __('Welcome to QuitSync! 👋') }}
                    </h2>
                    <p class="mt-2 text-sm text-gray-600">
                        {{ __("To help us track your progress and savings accurately, please tell us a bit about your smoking habits.") }}
                    </p>
                </header>

                <form method="POST" action="{{ route('onboarding.store') }}" class="mt-6 space-y-6">
                    @csrf

                    <div>
                        <x-input-label for="cigarettes_per_day" :value="__('Cigarettes Per Day')" />
                        <x-text-input id="cigarettes_per_day" name="cigarettes_per_day" type="number"
                            class="mt-1 block w-full" :value="old('cigarettes_per_day', 20)" required autofocus
                            autocomplete="cigarettes_per_day" />
                        <x-input-error class="mt-2" :messages="$errors->get('cigarettes_per_day')" />
                    </div>

                    <div>
                        <x-input-label for="pack_price" :value="__('Pack Price ($)')" />
                        <x-text-input id="pack_price" name="pack_price" type="number" step="0.01"
                            class="mt-1 block w-full" :value="old('pack_price', 10.00)" required
                            autocomplete="pack_price" />
                        <x-input-error class="mt-2" :messages="$errors->get('pack_price')" />
                    </div>

                    <div>
                        <x-input-label for="quit_date" :value="__('Quit Date')" />
                        <x-text-input id="quit_date" name="quit_date" type="datetime-local" class="mt-1 block w-full"
                            :value="old('quit_date')" autocomplete="quit_date" />
                        <x-input-error class="mt-2" :messages="$errors->get('quit_date')" />
                        <p class="mt-1 text-xs text-gray-500">Leave empty if you haven't quit yet.</p>
                    </div>

                    <div class="flex items-center justify-end gap-4 mt-8">
                        <x-primary-button class="w-full justify-center py-3">
                            {{ __('Start My Journey!') }}
                        </x-primary-button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</x-app-layout>