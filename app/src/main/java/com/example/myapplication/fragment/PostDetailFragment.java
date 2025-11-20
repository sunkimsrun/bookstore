package com.example.myapplication.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.myapplication.HomeActivity;
import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentPostDetailBinding;
import com.example.myapplication.model.PostCard;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PostDetailFragment extends Fragment implements OnMapReadyCallback {

    private FragmentPostDetailBinding binding;
    private HomeActivity homeActivity;
    private GoogleMap googleMap;
    private String currentPostId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPostDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeActivity = (HomeActivity) getActivity();
        showProgressBar();

        Bundle args = getArguments();
        if (args == null) {
            Log.e("PostDetailFragment", "No arguments provided");
            Toast.makeText(requireContext(), "Error loading post details", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        currentPostId = args.getString("postId");
        Log.d("PostDetailFragment", "Loading post with ID: " + currentPostId);

        if (currentPostId != null) {
            loadBookFromFirebase(currentPostId);
        } else {
            Log.e("PostDetailFragment", "Missing postId");
            Toast.makeText(requireContext(), "Error: Missing post information", Toast.LENGTH_SHORT).show();
            navigateBack();
        }

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        binding.tvReturn.setOnClickListener(v -> navigateBack());

        binding.btnCall.setOnClickListener(v -> {
            if (binding.displayPhone.getText() != null && !binding.displayPhone.getText().toString().isEmpty()) {
                String phone = binding.displayPhone.getText().toString();
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        setDefaultLocation();
    }

    private void loadBookFromFirebase(String postId) {
        Log.d("PostDetailFragment", "Loading book from Firebase: " + postId);

        DatabaseReference bookRef = FirebaseDatabase.getInstance().getReference("books").child(postId);
        bookRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!isAdded() || binding == null) {
                    hideProgressBar();
                    return;
                }

                if (!dataSnapshot.exists()) {
                    Log.e("PostDetailFragment", "Book not found in Firebase: " + postId);
                    hideProgressBar();
                    Toast.makeText(requireContext(), "Book not found", Toast.LENGTH_SHORT).show();
                    navigateBack();
                    return;
                }

                PostCard book = dataSnapshot.getValue(PostCard.class);
                if (book == null) {
                    Log.e("PostDetailFragment", "Failed to parse book data");
                    hideProgressBar();
                    Toast.makeText(requireContext(), "Error loading book", Toast.LENGTH_SHORT).show();
                    navigateBack();
                    return;
                }

                // Set the postId from the key
                book.setPostId(dataSnapshot.getKey());

                Log.d("PostDetailFragment", "Book loaded successfully: " + book.getTitle());
                Log.d("PostDetailFragment", "Book user ID: " + book.getUserId());

                displayBookData(book);
                loadUserData(book.getUserId(), book);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostDetailFragment", "Firebase error: " + error.getMessage());
                hideProgressBar();
                Toast.makeText(requireContext(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                navigateBack();
            }
        });
    }

    private void displayBookData(PostCard book) {
        try {
            // Set basic book information
            binding.displayTitle.setText(book.getTitle() != null ? book.getTitle() : "No Title");
            binding.displayDate.setText(book.getDate() != null ? book.getDate() : "Unknown Date");
            binding.displayInformation.setText(book.getInformation() != null ? book.getInformation() : "No description available");

            // Set email and phone from book data
            binding.displayEmail.setText(book.getEmail() != null && !book.getEmail().isEmpty() ? book.getEmail() : "No email provided");
            binding.displayPhone.setText(book.getPhone() != null && !book.getPhone().isEmpty() ? book.getPhone() : "No phone provided");

            // Set time
            if (book.getPostTime() != null && !book.getPostTime().isEmpty()) {
                binding.displayTime.setText(book.getPostTime());
                binding.displayTime.setVisibility(View.VISIBLE);
            } else {
                binding.displayTime.setVisibility(View.GONE);
            }

            // Set genre
            if (book.getGenre() != null && !book.getGenre().isEmpty()) {
                binding.displayGenre.setText(book.getGenre());
                binding.displayGenre.setVisibility(View.VISIBLE);
            } else {
                binding.displayGenre.setVisibility(View.GONE);
            }

            // Load book image
            if (book.getImageUrl() != null && !book.getImageUrl().isEmpty()) {
                Glide.with(requireContext())
                        .load(book.getImageUrl())
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .into(binding.displayImage);
                Log.d("PostDetailFragment", "Book image loaded: " + book.getImageUrl());
            } else {
                binding.displayImage.setImageResource(R.drawable.placeholder);
                Log.d("PostDetailFragment", "Using placeholder book image");
            }

            // Update map
            updateMapWithLocation(book);

            // Check if current user is the owner
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            if (book.getUserId() != null && mAuth.getCurrentUser() != null &&
                    book.getUserId().equals(mAuth.getCurrentUser().getUid())) {
                binding.btnCall.setVisibility(View.GONE);
            } else {
                binding.btnCall.setVisibility(View.VISIBLE);
            }

        } catch (Exception e) {
            Log.e("PostDetailFragment", "Error displaying book data: " + e.getMessage());
        }
    }

    private void loadUserData(String userId, PostCard book) {
        if (userId == null) {
            Log.e("PostDetailFragment", "User ID is null, cannot load user data");
            setDefaultUserData(book);
            hideProgressBar();
            return;
        }

        Log.d("PostDetailFragment", "Loading user data for ID: " + userId);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) {
                    hideProgressBar();
                    return;
                }

                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);

                    Log.d("PostDetailFragment", "User data found - Username: " + username + ", Image: " + profileImageUrl);

                    // Set user name
                    if (username != null && !username.isEmpty()) {
                        binding.userName.setText(username);
                        Log.d("PostDetailFragment", "User name set to: " + username);
                    } else {
                        binding.userName.setText("Unknown User");
                        Log.d("PostDetailFragment", "User name not found, using default");
                    }

                    // Set user image
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(requireContext())
                                .load(profileImageUrl)
                                .placeholder(R.drawable.placeholder)
                                .error(R.drawable.placeholder)
                                .into(binding.userImage);
                        Log.d("PostDetailFragment", "User image loaded: " + profileImageUrl);
                    } else {
                        binding.userImage.setImageResource(R.drawable.placeholder);
                        Log.d("PostDetailFragment", "Using placeholder user image");
                    }

                    // Update phone if user phone is available and book phone is empty
                    if ((book.getPhone() == null || book.getPhone().isEmpty()) && phone != null && !phone.isEmpty()) {
                        binding.displayPhone.setText(phone);
                    }

                    // Set up user image click listener
                    binding.userImage.setOnClickListener(v -> {
                        AccountFragment fragment = new AccountFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("userId", userId);
                        bundle.putBoolean("fromOther", true);
                        fragment.setArguments(bundle);
                        if (homeActivity != null) {
                            homeActivity.LoadFragment(fragment);
                        }
                    });

                } else {
                    Log.e("PostDetailFragment", "User data not found for ID: " + userId);
                    setDefaultUserData(book);
                }

                hideProgressBar();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostDetailFragment", "Error loading user data: " + error.getMessage());
                setDefaultUserData(book);
                hideProgressBar();
            }
        });
    }

    private void setDefaultUserData(PostCard book) {
        if (binding != null) {
            binding.userName.setText("Unknown User");
            binding.userImage.setImageResource(R.drawable.placeholder);

            // Set phone from book if available
            if (book.getPhone() != null && !book.getPhone().isEmpty()) {
                binding.displayPhone.setText(book.getPhone());
            }

            Log.d("PostDetailFragment", "Default user data set");
        }
    }

    private void updateMapWithLocation(PostCard book) {
        if (googleMap == null) return;

        try {
            String location = book.getLocation();
            LatLng postLocation;

            if (location != null && !location.isEmpty()) {
                if (location.toLowerCase().contains("rupp")) {
                    postLocation = new LatLng(11.5682157, 104.8899682);
                } else if (location.toLowerCase().contains("norton")) {
                    postLocation = new LatLng(11.5500, 104.9167);
                } else {
                    postLocation = new LatLng(11.5682157, 104.8899682);
                }
            } else {
                postLocation = new LatLng(11.5682157, 104.8899682);
            }

            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                    .position(postLocation)
                    .title(book.getTitle() != null ? book.getTitle() : "Book Location"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(postLocation, 14f));

        } catch (Exception e) {
            Log.e("PostDetailFragment", "Error updating map: " + e.getMessage());
            setDefaultLocation();
        }
    }

    private void setDefaultLocation() {
        if (googleMap != null) {
            LatLng rupp = new LatLng(11.5682157, 104.8899682);
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(rupp).title("RUPP"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(rupp, 14f));
        }
    }

    private void showProgressBar() {
        if (homeActivity != null) {
            homeActivity.showProgressBar();
        }
    }

    private void hideProgressBar() {
        if (homeActivity != null) {
            homeActivity.hideProgressBar();
        }
    }

    private void navigateBack() {
        if (homeActivity == null) return;

        Bundle args = getArguments();
        if (args != null && args.getBoolean("latestCard", false)) {
            homeActivity.LoadFragment(new HomeFragment());
            return;
        }

        String backDestination = args != null ? args.getString("back", "Home") : "Home";

        switch (backDestination) {
            case "View all books":
                homeActivity.LoadFragment(new AllBookFragment());
                break;
            case "Account":
                homeActivity.LoadFragment(new AccountFragment());
                break;
            case "Home":
            default:
                homeActivity.LoadFragment(new HomeFragment());
                break;
        }
    }
}