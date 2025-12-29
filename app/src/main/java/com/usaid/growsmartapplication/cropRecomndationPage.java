package com.usaid.growsmartapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class cropRecomndationPage extends AppCompatActivity {

    private ImageView soilPreviewImage;
    private View placeholderUI;
    private Uri imageUri;
    private TextView tvLocation, tvDate;
    private LocationManager locationManager;
    private Button btnGetRecommendation;
    private LocationListener gpsListener, networkListener;

    private String currentTemp = "N/A", currentWind = "N/A", currentHumidity = "N/A";
    private String actualCityName = "Unknown Location"; // 🔥 Store actual city
    private final String WEATHER_API_KEY = "c51b35b23b6219e591eeec29007e4e14";

    private Location bestLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crop_recomndation_page);

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

        ImageButton backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        tvDate.setText(new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date()));
        tvLocation.setText("Getting GPS...");

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
            startImprovedLocationTracking();
        }
    }

    private void startImprovedLocationTracking() {
        try {
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGpsEnabled && !isNetworkEnabled) {
                tvLocation.setText("Enable Location");
                Toast.makeText(this, "Please enable GPS in settings", Toast.LENGTH_LONG).show();
                return;
            }

            if (isGpsEnabled) {
                Location lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastGps != null && isLocationRecent(lastGps)) {
                    updateBestLocation(lastGps);
                }

                gpsListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        updateBestLocation(location);
                    }
                };

                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        2000,
                        5,
                        gpsListener
                );
            }

            if (isNetworkEnabled && bestLocation == null) {
                Location lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastNetwork != null) {
                    updateBestLocation(lastNetwork);
                }

                networkListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        if (bestLocation == null || bestLocation.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
                            updateBestLocation(location);
                        }
                    }
                };

                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000,
                        10,
                        networkListener
                );
            }

            if (bestLocation == null) {
                tvLocation.setText("Searching GPS satellites...");
            }

        } catch (SecurityException e) {
            tvLocation.setText("Permission Error");
        }
    }

    private boolean isLocationRecent(Location location) {
        long timeDiff = System.currentTimeMillis() - location.getTime();
        return timeDiff < 2 * 60 * 1000;
    }

    private void updateBestLocation(Location newLocation) {
        if (newLocation == null) return;

        if (bestLocation == null) {
            bestLocation = newLocation;
            getCityNameAndWeather(newLocation.getLatitude(), newLocation.getLongitude());
            return;
        }

        if (newLocation.getProvider().equals(LocationManager.GPS_PROVIDER)
                && bestLocation.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
            bestLocation = newLocation;
            getCityNameAndWeather(newLocation.getLatitude(), newLocation.getLongitude());
            return;
        }

        if (newLocation.getProvider().equals(bestLocation.getProvider())) {
            if (newLocation.getAccuracy() < bestLocation.getAccuracy() ||
                    newLocation.getTime() > bestLocation.getTime()) {
                bestLocation = newLocation;
                getCityNameAndWeather(newLocation.getLatitude(), newLocation.getLongitude());
            }
        }
    }

    // 🔥 NEW: Get actual city name using Geocoder, then fetch weather
    private void getCityNameAndWeather(double lat, double lon) {
        // First, get actual city name from coordinates
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);

                    // Try to get city name in order of preference
                    String city = address.getLocality(); // City name
                    if (city == null) city = address.getSubAdminArea(); // District
                    if (city == null) city = address.getAdminArea(); // Province
                    if (city == null) city = "Unknown Location";

                    actualCityName = city;

                    runOnUiThread(() -> {
                        tvLocation.setText(actualCityName + " (Loading weather...)");
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                actualCityName = "Location Found";
            }

            // Then fetch weather data
            runOnUiThread(() -> fetchWeather(lat, lon));
        }).start();
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

                        // 🔥 Use actual city name, not weather station name
                        String weatherStation = response.optString("name", "Unknown");

                        runOnUiThread(() -> {
                            // 🔥 Show only actual city name, no weather station
                            tvLocation.setText(actualCityName);
                            setWeatherStat(R.id.weatherTemp, currentTemp, "Temp");
                            setWeatherStat(R.id.weatherWind, currentWind, "Wind");
                            setWeatherStat(R.id.weatherRain, currentHumidity, "Humidity");

                            float accuracy = bestLocation != null ? bestLocation.getAccuracy() : 0;
                            if (accuracy > 1000) {
                                Toast.makeText(this, "⚠️ Location accuracy is low", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (JSONException e) {
                        tvLocation.setText(actualCityName + " (Weather Error)");
                    }
                },
                error -> {
                    tvLocation.setText(actualCityName + " (Network Error)");
                    Toast.makeText(this, "Failed to fetch weather", Toast.LENGTH_SHORT).show();
                }
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
                "Data: Location " + actualCityName + ", Weather " + currentTemp + "/" + currentHumidity +
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
            startImprovedLocationTracking();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null) {
            try {
                if (gpsListener != null) locationManager.removeUpdates(gpsListener);
                if (networkListener != null) locationManager.removeUpdates(networkListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startImprovedLocationTracking();
        }
    }
}