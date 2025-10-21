package com.example.myapplication;import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Use this for compatibility

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. First, set the content view. This creates the views from your XML.
        setContentView(R.layout.activity_main);

        // 2. Now that the view exists, you can safely find it.
        TextView textTitle = findViewById(R.id.textTitle);

        // 3. Create the gradient shader.
        //    Using ContextCompat is the modern, safer way to get colors.
        Shader shader = new LinearGradient(
                0, 0, 0, textTitle.getTextSize(),
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.light_blue2),
                Shader.TileMode.CLAMP);

        // 4. Apply the shader to the TextView's paint object.
        textTitle.getPaint().setShader(shader);

        // 5. Your existing code to navigate to the next screen after a delay.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, StartScreen2Activity.class));
            finish();
        }, 1500);
    }
}
