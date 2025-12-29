package com.usaid.growsmartapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class sinUpPage extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ProgressBar progressBar; // Added for loading state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sin_up_page);

        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Link Views
        TextView loginBtn2 = findViewById(R.id.loginBtn2);
        EditText UserName = findViewById(R.id.UserName);
        EditText signUpEmail = findViewById(R.id.signUpEmail);
        EditText signUpPassword = findViewById(R.id.signUpPassword);
        EditText signUpConfrimPassword = findViewById(R.id.signUpConfrimPassword);
        Button signUp = findViewById(R.id.signupBtn1);

        // Make sure to add <ProgressBar android:id="@+id/progressBar" ... /> in your XML
        progressBar = findViewById(R.id.signUpProgress);

        loginBtn2.setOnClickListener(v -> {
            Intent i = new Intent(sinUpPage.this, LoginPage.class);
            startActivity(i);
        });

        signUp.setOnClickListener(v -> {
            String Username = UserName.getText().toString().trim();
            String Email = signUpEmail.getText().toString().trim();
            String Password = signUpPassword.getText().toString().trim();
            String ConfrimedPassword = signUpConfrimPassword.getText().toString().trim();

            // Validation
            boolean isValid = true;
            if (Username.isEmpty()) { UserName.setError("Field is Empty"); isValid = false; }
            if (Email.isEmpty()) { signUpEmail.setError("Field is Empty"); isValid = false; }
            else if (!Patterns.EMAIL_ADDRESS.matcher(Email).matches()) { signUpEmail.setError("Invalid Email"); isValid = false; }
            if (Password.isEmpty()) { signUpPassword.setError("Field is Empty"); isValid = false; }
            else if (Password.length() < 6) { signUpPassword.setError("Min 6 chars"); isValid = false; }
            if (!Password.equals(ConfrimedPassword)) { signUpConfrimPassword.setError("Password Not Match"); isValid = false; }

            if (isValid) {
                // Show loading circle and disable button
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                signUp.setEnabled(false);

                // 1. Create User
                mAuth.createUserWithEmailAndPassword(Email, Password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                // 2. User created! Now attach the Username
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(Username)
                                            .build();

                                    user.updateProfile(profileUpdates).addOnCompleteListener(updateTask -> {
                                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                                        Toast.makeText(sinUpPage.this, "Welcome " + Username, Toast.LENGTH_SHORT).show();
                                        finish(); // Go back to Login
                                    });
                                }
                            } else {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                signUp.setEnabled(true);
                                Toast.makeText(sinUpPage.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }
}