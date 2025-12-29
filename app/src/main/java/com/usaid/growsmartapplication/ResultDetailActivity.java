package com.usaid.growsmartapplication;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ResultDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String type = getIntent().getStringExtra("ADVICE_TYPE");
        String aiValue = getIntent().getStringExtra("AI_VALUE");

        if (type != null) {
            switch (type) {
                case "WATER": setContentView(R.layout.activity_water_advice); break;
                case "NPK": setContentView(R.layout.activity_npk_advice); break;
                case "HARVEST": setContentView(R.layout.activity_harvest_advice); break;
            }
        }

        // Apply AI value to the card in the detail layout
        TextView tvDetailValue = findViewById(R.id.tvDetailValue);
        if (tvDetailValue != null && aiValue != null) {
            tvDetailValue.setText(aiValue);
        }

        ImageView btnBack = findViewById(R.id.btnBackAdvice);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }
}