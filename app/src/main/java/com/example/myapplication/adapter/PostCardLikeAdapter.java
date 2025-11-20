package com.example.myapplication.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.databinding.PostCardLikeBinding;
import com.example.myapplication.model.PostCard;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PostCardLikeAdapter extends RecyclerView.Adapter<PostCardLikeAdapter.ViewHolder> {

    private final List<PostCard> postCards;
    private final String currentUserId;
    private final FirebaseDatabase firebaseDatabase;
    private OnItemClickListener onItemClickListener;

    public PostCardLikeAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
        this.postCards = new ArrayList<>();
        this.firebaseDatabase = FirebaseDatabase.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PostCardLikeBinding binding = PostCardLikeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PostCard post = postCards.get(position);

        if (post == null) {
            return;
        }

        // Load book image with proper error handling
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(post.getImageUrl())
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(holder.binding.postImage);
        } else {
            holder.binding.postImage.setImageResource(R.drawable.placeholder);
        }

        // Set book details with null safety
        holder.binding.postTitle.setText(post.getTitle() != null ? post.getTitle() : "No Title");
        holder.binding.postInformation.setText(post.getInformation() != null ? post.getInformation() : "No description available");
        holder.binding.date.setText(post.getDate() != null ? post.getDate() : "No date");

        // Set phone from post data FIRST (this will be overridden by user data if available)
        if (post.getPhone() != null && !post.getPhone().isEmpty()) {
            holder.binding.userPhone.setText(post.getPhone());
            Log.d("PostCardLikeAdapter", "Post phone set: " + post.getPhone());
        } else {
            holder.binding.userPhone.setText("No phone");
        }

        // Set price if available
        if (post.getPrice() != null && !post.getPrice().isEmpty()) {
            holder.binding.postPrice.setText(post.getPrice() + " $");
        } else {
            holder.binding.postPrice.setText("Free");
        }

        // Set genre if available
        if (post.getGenre() != null && !post.getGenre().isEmpty()) {
            holder.binding.postGenre.setText(post.getGenre());
            holder.binding.postGenre.setVisibility(View.VISIBLE);
        } else {
            holder.binding.postGenre.setVisibility(View.GONE);
        }

        // Set location if available
        if (post.getLocation() != null && !post.getLocation().isEmpty()) {
            holder.binding.postLocation.setText(post.getLocation());
        } else {
            holder.binding.postLocation.setText("Book Store");
        }

        // Load user data
        if (post.getUserId() != null) {
            loadUserData(holder.binding, post.getUserId(), holder.itemView.getContext());
        } else {
            setDefaultUserData(holder.binding);
        }

        // Setup like button if user is logged in and post has valid ID
        if (currentUserId != null && post.getPostId() != null) {
            setupLikeButton(holder.binding, post);
            holder.binding.likeButton.setVisibility(View.VISIBLE);
        } else {
            holder.binding.likeButton.setVisibility(View.GONE);
        }

        // Set click listener for the entire card
        holder.itemView.setOnClickListener(view -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(post);
            }
        });

        // Load user data - this will override the phone if user data has phone
        if (post.getUserId() != null) {
            loadUserData(holder.binding, post.getUserId(), holder.itemView.getContext());
        } else {
            setDefaultUserData(holder.binding);
        }

        // Set click listener for the like button - only like functionality
        holder.binding.likeButton.setOnClickListener(view -> {
            // First navigate to PostDetailFragment
            if (onItemClickListener != null) {
                onItemClickListener.onLikeButtonClick(post);
            }

            // Then handle the like functionality only (no unlike)
            handleLikeButtonClick(holder.binding, post);
        });
    }

    private void handleLikeButtonClick(PostCardLikeBinding binding, PostCard post) {
        if (currentUserId == null) {
            Toast.makeText(binding.getRoot().getContext(), "Please log in to like books", Toast.LENGTH_SHORT).show();
            return;
        }

        if (post.getPostId() == null) {
            Toast.makeText(binding.getRoot().getContext(), "Cannot like this book", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference likedRef = firebaseDatabase.getReference("Users")
                .child(currentUserId)
                .child("likedBooks")
                .child(post.getPostId());

        // Check if already liked
        likedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Only like if not already liked
                    likedRef.setValue(post)
                            .addOnSuccessListener(aVoid -> {
                                updateLikeButtonAppearance(binding, true);
                                Toast.makeText(binding.getRoot().getContext(), "Added to liked books", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(binding.getRoot().getContext(), "Failed to like book", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    // Already liked - just show message
                    Toast.makeText(binding.getRoot().getContext(), "Book already in your liked books", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(binding.getRoot().getContext(), "Failed to check like status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLikeButton(PostCardLikeBinding binding, PostCard post) {
        if (currentUserId == null || post.getPostId() == null) {
            binding.likeButton.setVisibility(View.GONE);
            return;
        }

        // Check if current user has liked this book
        DatabaseReference likedRef = firebaseDatabase.getReference("Users")
                .child(currentUserId)
                .child("likedBooks")
                .child(post.getPostId());

        likedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isLiked = snapshot.exists();
                updateLikeButtonAppearance(binding, isLiked);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostCardLikeAdapter", "Error checking like status: " + error.getMessage());
            }
        });
    }

    private void updateLikeButtonAppearance(PostCardLikeBinding binding, boolean isLiked) {
        if (isLiked) {
            binding.likeButton.setIconResource(R.drawable.ic_heart_filled);
            binding.likeButton.setText("Liked");
            binding.likeButton.setBackgroundTintList(ContextCompat.getColorStateList(binding.getRoot().getContext(), R.color.light_blue));
            binding.likeButton.setEnabled(false); // Disable button when already liked
        } else {
            binding.likeButton.setIconResource(R.drawable.ic_heart);
            binding.likeButton.setText("Like");
            binding.likeButton.setBackgroundTintList(ContextCompat.getColorStateList(binding.getRoot().getContext(), R.color.light_blue2));
            binding.likeButton.setEnabled(true); // Enable button when not liked
        }
    }

    private void loadUserData(PostCardLikeBinding binding, String userId, Context context) {
        if (userId == null) {
            setDefaultUserData(binding);
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    // Try multiple possible phone field names
                    String phone = snapshot.child("phone").getValue(String.class);
                    if (phone == null || phone.isEmpty()) {
                        phone = snapshot.child("phoneNumber").getValue(String.class);
                    }
                    if (phone == null || phone.isEmpty()) {
                        phone = snapshot.child("userPhone").getValue(String.class);
                    }

                    Log.d("PostCardLikeAdapter", "User data - Username: " + username + ", Phone: " + phone);

                    // Set username
                    binding.userName.setText(username != null ? username : "Book Seller");

                    // Set phone if available
                    if (phone != null && !phone.isEmpty()) {
                        binding.userPhone.setText(phone);
                        Log.d("PostCardLikeAdapter", "User phone set to: " + phone);
                    } else {
                        binding.userPhone.setText("No phone");
                        Log.d("PostCardLikeAdapter", "No phone found for user");
                    }

                    // Load profile image
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(context)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.placeholder)
                                .error(R.drawable.placeholder)
                                .centerCrop()
                                .into(binding.userImage);
                        Log.d("PostCardLikeAdapter", "User image loaded: " + profileImageUrl);
                    } else {
                        binding.userImage.setImageResource(R.drawable.placeholder);
                        Log.d("PostCardLikeAdapter", "Using placeholder user image");
                    }
                } else {
                    Log.e("PostCardLikeAdapter", "User data not found for ID: " + userId);
                    setDefaultUserData(binding);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostCardLikeAdapter", "Error loading user data: " + error.getMessage());
                setDefaultUserData(binding);
            }
        });
    }

    private void setDefaultUserData(PostCardLikeBinding binding) {
        binding.userName.setText("Book Seller");
        binding.userPhone.setText("No phone");
        binding.userImage.setImageResource(R.drawable.placeholder);
        Log.d("PostCardLikeAdapter", "Default user data set");
    }

    @Override
    public int getItemCount() {
        return postCards.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setPostCards(List<PostCard> newCards) {
        postCards.clear();
        if (newCards != null) {
            postCards.addAll(newCards);
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        PostCardLikeBinding binding;

        public ViewHolder(@NonNull PostCardLikeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(PostCard postCard);
        void onLikeButtonClick(PostCard postCard);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
}