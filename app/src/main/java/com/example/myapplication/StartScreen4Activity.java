package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.databinding.ActivityStartScreen4Binding;

public class StartScreen4Activity extends AppCompatActivity {

    ActivityStartScreen4Binding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);


        binding = ActivityStartScreen4Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.prev.setOnClickListener(view -> {
            Intent intent = new Intent(StartScreen4Activity.this, StartScreen3Activity.class);
            startActivity(intent);
        });

        binding.textView3.setOnClickListener(v->{
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });
    }
}