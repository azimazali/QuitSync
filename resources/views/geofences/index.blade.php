<x-app-layout>
    <!-- Header removed -->

    <div class="py-12">
        <div class="max-w-7xl mx-auto sm:px-6 lg:px-8 space-y-6">

            <!-- Warning Alert (Hidden by default, shown via JS) -->
            <div id="geofenceAlert" class="hidden bg-red-100 border-l-4 border-red-500 text-red-700 p-4 mb-4"
                role="alert">
                <p class="font-bold">Warning: High Risk Zone</p>
                <p>You have entered <span id="zoneName" class="font-bold"></span>. Distract yourself, go take a walk!
                </p>
            </div>

            <!-- Map Section -->
            <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg p-6">
                <h3 class="text-lg font-medium text-gray-900 mb-4">Map & Zones</h3>
                <div id="map" class="w-full h-[500px] rounded-lg bg-gray-200"></div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <!-- Add Geofence Form -->
                <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg p-6">
                    <h3 class="text-lg font-medium text-gray-900 mb-2">Add Geofence</h3>
                    <p class="text-xs text-gray-500 mb-4">Click map to set location, then add details.</p>
                    <form action="{{ route('geofences.store') }}" method="POST">
                        @csrf
                        <input type="hidden" name="latitude" id="geo_lat">
                        <input type="hidden" name="longitude" id="geo_lng">

                        <div class="mb-3">
                            <label class="block text-sm font-medium text-gray-700">Name</label>
                            <input type="text" name="name" required
                                class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm"
                                placeholder="e.g. Bar">
                        </div>

                        <div class="mb-3">
                            <label class="block text-sm font-medium text-gray-700">Radius (meters)</label>
                            <input type="number" name="radius" value="100" min="10" max="5000"
                                class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm">
                        </div>

                        <div id="geo_coords_display" class="text-xs text-gray-400 mb-3">No location selected</div>

                        <button type="submit"
                            class="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-2 px-4 rounded shadow transition text-sm">
                            Create Zone
                        </button>
                    </form>
                </div>

                <!-- Active Geofences List -->
                <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg p-6">
                    <h3 class="text-lg font-medium text-gray-900 mb-4">Active Zones</h3>
                    <ul class="divide-y divide-gray-200">
                        @forelse ($geofences as $fence)
                            <li class="py-3 flex justify-between items-center group">
                                <div class="flex items-center gap-2">
                                    <div>
                                        <div class="text-sm font-medium text-gray-900">
                                            {{ str_replace(' [Auto]', '', $fence->name) }}
                                            @if(!empty($fence->is_recommended))
                                                <span
                                                    class="ml-2 text-xs font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded border border-indigo-200">[Recommendation]</span>
                                            @endif
                                            @if($fence->is_auto)
                                                <span
                                                    class="ml-2 text-xs font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded border border-blue-200">[Auto]</span>
                                            @endif
                                        </div>
                                        <div class="text-xs text-gray-500">{{ $fence->radius }}m radius</div>
                                    </div>
                                </div>

                                @if(empty($fence->is_recommended))
                                    <div class="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition">
                                        <a href="{{ route('geofences.show', $fence) }}"
                                            class="text-blue-400 hover:text-blue-600" title="View">
                                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                                    d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z">
                                                </path>
                                            </svg>
                                        </a>
                                        <a href="{{ route('geofences.edit', $fence) }}"
                                            class="text-green-400 hover:text-green-600" title="Edit">
                                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                                    d="M11 5H6a2 2 0 00-2 2v11a2 2 0 00 2 2h11a2 2 0 00 2-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z">
                                                </path>
                                            </svg>
                                        </a>
                                        <form action="{{ route('geofences.destroy', $fence) }}" method="POST"
                                            onsubmit="return confirm('Delete this zone?');">
                                            @csrf @method('DELETE')
                                            <button type="submit" class="text-red-400 hover:text-red-600" title="Delete">
                                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16">
                                                    </path>
                                                </svg>
                                            </button>
                                        </form>
                                    </div>
                                @else
                                    <div class="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition">
                                        <form action="{{ route('geofences.recommendations.dismiss') }}" method="POST"
                                            onsubmit="return confirm('Dismiss this recommendation?');">
                                            @csrf
                                            <input type="hidden" name="latitude" value="{{ $fence->latitude }}">
                                            <input type="hidden" name="longitude" value="{{ $fence->longitude }}">
                                            <button type="submit"
                                                class="text-gray-400 hover:text-gray-600 text-xs font-medium border border-gray-300 rounded px-2 py-1"
                                                title="Dismiss">
                                                Dismiss
                                            </button>
                                        </form>
                                    </div>
                                @endif
                            </li>
                        @empty
                            <li class="text-sm text-gray-500">No active zones.</li>
                        @endforelse
                    </ul>
                </div>
            </div>
        </div>
    </div>

    <script>
        window.map = null;
        let geocoder;
        window.userPosition = null;
        let userMarker;
        let tempMarker;
        const geofences = @json($geofences);

        let infoWindow;

        function initMap() {
            geocoder = new google.maps.Geocoder();
            infoWindow = new google.maps.InfoWindow();
            const defaultPos = { lat: 40.7128, lng: -74.0060 };

            window.map = new google.maps.Map(document.getElementById("map"), {
                zoom: 12,
                center: defaultPos,
                mapTypeControl: false,
                streetViewControl: false
            });

            // Locate User
            if (navigator.geolocation) {
                navigator.geolocation.watchPosition(
                    (position) => {
                        const pos = {
                            lat: position.coords.latitude,
                            lng: position.coords.longitude,
                        };
                        userPosition = pos;

                        if (!userMarker) {
                            userMarker = new google.maps.Marker({
                                position: pos,
                                map: map,
                                title: "You are here",
                                icon: {
                                    path: google.maps.SymbolPath.CIRCLE,
                                    scale: 8,
                                    fillColor: "#4285F4",
                                    fillOpacity: 1,
                                    strokeColor: "white",
                                    strokeWeight: 2,
                                }
                            });
                            map.setCenter(pos);

                            // Re-open info window on click
                            userMarker.addListener("click", () => {
                                updateUserAddress(pos);
                            });
                        } else {
                            userMarker.setPosition(pos);
                        }

                        checkGeofences(pos);
                        updateUserAddress(pos);
                    },
                    (error) => {
                        console.error("Geolocation error:", error);
                        handleLocationError(true, map.getCenter());
                    }
                );
            } else {
                handleLocationError(false, map.getCenter());
            }

            function updateUserAddress(pos) {
                if (!geocoder) return;
                geocoder.geocode({ location: pos }, (results, status) => {
                    let content = "Location found (Address unknown)";
                    if (status === "OK" && results[0]) {
                        content = results[0].formatted_address;
                    } else {
                        console.error("Geocoder failed: " + status);
                    }

                    if (infoWindow && userMarker) {
                        infoWindow.setContent(`<div style="color:black; font-weight:bold; min-width:150px">${content}</div>`);
                        infoWindow.open(map, userMarker);
                    }
                });
            }

            // Render Geofences
            geofences.forEach(fence => {
                const center = { lat: parseFloat(fence.latitude), lng: parseFloat(fence.longitude) };

                // Circle
                new google.maps.Circle({
                    strokeColor: "#FF0000",
                    strokeOpacity: 0.8,
                    strokeWeight: 2,
                    fillColor: "#FF0000",
                    fillOpacity: 0.20,
                    map,
                    center: center,
                    radius: parseInt(fence.radius),
                    clickable: false
                });

                // Marker text (optional, simpler to just hover)
                new google.maps.Marker({
                    position: center,
                    map: map,
                    label: { text: fence.name, color: "black", fontSize: "10px", fontWeight: "bold" },
                    icon: { path: google.maps.SymbolPath.CIRCLE, scale: 0 } // invisible marker just for label
                });
            });

            // Map Click listener for new Geofence
            map.addListener("click", (e) => {
                placeGeofenceMarker(e.latLng);
            });
        }

        function placeGeofenceMarker(latLng) {
            if (tempMarker) tempMarker.setMap(null);

            tempMarker = new google.maps.Marker({
                position: latLng,
                map: map,
                title: "New Zone Location"
            });

            document.getElementById('geo_lat').value = latLng.lat();
            document.getElementById('geo_lng').value = latLng.lng();
            document.getElementById('geo_coords_display').innerText = `Selected: ${latLng.lat().toFixed(4)}, ${latLng.lng().toFixed(4)}`;
        }

        let lastTriggerTime = 0;
        let exitDebounceTimer = null;
        const MIN_TRIGGER_INTERVAL = 5000; // 5 seconds between fresh alerts
        const EXIT_DEBOUNCE_DELAY = 10000; // 10 seconds delay before hiding alert

        function checkGeofences(pos) {
            const userLatLng = new google.maps.LatLng(pos.lat, pos.lng);
            let inZone = false;
            let zoneName = "";

            geofences.forEach(fence => {
                const fenceLatLng = new google.maps.LatLng(parseFloat(fence.latitude), parseFloat(fence.longitude));
                const distance = google.maps.geometry.spherical.computeDistanceBetween(userLatLng, fenceLatLng);

                if (distance <= fence.radius) {
                    inZone = true;
                    zoneName = fence.name;
                }
            });

            const alertBox = document.getElementById('geofenceAlert');
            const now = Date.now();

            if (inZone) {
                // User is inside a zone
                if (exitDebounceTimer) {
                    clearTimeout(exitDebounceTimer);
                    exitDebounceTimer = null;
                }

                // Check for debounce interval
                if (now - lastTriggerTime > MIN_TRIGGER_INTERVAL || alertBox.classList.contains('hidden')) {
                    document.getElementById('zoneName').innerText = zoneName;
                    alertBox.classList.remove('hidden');
                    lastTriggerTime = now;
                }
            } else {
                // User is outside
                // Only start exit timer if alert is currently visible and timer isn't already running
                if (!alertBox.classList.contains('hidden') && !exitDebounceTimer) {
                    exitDebounceTimer = setTimeout(() => {
                        alertBox.classList.add('hidden');
                        exitDebounceTimer = null;
                    }, EXIT_DEBOUNCE_DELAY);
                }
            }
        }

        function handleLocationError(browserHasGeolocation, pos) {
            console.log("Geolocation service failed.");
        }
    </script>

    <!-- Google Maps Script (Loaded last to ensure initMap is defined) -->
    <script
        src="https://maps.googleapis.com/maps/api/js?key={{ config('services.google.maps_key') }}&callback=initMap&libraries=geometry,marker"
        async defer></script>
</x-app-layout>