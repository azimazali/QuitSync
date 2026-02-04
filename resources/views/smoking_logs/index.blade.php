<x-app-layout>
    <!-- Header removed -->

    <div class="py-12">
        <div class="max-w-7xl mx-auto sm:px-6 lg:px-8 space-y-6">

            <!-- Message Notification -->
            @if (session('status'))
                <div class="bg-emerald-100 border border-emerald-400 text-emerald-700 px-4 py-3 rounded relative"
                    role="alert">
                    <span class="block sm:inline">{{ session('status') }}</span>
                </div>
            @endif

            <!-- Warning/Encouragement Notification -->
            @if (session('warning'))
                <div class="bg-amber-100 border-l-4 border-amber-500 text-amber-700 p-4 mb-6" role="alert">
                    <p class="font-bold">Message for You</p>
                    <p>{{ session('warning') }}</p>
                </div>
            @endif

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <!-- Log Activity Form -->
                <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg p-6">
                    <h3 class="text-lg font-medium text-gray-900 mb-4">Log Activity</h3>
                    <form action="{{ route('smoking-log.store') }}" method="POST" id="logForm">
                        @csrf
                        <input type="hidden" name="latitude" id="lat">
                        <input type="hidden" name="longitude" id="lng">
                        <input type="hidden" name="address" id="address">
                        <input type="hidden" name="type" id="logType" value="smoked">

                        <div class="mb-4">
                            <label for="notes" class="block text-sm font-medium text-gray-700">Notes
                                (Optional)</label>
                            <textarea name="notes" id="notes" rows="2"
                                class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm"></textarea>
                        </div>

                        <div class="mb-4">
                            <label for="quantity" class="block text-sm font-medium text-gray-700">How many
                                cigarettes? (If smoked)</label>
                            <input type="number" name="quantity" id="quantity" value="1" min="1" max="100" required
                                class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm">
                        </div>

                        <div class="grid grid-cols-2 gap-4">
                            <button type="button" onclick="getLocationAndSubmit('smoked')"
                                class="w-full bg-red-500 hover:bg-red-600 text-white font-bold py-3 px-4 rounded-lg shadow transition flex justify-center items-center">
                                <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z">
                                    </path>
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"></path>
                                </svg>
                                I Smoked
                            </button>
                            <button type="button" onclick="getLocationAndSubmit('resisted')"
                                class="w-full bg-emerald-500 hover:bg-emerald-600 text-white font-bold py-3 px-4 rounded-lg shadow transition flex justify-center items-center">
                                <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                                </svg>
                                I Resisted
                            </button>
                        </div>
                        <p id="locationStatus" class="text-xs text-gray-500 mt-2 text-center"></p>
                    </form>
                </div>

                <!-- Recent Logs -->
                <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg p-6">
                    <h3 class="text-lg font-medium text-gray-900 mb-4">Recent Logs</h3>
                    <ul class="divide-y divide-gray-200">
                        @forelse ($recentLogs as $log)
                            <li class="py-3">
                                <div class="text-sm font-medium text-gray-900 flex justify-between">
                                    <span>{{ $log->smoked_at->format('M d, h:i A') }}</span>
                                    <span
                                        class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full {{ $log->type === 'resisted' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800' }}">
                                        {{ ucfirst($log->type) }}
                                    </span>
                                </div>
                                <div class="text-xs text-gray-500 truncate">{{ $log->address ?? 'No address' }}
                                    @if($log->risk_level)
                                        <span
                                            class="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium
                                                        {{ $log->risk_level === 'high' ? 'bg-red-100 text-red-800' : '' }}
                                                        {{ $log->risk_level === 'moderate' ? 'bg-amber-100 text-amber-800' : '' }}
                                                        {{ $log->risk_level === 'low' ? 'bg-emerald-100 text-emerald-800' : '' }}">
                                            Risk: {{ ucfirst($log->risk_level) }}
                                        </span>
                                    @endif
                                </div>
                                @if($log->notes)
                                <div class="text-xs text-gray-400 italic">"{{ $log->notes }}"</div> @endif

                                <div class="flex items-center gap-2 mt-1">
                                    <a href="{{ route('smoking-log.edit', $log) }}"
                                        class="text-xs text-indigo-600 hover:text-indigo-900">Edit</a>
                                    <a href="{{ route('smoking-log.show', $log) }}"
                                        class="text-xs text-indigo-600 hover:text-indigo-900">View</a>
                                    <form action="{{ route('smoking-log.destroy', $log) }}" method="POST"
                                        onsubmit="return confirm('Delete this log?');" class="inline">
                                        @csrf
                                        @method('DELETE')
                                        <button type="submit"
                                            class="text-xs text-red-600 hover:text-red-900">Delete</button>
                                    </form>
                                </div>
                            </li>
                        @empty
                            <li class="text-sm text-gray-500">No logs yet.</li>
                        @endforelse
                    </ul>
                </div>
            </div>
        </div>
    </div>

    <!-- Google Maps Script for Geocoding (if needed, or just standard geolocation) -->
    <!-- Note: The previous logic used geocoder which requires the Google Maps API. -->
    <script>
        let geocoder;

        // We need to initialize Geocoder if the API is loaded. 
        // Since we aren't loading the full map here, we might need to load the JS API or just rely on browser nav given the constraints.
        // However, the original code used `geocoder` for address lookup.
        // I will include the script tag but only for libraries=places or geometry if needed, but here mostly for Geocoder.
    </script>

    <script src="https://maps.googleapis.com/maps/api/js?key={{ env('GOOGLE_MAPS_API_KEY') }}&callback=initGeocoder"
        async defer></script>

    <script>
        function initGeocoder() {
            geocoder = new google.maps.Geocoder();
        }

        function getLocationAndSubmit(type = 'smoked') {
            document.getElementById('logType').value = type;
            const statusFn = document.getElementById('locationStatus');
            statusFn.innerText = "Acquiring location...";

            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(
                    (position) => {
                        document.getElementById('lat').value = position.coords.latitude;
                        document.getElementById('lng').value = position.coords.longitude;

                        if (geocoder) {
                            const latlng = { lat: position.coords.latitude, lng: position.coords.longitude };
                            geocoder.geocode({ location: latlng }, (results, status) => {
                                if (status === "OK" && results[0]) {
                                    document.getElementById('address').value = results[0].formatted_address;
                                }
                                document.getElementById('logForm').submit();
                            });
                        } else {
                            document.getElementById('logForm').submit();
                        }
                    },
                    (error) => {
                        // Fallback submission
                        statusFn.innerText = "Location failed. Submitting anyway.";
                        document.getElementById('logForm').submit();
                    }
                );
            } else {
                document.getElementById('logForm').submit();
            }
        }
    </script>
</x-app-layout>