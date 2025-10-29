package com.example.myapplication.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentContactBinding;

public class ContactFragment extends Fragment {

    private View[] images;
    private View[] dots;
    private int currentIndex = 0;
    private final Handler sliderHandler = new Handler();

    private FragmentContactBinding binding;


    public ContactFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contact, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding = FragmentContactBinding.bind(view);

        binding.tvEmail.setOnClickListener(v -> sendEmail());
        binding.tvCall.setOnClickListener(v -> dialPhoneNumber());
        binding.tvLocation.setOnClickListener(v -> openMap());


        // Image Views
        images = new View[]{
                view.findViewById(R.id.contact_img1),
                view.findViewById(R.id.contact_img2),
                view.findViewById(R.id.contact_img3),
                view.findViewById(R.id.contact_img4),
                view.findViewById(R.id.contact_img5)
        };

        // Dot Views
        dots = new View[]{
                view.findViewById(R.id.dot_1),
                view.findViewById(R.id.dot_2),
                view.findViewById(R.id.dot_3),
                view.findViewById(R.id.dot_4),
                view.findViewById(R.id.dot_5)
        };

        startAutoSlide();
    }

    private void startAutoSlide() {
        sliderHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showNextImage();
                sliderHandler.postDelayed(this, 2000); // 2 seconds
            }
        }, 2000);
    }

    private void showNextImage() {
        for (int i = 0; i < images.length; i++) {
            if (i == currentIndex) fadeView(images[i], View.VISIBLE);
            else fadeView(images[i], View.GONE);

            dots[i].setBackgroundResource(i == currentIndex ?
                    R.drawable.dot_active : R.drawable.dot_inactive);
        }

        currentIndex = (currentIndex + 1) % images.length;
    }

    private void fadeView(View view, int visibility) {
        AlphaAnimation fade = new AlphaAnimation(0.3f, 1f);
        fade.setDuration(500);
        view.startAnimation(fade);
        view.setVisibility(visibility);
    }






//    ក្រោមនេះជា ការបង្ហាញពី Location, Phone, Email

    private static final String MAP_LOCATION_URI = "https://maps.app.goo.gl/zFmuSFf5TUCyji3Y8";
    private static final String CONTACT_PHONE_URI = "tel:+85511880778";
    private static final String CONTACT_EMAIL_URI = "kim@gmail.com";


    private void openMap() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MAP_LOCATION_URI));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No application can open the map link.", Toast.LENGTH_SHORT).show();
        }
    }

    private void dialPhoneNumber() {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(CONTACT_PHONE_URI));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No application can handle this action.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(CONTACT_EMAIL_URI));
        try {
            startActivity(Intent.createChooser(intent, "Send email using..."));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }
}
