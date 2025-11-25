package com.example.myapplication.fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.HomeActivity;
import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentPostBookBinding;
import com.example.myapplication.model.PostCard;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class PostBookFragment extends Fragment {

    private static final int IMAGE_PICK_CODE = 1000;
    HomeActivity homeActivity;
    private FragmentPostBookBinding binding;
    private Uri imageUri;
    private String selectedGenre = "";

    public PostBookFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPostBookBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            String email = currentUser.getEmail();
            binding.editText.setText(email);

            // Pre-fill phone number if available in user profile
            prefillUserData(currentUser.getUid());
        }

        homeActivity = (HomeActivity) getActivity();

        // --- Spinner setup ---
        setupGenreSpinner();

        // --- Phone prefix formatting ---
        binding.editPhone.setText("+855 ");
        binding.editPhone.setSelection(binding.editPhone.getText().length());
        binding.editPhone.addTextChangedListener(new TextWatcher() {
            boolean isFormatting;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;

                String input = s.toString();
                if (!input.startsWith("+855 ")) {
                    binding.editPhone.setText("+855 ");
                    binding.editPhone.setSelection(binding.editPhone.getText().length());
                } else if (input.length() > 6) {
                    char firstDigit = input.charAt(6);
                    if (firstDigit == '0') {
                        Toast.makeText(requireContext(), "First number after +855 cannot be 0", Toast.LENGTH_SHORT).show();
                        binding.editPhone.setText("+855 ");
                        binding.editPhone.setSelection(binding.editPhone.getText().length());
                    }
                }
                isFormatting = false;
            }
        });

        // --- Policy Checkbox ---
        binding.checkBoxPolicy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.btnCreate.setEnabled(isChecked);
            binding.btnCreate.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(),
                    isChecked ? R.color.blue : android.R.color.darker_gray));
        });

        // --- Image Picker ---
        binding.inputImage.setOnClickListener(v -> pickImageFromGallery());
        binding.selectImageOverlay.setOnClickListener(v -> pickImageFromGallery());

        // --- Date & Time Picker ---
        binding.tvSelectedDate.setOnClickListener(v -> showDatePicker());
        binding.tvSelectedTime.setOnClickListener(v -> showTimePicker());

        // --- Submit Button ---
        binding.btnCreate.setOnClickListener(v -> submitPost());

        return view;
    }

    private void prefillUserData(String userId) {
        // You can add code here to prefill user data from Firebase if needed
        // For example, if you store user phone numbers in the database
    }

    private void setupGenreSpinner() {
        String[] genres = {"Book Genre", "Japanese", "Mystery", "Comedy", "Historical", "Biography", "Horror", "Fantasy"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, genres);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerGenre.setAdapter(adapter);

        binding.spinnerGenre.setSelection(0); // default: Book Genre
        binding.spinnerGenre.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    selectedGenre = parent.getItemAtPosition(position).toString();
                } else {
                    selectedGenre = "";
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                    String formattedDay = String.format(Locale.getDefault(), "%02d", dayOfMonth);
                    String date = formattedDay + " " + months[month] + ", " + year;
                    binding.tvSelectedDate.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    binding.tvSelectedTime.setText(time);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);
        timePickerDialog.show();
    }

    private void submitPost() {
        String title = binding.inputTitle.getText().toString().trim();
        String info = binding.inputInformation.getText().toString().trim();
        String email = binding.editText.getText().toString().trim();
        String phone = binding.editPhone.getText().toString().trim();
        String date = binding.tvSelectedDate.getText().toString().trim();
        String time = binding.tvSelectedTime.getText().toString().trim();
        String priceInput = binding.inputprice.getText().toString().trim();
        String price = priceInput.isEmpty() ? "Free" : priceInput;

        Log.d("PostBookFragment", "Submitting post - Genre: " + selectedGenre);
        Log.d("PostBookFragment", "Date: " + date + ", Time: " + time);
        Log.d("PostBookFragment", "Image URI: " + (imageUri != null ? "Set" : "Null"));

        // Validation
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Please enter book title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (info.isEmpty()) {
            Toast.makeText(getContext(), "Please describe your book", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedGenre.isEmpty() || selectedGenre.equals("Book Genre")) {
            Toast.makeText(getContext(), "Please select a book genre", Toast.LENGTH_SHORT).show();
            return;
        }

        if (date.equals("Select Date")) {
            Toast.makeText(getContext(), "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (time.equals("Select Time")) {
            Toast.makeText(getContext(), "Please select a time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri == null) {
            Toast.makeText(getContext(), "Please select a book image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.length() <= 5) { // Only "+855 " is there
            Toast.makeText(getContext(), "Please enter your phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressBar();

        String filename = UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("book_images/" + filename);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        uploadDataToDatabase(title, info, email, phone, date, time, price, uri.toString(), selectedGenre);
                    });
                })
                .addOnFailureListener(e -> {
                    hideProgressBar();
                    Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("PostBookFragment", "Image upload failed: " + e.getMessage());
                });
    }

    private void uploadDataToDatabase(String title, String info, String email, String phone,
                                      String date, String time, String price, String imageUrl, String genre) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            hideProgressBar();
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        String generatedPostId = FirebaseDatabase.getInstance().getReference("books").push().getKey();
        if (generatedPostId == null) {
            generatedPostId = UUID.randomUUID().toString();
        }

        final String finalPostId = generatedPostId;

        PostCard postCard = new PostCard();
        postCard.setTitle(title);
        postCard.setInformation(info);
        postCard.setEmail(email);
        postCard.setPhone(phone);
        postCard.setDate(date);
        postCard.setPostTime(time);
        postCard.setImageUrl(imageUrl);
        postCard.setUserId(userId);
        postCard.setPostId(finalPostId);
        postCard.setGenre(genre);
        postCard.setPrice(price);
        postCard.setStatus("Available");
        postCard.setLocation("RUPP");

        String createdDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        postCard.setCreatedDate(createdDate);

        DatabaseReference bookRef = FirebaseDatabase.getInstance().getReference("books").child(finalPostId);

        bookRef.setValue(postCard)
                .addOnSuccessListener(aVoid -> {
                    hideProgressBar();
                    Log.d("PostBookFragment", "Book posted successfully with ID: " + finalPostId);

                    // Reset the form
                    resetForm();

                    // Navigate to detail screen
                    navigateToDetailScreen(finalPostId);

                    Toast.makeText(getContext(), "Book posted successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    hideProgressBar();
                    Log.e("PostBookFragment", "Database upload failed: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to post book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void resetForm() {
        binding.inputTitle.setText("");
        binding.inputInformation.setText("");
        binding.editPhone.setText("+855 ");
        binding.inputprice.setText("");
        binding.tvSelectedDate.setText("Select Date");
        binding.tvSelectedTime.setText("Select Time");
        binding.spinnerGenre.setSelection(0); // Reset to "Book Genre"
        binding.inputImage.setImageResource(R.drawable.bg_input_image);
        binding.inputImage.setBackgroundResource(R.drawable.bg_input_edit_text);
        binding.selectImageOverlay.setVisibility(View.VISIBLE);
        binding.checkBoxPolicy.setChecked(false);
        imageUri = null;
        selectedGenre = "";

        Log.d("PostBookFragment", "Form reset successfully");
    }

    private void navigateToDetailScreen(String postId) {
        try {
            if (getActivity() instanceof HomeActivity) {
                HomeActivity homeActivity = (HomeActivity) getActivity();

                // Create PostDetailFragment instance
                PostDetailFragment postDetailFragment = new PostDetailFragment();

                // Create bundle with postId
                Bundle bundle = new Bundle();
                bundle.putString("postId", postId);
                bundle.putString("back", "Home"); // Set back destination to Home

                postDetailFragment.setArguments(bundle);

                // Navigate to detail fragment using your existing LoadFragment method
                homeActivity.LoadFragment(postDetailFragment);
                Log.d("PostBookFragment", "Navigation to detail screen initiated for post: " + postId);
            } else {
                Log.e("PostBookFragment", "Activity is not HomeActivity, cannot navigate");
            }
        } catch (Exception e) {
            Log.e("PostBookFragment", "Error navigating to detail screen: " + e.getMessage());
            Toast.makeText(getContext(), "Error navigating to book details", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_CODE && data != null) {
                Uri sourceUri = data.getData();
                if (sourceUri != null) {
                    Uri destinationUri = Uri.fromFile(new File(requireContext().getCacheDir(), "cropped_" + UUID.randomUUID() + ".jpg"));

                    UCrop.Options options = new UCrop.Options();
                    options.setCircleDimmedLayer(false);
                    options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
                    options.setCompressionQuality(90);

                    // Portrait crop for book images
                    UCrop.of(sourceUri, destinationUri)
                            .withAspectRatio(3, 4)  // width : height
                            .withMaxResultSize(1080, 1440)
                            .withOptions(options)
                            .start(requireContext(), this);
                }
            } else if (requestCode == UCrop.REQUEST_CROP) {
                final Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    imageUri = resultUri;
                    binding.inputImage.setImageURI(imageUri);
                    binding.inputImage.setBackground(null);
                    binding.selectImageOverlay.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Image selected", Toast.LENGTH_SHORT).show();
                    Log.d("PostBookFragment", "Image cropped and set successfully");
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Toast.makeText(getContext(), "Crop error: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PostBookFragment", "Crop error: " + cropError.getMessage());
            }
        }
    }

    private void showProgressBar() {
        if (homeActivity != null) {
            homeActivity.showProgressBar();
            Log.d("PostBookFragment", "Progress bar shown");
        }
    }

    private void hideProgressBar() {
        if (homeActivity != null) {
            homeActivity.hideProgressBar();
            Log.d("PostBookFragment", "Progress bar hidden");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}