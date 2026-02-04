<x-app-layout>


    <div class="py-12">
        <div class="max-w-7xl mx-auto sm:px-6 lg:px-8">
            <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg">
                <div class="p-6 text-gray-900">
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <!-- Name -->
                        <div class="mb-4">
                            <x-input-label :value="__('Name')" />
                            <div class="text-lg font-bold text-gray-800 mt-1">{{ $user->name }}</div>
                        </div>

                        <!-- Email -->
                        <div class="mb-4">
                            <x-input-label :value="__('Email')" />
                            <div class="text-gray-700 mt-1">{{ $user->email }}</div>
                        </div>

                        <!-- Joined Date -->
                        <div class="mb-4">
                            <x-input-label :value="__('Joined At')" />
                            <div class="text-gray-700 mt-1">{{ $user->created_at->format('M d, Y h:i A') }}</div>
                        </div>

                        <!-- Quit Date -->
                        <div class="mb-4">
                            <x-input-label :value="__('Quit Date')" />
                            <div class="text-gray-700 mt-1">
                                {{ $user->quit_date ? \Carbon\Carbon::parse($user->quit_date)->format('M d, Y') : 'Not set' }}
                            </div>
                        </div>

                        <!-- Cigarettes Per Day -->
                        <div class="mb-4">
                            <x-input-label :value="__('Cigarettes Per Day')" />
                            <div class="text-gray-700 mt-1">{{ $user->cigarettes_per_day ?? 'Not set' }}</div>
                        </div>

                        <!-- Pack Price -->
                        <div class="mb-4">
                            <x-input-label :value="__('Pack Price')" />
                            <div class="text-gray-700 mt-1">
                                {{ $user->pack_price ? 'RM' . number_format($user->pack_price, 2) : 'Not set' }}
                            </div>
                        </div>
                    </div>

                    <div class="flex items-center justify-end mt-6">
                        <a href="{{ route('admin.users.index') }}"
                            class="text-sm font-medium text-indigo-600 hover:text-indigo-900 mr-4">
                            &larr; {{ __('Back to Users') }}
                        </a>
                        <a href="{{ route('admin.users.edit', $user) }}"
                            class="bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-2 px-4 rounded shadow transition text-sm">
                            {{ __('Edit User') }}
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</x-app-layout>