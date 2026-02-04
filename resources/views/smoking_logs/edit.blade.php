<x-app-layout>

    <div class="py-12">
        <div class="max-w-7xl mx-auto sm:px-6 lg:px-8">
            <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg">
                <div class="p-6 text-gray-900">
                    <form method="POST" action="{{ route('smoking-log.update', $smokingLog) }}">
                        @csrf
                        @method('PUT')

                        <!-- Quantity -->
                        <div>
                            <x-input-label for="quantity" :value="__('Cigarettes Smoked')" />
                            <x-text-input id="quantity" class="block mt-1 w-full" type="number" name="quantity"
                                :value="old('quantity', $smokingLog->quantity)" required autofocus />
                            <x-input-error :messages="$errors->get('quantity')" class="mt-2" />
                        </div>

                        <!-- Date/Time -->
                        <div class="mt-4">
                            <x-input-label for="smoked_at" :value="__('Time')" />
                            <x-text-input id="smoked_at" class="block mt-1 w-full" type="datetime-local"
                                name="smoked_at" :value="old('smoked_at', $smokingLog->smoked_at->format('Y-m-d\TH:i'))"
                                required />
                            <x-input-error :messages="$errors->get('smoked_at')" class="mt-2" />
                        </div>

                        <!-- Notes -->
                        <div class="mt-4">
                            <x-input-label for="notes" :value="__('Notes')" />
                            <textarea id="notes" name="notes"
                                class="block mt-1 w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500">{{ old('notes', $smokingLog->notes) }}</textarea>
                            <x-input-error :messages="$errors->get('notes')" class="mt-2" />
                        </div>

                        <div class="flex items-center justify-end mt-4">
                            <a href="{{ route('activity.index') }}" class="mr-4">
                                <x-secondary-button type="button">{{ __('Cancel') }}</x-secondary-button>
                            </a>
                            <x-primary-button>
                                {{ __('Update Log') }}
                            </x-primary-button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</x-app-layout>