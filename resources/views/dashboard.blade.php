<x-app-layout>
    <!-- Header removed -->

    <div class="py-12" x-data="dashboardLogger">
        <div class="max-w-7xl mx-auto sm:px-6 lg:px-8 space-y-6">

            <!-- Quick Actions -->
            <div class="grid grid-cols-2 gap-4 mb-6">
                <button @click="openLogModal('smoked')"
                    class="bg-red-500 hover:bg-red-600 text-white font-bold py-4 px-6 rounded-lg shadow-lg flex items-center justify-center transform transition hover:scale-105">
                    <svg class="w-8 h-8 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z">
                        </path>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"></path>
                    </svg>
                    <span class="text-xl">I Smoked</span>
                </button>
                <button @click="openLogModal('resisted')"
                    class="bg-emerald-500 hover:bg-emerald-600 text-white font-bold py-4 px-6 rounded-lg shadow-lg flex items-center justify-center transform transition hover:scale-105">
                    <svg class="w-8 h-8 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                    <span class="text-xl">I Resisted</span>
                </button>
            </div>

            <!-- Message Notification -->
            @if (session('status'))
                <div class="bg-emerald-100 border border-emerald-400 text-emerald-700 px-4 py-3 rounded relative"
                    role="alert">
                    <span class="block sm:inline">{{ session('status') }}</span>
                </div>
            @endif

            <!-- Top Row: Status & Savings -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <!-- Daily Status -->
                <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg p-6">
                    <h3 class="text-lg font-medium text-gray-900 mb-4">Today's Status</h3>
                    @if ($smokedToday)
                        <div class="flex flex-col items-center text-center text-amber-600 bg-amber-50 p-4 rounded-lg">
                            <svg class="w-12 h-12 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z">
                                </path>
                            </svg>
                            <strong class="block text-lg">You smoked today.</strong>
                            <span class="text-sm mt-1">Don't give up!</span>
                        </div>
                    @else
                        <div class="flex flex-col items-center text-center text-emerald-600 bg-emerald-50 p-4 rounded-lg">
                            <svg class="w-12 h-12 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                            </svg>
                            <strong class="block text-lg">Clean Streak!</strong>
                            <span class="text-sm mt-1">Keep it up!</span>
                        </div>
                    @endif
                </div>

                <!-- Total Savings Display -->
                <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg p-6">
                    <h3 class="text-lg font-medium text-gray-900 mb-2">Total Savings</h3>
                    <div class="flex flex-col items-center justify-center h-full pb-6">
                        <span class="text-4xl font-bold text-emerald-600 font-mono" id="savingsDisplay">$0.00</span>
                        <span class="text-sm text-gray-400 mt-2">Real-time Est.</span>
                    </div>
                </div>
            </div>

        </div>

        <!-- Bottom Row: Monthly Progress Calendar (Separate Container for Spacing) -->
        <div class="max-w-7xl mx-auto sm:px-6 lg:px-8 mt-8">
            <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg p-6">
                <h3 class="text-lg font-medium text-gray-900 mb-4">{{ now()->format('F Y') }} Progress</h3>

                <div class="grid grid-cols-7 gap-1 text-center text-xs font-semibold text-gray-500 mb-2">
                    <div>Su</div>
                    <div>Mo</div>
                    <div>Tu</div>
                    <div>We</div>
                    <div>Th</div>
                    <div>Fr</div>
                    <div>Sa</div>
                </div>

                <div class="grid grid-cols-7 gap-1 text-center text-sm">
                    @php
                        $startDay = \Carbon\Carbon::parse($calendar[0]['date'])->dayOfWeek;
                    @endphp

                    <!-- Empty cells for days before start of month -->
                    @for ($i = 0; $i < $startDay; $i++)
                        <div class="p-2"></div>
                    @endfor

                    @foreach ($calendar as $day)
                        <div class="p-2 rounded-lg flex items-center justify-center relative
                                                                        {{ $day['status'] === 'smoked' ? 'bg-red-100 text-red-700' : '' }}
                                                                        {{ $day['status'] === 'clean' ? 'bg-emerald-100 text-emerald-700' : '' }}
                                                                        {{ $day['status'] === 'future' ? 'text-gray-300' : '' }}
                                                                        {{ $day['is_today'] ? 'ring-2 ring-indigo-500 font-bold' : '' }}
                                                                    ">
                            {{ $day['day'] }}
                        </div>
                    @endforeach
                </div>
            </div>

        </div>

        <!-- Smart Insights Row (Separate Container for Spacing) -->
        <div class="max-w-7xl mx-auto sm:px-6 lg:px-8 mt-8">
            <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg p-6">
                <h3 class="text-lg font-medium text-gray-900 mb-4 flex items-center">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-indigo-500 mr-2" viewBox="0 0 20 20"
                        fill="currentColor">
                        <path fill-rule="evenodd"
                            d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
                            clip-rule="evenodd" />
                    </svg>
                    Smart Insight: Top Topics
                </h3>

                @if(count($topTopics) > 0)
                    <div class="space-y-3">
                        @foreach($topTopics as $tag => $count)
                            <a href="{{ route('forum.index', ['tag' => $tag]) }}"
                                class="flex items-center justify-between group hover:bg-gray-50 p-2 rounded transition cursor-pointer">
                                <span
                                    class="text-gray-700 font-medium group-hover:text-indigo-600 transition">#{{ $tag }}</span>
                                <div class="flex items-center">
                                    <div class="w-32 h-2 bg-gray-100 rounded-full mr-3 overflow-hidden">
                                        {{-- Visual bar length based on max count (rough approx) --}}
                                        @php $width = min(100, ($count / max($topTopics)) * 100); @endphp
                                        <div class="h-full bg-indigo-500" style="width: {{ $width }}%"></div>
                                    </div>
                                    <span class="text-xs text-gray-400 group-hover:text-indigo-500 transition">View {{ $count }}
                                        posts &rarr;</span>
                                </div>
                            </a>
                        @endforeach
                    </div>
                @else
                    <div class="text-center py-4 text-gray-500 text-sm">
                        <p>Write more in the Forum to generate insights!</p>
                        <p class="mt-1">We analyze your posts to find what might be triggering you.</p>
                    </div>
                @endif
            </div>

            <!-- Log Modal -->
            <div x-show="showModal" style="display: none;" class="fixed inset-0 z-50 overflow-y-auto"
                aria-labelledby="modal-title" role="dialog" aria-modal="true">
                <div class="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
                    <div x-show="showModal" x-transition:enter="ease-out duration-300"
                        x-transition:enter-start="opacity-0" x-transition:enter-end="opacity-100"
                        x-transition:leave="ease-in duration-200" x-transition:leave-start="opacity-100"
                        x-transition:leave-end="opacity-0"
                        class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" @click="showModal = false"
                        aria-hidden="true"></div>

                    <span class="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>

                    <div x-show="showModal" x-transition:enter="ease-out duration-300"
                        x-transition:enter-start="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
                        x-transition:enter-end="opacity-100 translate-y-0 sm:scale-100"
                        x-transition:leave="ease-in duration-200"
                        x-transition:leave-start="opacity-100 translate-y-0 sm:scale-100"
                        x-transition:leave-end="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
                        class="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg w-full">
                        <form action="{{ route('smoking-log.store') }}" method="POST" id="dashboardLogForm">
                            @csrf
                            <input type="hidden" name="latitude" x-model="lat">
                            <input type="hidden" name="longitude" x-model="lng">
                            <input type="hidden" name="address" x-model="address">
                            <input type="hidden" name="type" x-model="logType">

                            <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                                <div class="sm:flex sm:items-start">
                                    <div class="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left w-full">
                                        <h3 class="text-lg leading-6 font-medium text-gray-900" id="modal-title"
                                            x-text="logType === 'smoked' ? 'Log Smoking Event' : 'Log Resistance'"></h3>
                                        <div class="mt-4 space-y-4">
                                            <div x-show="logType === 'smoked'">
                                                <label class="block text-sm font-medium text-gray-700">Quantity</label>
                                                <input type="number" name="quantity" x-model="quantity" min="1"
                                                    max="100"
                                                    class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm">
                                            </div>
                                            <div>
                                                <label class="block text-sm font-medium text-gray-700">Notes
                                                    (Optional)</label>
                                                <textarea name="notes" x-model="notes" rows="3"
                                                    class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm"
                                                    placeholder="How are you feeling?"></textarea>
                                            </div>
                                            <p class="text-xs text-gray-500" x-text="locationStatus"></p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                                <button type="button" @click="submitLog()" :disabled="isSubmitting"
                                    class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 text-base font-medium text-white focus:outline-none focus:ring-2 focus:ring-offset-2 sm:ml-3 sm:w-auto sm:text-sm"
                                    :class="[
                                logType === 'smoked' ? 'bg-red-600 hover:bg-red-700 focus:ring-red-500' : 'bg-emerald-600 hover:bg-emerald-700 focus:ring-emerald-500', 
                                isSubmitting ? 'opacity-50 cursor-not-allowed' : ''
                            ]">
                                    <span x-show="!isSubmitting">Confirm Log</span>
                                    <span x-show="isSubmitting">Updating...</span>
                                </button>
                                <button type="button" @click="showModal = false"
                                    class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm">
                                    Cancel
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div> <!-- Closing div for dashboardLogger moved here -->

        <!-- Geocoder Script -->
        <script src="https://maps.googleapis.com/maps/api/js?key={{ env('GOOGLE_MAPS_API_KEY') }}&libraries=places"
            async defer></script>

        <script>


            const userSettings = {
                quitDate: "{{ Auth::user()->quit_date ? Auth::user()->quit_date->toIso8601String() : '' }}",
                cigsPerDay: {{ Auth::user()->cigarettes_per_day ?? 20 }},
                packPrice: {{ Auth::user()->pack_price ?? 10 }},
                totalPenalty: {{ $totalPenalty ?? 0 }}
        };

            // Savings Logic
            let savingsInterval;

            function startSavingsCounter() {
                if (!userSettings.quitDate) return;

                const quitTime = new Date(userSettings.quitDate).getTime();
                const pricePerCig = userSettings.packPrice / 20;
                const cigsPerSec = userSettings.cigsPerDay / 86400;
                const costPerSec = cigsPerSec * userSettings.packPrice / 20 * 20; // Simplified
                // The formula: Cost per sec = ((cigarrette per day / 20) x price per pack) / 86400 seconds.
                const costPerSecond = ((userSettings.cigsPerDay / 20) * userSettings.packPrice) / 86400;

                function update() {
                    const now = new Date().getTime();
                    const diffSeconds = (now - quitTime) / 1000;

                    if (diffSeconds > 0) {
                        const grossSavings = diffSeconds * costPerSecond;
                        // Net savings = Gross - Penalty (but not less than 0)
                        const netSavings = Math.max(0, grossSavings - userSettings.totalPenalty);

                        const el = document.getElementById('savingsDisplay');
                        if (el) el.innerText = 'RM' + netSavings.toFixed(4); // RM currency
                    }
                    requestAnimationFrame(update);
                }
                update();
            }

            startSavingsCounter();
        </script>
</x-app-layout>