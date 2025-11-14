package com.example.myapplication.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.example.myapplication.HomeActivity;
import com.example.myapplication.LoginActivity;
import com.example.myapplication.R;
import com.example.myapplication.adapter.PostCardRecommendationAdapter;
import com.example.myapplication.databinding.FragmentHomeBinding;
import com.example.myapplication.model.PostCard;
import com.example.myapplication.repository.IApiCallback;
import com.example.myapplication.repository.PostCardRepository;
import com.example.myapplication.service.PostCardServiceHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private PostCardRepository postCardRepository;
    private FirebaseAuth mAuth;
    private HomeActivity homeActivity;
    private PostCardRecommendationAdapter recommendedAdapter;
    private PostCard currentLatestBook;

    // Genre buttons
    private ImageButton btnJapanese, btnComedy, btnMystery, btnHistorical, btnBiography, btnHorror, btnFantasy;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        postCardRepository = new PostCardRepository();

        if (getActivity() instanceof HomeActivity) {
            homeActivity = (HomeActivity) getActivity();
        }

        showProgressBar();
        setupPromotionCards();
        setupTopCard();
        setupGenreButtons();
        setupRecommendedBooks();
        setupTopUsers();
        setupLatestBook();
        setupClickListeners();
    }

    private void setupPromotionCards() {
        View promotionCard1 = binding.getRoot().findViewById(R.id.promotionCard1);
        View promotionCard2 = binding.getRoot().findViewById(R.id.promotionCard2);

        // Setup Back to School Card
        if (promotionCard1 != null) {
            TextView titleText1 = promotionCard1.findViewById(R.id.titleText);
            TextView subText1 = promotionCard1.findViewById(R.id.subText);
            MaterialButton goButton1 = promotionCard1.findViewById(R.id.goButton);
            LottieAnimationView lottieAnimation1 = promotionCard1.findViewById(R.id.lottieAnimation);
            View cardContainer1 = promotionCard1.findViewById(R.id.cardContainer);

            titleText1.setText("Back to School");
            subText1.setText("Book collection for student back to school");

            if (cardContainer1 != null) {
                cardContainer1.setBackgroundResource(R.drawable.bg_promotion_yellow);
            }

            try {
                lottieAnimation1.setAnimation(R.raw.books_animation);
                lottieAnimation1.playAnimation();
            } catch (Exception e) {
                Log.d("HomeFragment", "No Lottie animation found for Back to School card");
            }

            promotionCard1.setOnClickListener(v -> openPromotionScreen("back_to_school"));
            goButton1.setOnClickListener(v -> openPromotionScreen("back_to_school"));
        } else {
            Log.e("HomeFragment", "promotionCard1 not found");
        }

        // Setup BAC II Vacation Card
        if (promotionCard2 != null) {
            TextView titleText2 = promotionCard2.findViewById(R.id.titleText);
            TextView subText2 = promotionCard2.findViewById(R.id.subText);
            MaterialButton goButton2 = promotionCard2.findViewById(R.id.goButton);
            LottieAnimationView lottieAnimation2 = promotionCard2.findViewById(R.id.lottieAnimation);
            View cardContainer2 = promotionCard2.findViewById(R.id.cardContainer);

            titleText2.setText("BAC II Vacation");
            subText2.setText("Book collection for students for High School");

            if (cardContainer2 != null) {
                cardContainer2.setBackgroundResource(R.drawable.bg_promotion_pink);
            }

            try {
                lottieAnimation2.setAnimation(R.raw.books_animation);
                lottieAnimation2.playAnimation();
            } catch (Exception e) {
                Log.d("HomeFragment", "No Lottie animation found for BAC II Vacation card");
            }

            promotionCard2.setOnClickListener(v -> openPromotionScreen("bac_ii_vacation"));
            goButton2.setOnClickListener(v -> openPromotionScreen("bac_ii_vacation"));
        } else {
            Log.e("HomeFragment", "promotionCard2 not found");
        }
    }

    private void setupGenreButtons() {
        // Find genre buttons by their IDs
        btnJapanese = binding.getRoot().findViewById(R.id.japaneseGenre);
        btnComedy = binding.getRoot().findViewById(R.id.comedyGenre);
        btnMystery = binding.getRoot().findViewById(R.id.mysteryGenre);
        btnHistorical = binding.getRoot().findViewById(R.id.historicalGenre);
        btnBiography = binding.getRoot().findViewById(R.id.biographyGenre);
        btnHorror = binding.getRoot().findViewById(R.id.horrorGenre);
        btnFantasy = binding.getRoot().findViewById(R.id.fantasyGenre);

        // Set click listeners for genre buttons
        View.OnClickListener genreClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String genre = getGenreFromButtonId(v.getId());
                if (genre != null) {
                    openAllBooksFragmentWithGenre(genre);
                }
            }
        };

        // Apply click listeners to all genre buttons
        if (btnJapanese != null) btnJapanese.setOnClickListener(genreClickListener);
        if (btnComedy != null) btnComedy.setOnClickListener(genreClickListener);
        if (btnMystery != null) btnMystery.setOnClickListener(genreClickListener);
        if (btnHistorical != null) btnHistorical.setOnClickListener(genreClickListener);
        if (btnBiography != null) btnBiography.setOnClickListener(genreClickListener);
        if (btnHorror != null) btnHorror.setOnClickListener(genreClickListener);
        if (btnFantasy != null) btnFantasy.setOnClickListener(genreClickListener);
    }

    private String getGenreFromButtonId(int buttonId) {
        // Map button IDs to genres
        if (buttonId == R.id.japaneseGenre) return "Japanese";
        if (buttonId == R.id.comedyGenre) return "Comedy";
        if (buttonId == R.id.mysteryGenre) return "Mystery";
        if (buttonId == R.id.historicalGenre) return "Historical";
        if (buttonId == R.id.biographyGenre) return "Biography";
        if (buttonId == R.id.horrorGenre) return "Horror";
        if (buttonId == R.id.fantasyGenre) return "Fantasy";
        return null;
    }

    private void openAllBooksFragmentWithGenre(String genre) {
        AllBookFragment fragment = new AllBookFragment();
        Bundle args = new Bundle();
        args.putString("selected_genre", genre);
        fragment.setArguments(args);

        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).binding.navigationView.setCheckedItem(R.id.nav_all_book);
            ((HomeActivity) getActivity()).LoadFragment(fragment);
        }
    }

    private void openPromotionScreen(String promotionType) {
        PromotionScreenFragment fragment = new PromotionScreenFragment();
        Bundle args = new Bundle();
        args.putString("promotionType", promotionType);
        fragment.setArguments(args);

        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).LoadFragment(fragment);
        }
    }

    private void setupTopCard() {
        View topCard = binding.getRoot().findViewById(R.id.cardContainer);
        if (topCard != null) {
            TextView titleText = topCard.findViewById(R.id.titleText);
            TextView subText = topCard.findViewById(R.id.subText);
            MaterialButton goButton = topCard.findViewById(R.id.goButton);
            LottieAnimationView lottieAnimation = topCard.findViewById(R.id.lottieAnimation);

            titleText.setText("Book Store");
            subText.setText("Discover amazing books\nfrom various genres");

            try {
                lottieAnimation.setAnimation(R.raw.books_animation);
                lottieAnimation.playAnimation();
            } catch (Exception e) {
                Log.d("HomeFragment", "No Lottie animation found");
            }

            goButton.setOnClickListener(v -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).binding.navigationView.setCheckedItem(R.id.nav_all_book);
                    ((HomeActivity) getActivity()).LoadFragment(new AllBookFragment());
                }
            });
        }
    }

    private void setupRecommendedBooks() {
        // Get the RecyclerView (previously ViewPager2)
        RecyclerView recommendedRecyclerView = binding.viewPagerRecommendation;

        // Set horizontal layout manager for horizontal scrolling
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recommendedRecyclerView.setLayoutManager(layoutManager);

        // Use PostCardRecommendationAdapter
        recommendedAdapter = new PostCardRecommendationAdapter();
        recommendedRecyclerView.setAdapter(recommendedAdapter);

        setupRecommendedAdapterClickListener();

        PostCardServiceHelper serviceHelper = new PostCardServiceHelper(postCardRepository);
        serviceHelper.getTop5MostLikedBooks(new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> likedBooks) {
                binding.recommendBook.setText("Most Liked Books");
                recommendedAdapter.setPostCards(likedBooks);
                Log.d("HomeFragment", "Loaded " + likedBooks.size() + " most liked books");

                // Update dot visibility based on number of items
                updateDotIndicators(likedBooks.size());
                hideProgressBar();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("HomeFragment", "Error fetching most liked books: " + errorMessage);
                binding.recommendBook.setText("Popular Books");
                loadDefaultBooks();
            }
        });
    }

    private void updateDotIndicators(int itemCount) {
        // Show/hide dots based on item count
        ImageView[] dots = {
                binding.bgBlueRecommendation,
                binding.bgDot2Recommendation1,
                binding.bgDot2Recommendation2,
                binding.bgDot2Recommendation3,
                binding.bgDot2Recommendation4
        };

        for (int i = 0; i < dots.length; i++) {
            if (i < itemCount) {
                dots[i].setVisibility(View.VISIBLE);
                // Set first dot as active, others as inactive
                if (i == 0) {
                    dots[i].setImageResource(R.drawable.blue_dot);
                } else {
                    dots[i].setImageResource(R.drawable.gray_dot);
                }
            } else {
                dots[i].setVisibility(View.GONE);
            }
        }
    }

    private void setupRecommendedAdapterClickListener() {
        recommendedAdapter.setOnItemClickListener(new PostCardRecommendationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(PostCard postCard) {
                openBookDetails(postCard);
            }

            @Override
            public void onExploreClick(PostCard postCard) {
                openBookDetails(postCard);
            }
        });
    }

    private void loadDefaultBooks() {
        postCardRepository.getLatestPosts("books", "date", 5, new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> recentBooks) {
                recommendedAdapter.setPostCards(recentBooks);
                updateDotIndicators(recentBooks.size());
                hideProgressBar();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("HomeFragment", "Error loading default books: " + errorMessage);
                hideProgressBar();
            }
        });
    }

    private void setupTopUsers() {
        PostCardServiceHelper serviceHelper = new PostCardServiceHelper(postCardRepository);

        serviceHelper.getTop3UsersWithMostBooks(new IApiCallback<List<Map.Entry<String, Integer>>>() {
            @Override
            public void onSuccess(List<Map.Entry<String, Integer>> result) {
                for (int i = 0; i < result.size(); i++) {
                    Map.Entry<String, Integer> entry = result.get(i);
                    String userId = entry.getKey();
                    int bookCount = entry.getValue();
                    Log.d("HomeFragment", "Top User " + (i+1) + ": " + userId + " with " + bookCount + " books");
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("HomeFragment", "Error fetching top users: " + errorMessage);
            }
        });
    }

    private void setupLatestBook() {
        postCardRepository.getLatestPosts("books", "date", 1, new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> postCards) {
                if (postCards == null || postCards.isEmpty()) {
                    Log.d("HomeFragment", "No books found in database");
                    hideProgressBar();
                    return;
                }

                PostCard book = postCards.get(0);
                currentLatestBook = book;
                displayLatestBook(book);
                hideProgressBar();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("HomeFragment", "Failed to load latest book: " + errorMessage);
                hideProgressBar();
            }
        });
    }

    private void displayLatestBook(PostCard book) {
        // Use proper binding references
        Glide.with(requireContext())
                .load(book.getImageUrl())
                .centerCrop()
                .placeholder(R.drawable.placeholder)
                .into(binding.postImage);

        binding.postTitle.setText(book.getTitle());
        binding.postInformation.setText(book.getInformation());
        binding.date.setText(book.getDate());

        if (book.getPrice() != null && !book.getPrice().isEmpty()) {
            binding.postPrice.setText(book.getPrice() + " $");
        } else {
            binding.postPrice.setText("Free");
        }

        if (book.getGenre() != null && !book.getGenre().isEmpty()) {
            binding.postGenre.setText(book.getGenre());
            binding.postGenre.setVisibility(View.VISIBLE);
        } else {
            binding.postGenre.setVisibility(View.GONE);
        }

        if (book.getLocation() != null && !book.getLocation().isEmpty()) {
            binding.postLocation.setText(book.getLocation());
        } else {
            binding.postLocation.setText("Book Store");
        }

        // Find the checkbox in the latest card and set text
        Chip checkbox = binding.latestCard.findViewById(R.id.checkbox);
        if (checkbox != null) {
            checkbox.setText("View Details");
        }

        loadUserData(book.getUserId(), requireContext());
    }

    private void setupClickListeners() {
        // Set up click listener for the entire latest card
        try {
            binding.latestCard.setOnClickListener(v -> {
                // Only navigate if we have a book
                if (currentLatestBook != null) {
                    openBookDetails(currentLatestBook);
                } else {
                    Toast.makeText(requireContext(), "No books available yet", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("HomeFragment", "Error setting latest card click listener", e);
        }

        // Set up click listener for the checkbox
        try {
            // Find the checkbox view in the latest card layout
            Chip checkbox = binding.latestCard.findViewById(R.id.checkbox);
            if (checkbox != null) {
                checkbox.setOnClickListener(v -> {
                    // Only navigate if we have a book
                    if (currentLatestBook != null) {
                        openBookDetails(currentLatestBook);
                    } else {
                        Toast.makeText(requireContext(), "No books available yet", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.d("HomeFragment", "Checkbox click listener set successfully");
            } else {
                Log.e("HomeFragment", "Checkbox not found in latest card");
            }
        } catch (Exception e) {
            Log.e("HomeFragment", "Error setting checkbox click listener", e);
        }

        // Set up skip button
        binding.tvSkip.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).binding.navigationView.setCheckedItem(R.id.nav_all_book);
                ((HomeActivity) getActivity()).LoadFragment(new AllBookFragment());
            }
        });

        // Set up create book button
        binding.cardCreate.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                startActivity(new Intent(getActivity(), LoginActivity.class));
                Toast.makeText(getActivity(), "Please login to create a book", Toast.LENGTH_SHORT).show();
            } else {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).LoadFragment(new PostBookFragment());
                }
            }
        });
    }

    private void loadUserData(String userId, Context context) {
        if (userId == null) {
            // Use proper binding references
            binding.userName.setText("Book Seller");
            binding.userPhone.setText("No phone");
            binding.userImage.setImageResource(R.drawable.placeholder);
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String username = snapshot.child("username").getValue(String.class);
                String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                String phone = snapshot.child("phoneNumber").getValue(String.class);

                // Use proper binding references
                binding.userName.setText(username != null ? username : "Book Seller");

                if (phone != null && !phone.isEmpty()) {
                    binding.userPhone.setText(phone);
                } else {
                    binding.userPhone.setText("No phone");
                }

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(context).load(imageUrl).placeholder(R.drawable.placeholder).centerCrop().into(binding.userImage);
                } else {
                    binding.userImage.setImageResource(R.drawable.placeholder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Use proper binding references
                binding.userName.setText("Book Seller");
                binding.userPhone.setText("No phone");
                binding.userImage.setImageResource(R.drawable.placeholder);
            }
        });
    }

    private void openBookDetails(PostCard book) {
        if (book == null) {
            Toast.makeText(requireContext(), "No book details available", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("HomeFragment", "Opening book details: " + book.getTitle() + " ID: " + book.getPostId());

        PostDetailFragment fragment = new PostDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("latestCard", true);
        bundle.putString("postId", book.getPostId()); // Make sure this is not null
        bundle.putString("status", "Available");
        bundle.putString("source", "bookstore");

        if (getActivity() instanceof HomeActivity) {
            int selectedId = ((HomeActivity) getActivity()).binding.navigationView.getCheckedItem().getItemId();

            if (selectedId == R.id.nav_all_book) {
                bundle.putString("back", "View all books");
            } else if (selectedId == R.id.nav_account) {
                bundle.putString("back", "Account");
            } else {
                bundle.putString("back", "Home");
            }

            fragment.setArguments(bundle);
            ((HomeActivity) getActivity()).LoadFragment(fragment);
        }
    }

    private void showProgressBar() {
        if (homeActivity != null) homeActivity.showProgressBar();
    }

    private void hideProgressBar() {
        if (homeActivity != null) homeActivity.hideProgressBar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}