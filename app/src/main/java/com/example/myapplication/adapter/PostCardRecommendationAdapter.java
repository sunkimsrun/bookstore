package com.example.myapplication.adapter;

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