package com.usaid.growsmartapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class cropRecomndationPage extends AppCompatActivity {

    private ImageView soilPreviewImage;
    private View placeholderUI;
    private Uri imageUri;
    private TextView tvLocation, tvDate;
    private LocationManager locationManager;
    private Button btnGetRecommendation;

    private String currentTemp = "N/A", currentWind = "N/A", currentHumidity = "N/A";
    private final String WEATHER_API_KEY = "c51b35b23b6219e591eeec29007e4e14";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crop_recomndation_page);

        // 🔥 FIX: Handle notch/status bar properly
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvLocation = findViewById(R.id.tvFieldLocation);
        tvDate = findViewById(R.id.tvCurrentDate);
        soilPreviewImage = findViewById(R.id.soilPreviewImage);
        placeholderUI = findViewById(R.id.placeholderUI);
        CardView soilImageCard = findViewById(R.id.soilImageCard);
        btnGetRecommendation = findViewById(R.id.btn_get_recommendation);

        // 🔥 FIX: Back button implementation
        ImageButton backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> {
            finish(); // Simply close this activity
        });

        tvDate.setText(new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date()));
        tvLocation.setText("Waiting for GPS...");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        checkLocationPermissions();

        ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        soilPreviewImage.setImageURI(imageUri);
                        soilPreviewImage.setVisibility(View.VISIBLE);
                        placeholderUI.setVisibility(View.GONE);
                    }
                }
        );

        soilImageCard.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        setupSpinners();
        btnGetRecommendation.setOnClickListener(v -> handleRecommendationRequest());
    }

    private void checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        } else {
            startNativeLocationTracking();
        }
    }

    private void startNativeLocationTracking() {
        try {
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGpsEnabled && !isNetworkEnabled) {
                tvLocation.setText("Enable Location");
                return;
            }

            Location lastLoc = null;
            if (isNetworkEnabled) lastLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (lastLoc == null && isGpsEnabled) lastLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastLoc != null) fetchWeather(lastLoc.getLatitude(), lastLoc.getLongitude());

            String provider = isNetworkEnabled ? LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;
            locationManager.requestLocationUpdates(provider, 5000, 10, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    fetchWeather(location.getLatitude(), location.getLongitude());
                    locationManager.removeUpdates(this);
                }
                @Override public void onProviderEnabled(@NonNull String provider) {}
                @Override public void onProviderDisabled(@NonNull String provider) {}
            });

        } catch (SecurityException e) {
            tvLocation.setText("Permission Error");
        }
    }

    private void fetchWeather(double lat, double lon) {
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=" + WEATHER_API_KEY + "&units=metric";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject main = response.getJSONObject("main");
                        currentTemp = Math.round(main.getDouble("temp")) + "°C";
                        currentHumidity = main.getString("humidity") + "%";
                        currentWind = response.getJSONObject("wind").getString("speed") + " km/h";
                        String cityName = response.optString("name", "Field Location");

                        runOnUiThread(() -> {
                            tvLocation.setText(cityName);
                            setWeatherStat(R.id.weatherTemp, currentTemp, "Temp");
                            setWeatherStat(R.id.weatherWind, currentWind, "Wind");
                            setWeatherStat(R.id.weatherRain, currentHumidity, "Humidity");
                        });
                    } catch (JSONException e) {
                        tvLocation.setText("Error");
                    }
                },
                error -> tvLocation.setText("Net Error")
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void handleRecommendationRequest() {
        if (imageUri == null) {
            Toast.makeText(this, "Upload soil photo first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGetRecommendation.setEnabled(false);
        btnGetRecommendation.setText("Analyzing...");

        String prompt = "Role: Agronomist for Punjab, Pakistan.\n" +
                "Task: Analyze image and data. Suggest ONE best crop.\n" +
                "Data: Location " + tvLocation.getText() + ", Weather " + currentTemp + "/" + currentHumidity +
                ", Irrigation: " + getSpinnerValue(R.id.lay_irr) + "\n\n" +
                "STRICT RULES:\n" +
                "1. NO ASTERISKS (**), NO BOLD, NO MARKDOWN.\n" +
                "2. KEEP ANSWERS SHORT (Max 10 words per line).\n" +
                "3. USE THIS EXACT FORMAT:\n" +
                "CROP: [Name Only]\n" +
                "NPK: [Ratio Only]\n" +
                "WATER: [Frequency Only]\n" +
                "DAYS: [Range Only]\n" +
                "TIP: [One Short Tip]";

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("AI_PROMPT", prompt);
        intent.putExtra("IMAGE_URI", imageUri.toString());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);

        btnGetRecommendation.postDelayed(() -> {
            btnGetRecommendation.setEnabled(true);
            btnGetRecommendation.setText("Get AI Recommendation");
        }, 2000);
    }

    private String getSpinnerValue(int layoutId) {
        try {
            Spinner s = findViewById(layoutId).findViewById(R.id.fieldSpinner);
            return s.getSelectedItem().toString();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private void setWeatherStat(int id, String val, String lbl) {
        View v = findViewById(id);
        if (v != null) {
            ((TextView) v.findViewById(R.id.statValue)).setText(val);
            ((TextView) v.findViewById(R.id.statLabel)).setText(lbl);
        }
    }

    private void setupSpinners() {
        setupWaterField(R.id.lay_irr, "Irrigation", new String[]{"Flood", "Drip", "Manual"});
        setupWaterField(R.id.lay_src, "Source", new String[]{"Borewell", "Canal", "Rain"});
        setupWaterField(R.id.lay_drain, "Drainage", new String[]{"Good", "Poor"});
        setupWaterField(R.id.lay_freq, "Frequency", new String[]{"Daily", "Weekly"});
    }

    private void setupWaterField(int id, String label, String[] opts) {
        View layout = findViewById(id);
        if (layout != null) {
            ((TextView) layout.findViewById(R.id.spinnerLabel)).setText(label);
            Spinner s = layout.findViewById(R.id.fieldSpinner);
            ArrayAdapter<String> adp = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opts);
            adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            s.setAdapter(adp);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startNativeLocationTracking();
        }
    }
}