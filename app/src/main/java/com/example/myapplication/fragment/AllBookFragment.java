package com.example.myapplication.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.HomeActivity;
import com.example.myapplication.R;
import com.example.myapplication.adapter.PostCardAdapter;
import com.example.myapplication.model.PostCard;
import com.example.myapplication.repository.IApiCallback;
import com.example.myapplication.repository.PostCardRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllBookFragment extends Fragment {

    private final List<PostCard> fullPostList = new ArrayList<>();
    private final List<PostCard> filteredList = new ArrayList<>();
    private PostCardAdapter adapter;
    private PostCardRepository postCardRepository;

    private View noFoundLayout;
    private TextView textViewCount;
    private RecyclerView recyclerView;
    private ChipGroup chipGroup;
    private boolean isPreselectingGenre = false;

    public AllBookFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        showProgressBar();
        return inflater.inflate(R.layout.fragment_all_book, container, false);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        postCardRepository = new PostCardRepository();

        recyclerView = view.findViewById(R.id.rcv_list_post);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getUid() : null;

        textViewCount = view.findViewById(R.id.textView);
        noFoundLayout = view.findViewById(R.id.noFound);
        noFoundLayout.setVisibility(View.GONE);

        // Use PostCardAdapter for AllBookFragment (without like button)
        adapter = new PostCardAdapter(currentUserId);
        recyclerView.setAdapter(adapter);

        setupAdapterClickListener();

        EditText editText = view.findViewById(R.id.editText);
        chipGroup = view.findViewById(R.id.filterChipGroup);

        // Search functionality
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBySearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Genre filter functionality
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            // Skip if we're programmatically preselecting a genre
            if (isPreselectingGenre) {
                return;
            }

            if (checkedIds.isEmpty()) {
                // No chip selected, show all posts
                filteredList.clear();
                filteredList.addAll(fullPostList);
            } else {
                for (int checkedId : checkedIds) {
                    if (checkedId == R.id.chip_clear_filter) {
                        // Clear all filters
                        isPreselectingGenre = true;
                        group.clearCheck();
                        isPreselectingGenre = false;
                        filteredList.clear();
                        filteredList.addAll(fullPostList);
                        break;
                    } else {
                        // Filter by selected genre
                        String selectedGenre = getGenreFromChipId(checkedId);
                        filterByGenre(selectedGenre);
                    }
                }
            }
            sortPostsByDate();
            updateAdapterAndUI();
        });

        loadAllBooks();
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
        bundle.putString("source", "all_books");
        bundle.putString("back", "View all books");

        fragment.setArguments(bundle);

        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).LoadFragment(fragment);
        }
    }

    private void loadAllBooks() {
        showProgressBar();
        fullPostList.clear();
        filteredList.clear();

        // Load all books using repository
        postCardRepository.getAllPosts("books", new IApiCallback<List<PostCard>>() {
            @Override
            public void onSuccess(List<PostCard> postCards) {
                if (postCards != null) {
                    fullPostList.addAll(postCards);
                    filteredList.addAll(fullPostList);
                    sortPostsByDate();

                    // Check for preselected genre after loading books
                    checkForPreselectedGenre();
                }
                updateAdapterAndUI();
                hideProgressBar();
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressBar();
                updateAdapterAndUI();
                // Show error message if needed
            }
        });
    }

    private void checkForPreselectedGenre() {
        // Check if a genre was preselected from HomeFragment
        Bundle args = getArguments();
        if (args != null && args.containsKey("selected_genre")) {
            String preselectedGenre = args.getString("selected_genre");
            if (preselectedGenre != null && !preselectedGenre.isEmpty()) {
                // Automatically select the corresponding chip
                selectGenreChip(preselectedGenre);
            }
        }
    }

    private void selectGenreChip(String genre) {
        int chipId = getChipIdFromGenre(genre);
        if (chipId != -1 && chipGroup != null) {
            isPreselectingGenre = true;
            chipGroup.check(chipId);
            isPreselectingGenre = false;

            // Manually apply the filter since we're suppressing the chip group listener
            filterByGenre(genre);
            sortPostsByDate();
            updateAdapterAndUI();
        }
    }

    private int getChipIdFromGenre(String genre) {
        switch (genre) {
            case "Japanese": return R.id.chip_japanese;
            case "Comedy": return R.id.chip_comedy;
            case "Mystery": return R.id.chip_mystery;
            case "Historical": return R.id.chip_historical;
            case "Biography": return R.id.chip_biography;
            case "Horror": return R.id.chip_horror;
            case "Fantasy": return R.id.chip_fantasy;
            default: return -1;
        }
    }

    private void sortPostsByDate() {
        filteredList.sort((p1, p2) -> {
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
                Date d1 = p1.getDate() != null ? sdf.parse(p1.getDate()) : new Date(0);
                Date d2 = p2.getDate() != null ? sdf.parse(p2.getDate()) : new Date(0);
                return d2.compareTo(d1); // Most recent first
            } catch (ParseException e) {
                return 0;
            }
        });
    }

    private void filterBySearch(String query) {
        if (query.isEmpty()) {
            filteredList.clear();
            filteredList.addAll(fullPostList);
        } else {
            List<PostCard> tempList = new ArrayList<>();
            for (PostCard post : fullPostList) {
                if ((post.getTitle() != null && post.getTitle().toLowerCase().contains(query.toLowerCase())) ||
                        (post.getInformation() != null && post.getInformation().toLowerCase().contains(query.toLowerCase())) ||
                        (post.getGenre() != null && post.getGenre().toLowerCase().contains(query.toLowerCase()))) {
                    tempList.add(post);
                }
            }
            filteredList.clear();
            filteredList.addAll(tempList);
        }
        sortPostsByDate();
        updateAdapterAndUI();
    }

    private void filterByGenre(String genre) {
        List<PostCard> tempList = new ArrayList<>();
        for (PostCard post : fullPostList) {
            if (post.getGenre() != null && post.getGenre().equalsIgnoreCase(genre)) {
                tempList.add(post);
            }
        }
        filteredList.clear();
        filteredList.addAll(tempList);
        sortPostsByDate();
        updateAdapterAndUI();
    }

    private String getGenreFromChipId(int chipId) {
        if (chipId == R.id.chip_japanese) return "Japanese";
        if (chipId == R.id.chip_comedy) return "Comedy";
        if (chipId == R.id.chip_mystery) return "Mystery";
        if (chipId == R.id.chip_historical) return "Historical";
        if (chipId == R.id.chip_biography) return "Biography";
        if (chipId == R.id.chip_horror) return "Horror";
        if (chipId == R.id.chip_fantasy) return "Fantasy";
        return "";
    }

    private void updateAdapterAndUI() {
        adapter.setPostCards(filteredList);

        if (textViewCount != null) {
            textViewCount.setText(filteredList.size() + " books found");
        }

        if (filteredList.isEmpty()) {
            noFoundLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noFoundLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showProgressBar() {
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).showProgressBar();
        }
    }

    private void hideProgressBar() {
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).hideProgressBar();
        }
    }
}