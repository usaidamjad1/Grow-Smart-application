package com.usaid.growsmartapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PlantDiseaseResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_disease_result);

        String aiResponse = getIntent().getStringExtra("AI_RESPONSE");
        String imagePath = getIntent().getStringExtra("IMAGE_PATH");

        // 🔥 FIX: Load image from file path
        if (imagePath != null) {
            try {
                java.io.File imageFile = new java.io.File(imagePath);
                if (imageFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    ImageView plantImageResult = findViewById(R.id.plantImageResult);
                    plantImageResult.setImageBitmap(bitmap);
                }
            } catch (Exception e) {
                android.util.Log.e("PlantResult", "Error loading image: " + e.getMessage());
            }
        }

        // Parse and display results
        TextView tvPlantName = findViewById(R.id.tvPlantName);
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvDisease = findViewById(R.id.tvDisease);
        TextView tvSeverity = findViewById(R.id.tvSeverity);
        TextView tvSymptoms = findViewById(R.id.tvSymptoms);
        TextView tvTreatment = findViewById(R.id.tvTreatment);
        TextView tvPrevention = findViewById(R.id.tvPrevention);

        if (aiResponse != null) {
            tvPlantName.setText(extractValue(aiResponse, "PLANT:"));

            String status = extractValue(aiResponse, "STATUS:");
            tvStatus.setText(status);

            // Change status color
            if (status.toLowerCase().contains("healthy")) {
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }

            tvDisease.setText(extractValue(aiResponse, "DISEASE:"));
            tvSeverity.setText(extractValue(aiResponse, "SEVERITY:"));
            tvSymptoms.setText(extractValue(aiResponse, "SYMPTOMS:"));
            tvTreatment.setText(extractValue(aiResponse, "TREATMENT:"));
            tvPrevention.setText(extractValue(aiResponse, "PREVENTION:"));
        }

        findViewById(R.id.btnBackToScan).setOnClickListener(v -> finish());
        findViewById(R.id.btnDoneScan).setOnClickListener(v -> {
            finish();
            // Go back to home
        });
    }

    private String extractValue(String text, String key) {
        if (text == null || !text.contains(key)) return "N/A";

        try {
            String temp = text.split(key)[1];
            String[] stopKeys = {"PLANT:", "STATUS:", "DISEASE:", "SEVERITY:", "SYMPTOMS:", "TREATMENT:", "PREVENTION:"};

            for (String s : stopKeys) {
                if (temp.contains(s)) temp = temp.split(s)[0];
            }

            return temp.replace("*", "")
                    .replace("#", "")
                    .replace("[", "")
                    .replace("]", "")
                    .trim();
        } catch (Exception e) {
            return "N/A";
        }
    }
}