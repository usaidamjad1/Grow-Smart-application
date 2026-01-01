package com.usaid.growsmartapplication;


import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import com.usaid.growsmartapplication.
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;

public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";
    private final String GEMINI_API_KEY =
    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=" + GEMINI_API_KEY;

    private TextView tvCrop, tvTip, tvMatch;
    private View rowNPK, rowWater, rowMaturity, loadingOverlay;
    private String rawAiResponse = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        tvCrop = findViewById(R.id.tvRecommendedCrop);
        tvTip = findViewById(R.id.tvAiTip);
        tvMatch = findViewById(R.id.tvConfidence);
        rowNPK = findViewById(R.id.rowNPK);
        rowWater = findViewById(R.id.rowWater);
        rowMaturity = findViewById(R.id.rowMaturity);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        findViewById(R.id.btnBackAdvice).setOnClickListener(v -> finish());
        findViewById(R.id.btnDone).setOnClickListener(v -> finish());

        rowNPK.setOnClickListener(v -> openDetail("NPK", "NPK:"));
        rowWater.setOnClickListener(v -> openDetail("WATER", "WATER:"));
        rowMaturity.setOnClickListener(v -> openDetail("HARVEST", "DAYS:"));

        String prompt = getIntent().getStringExtra("AI_PROMPT");
        String uriStr = getIntent().getStringExtra("IMAGE_URI");

        if (uriStr != null && prompt != null) {
            startGeminiAnalysis(prompt, Uri.parse(uriStr));
        } else {
            Log.e(TAG, "❌ No prompt or image URI received!");
        }
    }

    private void openDetail(String type, String key) {
        if (rawAiResponse.isEmpty()) return;
        Intent intent = new Intent(this, ResultDetailActivity.class);
        intent.putExtra("ADVICE_TYPE", type);
        intent.putExtra("AI_VALUE", extractCleanText(rawAiResponse, key));
        startActivity(intent);
    }

    private void startGeminiAnalysis(String userPrompt, Uri uri) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        Log.d(TAG, "🚀 Starting Gemini analysis...");

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            String base64Image = encodeImageToBase64(bitmap);

            JSONObject jsonBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            contentObj.put("role", "user");
            JSONArray parts = new JSONArray();

            JSONObject textPart = new JSONObject();
            textPart.put("text", userPrompt);
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
            generationConfig.put("temperature", 0.0);
            generationConfig.put("maxOutputTokens", 500);
            jsonBody.put("generationConfig", generationConfig);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, GEMINI_URL, jsonBody,
                    response -> {
                        loadingOverlay.setVisibility(View.GONE);
                        Log.d(TAG, "✅ Gemini API response received");
                        try {
                            rawAiResponse = response.getJSONArray("candidates")
                                    .getJSONObject(0).getJSONObject("content")
                                    .getJSONArray("parts").getJSONObject(0).getString("text");
                            Log.d(TAG, "📝 AI Response: " + rawAiResponse);
                            updateUI(rawAiResponse);
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error parsing response: " + e.getMessage());
                            tvCrop.setText("Processing Error");
                        }
                    },
                    error -> {
                        loadingOverlay.setVisibility(View.GONE);
                        Log.e(TAG, "❌ Gemini API error: " + error.toString());
                        tvCrop.setText("Connection Failed");
                        Toast.makeText(this, "Failed to connect to AI", Toast.LENGTH_SHORT).show();
                    }
            );
            request.setRetryPolicy(new DefaultRetryPolicy(60000, 1, 1f));
            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            loadingOverlay.setVisibility(View.GONE);
            Log.e(TAG, "❌ Exception: " + e.getMessage());
        }
    }

    private String encodeImageToBase64(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    private void updateUI(String response) {
        String cropName = extractCleanText(response, "CROP:");
        Log.d(TAG, "🌾 Extracted Crop Name: " + cropName);

        tvCrop.setText(cropName);
        tvTip.setText(extractCleanText(response, "TIP:"));
        tvMatch.setText("Analysis Successful");

        setRow(rowNPK, "Nutrients", extractCleanText(response, "NPK:"));
        setRow(rowWater, "Watering", extractCleanText(response, "WATER:"));
        setRow(rowMaturity, "Days to Harvest", extractCleanText(response, "DAYS:"));

        // 🔥 SAVE TO FIREBASE
        if (!cropName.equals("N/A") && !cropName.isEmpty()) {
            Log.d(TAG, "💾 Attempting to save to Firebase...");
            saveRecommendationToCloud(cropName, response);
        } else {
            Log.e(TAG, "❌ Cannot save - crop name is invalid: " + cropName);
            Toast.makeText(this, "Failed to save recommendation", Toast.LENGTH_SHORT).show();
        }
    }

    private String extractCleanText(String text, String key) {
        if (text == null || !text.contains(key)) {
            Log.w(TAG, "⚠️ Key '" + key + "' not found in response");
            return "N/A";
        }

        try {
            String temp = text.split(key)[1];

            String[] stopKeys = {"CROP:", "NPK:", "WATER:", "DAYS:", "TIP:"};
            for (String s : stopKeys) {
                if (temp.contains(s)) temp = temp.split(s)[0];
            }

            String result = temp.replace("*", "")
                    .replace("#", "")
                    .replace("[", "")
                    .replace("]", "")
                    .trim();

            Log.d(TAG, "✂️ Extracted " + key + " → " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting " + key + ": " + e.getMessage());
            return "N/A";
        }
    }

    private void setRow(View row, String label, String value) {
        if (row != null) {
            ((TextView) row.findViewById(R.id.rowLabel)).setText(label);
            ((TextView) row.findViewById(R.id.rowValue)).setText(value);
        }
    }

    private void saveRecommendationToCloud(String crop, String fullAiText) {
        String uid = FirebaseAuth.getInstance().getUid();

        Log.d(TAG, "🔐 Current User UID: " + uid);

        if (uid == null) {
            Log.e(TAG, "❌ USER NOT LOGGED IN - Cannot save to Firebase!");
            Toast.makeText(this, "Please log in to save recommendations", Toast.LENGTH_LONG).show();
            return;
        }

        // 🔥 Check network connectivity
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        Log.d(TAG, "📶 Network connected: " + isConnected);

        if (!isConnected) {
            Log.e(TAG, "❌ NO INTERNET CONNECTION!");
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
            return;
        }

        // 🔥 Check if Firebase Database is initialized
        try {
            // 🔥 Use specific database URL (already configured in Application class)
            FirebaseDatabase database = FirebaseDatabase.getInstance();

            String dbUrl = database.getReference().toString();
            Log.d(TAG, "🔥 Firebase Database URL: " + dbUrl);
        } catch (Exception e) {
            Log.e(TAG, "❌ Firebase Database not initialized: " + e.getMessage());
            Toast.makeText(this, "Firebase error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference userHistoryRef = FirebaseDatabase.getInstance()
                .getReference("History")
                .child(uid);

        String currentTime = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date());
        HistoryModel newEntry = new HistoryModel(crop, currentTime, fullAiText);

        Log.d(TAG, "📦 Saving to Firebase: Crop=" + crop + ", Time=" + currentTime);
        Log.d(TAG, "📍 Database path: History/" + uid);

        // 🔥 Push and log every step
        DatabaseReference pushRef = userHistoryRef.push();
        String pushKey = pushRef.getKey();
        Log.d(TAG, "🔑 Generated push key: " + pushKey);

        // 🔥 Add timeout handler
        android.os.Handler handler = new android.os.Handler();
        handler.postDelayed(() -> {
            Log.e(TAG, "⏰ TIMEOUT: Firebase operation took too long (10 seconds)");
            Toast.makeText(ResultActivity.this, "⏰ Save timeout - check database rules", Toast.LENGTH_LONG).show();
        }, 10000);

        pushRef.setValue(newEntry)
                .addOnSuccessListener(aVoid -> {
                    handler.removeCallbacksAndMessages(null); // Cancel timeout
                    Log.d(TAG, "✅ SUCCESSFULLY SAVED TO FIREBASE!");
                    Log.d(TAG, "✅ Saved at: History/" + uid + "/" + pushKey);
                    Toast.makeText(ResultActivity.this, "✅ Saved: " + crop, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    handler.removeCallbacksAndMessages(null); // Cancel timeout
                    Log.e(TAG, "❌ FIREBASE SAVE FAILED!");
                    Log.e(TAG, "❌ Error: " + e.getClass().getName());
                    Log.e(TAG, "❌ Message: " + e.getMessage());
                    if (e.getCause() != null) {
                        Log.e(TAG, "❌ Cause: " + e.getCause().getMessage());
                    }
                    Toast.makeText(ResultActivity.this, "❌ Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                })
                .addOnCompleteListener(task -> {
                    Log.d(TAG, "🏁 Firebase operation completed. Success: " + task.isSuccessful());
                    if (!task.isSuccessful() && task.getException() != null) {
                        Log.e(TAG, "🏁 Exception: " + task.getException().getMessage());
                    }
                });
    }
}