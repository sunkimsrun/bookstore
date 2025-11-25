package com.example.myapplication.adapter;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.PostCard;

import java.util.ArrayList;
import java.util.List;

public class PostCardRecommendationAdapter extends RecyclerView.Adapter<PostCardRecommendationAdapter.ViewHolder> {

    private List<PostCard> postCards;
    private OnItemClickListener onItemClickListener;
    private final Handler sliderHandler = new Handler();
    private boolean isAutoSliding = false;
    private int currentPosition = 0;

    public PostCardRecommendationAdapter() {
        this.postCards = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PostCard post = postCards.get(position);

        if (post == null) return;

        // Load book cover image
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(post.getImageUrl())
                    .placeholder(R.drawable.code1)
                    .error(R.drawable.code1)
                    .into(holder.bgImage);
        } else {
            holder.bgImage.setImageResource(R.drawable.code1);
        }

        // Set book details
        holder.tvTitle.setText(post.getTitle() != null ? post.getTitle() : "Explore our Book");
        holder.tvGenre.setText(post.getGenre() != null ? post.getGenre() : "Genre Type");

        // Set click listeners
        holder.btnExplore.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onExploreClick(post);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postCards.size();
    }

    public void setPostCards(List<PostCard> postCards) {
        this.postCards = postCards != null ? postCards : new ArrayList<>();
        notifyDataSetChanged();

        // Start auto-slide when data is set and there are multiple items
        if (postCards != null && postCards.size() > 1) {
            startAutoSlide();
        }
    }

    private void startAutoSlide() {
        if (isAutoSliding) return;

        isAutoSliding = true;
        sliderHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (postCards.size() > 1) {
                    showNextCard();
                    sliderHandler.postDelayed(this, 3000); // 3 seconds interval
                }
            }
        }, 3000);
    }

    private void showNextCard() {
        if (postCards.isEmpty()) return;

        currentPosition = (currentPosition + 1) % postCards.size();

        // This will trigger the RecyclerView to smoothly scroll to the next position
        if (getRecyclerView() != null) {
            getRecyclerView().smoothScrollToPosition(currentPosition);
        }
    }

    // Helper method to get the RecyclerView this adapter is attached to
    private RecyclerView getRecyclerView() {
        // This is a simplified approach - in a real scenario, you might want to pass the RecyclerView reference
        return null; // You'll need to implement this based on your setup
    }

    public void stopAutoSlide() {
        isAutoSliding = false;
        sliderHandler.removeCallbacksAndMessages(null);
    }

    public void resumeAutoSlide() {
        if (postCards != null && postCards.size() > 1 && !isAutoSliding) {
            startAutoSlide();
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        // Start auto-slide when attached to RecyclerView
        if (postCards != null && postCards.size() > 1) {
            startAutoSlide();
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // Stop auto-slide when detached to prevent memory leaks
        stopAutoSlide();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView bgImage;
        TextView tvTitle;
        TextView tvGenre;
        Button btnExplore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            bgImage = itemView.findViewById(R.id.bgImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvGenre = itemView.findViewById(R.id.tvGenre);
            btnExplore = itemView.findViewById(R.id.btnExplore);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(PostCard postCard);
        void onExploreClick(PostCard postCard);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
}