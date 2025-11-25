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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                Map<String, Integer> userBookCount = new HashMap<>();
                for (PostCard post : postCards) {
                    String userId = post.getUserId();
                    userBookCount.put(userId, userBookCount.getOrDefault(userId, 0) + 1);
                }

                List<Map.Entry<String, Integer>> top3 = userBookCount.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .limit(3)
                        .collect(Collectors.toList());

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
                // Get likes count for each book from Firebase
                getAllBooksLikeCountFromUsers(new IApiCallback<Map<String, Integer>>() {
                    @Override
                    public void onSuccess(Map<String, Integer> bookLikeCount) {
                        // Sort books by like count (highest first) and get top 7
                        List<PostCard> topLikedBooks = new ArrayList<>();

                        // Create a copy to avoid stream issues
                        List<PostCard> allBooks = new ArrayList<>(postCards);

                        for (PostCard book : allBooks) {
                            if (book.getPostId() != null) {
                                int likes = bookLikeCount.getOrDefault(book.getPostId(), 0);
                                Log.d("PostCardServiceHelper", "Book: " + book.getTitle() + " - Likes: " + likes);
                            }
                        }

                        // Sort manually to avoid stream issues on older Android versions
                        allBooks.sort((p1, p2) -> {
                            int likes1 = bookLikeCount.getOrDefault(p1.getPostId(), 0);
                            int likes2 = bookLikeCount.getOrDefault(p2.getPostId(), 0);
                            return Integer.compare(likes2, likes1); // Descending order
                        });

                        // Get top 7
                        for (int i = 0; i < Math.min(7, allBooks.size()); i++) {
                            topLikedBooks.add(allBooks.get(i));
                        }

                        // If we have less than 7 liked books, add some from the remaining books
                        if (topLikedBooks.size() < 7) {
                            for (PostCard book : allBooks) {
                                if (!topLikedBooks.contains(book) && topLikedBooks.size() < 7) {
                                    topLikedBooks.add(book);
                                }
                            }
                        }

                        // Log the results
                        for (PostCard post : topLikedBooks) {
                            int likes = bookLikeCount.getOrDefault(post.getPostId(), 0);
                            Log.d("PostCardServiceHelper", "Top Book - PostId: " + post.getPostId() +
                                    ", Likes: " + likes + ", Title: " + post.getTitle());
                        }

                        callback.onSuccess(topLikedBooks);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e("PostCardServiceHelper", "Error getting book likes: " + errorMessage);
                        // Fallback: return first 7 books if we can't get likes
                        List<PostCard> top7 = new ArrayList<>();
                        for (int i = 0; i < Math.min(7, postCards.size()); i++) {
                            top7.add(postCards.get(i));
                        }
                        callback.onSuccess(top7);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * Get all books like count from Firebase
     */
    private void getAllBooksLikeCount(final IApiCallback<Map<String, Integer>> callback) {
        DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference("bookLikes");

        likesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Integer> bookLikeCount = new HashMap<>();

                for (DataSnapshot bookSnapshot : dataSnapshot.getChildren()) {
                    String bookId = bookSnapshot.getKey();
                    int likeCount = 0;

                    // Count the number of users who liked this book
                    if (bookSnapshot.exists()) {
                        likeCount = (int) bookSnapshot.getChildrenCount();
                    }

                    bookLikeCount.put(bookId, likeCount);
                    Log.d("PostCardServiceHelper", "Book " + bookId + " has " + likeCount + " likes");
                }

                callback.onSuccess(bookLikeCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
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

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    DataSnapshot likedBooksSnapshot = userSnapshot.child("likedBooks");

                    if (likedBooksSnapshot.exists()) {
                        for (DataSnapshot bookSnapshot : likedBooksSnapshot.getChildren()) {
                            String bookId = bookSnapshot.getKey();
                            if (bookId != null) {
                                bookLikeCount.put(bookId, bookLikeCount.getOrDefault(bookId, 0) + 1);
                            }
                        }
                    }
                }

                Log.d("PostCardServiceHelper", "Found " + bookLikeCount.size() + " books with likes");
                callback.onSuccess(bookLikeCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("PostCardServiceHelper", "Error getting user likes: " + databaseError.getMessage());
                callback.onError(databaseError.getMessage());
            }
        });
    }

    /**
     * Get top 7 books of a specific genre
     */
    public void getTop7BooksByGenre(String genre, final IApiCallback<List<PostCard>> callback) {
        repository.getAllPosts("books", new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> postCards) {
                List<PostCard> filtered = new ArrayList<>();
                for (PostCard post : postCards) {
                    if (genre != null && genre.equalsIgnoreCase(post.getGenre())) {
                        filtered.add(post);
                        if (filtered.size() >= 7) {
                            break;
                        }
                    }
                }

                for (PostCard post : filtered) {
                    Log.d("PostCardServiceHelper", "Genre Book - PostId: " + post.getPostId() + ", Genre: " + post.getGenre() + ", Price: " + post.getPrice());
                }

                callback.onSuccess(filtered);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }
}