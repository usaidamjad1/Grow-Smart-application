package com.usaid.growsmartapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class setting_menuPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_menu_page);

        // Back Button
        View backBtn = findViewById(R.id.btnBackAdvice);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        // Setup the Rows
        setupRow(R.id.rowProfile, "Profile Settings", android.R.drawable.ic_menu_myplaces);
        setupRow(R.id.rowNotifications, "Notifications", android.R.drawable.ic_lock_silent_mode_off);
        setupRow(R.id.rowHelp, "Help & Support", android.R.drawable.ic_menu_help);
        setupRow(R.id.rowAbout, "About App", android.R.drawable.ic_dialog_info);

        // Click Listeners for each row
        findViewById(R.id.rowProfile).setOnClickListener(v -> {
            Intent intent = new Intent(setting_menuPage.this, ProfileSettingsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.rowNotifications).setOnClickListener(v -> {
            Intent intent = new Intent(setting_menuPage.this, NotificationsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.rowHelp).setOnClickListener(v -> {
            Intent intent = new Intent(setting_menuPage.this, HelpSupportActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.rowAbout).setOnClickListener(v -> {
            Intent intent = new Intent(setting_menuPage.this, AboutAppActivity.class);
            startActivity(intent);
        });

        // 🔥 FIX: Proper logout with Firebase sign out and clear task
        View logout = findViewById(R.id.btnLogout);
        if (logout != null) {
            logout.setOnClickListener(v -> {
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut();

                // Clear all previous activities and go to login
                Intent i = new Intent(setting_menuPage.this, LoginPage.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            });
        }
    }

    private void setupRow(int id, String title, int iconRes) {
        View row = findViewById(id);
        if (row != null) {
            TextView label = row.findViewById(R.id.rowLabel);
            ImageView icon = row.findViewById(R.id.rowIcon);

            if (label != null) label.setText(title);
            if (icon != null) icon.setImageResource(iconRes);
        }
    }
}