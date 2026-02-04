<x-app-layout>

    <div class="py-12">
        <div class="max-w-7xl mx-auto sm:px-6 lg:px-8">
            <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg p-6">

                <form action="{{ route('geofences.update', $geofence) }}" method="POST">
                    @csrf
                    @method('PUT')

                    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <!-- Form Inputs -->
                        <div class="md:col-span-1 space-y-4">
                            <div>
                                <label class="block text-sm font-medium text-gray-700">Name</label>
                                <input type="text" name="name" value="{{ $geofence->name }}" required
                                    class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm">
                            </div>

                            <div>
                                <label class="block text-sm font-medium text-gray-700">Radius (meters)</label>
                                <input type="number" id="radiusInput" name="radius" value="{{ $geofence->radius }}"
                                    min="10" max="5000"
                                    class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm">
                            </div>

                            <input type="hidden" name="latitude" id="lat" value="{{ $geofence->latitude }}">
                            <input type="hidden" name="longitude" id="lng" value="{{ $geofence->longitude }}">

                            <div class="pt-4 flex items-center justify-between">
                                <a href="{{ route('geofences.index') }}">
                                    <x-secondary-button type="button">Cancel</x-secondary-button>
                                </a>
                                <button type="submit"
                                    class="bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-2 px-4 rounded shadow transition">
                                    Update Zone
                                </button>
                            </div>
                            <p class="text-xs text-gray-500 mt-2">Drag the red circle on the map to move the zone.</p>
                        </div>

                        <!-- Map -->
                        <div class="md:col-span-2">
                            <div id="map" class="w-full h-[500px] rounded-lg bg-gray-200"></div>
                        </div>
                    </div>
                </form>

            </div>
        </div>
    </div>

    <script>
        let map;
        let circle;

        function initMap() {
            const center = { lat: {{ $geofence->latitude }}, lng: {{ $geofence->longitude }} };
            const radius = {{ $geofence->radius }};

            map = new google.maps.Map(document.getElementById("map"), {
                zoom: 15,
                center: center,
            });

            // Draggable Circle
            circle = new google.maps.Circle({
                strokeColor: "#FF0000",
                strokeOpacity: 0.8,
                strokeWeight: 2,
                fillColor: "#FF0000",
                fillOpacity: 0.35,
                map,
                center: center,
                radius: radius,
                draggable: true,
                editable: true
            });

            // Listeners for radius/center change
            circle.addListener('center_changed', updateForm);
            circle.addListener('radius_changed', updateForm);

            // Sync Input -> Map
            document.getElementById('radiusInput').addEventListener('input', function (e) {
                const val = parseInt(e.target.value);
                if (val && val > 0) circle.setRadius(val);
            });
        }

        function updateForm() {
            const center = circle.getCenter();
            document.getElementById('lat').value = center.lat();
            document.getElementById('lng').value = center.lng();
            document.getElementById('radiusInput').value = Math.round(circle.getRadius());
        }
    </script>
    <script
        src="https://maps.googleapis.com/maps/api/js?key={{ env('GOOGLE_MAPS_API_KEY') }}&callback=initMap&libraries=geometry"
        async defer></script>
</x-app-layout>