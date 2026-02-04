<?php

namespace App\Http\Controllers;

use App\Models\Post;
use App\Services\SentimentService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;

class ForumController extends Controller
{
    protected $sentimentService;

    public function __construct(SentimentService $sentimentService)
    {
        $this->sentimentService = $sentimentService;
    }

    public function index(Request $request)
    {
        $query = Post::with('user')->withCount('comments')->latest();

        if ($request->has('tag')) {
            $query->whereJsonContains('tags', $request->tag);
        }

        if ($request->has('category')) {
            $query->where('category', $request->category);
        }

        $posts = $query->simplePaginate(10);
        return view('forum.index', compact('posts'));
    }

    public function create()
    {
        return view('forum.create');
    }

    public function store(Request $request)
    {
        $request->validate([
            'title' => 'required|string|max:255',
            'body' => 'required|string|min:10',
        ]);

        // Analyze Sentiment & NLP
        $analysis = $this->sentimentService->analyze($request->body);
        $tags = $this->sentimentService->analyzeEntities($request->body);
        $category = $this->sentimentService->classifyContent($request->body);

        $post = Post::create([
            'user_id' => Auth::id(),
            'title' => $request->title,
            'body' => $request->body,
            'sentiment_score' => $analysis['score'],
            'sentiment_magnitude' => $analysis['magnitude'],
            'risk_level' => $analysis['risk_level'],
            'tags' => $tags,
            'category' => $category,
        ]);

        if ($analysis['risk_level'] === 'high') {
            return redirect()->route('forum.index')
                ->with('status', 'Post created.')
                ->with('warning', 'We noticed you seem to be going through a tough time. Remember, this community supports you. Consider reaching out to a helpline if you need immediate assistance.');
        }

        return redirect()->route('forum.index')->with('status', 'Post created successfully!');
    }

    public function show(Post $post)
    {
        $post->load('user');
        return view('forum.show', compact('post'));
    }

    public function edit(Post $post)
    {
        if ($post->user_id !== Auth::id()) {
            abort(403);
        }
        return view('forum.edit', compact('post'));
    }

    public function update(Request $request, Post $post)
    {
        if ($post->user_id !== Auth::id()) {
            abort(403);
        }

        $request->validate([
            'title' => 'required|string|max:255',
            'body' => 'required|string|min:10',
        ]);

        // Analyze Sentiment again if body changed?
        // Ideally yes, but for now let's just update the content to keep it simple unless requested otherwise.
        // However, updating content might change risk level. Let's re-analyze to be safe.

        $analysis = $this->sentimentService->analyze($request->body);
        $tags = $this->sentimentService->analyzeEntities($request->body);
        $category = $this->sentimentService->classifyContent($request->body);

        $post->update([
            'title' => $request->title,
            'body' => $request->body,
            'sentiment_score' => $analysis['score'],
            'sentiment_magnitude' => $analysis['magnitude'],
            'risk_level' => $analysis['risk_level'],
            'tags' => $tags,
            'category' => $category,
        ]);

        if ($analysis['risk_level'] === 'high') {
            return redirect()->route('forum.show', $post)
                ->with('status', 'Post updated.')
                ->with('warning', 'We noticed you seem to be going through a tough time. Remember, this community supports you.');
        }

        return redirect()->route('forum.show', $post)->with('status', 'Post updated successfully!');
    }

    public function destroy(Post $post)
    {
        // Allow deletion if user is owner OR user is admin
        if ($post->user_id !== Auth::id() && !Auth::user()->is_admin) {
            abort(403);
        }

        $post->delete();

        return redirect()->route('forum.index')->with('status', 'Post deleted successfully!');
    }

    public function toggleLock(Post $post)
    {
        if (!Auth::user()->is_admin) {
            abort(403);
        }

        $post->update(['is_locked' => !$post->is_locked]);

        $status = $post->is_locked ? 'Post locked.' : 'Post unlocked.';

        return back()->with('status', $status);
    }
}
