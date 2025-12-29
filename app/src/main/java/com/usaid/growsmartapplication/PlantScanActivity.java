
package com.usaid.growsmartapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.usaid.growsmartapplication.PlantDiseaseResultActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class PlantScanActivity extends AppCompatActivity {

    private static final String TAG = "PlantScanActivity";
    private static final int CAMERA_PERMISSION_CODE = 100;

    private final String GEMINI_API_KEY = "AIzaSyA0epouq2P3DpFGcLfYsg2r48X6tYEsUUE";
    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=" + GEMINI_API_KEY;

    private ImageView plantImage, placeholderIcon;
    private TextView placeholderText;
    private View loadingOverlay, placeholderContainer;
    private Uri imageUri;
    private Bitmap capturedBitmap;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_scan);

        // Initialize views
        plantImage = findViewById(R.id.plantImage);
        placeholderIcon = findViewById(R.id.placeholderIcon);
        placeholderText = findViewById(R.id.placeholderText);
        placeholderContainer = findViewById(R.id.placeholderContainer);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAnalyze).setOnClickListener(v -> analyzeImage());

        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        capturedBitmap = (Bitmap) result.getData().getExtras().get("data");
                        displayImage(capturedBitmap);
                    }
                }
        );

        // Gallery launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        try {
                            capturedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            displayImage(capturedBitmap);
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading image: " + e.getMessage());
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Image container click - show camera/gallery options
        findViewById(R.id.imageContainer).setOnClickListener(v -> showImageSourceDialog());
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(new String[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermissionAndOpen();
            } else {
                openGallery();
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(intent);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayImage(Bitmap bitmap) {
        placeholderContainer.setVisibility(View.GONE);
        plantImage.setVisibility(View.VISIBLE);
        plantImage.setImageBitmap(bitmap);
    }

    private void analyzeImage() {
        if (capturedBitmap == null) {
            Toast.makeText(this, "Please capture or select a plant image first", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingOverlay.setVisibility(View.VISIBLE);
        Log.d(TAG, "🔬 Starting plant disease analysis...");

        String prompt = "You are an expert plant pathologist. Analyze this image carefully.\n\n" +
                "CRITICAL RULES:\n" +
                "1. If this is NOT a plant/leaf image, respond ONLY with: 'NOT_A_PLANT'\n" +
                "2. If it IS a plant, provide analysis in this EXACT format:\n\n" +
                "PLANT: [Plant name]\n" +
                "STATUS: [Healthy/Diseased]\n" +
                "DISEASE: [Disease name or 'None']\n" +
                "SEVERITY: [Mild/Moderate/Severe or 'N/A']\n" +
                "SYMPTOMS: [Brief symptoms]\n" +
                "TREATMENT: [Treatment recommendation]\n" +
                "PREVENTION: [Prevention tips]\n\n" +
                "Keep responses SHORT and CLEAR. NO asterisks, NO bold text.";

        try {
            String base64Image = encodeImageToBase64(capturedBitmap);

            JSONObject jsonBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            contentObj.put("role", "user");
            JSONArray parts = new JSONArray();

            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);
            parts.put(textPart);

            JSONObject imagePart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mime_type", "image/jpeg");
            inlineData.put("data", base64Image);
            imagePart.put("inline_data", inlineData);
            parts.put(imagePart);

            contentObj.put("parts", parts);
            contents.put(contentObj);
            jsonBody.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.2);
            generationConfig.put("maxOutputTokens", 500);
            jsonBody.put("generationConfig", generationConfig);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, GEMINI_URL, jsonBody,
                    response -> {
                        loadingOverlay.setVisibility(View.GONE);
                        Log.d(TAG, "✅ AI analysis complete");
                        try {
                            String aiResponse = response.getJSONArray("candidates")
                                    .getJSONObject(0).getJSONObject("content")
                                    .getJSONArray("parts").getJSONObject(0).getString("text");

                            Log.d(TAG, "📝 AI Response: " + aiResponse);

                            if (aiResponse.contains("NOT_A_PLANT")) {
                                showNotPlantDialog();
                            } else {
                                showResultPage(aiResponse);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Parse error: " + e.getMessage());
                            Toast.makeText(this, "Error processing result", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        loadingOverlay.setVisibility(View.GONE);
                        Log.e(TAG, "❌ API error: " + error.toString());
                        Toast.makeText(this, "Analysis failed. Check internet connection.", Toast.LENGTH_LONG).show();
                    }
            );

            request.setRetryPolicy(new DefaultRetryPolicy(60000, 1, 1f));
            Volley.newRequestQueue(this).add(request);

        } catch (Exception e) {
            loadingOverlay.setVisibility(View.GONE);
            Log.e(TAG, "❌ Exception: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    private void showNotPlantDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Not a Plant Image")
                .setMessage("This doesn't appear to be a plant or leaf image. Please take a clear photo of a plant leaf for disease detection.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Take Another Photo", (dialog, which) -> showImageSourceDialog())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showResultPage(String aiResponse) {
        // 🔥 FIX: Save image to file instead of passing in Intent
        try {
            // Save bitmap to cache directory
            java.io.File cacheDir = getCacheDir();
            java.io.File imageFile = new java.io.File(cacheDir, "plant_scan_temp.jpg");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(imageFile);
            capturedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();

            Log.d(TAG, "✅ Image saved to: " + imageFile.getAbsolutePath());

            Intent intent = new Intent(this, PlantDiseaseResultActivity.class);
            intent.putExtra("AI_RESPONSE", aiResponse);
            intent.putExtra("IMAGE_PATH", imageFile.getAbsolutePath());
            startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error saving image: " + e.getMessage());
            Toast.makeText(this, "Error displaying result", Toast.LENGTH_SHORT).show();
        }
    }
}