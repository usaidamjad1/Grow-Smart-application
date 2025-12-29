package com.usaid.growsmartapplication;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "🚀 Application starting...");

        try {
            // 🔥 MANUAL FIREBASE INITIALIZATION WITH DATABASE URL
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:1040016111752:android:d1aaa5d617ea8796fec73d")
                    .setApiKey("AIzaSyDQAHD531HJBeyB8zrKDz6LgHeBMIcy2aI")
                    .setDatabaseUrl("https://grow-smart-533b7-default-rtdb.firebaseio.com")
                    .setProjectId("grow-smart-533b7")
                    .setStorageBucket("grow-smart-533b7.firebasestorage.app")
                    .build();

            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this, options);
                Log.d(TAG, "✅ Firebase initialized with custom options");
            }

            // 🔥 CRITICAL: Enable persistence BEFORE any database usage
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.setPersistenceEnabled(true);
            database.setLogLevel(com.google.firebase.database.Logger.Level.DEBUG);

            Log.d(TAG, "✅ Firebase Database configured successfully");
            Log.d(TAG, "✅ Database URL: " + database.getReference().toString());

        } catch (Exception e) {
            Log.e(TAG, "❌ Firebase initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}