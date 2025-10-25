package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();


        // Event Listeners
        binding.tvSkipForNow.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, HomeActivity.class)));
        binding.tvForgetPassword.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgetPasswordActivity.class)));
        binding.tvRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        binding.btnLogIn.setOnClickListener(v -> handleEmailLogin());

        setupPasswordToggle(binding.etPassword, true);

    }

    private void handleEmailLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Please enter email!");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Invalid email format!");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required!");
            return;
        }

        loginWithEmail(email, password);
    }

    private void loginWithEmail(String email, String password) {
        binding.loadingBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    binding.loadingBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Login failed: " +
                                Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPasswordToggle(EditText editText, boolean isMainPassword) {
        editText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (editText.getRight() - editText.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                    if (isMainPassword) {
                        isPasswordVisible = !isPasswordVisible;
                        updatePasswordVisibility(editText, isPasswordVisible);
                    }
                    return true;
                }
            }
            return false;
        });
    }

    private void updatePasswordVisibility(EditText editText, boolean visible) {
        if (visible) {
            // Show password
            editText.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                            android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            );
            editText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ico_lock, 0, R.drawable.ico_eye_closed, 0);
        } else {
            // Hide password
            editText.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            );
            editText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ico_lock, 0, R.drawable.ico_eye_open, 0);
        }

        // Keep cursor at end
        editText.setSelection(editText.getText().length());
    }

}