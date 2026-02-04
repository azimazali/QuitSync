<x-app-layout>

    <div class="py-12">
        <div class="max-w-7xl mx-auto sm:px-6 lg:px-8">
            <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg p-6">
                <div class="flex justify-between items-center mb-6">
                    <div>
                        <div class="flex items-center gap-4">
                            <a href="{{ route('geofences.index') }}">
                                <x-secondary-button type="button">
                                    &larr; Back to Zones
                                </x-secondary-button>
                            </a>
                            <div>
                                <h1 class="text-2xl font-bold text-gray-900">{{ $geofence->name }}</h1>
                                <p class="text-gray-500">Radius: {{ $geofence->radius }} meters</p>
                            </div>
                        </div>
                    </div>
                    <div class="flex gap-2">
                        <a href="{{ route('geofences.edit', $geofence) }}"
                            class="bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-2 px-4 rounded shadow transition text-sm">
                            Edit Zone
                        </a>
                        <form action="{{ route('geofences.destroy', $geofence) }}" method="POST"
                            onsubmit="return confirm('Delete this zone?');">
                            @csrf @method('DELETE')
                            <button type="submit"
                                class="bg-red-500 hover:bg-red-600 text-white font-bold py-2 px-4 rounded shadow transition text-sm">
                                Delete
                            </button>
                        </form>
                    </div>
                </div>

                <div id="map" class="w-full h-[500px] rounded-lg bg-gray-200"></div>
                <p class="text-xs text-center text-gray-400 mt-2">Map shows the selected zone.</p>
            </div>
        </div>
    </div>

    <script>
        function initMap() {
            const center = { lat: {{ $geofence->latitude }}, lng: {{ $geofence->longitude }} };
            const radius = {{ $geofence->radius }};

            const map = new google.maps.Map(document.getElementById("map"), {
                zoom: 15,
                center: center,
            });

            new google.maps.Circle({
                strokeColor: "#FF0000",
                strokeOpacity: 0.8,
                strokeWeight: 2,
                fillColor: "#FF0000",
                fillOpacity: 0.35,
                map,
                center: center,
                radius: radius,
            });
        }
    </script>
    <script
        src="https://maps.googleapis.com/maps/api/js?key={{ env('GOOGLE_MAPS_API_KEY') }}&callback=initMap&libraries=geometry"
        async defer></script>
</x-app-layout>