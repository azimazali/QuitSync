<?php

namespace App\Http\Controllers;

use App\Models\Comment;
use App\Models\Post;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;

class CommentController extends Controller
{
    public function store(Request $request, Post $post)
    {
        if ($post->is_locked) {
            return back()->with('error', 'This post is locked. Comments are disabled.');
        }

        $request->validate([
            'body' => 'required|string|max:1000',
        ]);

        $comment = Comment::create([
            'user_id' => Auth::id(),
            'forum_post_id' => $post->id,
            'body' => $request->body,
        ]);

        return back()->with('status', 'Comment posted!');
    }

    public function destroy(Comment $comment)
    {
        if ($comment->user_id !== Auth::id()) {
            abort(403);
        }

        $comment->delete();

        return back()->with('status', 'Comment deleted.');
    }
}
