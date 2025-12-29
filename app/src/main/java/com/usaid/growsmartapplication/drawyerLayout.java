package com.usaid.growsmartapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class drawyerLayout extends AppCompatActivity {

    private static final String TAG = "DrawerDebug";
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;

    private DrawerLayout drawer;
    private TextView tvRecentCrop, tvRecentTime;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔥 FIX: Set edge-to-edge BEFORE setContentView
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_drawyer_layout);

        Log.d(TAG, "onCreate started");

        // Initialize Views
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        tvRecentCrop = findViewById(R.id.tvRecentCropName);
        tvRecentTime = findViewById(R.id.tvRecentTime);
        ImageButton menuBtn = findViewById(R.id.MenuBtn);

        // Check if NavigationView was found
        if (navigationView == null) {
            Log.e(TAG, "❌ NavigationView is NULL!");
            Toast.makeText(this, "ERROR: NavigationView not found", Toast.LENGTH_LONG).show();
            return;
        }
        Log.d(TAG, "✅ NavigationView found");

        // Setup User Header
        setupUserHeader();

        // Menu Button
        menuBtn.setOnClickListener(v -> {
            Log.d(TAG, "Menu button clicked");
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                drawer.openDrawer(GravityCompat.START);
            }
        });

        // Home Buttons
        findViewById(R.id.btnStartTest).setOnClickListener(v -> {
            Log.d(TAG, "Start Test clicked");
            startActivity(new Intent(this, cropRecomndationPage.class));
        });

        findViewById(R.id.scanPlantBtn).setOnClickListener(v -> {
            Intent intent = new Intent(drawyerLayout.this, PlantScanActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.historyBtn).setOnClickListener(v -> {
            Log.d(TAG, "History clicked");
            startActivity(new Intent(this, HistoryActivity.class));
        });

        // 🔥 METHOD 1: Using setNavigationItemSelectedListener
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Log.d(TAG, "🎯 onNavigationItemSelected FIRED!");
                Log.d(TAG, "Item ID: " + item.getItemId());
                Log.d(TAG, "Item Title: " + item.getTitle());

                Toast.makeText(drawyerLayout.this, "Clicked: " + item.getTitle(), Toast.LENGTH_SHORT).show();

                int id = item.getItemId();

                if (id == R.id.nav_settings) {
                    Log.d(TAG, "Opening Settings");
                    startActivity(new Intent(drawyerLayout.this, setting_menuPage.class));
                } else if (id == R.id.nav_help) {
                    Log.d(TAG, "Opening Help");
                    startActivity(new Intent(drawyerLayout.this, HelpSupportActivity.class));
                } else if (id == R.id.nav_about) {
                    Log.d(TAG, "Opening About");
                    startActivity(new Intent(drawyerLayout.this, AboutAppActivity.class));
                } else if (id == R.id.nav_logout) {
                    Log.d(TAG, "Logging out");
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(drawyerLayout.this, LoginPage.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Log.e(TAG, "❌ Unknown menu item: " + id);
                }

                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        // 🔥 METHOD 2: Backup - Direct click on menu items (nuclear option)
        navigationView.getMenu().findItem(R.id.nav_settings).setOnMenuItemClickListener(item -> {
            Log.d(TAG, "🚀 DIRECT CLICK: Settings");
            Toast.makeText(this, "Direct click: Settings", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(drawyerLayout.this, setting_menuPage.class));
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });

        navigationView.getMenu().findItem(R.id.nav_logout).setOnMenuItemClickListener(item -> {
            Log.d(TAG, "🚀 DIRECT CLICK: Logout");
            Toast.makeText(this, "Direct click: Logout", Toast.LENGTH_SHORT).show();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(drawyerLayout.this, LoginPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        });

        Log.d(TAG, "All listeners set up successfully");

        loadRecentFromCloud();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void loadRecentFromCloud() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("History").child(uid);
        ref.limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvRecentCrop.setText("No History");
                    return;
                }
                for (DataSnapshot child : snapshot.getChildren()) {
                    HistoryModel latest = child.getValue(HistoryModel.class);
                    if (latest != null) {
                        tvRecentCrop.setText(latest.cropName);
                        tvRecentTime.setText(latest.date);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        });
    }

    private void setupUserHeader() {
        try {
            View headerView = navigationView.getHeaderView(0);
            TextView name = headerView.findViewById(R.id.textView);
            TextView email = headerView.findViewById(R.id.textView2);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                email.setText(user.getEmail());
                if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                    name.setText(user.getDisplayName());
                } else {
                    name.setText("User");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up header: " + e.getMessage());
        }
    }

    private void checkPermissionAndOpenFile() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }
}