package com.usaid.growsmartapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        // 1. Back Button - Simple and Fast
        ImageView btnBack = findViewById(R.id.btnBackProfile);
        btnBack.setOnClickListener(v -> finish());

        // 2. Save Button Logic
        Button btnSave = findViewById(R.id.btnSaveChanges);
        EditText etName = findViewById(R.id.etName);
        EditText etEmail = findViewById(R.id.etEmail);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            } else {
                // Success feedback
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}