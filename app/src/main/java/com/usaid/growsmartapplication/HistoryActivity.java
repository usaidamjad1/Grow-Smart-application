package com.usaid.growsmartapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";
    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private ArrayList<HistoryModel> historyList;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Log.d(TAG, "🚀 HistoryActivity started");

        findViewById(R.id.btnBackHistory).setOnClickListener(v -> finish());

        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList);
        rvHistory.setAdapter(adapter);

        loadCloudHistory();
    }

    private void loadCloudHistory() {
        String uid = FirebaseAuth.getInstance().getUid();

        Log.d(TAG, "🔐 Current User UID: " + uid);

        if (uid == null) {
            Log.e(TAG, "❌ User not logged in!");
            Toast.makeText(this, "Please log in to view history", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("History")
                .child(uid);

        Log.d(TAG, "📍 Loading from: History/" + uid);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "📥 Firebase response received");
                Log.d(TAG, "📊 Snapshot exists: " + snapshot.exists());
                Log.d(TAG, "📊 Children count: " + snapshot.getChildrenCount());

                historyList.clear();

                if (!snapshot.exists()) {
                    Log.w(TAG, "⚠️ No history data found");
                    Toast.makeText(HistoryActivity.this, "No history yet", Toast.LENGTH_SHORT).show();
                    return;
                }

                int count = 0;
                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        HistoryModel model = data.getValue(HistoryModel.class);
                        if (model != null) {
                            historyList.add(model);
                            count++;
                            Log.d(TAG, "✅ Loaded item " + count + ": " + model.cropName);
                        } else {
                            Log.e(TAG, "❌ Null model at key: " + data.getKey());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error parsing data: " + e.getMessage());
                    }
                }

                Collections.reverse(historyList); // Newest on top
                adapter.notifyDataSetChanged();

                Log.d(TAG, "🎉 Loaded " + historyList.size() + " items successfully");
                Toast.makeText(HistoryActivity.this, "Loaded " + historyList.size() + " recommendations", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Firebase error: " + error.getMessage());
                Log.e(TAG, "❌ Error code: " + error.getCode());
                Log.e(TAG, "❌ Error details: " + error.getDetails());
                Toast.makeText(HistoryActivity.this, "Error loading history: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- Adapter Class ---
    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        ArrayList<HistoryModel> mList;

        HistoryAdapter(ArrayList<HistoryModel> list) {
            this.mList = list;
            Log.d(TAG, "📋 Adapter created with " + list.size() + " items");
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            Log.d(TAG, "🔨 ViewHolder created");
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryModel item = mList.get(position);
            Log.d(TAG, "🖼️ Binding item " + position + ": " + item.cropName);
            holder.crop.setText(item.cropName + " Recommendation");
            holder.date.setText(item.date);
        }

        @Override
        public int getItemCount() {
            Log.d(TAG, "📊 getItemCount: " + mList.size());
            return mList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView crop, date;

            ViewHolder(View v) {
                super(v);
                crop = v.findViewById(R.id.tvHistoryCrop);
                date = v.findViewById(R.id.tvHistoryDate);

                if (crop == null) Log.e(TAG, "❌ tvHistoryCrop is NULL!");
                if (date == null) Log.e(TAG, "❌ tvHistoryDate is NULL!");
            }
        }
    }
}