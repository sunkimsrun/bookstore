package com.example.myapplication.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.HomeActivity;
import com.example.myapplication.R;
import com.example.myapplication.adapter.PostCardLikeAdapter;
import com.example.myapplication.databinding.FragmentPromotionScreenBinding;
import com.example.myapplication.model.PostCard;
import com.example.myapplication.repository.IApiCallback;
import com.example.myapplication.repository.PostCardRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class PromotionScreenFragment extends Fragment {

    private FragmentPromotionScreenBinding binding;
    private PostCardLikeAdapter adapter;
    private PostCardRepository postCardRepository;
    private String promotionType;
    private HomeActivity homeActivity;

    public PromotionScreenFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPromotionScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof HomeActivity) {
            homeActivity = (HomeActivity) getActivity();
        }

        // Get promotion type from arguments
        if (getArguments() != null) {
            promotionType = getArguments().getString("promotionType", "flash_sale");
        } else {
            promotionType = "flash_sale";
        }

        Log.d("PromotionScreen", "=== STARTING PROMOTION SCREEN ===");
        Log.d("PromotionScreen", "Promotion type: " + promotionType);

        postCardRepository = new PostCardRepository();
        setupRecyclerView();
        setupHeader();
        setupAdapterClickListener();
        loadPromotionBooks();
    }

    private void setupHeader() {
        switch (promotionType) {
            case "flash_sale":
                binding.tvHeaderTitle.setText("Flash Sale! 🔥");
                binding.tvHeaderSubtitle.setText("50% OFF on all fiction books\nLimited time offer!");
                binding.headerBackground.setBackgroundColor(getResources().getColor(R.color.yellow));
                break;
            case "student_discount":
                binding.tvHeaderTitle.setText("Student Special 🎓");
                binding.tvHeaderSubtitle.setText("30% OFF for students\nValid with student ID");
                binding.headerBackground.setBackgroundColor(getResources().getColor(R.color.pink));
                break;
            default:
                binding.tvHeaderTitle.setText("Special Promotion");
                binding.tvHeaderSubtitle.setText("Amazing book collection");
        }
    }

    private void setupRecyclerView() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getUid() : null;

        adapter = new PostCardLikeAdapter(currentUserId);
        binding.promotionPost.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.promotionPost.setAdapter(adapter);

        Log.d("PromotionScreen", "RecyclerView setup completed");
    }

    private void setupAdapterClickListener() {
        adapter.setOnItemClickListener(new PostCardLikeAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(PostCard postCard) {
                openBookDetails(postCard);
            }

            @Override
            public void onLikeButtonClick(PostCard postCard) {
                // ✅ CORRECT: Navigate to PostDetailFragment like other fragments
                openBookDetails(postCard);
            }
        });
    }

    private void openBookDetails(PostCard book) {
        PostDetailFragment fragment = new PostDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putString("postId", book.getPostId());
        bundle.putString("status", "Available");
        bundle.putString("source", "promotion");
        bundle.putString("back", "Promotion");

        fragment.setArguments(bundle);

        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).LoadFragment(fragment);
        }
    }

    private void loadPromotionBooks() {
        showProgressBar();
        Log.d("PromotionScreen", "Loading promotion books for: " + promotionType);

        // Load all books first, then filter by genre
        postCardRepository.getAllPosts("books", new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> allBooks) {
                hideProgressBar();
                Log.d("PromotionScreen", "SUCCESS: Loaded " + allBooks.size() + " books from API");

                if (allBooks.isEmpty()) {
                    showEmptyState("No books found in database");
                    Toast.makeText(requireContext(), "No books available", Toast.LENGTH_LONG).show();
                    return;
                }

                // Log first few books for debugging
                for (int i = 0; i < Math.min(allBooks.size(), 3); i++) {
                    PostCard book = allBooks.get(i);
                    Log.d("PromotionScreen", "Book " + (i+1) + ": " + book.getTitle() + " | Genre: " + book.getGenre());
                }

                // Filter books based on promotion type
                List<PostCard> filteredBooks = filterBooksByPromotion(allBooks);

                if (filteredBooks.isEmpty()) {
                    Log.d("PromotionScreen", "No books matched the promotion filter, showing all books");
                    adapter.setPostCards(allBooks);
                    Toast.makeText(requireContext(),
                            "Showing all " + allBooks.size() + " books",
                            Toast.LENGTH_SHORT).show();
                } else {
                    adapter.setPostCards(filteredBooks);
                    Toast.makeText(requireContext(),
                            "Found " + filteredBooks.size() + " books for " + getPromotionTitle(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressBar();
                Log.e("PromotionScreen", "API ERROR: " + errorMessage);
                showEmptyState("Failed to load books: " + errorMessage);
                Toast.makeText(requireContext(), "Load failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private List<PostCard> filterBooksByPromotion(List<PostCard> allBooks) {
        List<PostCard> filteredBooks = new ArrayList<>();
        List<String> targetGenres = getTargetGenres();

        Log.d("PromotionScreen", "Filtering books. Looking for genres: " + targetGenres);

        for (PostCard book : allBooks) {
            if (book.getGenre() != null) {
                for (String targetGenre : targetGenres) {
                    if (book.getGenre().equalsIgnoreCase(targetGenre)) {
                        filteredBooks.add(book);
                        Log.d("PromotionScreen", "MATCH: " + book.getTitle() + " | Genre: " + book.getGenre());
                        break;
                    }
                }
            }
        }

        Log.d("PromotionScreen", "Filtered " + filteredBooks.size() + " books for promotion");
        return filteredBooks;
    }

    private List<String> getTargetGenres() {
        List<String> genres = new ArrayList<>();

        switch (promotionType) {
            case "flash_sale":
                // Fiction genres for flash sale
                genres.add("Fantasy");
                genres.add("Mystery");
                genres.add("Horror");
                genres.add("Comedy");
                break;
            case "student_discount":
                // Educational genres for student discount
                genres.add("Japanese");
                genres.add("Historical");
                genres.add("Biography");
                break;
            default:
                genres.add("Fantasy");
        }

        return genres;
    }

    private String getPromotionTitle() {
        switch (promotionType) {
            case "flash_sale":
                return "Flash Sale";
            case "student_discount":
                return "Student Discount";
            default:
                return "Promotion";
        }
    }

    private void showEmptyState(String message) {
        Log.d("PromotionScreen", "Empty state: " + message);
        adapter.setPostCards(new ArrayList<PostCard>());
    }

    private void showProgressBar() {
        if (homeActivity != null) {
            homeActivity.showProgressBar();
        }
        Log.d("PromotionScreen", "Progress bar shown");
    }

    private void hideProgressBar() {
        if (homeActivity != null) {
            homeActivity.hideProgressBar();
        }
        Log.d("PromotionScreen", "Progress bar hidden");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}