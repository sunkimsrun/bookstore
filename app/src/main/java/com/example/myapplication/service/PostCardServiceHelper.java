package com.example.myapplication.service;

import android.util.Log;

import com.example.myapplication.model.PostCard;
import com.example.myapplication.repository.IApiCallback;
import com.example.myapplication.repository.PostCardRepository;

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
     * Get top 3 books with the highest price
     */
    public void getTop3MostExpensiveBooks(final IApiCallback<List<PostCard>> callback) {
        repository.getAllPosts("books", new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> postCards) {
                List<PostCard> top3 = postCards.stream()
                        .filter(post -> post.getPrice() != null && !post.getPrice().isEmpty())
                        .sorted((p1, p2) -> {
                            try {
                                double price1 = Double.parseDouble(p1.getPrice());
                                double price2 = Double.parseDouble(p2.getPrice());
                                return Double.compare(price2, price1);
                            } catch (NumberFormatException e) {
                                return 0;
                            }
                        })
                        .limit(3)
                        .collect(Collectors.toList());

                for (PostCard post : top3) {
                    Log.d("PostCardServiceHelper", "PostId: " + post.getPostId() + ", Price: " + post.getPrice() + ", Genre: " + post.getGenre());
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
     * Get top 3 books of a specific genre
     */
    public void getTop3BooksByGenre(String genre, final IApiCallback<List<PostCard>> callback) {
        repository.getAllPosts("books", new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> postCards) {
                List<PostCard> filtered = postCards.stream()
                        .filter(post -> genre.equalsIgnoreCase(post.getGenre()))
                        .limit(3)
                        .collect(Collectors.toList());

                for (PostCard post : filtered) {
                    Log.d("PostCardServiceHelper", "PostId: " + post.getPostId() + ", Genre: " + post.getGenre() + ", Price: " + post.getPrice());
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
