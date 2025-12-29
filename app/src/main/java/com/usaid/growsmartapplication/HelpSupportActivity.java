package com.usaid.growsmartapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HelpSupportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        // 1. Back Button
        findViewById(R.id.btnBackHelp).setOnClickListener(v -> finish());

        // 2. Setup Spinner Topics
        Spinner spinner = findViewById(R.id.spinnerTopic);
        String[] topics = {"General Inquiry", "Bug Report", "Account Issue", "Payment Help"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, topics);
        spinner.setAdapter(adapter);

        // 3. Send Button Action
        findViewById(R.id.btnSendMessage).setOnClickListener(v -> {
            EditText message = findViewById(R.id.etMessage);
            if (message.getText().toString().isEmpty()) {
                Toast.makeText(this, "Please write a message", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Support ticket sent!", Toast.LENGTH_LONG).show();
                finish(); // Close after sending
            }
        });
    }
}