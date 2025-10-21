package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.databinding.ActivityStartScreen2Binding;

public class StartScreen2Activity extends AppCompatActivity {

    ActivityStartScreen2Binding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);



        binding = ActivityStartScreen2Binding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        binding.selectedEnglish.setOnClickListener(view -> {
            Intent intent = new Intent(StartScreen2Activity.this, StartScreen3Activity.class);
            startActivity(intent);
        });
    }
}