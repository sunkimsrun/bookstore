package com.example.myapplication.fragment;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
    private PostCard currentLatestBook1, currentLatestBook2, currentLatestBook3;

    // Auto-slide variables
    private Handler sliderHandler = new Handler();
    private boolean isAutoSliding = false;
    private int currentPosition = 0;
    private static final long SLIDE_INTERVAL = 3000; // 3 seconds

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
        setupLatestBooks();
        setupClickListeners();
    }

    private void setupPromotionCards() {
        View promotionCard1 = binding.getRoot().findViewById(R.id.promotionCard1);
        View promotionCard2 = binding.getRoot().findViewById(R.id.promotionCard2);

        // Setup Promotion Card 1
        if (promotionCard1 != null) {
            TextView titleText1 = promotionCard1.findViewById(R.id.titleText);
            TextView subText1 = promotionCard1.findViewById(R.id.subText);
            MaterialButton goButton1 = promotionCard1.findViewById(R.id.goButton);
            LottieAnimationView lottieAnimation1 = promotionCard1.findViewById(R.id.lottieAnimation);
            View cardContainer1 = promotionCard1.findViewById(R.id.cardContainer);

            titleText1.setText("Flash Sale! 🔥");
            subText1.setText("50% OFF on all fiction books\nLimited time offer!");

            if (cardContainer1 != null) {
                cardContainer1.setBackgroundResource(R.drawable.bg_promotion_yellow);
            }

            try {
                lottieAnimation1.setAnimation(R.raw.books_animation2);
                lottieAnimation1.playAnimation();
            } catch (Exception e) {
                Log.d("HomeFragment", "No Lottie animation found for Flash Sale card");
            }

            promotionCard1.setOnClickListener(v -> openPromotionScreen("flash_sale"));
            goButton1.setOnClickListener(v -> openPromotionScreen("flash_sale"));
        } else {
            Log.e("HomeFragment", "promotionCard1 not found");
        }

        // Setup Special Discount Card 2 - Student Discount
        if (promotionCard2 != null) {
            TextView titleText2 = promotionCard2.findViewById(R.id.titleText);
            TextView subText2 = promotionCard2.findViewById(R.id.subText);
            MaterialButton goButton2 = promotionCard2.findViewById(R.id.goButton);
            LottieAnimationView lottieAnimation2 = promotionCard2.findViewById(R.id.lottieAnimation);
            View cardContainer2 = promotionCard2.findViewById(R.id.cardContainer);

            titleText2.setText("Student Special 🎓");
            subText2.setText("30% OFF for students\nValid with student ID");

            if (cardContainer2 != null) {
                cardContainer2.setBackgroundResource(R.drawable.bg_promotion_pink);
            }

            try {
                lottieAnimation2.setAnimation(R.raw.books_animation);
                lottieAnimation2.playAnimation();
            } catch (Exception e) {
                Log.d("HomeFragment", "No Lottie animation found for Student Special card");
            }

            promotionCard2.setOnClickListener(v -> openPromotionScreen("student_discount"));
            goButton2.setOnClickListener(v -> openPromotionScreen("student_discount"));
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

            titleText.setText("Flash Sale! 🔥");
            subText.setText("50% OFF on all fiction books\nLimited time offer!");

            try {
                // Set first animation
                lottieAnimation.setAnimation(R.raw.books_animation);
                long duration = lottieAnimation.getDuration();
                Log.d("HomeFragment", "First animation duration: " + duration);
                lottieAnimation.playAnimation();

                // If duration is 0, set a default (1 second)
                if (duration == 0) {
                    duration = 1000;
                }

                // Switch to second animation after the duration of the first
                new Handler().postDelayed(() -> {
                    try {
                        lottieAnimation.setAnimation(R.raw.books_animation2);
                        lottieAnimation.playAnimation();
                        Log.d("HomeFragment", "Second animation started");
                    } catch (Exception e) {
                        Log.d("HomeFragment", "Second Lottie animation not found", e);
                    }
                }, duration);

            } catch (Exception e) {
                Log.d("HomeFragment", "First Lottie animation not found", e);
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
        serviceHelper.getTop7MostLikedBooks(new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> likedBooks) {
                binding.recommendBook.setText("Recommended Books");
                recommendedAdapter.setPostCards(likedBooks);
                Log.d("HomeFragment", "Loaded " + likedBooks.size() + " most liked books");

                // Update dot visibility based on number of items
                updateDotIndicators(likedBooks.size());

                // Start auto-slide if there are multiple books
                if (likedBooks.size() > 1) {
                    startAutoSlide();
                }

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

    // Auto-slide functionality
    private void startAutoSlide() {
        if (isAutoSliding) return;

        isAutoSliding = true;
        sliderHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (recommendedAdapter != null && recommendedAdapter.getItemCount() > 1) {
                    showNextCard();
                    sliderHandler.postDelayed(this, SLIDE_INTERVAL);
                }
            }
        }, SLIDE_INTERVAL);
    }

    private void showNextCard() {
        if (recommendedAdapter == null || recommendedAdapter.getItemCount() == 0) return;

        currentPosition = (currentPosition + 1) % recommendedAdapter.getItemCount();
        binding.viewPagerRecommendation.smoothScrollToPosition(currentPosition);

        // Update dot indicators to show current position
        updateActiveDot(currentPosition);
    }

    private void stopAutoSlide() {
        isAutoSliding = false;
        sliderHandler.removeCallbacksAndMessages(null);
    }

    private void updateActiveDot(int position) {
        ImageView[] dots = {
                binding.bgBlueRecommendation,
                binding.bgDot2Recommendation1,
                binding.bgDot2Recommendation2,
                binding.bgDot2Recommendation3,
                binding.bgDot2Recommendation4
        };

        for (int i = 0; i < dots.length; i++) {
            if (dots[i].getVisibility() == View.VISIBLE) {
                if (i == position) {
                    dots[i].setImageResource(R.drawable.blue_dot);
                } else {
                    dots[i].setImageResource(R.drawable.gray_dot);
                }
            }
        }
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

                // Start auto-slide if there are multiple books
                if (recentBooks.size() > 1) {
                    startAutoSlide();
                }

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

    private void setupLatestBooks() {
        postCardRepository.getLatestPosts("books", "date", 3, new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> postCards) {
                if (postCards == null || postCards.isEmpty()) {
                    Log.d("HomeFragment", "No books found in database");
                    hideProgressBar();
                    return;
                }

                // Display books based on available data
                if (postCards.size() >= 1) {
                    currentLatestBook1 = postCards.get(0);
                    displayLatestBook(currentLatestBook1, 1);
                }
                if (postCards.size() >= 2) {
                    currentLatestBook2 = postCards.get(1);
                    displayLatestBook(currentLatestBook2, 2);
                }
                if (postCards.size() >= 3) {
                    currentLatestBook3 = postCards.get(2);
                    displayLatestBook(currentLatestBook3, 3);
                }

                hideProgressBar();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("HomeFragment", "Failed to load latest books: " + errorMessage);
                hideProgressBar();
            }
        });
    }

    private void displayLatestBook(PostCard book, int cardNumber) {
        switch (cardNumber) {
            case 1:
                // Display in first card
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

                // Find the checkbox in the first card and set text
                Chip checkbox1 = binding.latestCard.findViewById(R.id.checkbox);
                if (checkbox1 != null) {
                    checkbox1.setText("View Book");
                }

                loadUserData(book.getUserId(), requireContext(), 1);
                break;

            case 2:
                // Display in second card
                Glide.with(requireContext())
                        .load(book.getImageUrl())
                        .centerCrop()
                        .placeholder(R.drawable.placeholder)
                        .into(binding.postImage2);

                binding.postTitle2.setText(book.getTitle());
                binding.postInformation2.setText(book.getInformation());
                binding.date2.setText(book.getDate());

                if (book.getPrice() != null && !book.getPrice().isEmpty()) {
                    binding.postPrice2.setText(book.getPrice() + " $");
                } else {
                    binding.postPrice2.setText("Free");
                }

                if (book.getGenre() != null && !book.getGenre().isEmpty()) {
                    binding.postGenre2.setText(book.getGenre());
                    binding.postGenre2.setVisibility(View.VISIBLE);
                } else {
                    binding.postGenre2.setVisibility(View.GONE);
                }

                if (book.getLocation() != null && !book.getLocation().isEmpty()) {
                    binding.postLocation2.setText(book.getLocation());
                } else {
                    binding.postLocation2.setText("Book Store");
                }

                // Find the checkbox in the second card and set text
                Chip checkbox2 = binding.latestCard2.findViewById(R.id.checkbox2);
                if (checkbox2 != null) {
                    checkbox2.setText("View Book");
                }

                loadUserData(book.getUserId(), requireContext(), 2);
                break;

            case 3:
                // Display in third card
                Glide.with(requireContext())
                        .load(book.getImageUrl())
                        .centerCrop()
                        .placeholder(R.drawable.placeholder)
                        .into(binding.postImage3);

                binding.postTitle3.setText(book.getTitle());
                binding.postInformation3.setText(book.getInformation());
                binding.date3.setText(book.getDate());

                if (book.getPrice() != null && !book.getPrice().isEmpty()) {
                    binding.postPrice3.setText(book.getPrice() + " $");
                } else {
                    binding.postPrice3.setText("Free");
                }

                if (book.getGenre() != null && !book.getGenre().isEmpty()) {
                    binding.postGenre3.setText(book.getGenre());
                    binding.postGenre3.setVisibility(View.VISIBLE);
                } else {
                    binding.postGenre3.setVisibility(View.GONE);
                }

                if (book.getLocation() != null && !book.getLocation().isEmpty()) {
                    binding.postLocation3.setText(book.getLocation());
                } else {
                    binding.postLocation3.setText("Book Store");
                }

                // Find the checkbox in the third card and set text
                Chip checkbox3 = binding.latestCard3.findViewById(R.id.checkbox3);
                if (checkbox3 != null) {
                    checkbox3.setText("View Book");
                }

                loadUserData(book.getUserId(), requireContext(), 3);
                break;
        }
    }

    private void setupClickListeners() {
        // Set up click listeners for all three latest cards
        setupCardClickListener(binding.latestCard, currentLatestBook1, 1);
        setupCardClickListener(binding.latestCard2, currentLatestBook2, 2);
        setupCardClickListener(binding.latestCard3, currentLatestBook3, 3);

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

    private void setupCardClickListener(View cardView, PostCard book, int cardNumber) {
        try {
            cardView.setOnClickListener(v -> {
                PostCard currentBook = getCurrentBookByCardNumber(cardNumber);
                if (currentBook != null) {
                    openBookDetails(currentBook);
                } else {
                    Toast.makeText(requireContext(), "No books available yet", Toast.LENGTH_SHORT).show();
                }
            });

            // Set up click listener for the checkbox
            Chip checkbox = cardView.findViewById(getCheckboxIdByCardNumber(cardNumber));
            if (checkbox != null) {
                checkbox.setOnClickListener(v -> {
                    PostCard currentBook = getCurrentBookByCardNumber(cardNumber);
                    if (currentBook != null) {
                        openBookDetails(currentBook);
                    } else {
                        Toast.makeText(requireContext(), "No books available yet", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.d("HomeFragment", "Checkbox " + cardNumber + " click listener set successfully");
            } else {
                Log.e("HomeFragment", "Checkbox " + cardNumber + " not found in card");
            }
        } catch (Exception e) {
            Log.e("HomeFragment", "Error setting card " + cardNumber + " click listener", e);
        }
    }

    private PostCard getCurrentBookByCardNumber(int cardNumber) {
        switch (cardNumber) {
            case 1: return currentLatestBook1;
            case 2: return currentLatestBook2;
            case 3: return currentLatestBook3;
            default: return null;
        }
    }

    private int getCheckboxIdByCardNumber(int cardNumber) {
        switch (cardNumber) {
            case 1: return R.id.checkbox;
            case 2: return R.id.checkbox2;
            case 3: return R.id.checkbox3;
            default: return R.id.checkbox;
        }
    }

    private void loadUserData(String userId, Context context, int cardNumber) {
        if (userId == null) {
            setDefaultUserData(cardNumber);
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String username = snapshot.child("username").getValue(String.class);
                String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                String phone = snapshot.child("phoneNumber").getValue(String.class);

                setUserData(cardNumber, username, phone, imageUrl, context);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setDefaultUserData(cardNumber);
            }
        });
    }

    private void setUserData(int cardNumber, String username, String phone, String imageUrl, Context context) {
        switch (cardNumber) {
            case 1:
                binding.userName.setText(username != null ? username : "Book Seller");
                binding.userPhone.setText(phone != null && !phone.isEmpty() ? phone : "No phone");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(context).load(imageUrl).placeholder(R.drawable.placeholder).centerCrop().into(binding.userImage);
                } else {
                    binding.userImage.setImageResource(R.drawable.placeholder);
                }
                break;

            case 2:
                binding.userName2.setText(username != null ? username : "Book Seller");
                binding.userPhone2.setText(phone != null && !phone.isEmpty() ? phone : "No phone");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(context).load(imageUrl).placeholder(R.drawable.placeholder).centerCrop().into(binding.userImage2);
                } else {
                    binding.userImage2.setImageResource(R.drawable.placeholder);
                }
                break;

            case 3:
                binding.userName3.setText(username != null ? username : "Book Seller");
                binding.userPhone3.setText(phone != null && !phone.isEmpty() ? phone : "No phone");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(context).load(imageUrl).placeholder(R.drawable.placeholder).centerCrop().into(binding.userImage3);
                } else {
                    binding.userImage3.setImageResource(R.drawable.placeholder);
                }
                break;
        }
    }

    private void setDefaultUserData(int cardNumber) {
        switch (cardNumber) {
            case 1:
                binding.userName.setText("Book Seller");
                binding.userPhone.setText("No phone");
                binding.userImage.setImageResource(R.drawable.placeholder);
                break;

            case 2:
                binding.userName2.setText("Book Seller");
                binding.userPhone2.setText("No phone");
                binding.userImage2.setImageResource(R.drawable.placeholder);
                break;

            case 3:
                binding.userName3.setText("Book Seller");
                binding.userPhone3.setText("No phone");
                binding.userImage3.setImageResource(R.drawable.placeholder);
                break;
        }
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

    // Lifecycle methods for auto-slide
    @Override
    public void onResume() {
        super.onResume();
        // Resume auto-slide if there are multiple books
        if (recommendedAdapter != null && recommendedAdapter.getItemCount() > 1 && !isAutoSliding) {
            startAutoSlide();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoSlide();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoSlide();
        binding = null;
    }

    private void showProgressBar() {
        if (homeActivity != null) homeActivity.showProgressBar();
    }

    private void hideProgressBar() {
        if (homeActivity != null) homeActivity.hideProgressBar();
    }
}