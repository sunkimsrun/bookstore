package com.example.myapplication.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.HomeActivity;
import com.example.myapplication.R;
import com.example.myapplication.adapter.PostCardAdapter;
import com.example.myapplication.databinding.FragmentAccountBinding;
import com.example.myapplication.model.PostCard;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseStorage firebaseStorage;
    private FirebaseDatabase firebaseDatabase;
    private PostCardAdapter adapter;
    private Uri imageUri;
    private HomeActivity homeActivity;
    private String userIdToUse;
    private static final int CAMERA_PERMISSION_CODE = 1001;
    private static final int GALLERY_PERMISSION_CODE = 1002;

    // Track current display mode
    private boolean showingLikedBooks = false;

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            imageUri = result.getData().getData();
                            if (imageUri != null) {
                                uploadImageToFirebase();
                            }
                        }
                    });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && imageUri != null) {
                            uploadImageToFirebase();
                        }
                    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        initializeFirebase();
        return binding.getRoot();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof HomeActivity) {
            homeActivity = (HomeActivity) getActivity();
        }

        initializeUser();
        setupRecyclerView();
        setupClickListeners();
        setupNavigationBack();
        setupAdapterClickListener();
    }

    private void initializeUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getUid() : null;

        if (currentUserId == null && (getArguments() == null || !getArguments().containsKey("userId"))) {
            Toast.makeText(requireContext(), "No user found.", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return;
        }

        userIdToUse = getArguments() != null && getArguments().containsKey("userId")
                ? getArguments().getString("userId")
                : currentUserId;

        Log.d("AccountFragment", "Initializing user with ID: " + userIdToUse);

        boolean isViewingOwnAccount = currentUser != null && currentUser.getUid().equals(userIdToUse);
        updateUIForAccountType(isViewingOwnAccount);
        updateUserDisplay(userIdToUse);

        // Load user's own books by default
        loadUserBooks(userIdToUse);
    }

    private void updateUIForAccountType(boolean isViewingOwnAccount) {
        binding.tvBack.setVisibility(isViewingOwnAccount ? View.GONE : View.VISIBLE);
        binding.editIcon.setVisibility(isViewingOwnAccount ? View.VISIBLE : View.GONE);
        binding.button2.setVisibility(isViewingOwnAccount ? View.VISIBLE : View.GONE);

        // Only show liked books feature if viewing own account
        binding.liked.setVisibility(isViewingOwnAccount ? View.VISIBLE : View.GONE);
        binding.liked.setText("Liked Books");
    }

    private void setupClickListeners() {
        binding.editIcon.setOnClickListener(v -> showImagePickerDialog());

        binding.button2.setOnClickListener(v -> {
            if (homeActivity != null) {
                homeActivity.LoadFragment(new AccountInformationFragment());
            }
        });

        binding.liked.setOnClickListener(v -> {
            // Toggle between showing user's books and liked books
            showingLikedBooks = !showingLikedBooks;

            if (showingLikedBooks) {
                binding.liked.setText("My Books");
                loadLikedBooks(userIdToUse);
            } else {
                binding.liked.setText("Liked Books");
                loadUserBooks(userIdToUse);
            }
        });
    }

    private void setupAdapterClickListener() {
        adapter.setOnItemClickListener(new PostCardAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(PostCard postCard) {
                openBookDetails(postCard);
            }

            @Override
            public void onCheckboxClick(PostCard postCard) {
                openBookDetails(postCard);
            }
        });
    }

    private void openBookDetails(PostCard book) {
        PostDetailFragment fragment = new PostDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putString("postId", book.getPostId());
        bundle.putString("status", "Available");
        bundle.putString("source", "account");
        bundle.putString("back", "Account");

        fragment.setArguments(bundle);

        if (homeActivity != null) {
            homeActivity.LoadFragment(fragment);
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermission();
            } else {
                checkGalleryPermission();
            }
        });
        builder.show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    private void checkGalleryPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
            launchGallery();
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, GALLERY_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == GALLERY_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchGallery();
            } else {
                Toast.makeText(requireContext(), "Storage permission is required to select images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchCamera() {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "Profile Picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "User profile picture");
            imageUri = requireActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (imageUri != null) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                cameraLauncher.launch(cameraIntent);
            } else {
                Toast.makeText(requireContext(), "Failed to create image file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to open gallery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.rcvListPost;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new PostCardAdapter(userIdToUse);
        recyclerView.setAdapter(adapter);
    }

    private void setupNavigationBack() {
        if (homeActivity == null) return;

        binding.tvBack.setText("Back to Home");
        binding.tvBack.setOnClickListener(v -> {
            if (homeActivity != null) {
                homeActivity.LoadFragment(new HomeFragment());
            }
        });
    }

    private void uploadImageToFirebase() {
        if (imageUri == null) {
            Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressBar();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            hideProgressBar();
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference imageRef = firebaseStorage.getReference("profile_images/" + user.getUid() + ".jpg");

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            saveImageUrlToDatabase(uri.toString());
                            Glide.with(requireContext()).load(uri).into(binding.profileImage);
                            hideProgressBar();
                            Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            hideProgressBar();
                            Toast.makeText(requireContext(), "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    hideProgressBar();
                    Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveImageUrlToDatabase(String url) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        firebaseDatabase.getReference("Users").child(user.getUid()).child("profileImageUrl").setValue(url);
    }

    public void updateUserDisplay(String userId) {
        showProgressBar();
        if (userId != null) {
            getUserDataFromDatabase(userId);
        } else {
            hideProgressBar();
            Toast.makeText(requireContext(), "User ID is null", Toast.LENGTH_SHORT).show();
        }
    }

    private void getUserDataFromDatabase(String userID) {
        Log.d("AccountFragment", "Getting user data for ID: " + userID);

        firebaseDatabase.getReference("Users").child(userID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded() || binding == null) {
                            hideProgressBar();
                            return;
                        }

                        if (snapshot.exists()) {
                            Log.d("AccountFragment", "User data found, processing...");

                            // Get all available fields with fallbacks
                            String username = getValueWithFallback(snapshot, "username", "name", "User");
                            String email = getValueWithFallback(snapshot, "email", "userEmail", "No email provided");
                            String phone = getValueWithFallback(snapshot, "phone", "phoneNumber", "No phone number");
                            String gender = getValueWithFallback(snapshot, "gender", "userGender", "Not specified");
                            String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                            Log.d("AccountFragment", "User data - Username: " + username + ", Email: " + email + ", Phone: " + phone);

                            // Update UI
                            binding.nameText.setText(username);
                            binding.email.setText(email);
                            binding.phoneNumber.setText(phone);
                            binding.gender.setText(gender);

                            // Load profile image
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(requireContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.placeholder)
                                        .error(R.drawable.placeholder)
                                        .centerCrop()
                                        .into(binding.profileImage);
                                Log.d("AccountFragment", "Profile image loaded: " + imageUrl);
                            } else {
                                binding.profileImage.setImageResource(R.drawable.placeholder);
                                Log.d("AccountFragment", "Using placeholder profile image");
                            }
                        } else {
                            Log.e("AccountFragment", "User data not found for ID: " + userID);
                            setDefaultUserData();
                            Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show();
                        }
                        hideProgressBar();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("AccountFragment", "Failed to load user data: " + error.getMessage());
                        hideProgressBar();
                        setDefaultUserData();
                        Toast.makeText(requireContext(), "Failed to load user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getValueWithFallback(DataSnapshot snapshot, String primaryKey, String fallbackKey, String defaultValue) {
        String value = snapshot.child(primaryKey).getValue(String.class);
        if (value == null || value.isEmpty()) {
            value = snapshot.child(fallbackKey).getValue(String.class);
        }
        return value != null ? value : defaultValue;
    }

    private void setDefaultUserData() {
        if (binding != null) {
            binding.nameText.setText("User");
            binding.email.setText("No email provided");
            binding.phoneNumber.setText("No phone number");
            binding.gender.setText("Not specified");
            binding.profileImage.setImageResource(R.drawable.placeholder);
        }
    }

    private void loadUserBooks(String userId) {
        if (userId == null) {
            Log.e("AccountFragment", "User ID is null, cannot load books");
            binding.noFound.setVisibility(View.VISIBLE);
            return;
        }

        Log.d("AccountFragment", "Loading user books for ID: " + userId);
        loadBooksFromDatabase(userId);
    }

    private void loadLikedBooks(String userId) {
        if (userId == null) {
            binding.noFound.setVisibility(View.VISIBLE);
            return;
        }

        showProgressBar();
        Log.d("AccountFragment", "Loading liked books for user: " + userId);

        firebaseDatabase.getReference("Users").child(userId).child("likedBooks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<PostCard> likedBooks = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            PostCard book = snapshot.getValue(PostCard.class);
                            if (book != null) {
                                book.setPostId(snapshot.getKey());
                                likedBooks.add(book);
                            }
                        }

                        if (adapter != null) {
                            adapter.setPostCards(likedBooks);
                            Log.d("AccountFragment", "Loaded " + likedBooks.size() + " liked books");
                        }

                        updateEmptyState();
                        hideProgressBar();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("AccountFragment", "Failed to load liked books: " + error.getMessage());
                        hideProgressBar();
                        Toast.makeText(requireContext(), "Failed to load liked books", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadBooksFromDatabase(String userId) {
        showProgressBar();
        Log.d("AccountFragment", "Loading books from database for user: " + userId);

        firebaseDatabase.getReference("books").orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<PostCard> userBooks = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            PostCard book = snapshot.getValue(PostCard.class);
                            if (book != null) {
                                book.setPostId(snapshot.getKey());
                                userBooks.add(book);
                                Log.d("AccountFragment", "Loaded book: " + book.getTitle() + ", UserId: " + book.getUserId());
                            }
                        }

                        if (adapter != null) {
                            adapter.setPostCards(userBooks);
                            Log.d("AccountFragment", "Set " + userBooks.size() + " books to adapter");
                        } else {
                            Log.e("AccountFragment", "Adapter is null, cannot set books");
                        }

                        updateEmptyState();
                        hideProgressBar();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("AccountFragment", "Failed to load books: " + error.getMessage());
                        hideProgressBar();
                        Toast.makeText(requireContext(), "Failed to load books: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmptyState() {
        if (adapter == null) {
            Log.e("AccountFragment", "Adapter is null in updateEmptyState");
            binding.noFound.setVisibility(View.VISIBLE);
            binding.rcvListPost.setVisibility(View.GONE);
            return;
        }

        if (adapter.getItemCount() == 0) {
            binding.noFound.setVisibility(View.VISIBLE);
            binding.rcvListPost.setVisibility(View.GONE);
            Log.d("AccountFragment", "No books found, showing empty state");
        } else {
            binding.noFound.setVisibility(View.GONE);
            binding.rcvListPost.setVisibility(View.VISIBLE);
            Log.d("AccountFragment", "Books found, hiding empty state");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userIdToUse != null) {
            Log.d("AccountFragment", "onResume - Updating user display for: " + userIdToUse);
            updateUserDisplay(userIdToUse);
            // Reload based on current display mode
            if (showingLikedBooks) {
                loadLikedBooks(userIdToUse);
            } else {
                loadUserBooks(userIdToUse);
            }
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