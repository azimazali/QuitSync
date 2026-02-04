<x-app-layout>

    <div class="py-12">
        <div class="max-w-7xl mx-auto sm:px-6 lg:px-8">

            @if (session('status'))
                <div class="bg-emerald-100 border border-emerald-400 text-emerald-700 px-4 py-3 rounded relative mb-6"
                    role="alert">
                    <span class="block sm:inline">{{ session('status') }}</span>
                </div>
            @endif

            @if (session('warning'))
                <div class="bg-red-100 border-l-4 border-red-500 text-red-700 p-4 mb-6" role="alert">
                    <p class="font-bold">Support Alert</p>
                    <p>{{ session('warning') }}</p>
                </div>
            @endif

            <div class="flex justify-between items-center mb-6">
                <h2 class="text-2xl font-bold text-gray-800">Community Forum</h2>
                <a href="{{ route('forum.create') }}" class="bg-emerald-600 hover:bg-emerald-700 text-white font-bold py-2 px-4 rounded shadow transition flex items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
                    </svg>
                    New Post
                </a>
            </div>

            <div class="space-y-6">
                @forelse ($posts as $post)
                    <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg hover:shadow-md transition">
                        <div class="p-6">
                            <div class="flex justify-between items-start">
                                <a href="{{ route('forum.show', $post) }}" class="group">
                                    <h3 class="text-xl font-bold text-gray-900 group-hover:text-emerald-600 transition">
                                        {{ $post->title }}</h3>
                                    <p class="text-sm text-gray-500 mt-1">Posted by {{ $post->user->name }} •
                                        {{ $post->created_at->diffForHumans() }}</p>
                                </a>
                                <div class="flex items-center gap-2">
                                    <span class="px-2 py-1 text-xs font-semibold rounded-full 
                                            @if($post->risk_level === 'high') bg-red-100 text-red-800
                                            @elseif($post->risk_level === 'moderate') bg-yellow-100 text-yellow-800
                                            @else bg-green-100 text-green-800 @endif">
                                        {{ ucfirst($post->risk_level) }} Risk
                                    </span>
                                    
                                    @if($post->is_locked)
                                        <span class="px-2 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-800 border border-gray-200">
                                            🔒 Locked
                                        </span>
                                    @endif

                                    {{-- Category Badge --}}
                                    @if($post->category)
                                        <span class="px-2 py-1 text-xs font-semibold rounded-full bg-indigo-50 text-indigo-700 border border-indigo-100" title="Auto-Categorized">
                                            📂 {{ str_replace('/', '', strrchr($post->category, '/') ?: $post->category) }}
                                        </span>
                                    @endif

                                    {{-- Entity Tags --}}
                                    @if(!empty($post->tags))
                                        <div class="hidden sm:flex -space-x-1 overflow-hidden">
                                            @foreach($post->tags as $tag)
                                                <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800 border border-gray-200" style="margin-right: 4px;">
                                                    #{{ $tag }}
                                                </span>
                                            @endforeach
                                        </div>
                                    @endif

                                    @if ($post->user_id === Auth::id() || Auth::user()->is_admin)
                                        <div class="flex items-center space-x-2 ml-2">
                                            
                                            {{-- Lock/Unlock --}}
                                            @if(Auth::user()->is_admin)
                                                <form action="{{ route('forum.lock', $post) }}" method="POST">
                                                    @csrf
                                                    <button type="submit" class="text-gray-400 hover:text-yellow-600 transition" title="{{ $post->is_locked ? 'Unlock' : 'Lock' }}">
                                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="{{ $post->is_locked ? 'M8 11V7a4 4 0 118 0m-4 8v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2z' : 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z' }}" />
                                                        </svg>
                                                    </button>
                                                </form>
                                            @endif

                                            {{-- Edit (Only Owner) --}}
                                            @if($post->user_id === Auth::id())
                                                <a href="{{ route('forum.edit', $post) }}" class="text-gray-400 hover:text-blue-600 transition">
                                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                                    </svg>
                                                </a>
                                            @endif

                                            {{-- Delete (Owner or Admin) --}}
                                            <form action="{{ route('forum.destroy', $post) }}" method="POST" onsubmit="return confirm('Are you sure you want to delete this post?');">
                                                @csrf
                                                @method('DELETE')
                                                <button type="submit" class="text-gray-400 hover:text-red-600 transition">
                                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                    </svg>
                                                </button>
                                            </form>
                                        </div>
                                    @endif
                                </div>
                            </div>
                            <p class="mt-4 text-gray-600 line-clamp-3">
                                {{ Str::limit($post->body, 200) }}
                            </p>
                            <div class="mt-4 flex items-center justify-between">
                                <a href="{{ route('forum.show', $post) }}"
                                    class="text-emerald-600 hover:text-emerald-800 text-sm font-medium">Read more &rarr;</a>
                                <span class="text-xs text-gray-500 flex items-center gap-1">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                                    </svg>
                                    {{ $post->comments_count }} {{ Str::plural('Comment', $post->comments_count) }}
                                </span>
                            </div>
                        </div>
                    </div>
                @empty
                    <div class="text-center py-10 bg-white rounded-lg shadow-sm">
                        <p class="text-gray-500 text-lg">No posts yet. Be the first to share!</p>
                    </div>
                @endforelse

                <div class="mt-4">
                    {{ $posts->links() }}
                </div>
            </div>
        </div>
    </div>
</x-app-layout>