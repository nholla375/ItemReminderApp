package com.example.itemreminderapp;

import android.content.Intent;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {
    private EditText emailEditText;
    private EditText passwordEditText;
    private TextView subtitleTextView;
    private ProgressBar progressBar;
    private Button primaryButton;
    private Button switchButton;
    private boolean signupMode = false;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (auth.getCurrentUser() != null) {
            FirebaseUtils.ensureUserProfileExists();
            openHome();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        subtitleTextView = findViewById(R.id.authSubtitleTextView);
        progressBar = findViewById(R.id.authProgressBar);
        primaryButton = findViewById(R.id.authPrimaryButton);
        switchButton = findViewById(R.id.authSwitchButton);

        primaryButton.setOnClickListener(v -> submitAuthForm());
        switchButton.setOnClickListener(v -> {
            signupMode = !signupMode;
            updateModeText();
        });
    }

    private void submitAuthForm() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        if (signupMode) {
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUtils.ensureUserProfileExists();
                        openHome();
                    })
                    .addOnFailureListener(error -> showAuthError(error.getMessage()));
        } else {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUtils.ensureUserProfileExists();
                        openHome();
                    })
                    .addOnFailureListener(error -> showAuthError(error.getMessage()));
        }
    }

    private void updateModeText() {
        if (signupMode) {
            subtitleTextView.setText("Create an account to save and find your items.");
            primaryButton.setText("Create Account");
            switchButton.setText("I Already Have an Account");
        } else {
            subtitleTextView.setText("Sign in to save and find your items.");
            primaryButton.setText("Log In");
            switchButton.setText("Create Account");
        }
    }

    private void showAuthError(String message) {
        setLoading(false);
        Toast.makeText(this, message == null ? "Authentication failed. Please try again." : message,
                Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        primaryButton.setEnabled(!loading);
        switchButton.setEnabled(!loading);
        emailEditText.setEnabled(!loading);
        passwordEditText.setEnabled(!loading);
    }

    private void openHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
