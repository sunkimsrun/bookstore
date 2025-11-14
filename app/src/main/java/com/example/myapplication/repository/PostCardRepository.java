package com.example.myapplication.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.model.PostCard;
import com.example.myapplication.service.PostCardService;
import com.example.myapplication.util.RetrofitClient;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostCardRepository {

    private final PostCardService postCardService;

    public PostCardRepository() {
        postCardService = RetrofitClient.getClient().create(PostCardService.class);
    }

    public void getAllPosts(String itemType, final IApiCallback<List<PostCard>> callback) {
        Log.d("PostCardRepository", "Getting all posts for: " + itemType);

        // Remove the orderBy parameter from the URL since it's causing the 400 error
        // We'll use the basic endpoint without ordering
        postCardService.getCards(itemType).enqueue(new Callback<Map<String, PostCard>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, PostCard>> call, @NonNull Response<Map<String, PostCard>> response) {
                Log.d("PostCardRepository", "Response code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    List<PostCard> posts = new ArrayList<>(response.body().values());
                    Log.d("PostCardRepository", "Successfully loaded " + posts.size() + " posts");

                    // Sort locally by date (newest first)
                    sortPostsByDateDescending(posts);

                    callback.onSuccess(posts);
                } else {
                    String errorMsg = getErrorMessage(response);
                    Log.e("PostCardRepository", "Error in getAllPosts: " + errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, PostCard>> call, @NonNull Throwable t) {
                Log.e("PostCardRepository", "getAllPosts failure", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getPost(String itemType, String postId, final IApiCallback<PostCard> callback) {
        postCardService.getCard(itemType, postId).enqueue(new Callback<PostCard>() {
            @Override
            public void onResponse(@NonNull Call<PostCard> call, @NonNull Response<PostCard> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<PostCard> call, @NonNull Throwable t) {
                Log.e("PostCardRepository", "getPost failure", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getLatestPosts(String itemType, String orderBy, int limit, final IApiCallback<List<PostCard>> callback) {
        Log.d("PostCardRepository", "Getting latest posts: " + itemType);

        // Use getAllPosts and then take the first 'limit' items
        getAllPosts(itemType, new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> allPosts) {
                List<PostCard> limitedPosts = allPosts.subList(0, Math.min(limit, allPosts.size()));
                Log.d("PostCardRepository", "Returning " + limitedPosts.size() + " latest posts");
                callback.onSuccess(limitedPosts);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void getCardsByDates(String itemType, String orderBy, String startDate, String endDate, final IApiCallback<List<PostCard>> callback) {
        // For now, use getAllPosts and filter locally to avoid index issues
        getAllPosts(itemType, new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> allPosts) {
                // Filter by date range locally
                List<PostCard> filteredPosts = filterPostsByDateRange(allPosts, startDate, endDate);
                callback.onSuccess(filteredPosts);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void getBooksByGenre(String genre, final IApiCallback<List<PostCard>> callback) {
        String itemType = "books";
        Log.d("PostCardRepository", "Fetching books for genre: " + genre);

        // Get all books and filter by genre locally
        getAllPosts(itemType, new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> allBooks) {
                List<PostCard> filteredBooks = new ArrayList<>();

                for (PostCard book : allBooks) {
                    if (book.getGenre() != null && book.getGenre().equalsIgnoreCase(genre)) {
                        filteredBooks.add(book);
                    }
                }

                Log.d("PostCardRepository", "Filtered " + filteredBooks.size() + " books for genre: " + genre);
                callback.onSuccess(filteredBooks);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void createPost(String itemType, String postId, PostCard postCard, final IApiCallback<PostCard> callback) {
        postCardService.createCard(itemType, postId, postCard).enqueue(new Callback<PostCard>() {
            @Override
            public void onResponse(@NonNull Call<PostCard> call, @NonNull Response<PostCard> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<PostCard> call, @NonNull Throwable t) {
                Log.e("PostCardRepository", "createPost failure", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void deletePost(String itemType, String postId, final IApiCallback<String> callback) {
        postCardService.deleteCard(itemType, postId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess("Post deleted successfully");
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e("PostCardRepository", "deletePost failure", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    private void sortPostsByDateDescending(List<PostCard> posts) {
        posts.sort((p1, p2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date d1 = sdf.parse(p1.getCreatedDate());
                Date d2 = sdf.parse(p2.getCreatedDate());
                return d2.compareTo(d1); // Descending order (newest first)
            } catch (ParseException e) {
                Log.e("PostCardRepository", "Date parsing error", e);
                return 0;
            }
        });
    }

    private List<PostCard> filterPostsByDateRange(List<PostCard> posts, String startDate, String endDate) {
        List<PostCard> filtered = new ArrayList<>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);

            for (PostCard post : posts) {
                Date postDate = sdf.parse(post.getDate());
                if (!postDate.before(start) && !postDate.after(end)) {
                    filtered.add(post);
                }
            }
        } catch (ParseException e) {
            Log.e("PostCardRepository", "Date parsing error in filter", e);
        }
        return filtered;
    }

    private String getErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                return "Error: " + response.code() + " - " + response.errorBody().string();
            }
        } catch (IOException e) {
            return "Error: " + response.code() + " - (error parsing body)";
        }
        return "Error: " + response.code() + " - " + response.message();
    }
}