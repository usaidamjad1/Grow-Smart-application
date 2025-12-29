package com.usaid.growsmartapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class LoginPage extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText emailField, passwordField;
    private Button loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        // 🔥 FIX: Auto-login check
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginPage.this, drawyerLayout.class));
            finish();
            return;
        }

        // 🔥 FIX: Removed duplicate declaration
        loginBtn = findViewById(R.id.loginBtn);
        Button signUpBtn = findViewById(R.id.signUpBtn);
        emailField = findViewById(R.id.loginEmail);
        passwordField = findViewById(R.id.loginPassword);

        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LoginPage.this, sinUpPage.class);
                startActivity(i);
            }
        });

        TextView forgotPassword = findViewById(R.id.forgotPassword);

        forgotPassword.setOnClickListener(v -> {
            EditText resetEmail = new EditText(v.getContext());
            AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(v.getContext());
            passwordResetDialog.setTitle("Reset Password?");
            passwordResetDialog.setMessage("Enter your email to receive a reset link.");
            passwordResetDialog.setView(resetEmail);

            passwordResetDialog.setPositiveButton("Send", (dialog, which) -> {
                String email = resetEmail.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(LoginPage.this, "Email is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.sendPasswordResetEmail(email).addOnSuccessListener(unused ->
                        Toast.makeText(LoginPage.this, "Reset link sent to your email.", Toast.LENGTH_SHORT).show()
                ).addOnFailureListener(e ->
                        Toast.makeText(LoginPage.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            });

            passwordResetDialog.setNegativeButton("Cancel", (dialog, which) -> {
                // Close the dialog
            });

            passwordResetDialog.create().show();
        });

        loginBtn.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginPage.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginPage.this, drawyerLayout.class);
                            startActivity(intent);
                            finish(); // 🔥 FIX: Added finish() to prevent back navigation
                        } else {
                            Toast.makeText(LoginPage.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}