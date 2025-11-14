package com.example.myapplication.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.databinding.PostCardBinding;
import com.example.myapplication.model.PostCard;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PostCardAdapter extends RecyclerView.Adapter<PostCardAdapter.ViewHolder> {

    private final List<PostCard> postCards;
    private final String currentUserId;
    private OnItemClickListener onItemClickListener;

    public PostCardAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
        this.postCards = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PostCardBinding binding = PostCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PostCard post = postCards.get(position);

        if (post == null) {
            return;
        }

        Log.d("PostCardAdapter", "Binding post: " + post.getTitle() + ", UserId: " + post.getUserId());

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

        // Always set button text to "View"
        holder.binding.checkbox.setText("View");

        // Load user data
        if (post.getUserId() != null) {
            loadUserData(holder.binding, post.getUserId(), holder.itemView.getContext(), post);
        } else {
            setDefaultUserData(holder.binding, post);
        }

        // Set click listener for the entire card
        holder.itemView.setOnClickListener(view -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(post);
            }
        });

        // Set click listener for the checkbox (View button)
        holder.binding.checkbox.setOnClickListener(view -> {
            if (onItemClickListener != null) {
                onItemClickListener.onCheckboxClick(post);
            }
        });
    }

    private void loadUserData(PostCardBinding binding, String userId, Context context, PostCard post) {
        if (userId == null) {
            setDefaultUserData(binding, post);
            return;
        }

        Log.d("PostCardAdapter", "Loading user data for userId: " + userId);

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    String phone = snapshot.child("phoneNumber").getValue(String.class);

                    Log.d("PostCardAdapter", "User data found - Username: " + username + ", Phone: " + phone);

                    // Set username
                    binding.userName.setText(username != null ? username : "Book Seller");

                    // Set phone - use post phone first, then user phone as fallback
                    String displayPhone = post.getPhone();
                    if (displayPhone == null || displayPhone.isEmpty()) {
                        displayPhone = phone;
                    }
                    if (displayPhone != null && !displayPhone.isEmpty()) {
                        binding.userPhone.setText(displayPhone);
                    } else {
                        binding.userPhone.setText("No phone");
                    }

                    // Load profile image
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(context)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.placeholder)
                                .error(R.drawable.placeholder)
                                .centerCrop()
                                .into(binding.userImage);
                        Log.d("PostCardAdapter", "User image loaded: " + profileImageUrl);
                    } else {
                        binding.userImage.setImageResource(R.drawable.placeholder);
                        Log.d("PostCardAdapter", "Using placeholder user image");
                    }
                } else {
                    Log.e("PostCardAdapter", "User data not found for userId: " + userId);
                    setDefaultUserData(binding, post);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostCardAdapter", "Error loading user data: " + error.getMessage());
                setDefaultUserData(binding, post);
            }
        });
    }

    private void setDefaultUserData(PostCardBinding binding, PostCard post) {
        binding.userName.setText("Book Seller");

        // Use post phone if available
        if (post.getPhone() != null && !post.getPhone().isEmpty()) {
            binding.userPhone.setText(post.getPhone());
        } else {
            binding.userPhone.setText("No phone");
        }

        binding.userImage.setImageResource(R.drawable.placeholder);
        Log.d("PostCardAdapter", "Default user data set");
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
            Log.d("PostCardAdapter", "Set " + newCards.size() + " posts");
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        PostCardBinding binding;

        public ViewHolder(@NonNull PostCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(PostCard postCard);
        void onCheckboxClick(PostCard postCard);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
}