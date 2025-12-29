package com.usaid.growsmartapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Hide the action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Delay for 2 seconds then start the next activity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Replace 'MainActivity' with your actual starting page name
            Intent intent = new Intent(SplashActivity.this, LoginPage.class);
            startActivity(intent);
            finish(); // Destroys Splash so user can't go back to it
        }, 2000);
    }
}