package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.databinding.ActivityStartScreen3Binding;

public class StartScreen3Activity extends AppCompatActivity {

    ActivityStartScreen3Binding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityStartScreen3Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.prev.setOnClickListener(view -> {
            Intent intent = new Intent(StartScreen3Activity.this, StartScreen2Activity.class);
            startActivity(intent);
        });

        binding.textView3.setOnClickListener(view -> {
            Intent intent = new Intent(StartScreen3Activity.this, StartScreen4Activity.class);
            startActivity(intent);
        });

        binding.textView2.setOnClickListener(view -> {
            Intent intent = new Intent(StartScreen3Activity.this, LoginActivity.class);
            startActivity(intent);
        });

    }
}