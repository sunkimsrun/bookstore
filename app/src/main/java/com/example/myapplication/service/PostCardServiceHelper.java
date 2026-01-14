package com.example.myapplication.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.model.PostCard;
import com.example.myapplication.repository.IApiCallback;
import com.example.myapplication.repository.PostCardRepository;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostCardServiceHelper {

    private final PostCardRepository repository;

    public PostCardServiceHelper(PostCardRepository repository) {
        this.repository = repository;
    }

    /**
     * Get top 3 users who posted the most books
     */
    public void getTop3UsersWithMostBooks(final IApiCallback<List<Map.Entry<String, Integer>>> callback) {
        repository.getAllPosts("books", new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> postCards) {
                if (postCards == null || postCards.isEmpty()) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                Map<String, Integer> userBookCount = new HashMap<>();
                for (PostCard post : postCards) {
                    String userId = post.getUserId();
                    if (userId != null && !userId.isEmpty()) {
                        userBookCount.put(userId, userBookCount.getOrDefault(userId, 0) + 1);
                    }
                }

                List<Map.Entry<String, Integer>> top3 = new ArrayList<>(userBookCount.entrySet());
                // Sort by value in descending order
                top3.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

                // Limit to top 3
                if (top3.size() > 3) {
                    top3 = top3.subList(0, 3);
                }

                for (Map.Entry<String, Integer> entry : top3) {
                    Log.d("PostCardServiceHelper", "UserId: " + entry.getKey() + ", Books Posted: " + entry.getValue());
                }

                callback.onSuccess(top3);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * Get top 7 most liked books based on user likes
     */
    public void getTop7MostLikedBooks(final IApiCallback<List<PostCard>> callback) {
        // First get all books
        repository.getAllPosts("books", new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> postCards) {
                if (postCards == null || postCards.isEmpty()) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                Log.d("PostCardServiceHelper", "Found " + postCards.size() + " books to check for likes");

                // Get likes count for each book from Firebase
                getAllBooksLikeCountFromUsers(new IApiCallback<Map<String, Integer>>() {
                    @Override
                    public void onSuccess(Map<String, Integer> bookLikeCountParam) {
                        // Use a final variable for the lambda
                        final Map<String, Integer> bookLikeCount = bookLikeCountParam != null ? bookLikeCountParam : new HashMap<>();

                        Log.d("PostCardServiceHelper", "Received bookLikeCount with " + bookLikeCount.size() + " entries");

                        // Sort books by like count (highest first) and get top 7
                        List<PostCard> topLikedBooks = new ArrayList<>();

                        // Create a copy to avoid modification issues
                        List<PostCard> allBooks = new ArrayList<>(postCards);

                        // Log like counts for debugging
                        for (PostCard book : allBooks) {
                            String postId = book.getPostId();
                            if (postId != null) {
                                int likes = bookLikeCount.getOrDefault(postId, 0);
                                if (likes > 0) {
                                    Log.d("PostCardServiceHelper", "Book: " + book.getTitle() + " - Likes: " + likes);
                                }
                            }
                        }

                        // Sort by likes (descending) - using a final variable for the comparator
                        final Map<String, Integer> finalLikeCount = bookLikeCount;
                        allBooks.sort((p1, p2) -> {
                            String postId1 = p1.getPostId();
                            String postId2 = p2.getPostId();

                            if (postId1 == null && postId2 == null) {
                                return 0;
                            }
                            if (postId1 == null) {
                                return 1; // Put null postId at the end
                            }
                            if (postId2 == null) {
                                return -1; // Put null postId at the end
                            }

                            int likes1 = finalLikeCount.getOrDefault(postId1, 0);
                            int likes2 = finalLikeCount.getOrDefault(postId2, 0);
                            return Integer.compare(likes2, likes1); // Descending order
                        });

                        // Get top 7
                        for (int i = 0; i < Math.min(7, allBooks.size()); i++) {
                            topLikedBooks.add(allBooks.get(i));
                        }

                        // If we have less than 7 liked books, add some from the remaining books
                        if (topLikedBooks.size() < 7 && allBooks.size() > topLikedBooks.size()) {
                            for (int i = topLikedBooks.size(); i < Math.min(7, allBooks.size()); i++) {
                                topLikedBooks.add(allBooks.get(i));
                            }
                        }

                        // Log the results
                        Log.d("PostCardServiceHelper", "Selected " + topLikedBooks.size() + " top books");
                        for (PostCard post : topLikedBooks) {
                            String postId = post.getPostId();
                            int likes = postId != null ? finalLikeCount.getOrDefault(postId, 0) : 0;
                            Log.d("PostCardServiceHelper", "Top Book - Title: " + post.getTitle() +
                                    ", Likes: " + likes + ", PostId: " + postId);
                        }

                        callback.onSuccess(topLikedBooks);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e("PostCardServiceHelper", "Error getting book likes: " + errorMessage);
                        // Fallback: return first 7 books if we can't get likes
                        List<PostCard> top7 = new ArrayList<>();
                        int limit = Math.min(7, postCards.size());
                        for (int i = 0; i < limit; i++) {
                            top7.add(postCards.get(i));
                        }
                        Log.d("PostCardServiceHelper", "Using fallback: returning " + top7.size() + " books");
                        callback.onSuccess(top7);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("PostCardServiceHelper", "Error loading books: " + errorMessage);
                callback.onSuccess(new ArrayList<>()); // Return empty list on error
            }
        });
    }

    /**
     * Alternative method to get book likes by querying user likes
     */
    private void getAllBooksLikeCountFromUsers(final IApiCallback<Map<String, Integer>> callback) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Integer> bookLikeCount = new HashMap<>();

                if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                    int userCount = 0;
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        userCount++;
                        DataSnapshot likedBooksSnapshot = userSnapshot.child("likedBooks");

                        if (likedBooksSnapshot.exists() && likedBooksSnapshot.hasChildren()) {
                            for (DataSnapshot bookSnapshot : likedBooksSnapshot.getChildren()) {
                                String bookId = bookSnapshot.getKey();
                                if (bookId != null && !bookId.isEmpty()) {
                                    bookLikeCount.put(bookId, bookLikeCount.getOrDefault(bookId, 0) + 1);
                                }
                            }
                        }
                    }
                    Log.d("PostCardServiceHelper", "Processed " + userCount + " users for likes");
                } else {
                    Log.d("PostCardServiceHelper", "No users found or empty snapshot");
                }

                Log.d("PostCardServiceHelper", "Found " + bookLikeCount.size() + " books with likes");
                callback.onSuccess(bookLikeCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("PostCardServiceHelper", "Error getting user likes: " + databaseError.getMessage());
                callback.onSuccess(new HashMap<>()); // Return empty map on error instead of calling onError
            }
        });
    }

    /**
     * Get top 7 books of a specific genre
     */
    public void getTop7BooksByGenre(String genre, final IApiCallback<List<PostCard>> callback) {
        if (genre == null || genre.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        repository.getAllPosts("books", new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> postCards) {
                if (postCards == null || postCards.isEmpty()) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                List<PostCard> filtered = new ArrayList<>();
                for (PostCard post : postCards) {
                    if (post.getGenre() != null && genre.equalsIgnoreCase(post.getGenre())) {
                        filtered.add(post);
                        if (filtered.size() >= 7) {
                            break;
                        }
                    }
                }

                Log.d("PostCardServiceHelper", "Found " + filtered.size() + " books for genre: " + genre);
                for (PostCard post : filtered) {
                    Log.d("PostCardServiceHelper", "Genre Book - Title: " + post.getTitle() +
                            ", Genre: " + post.getGenre() + ", Price: " + post.getPrice());
                }

                callback.onSuccess(filtered);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("PostCardServiceHelper", "Error loading books for genre " + genre + ": " + errorMessage);
                callback.onSuccess(new ArrayList<>());
            }
        });
    }
}