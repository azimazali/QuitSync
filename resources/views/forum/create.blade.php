<x-app-layout>
    <!-- Header removed -->

    <div class="py-12">
        <div class="max-w-3xl mx-auto sm:px-6 lg:px-8">
            <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg">
                <div class="p-6 bg-white border-b border-gray-200">
                    <form action="{{ route('forum.store') }}" method="POST">
                        @csrf
                        <div class="mb-4">
                            <label for="title" class="block text-sm font-medium text-gray-700">Title</label>
                            <input type="text" name="title" id="title"
                                class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm"
                                required>
                        </div>

                        <div class="mb-6">
                            <label for="body" class="block text-sm font-medium text-gray-700">Content</label>
                            <textarea name="body" id="body" rows="6"
                                class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm"
                                required placeholder="Share your journey..."></textarea>
                            <p class="text-xs text-gray-500 mt-2">Your post will be analyzed to help us support you
                                better.</p>
                        </div>

                        <div class="flex justify-end gap-3">
                            <a href="{{ route('forum.index') }}"
                                class="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300 transition text-sm">Cancel</a>
                            <button type="submit"
                                class="px-4 py-2 bg-emerald-600 text-white rounded-md hover:bg-emerald-700 transition text-sm font-bold">Post</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</x-app-layout>