package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.databinding.ActivityHomeBinding;
import com.example.myapplication.fragment.AccountFragment;
import com.example.myapplication.fragment.AllBookFragment;
import com.example.myapplication.fragment.ContactFragment;
import com.example.myapplication.fragment.HomeFragment;
import com.example.myapplication.fragment.PrivacyPolicyFragment;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    public ActivityHomeBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        binding.navigationView.getMenu().findItem(R.id.nav_signout).setVisible(mAuth.getCurrentUser() != null);

        binding.btnHamburger.setOnClickListener(view -> {
            if (!binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.openDrawer(GravityCompat.START);
            } else {
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
        });

        View headerView = binding.navigationView.getHeaderView(0);
        View btnClose = headerView.findViewById(R.id.btnDrawerClose);
        btnClose.setOnClickListener(v -> binding.drawerLayout.closeDrawer(GravityCompat.START));

        binding.navigationView.setNavigationItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();

            if (itemId == R.id.nav_home) {
                LoadFragment(new HomeFragment());
            } else if (itemId == R.id.nav_account) {
                if (mAuth.getCurrentUser() != null) {
                    LoadFragment(new AccountFragment());
                } else {
                    startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                    finish();
                }
            } else if (itemId == R.id.nav_all_book) {
                LoadFragment(new AllBookFragment());
            } else if (itemId == R.id.nav_privacy) {
                LoadFragment(new PrivacyPolicyFragment());
            } else if (itemId == R.id.nav_contact) {
                LoadFragment(new ContactFragment());
            } else if (itemId == R.id.nav_signout) {
                binding.loadingBar.setVisibility(View.VISIBLE);
                mAuth.signOut();
                binding.loadingBar.setVisibility(View.GONE);
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        binding.navigationView.setCheckedItem(R.id.nav_home);
        LoadFragment(new HomeFragment());
    }

    public void LoadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameContainer, fragment)
                .commit();
    }

    public void showProgressBar() {
        binding.loadingBar.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        binding.loadingBar.postDelayed(() -> {
            binding.loadingBar.setVisibility(View.GONE);
        }, 1500);
    }
}