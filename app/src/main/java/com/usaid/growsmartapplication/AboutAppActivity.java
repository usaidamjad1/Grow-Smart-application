package com.usaid.growsmartapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AboutAppActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_app);

        // 1. Back Button
        findViewById(R.id.btnBackAbout).setOnClickListener(v -> finish());

        // 2. Setup the mini-rows inside About
        setupMiniRow(R.id.rowPrivacy, "Privacy Policy", android.R.drawable.ic_lock_lock);
        setupMiniRow(R.id.rowTerms, "Terms of Service", android.R.drawable.ic_menu_agenda);
        setupMiniRow(R.id.rowWebsite, "Visit Website", android.R.drawable.ic_menu_view);
    }

    private void setupMiniRow(int id, String title, int iconRes) {
        View row = findViewById(id);
        if (row != null) {
            ((TextView)row.findViewById(R.id.rowLabel)).setText(title);
            ((ImageView)row.findViewById(R.id.rowIcon)).setImageResource(iconRes);
            ((ImageView)row.findViewById(R.id.rowIcon)).setColorFilter(getResources().getColor(android.R.color.darker_gray));
        }
    }
}