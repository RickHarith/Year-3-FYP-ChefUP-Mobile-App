package com.example.fyp_chefup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextPassword, editTextRetypePassword;
    private Spinner spinnerRole;
    private Button buttonSignUp;
    private TextView loginPageTextView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextRetypePassword = findViewById(R.id.editTextRetypePassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        loginPageTextView = findViewById(R.id.loginPageTextView);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        buttonSignUp.setOnClickListener(view -> signUp());

        loginPageTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to login page
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void signUp() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String retypePassword = editTextRetypePassword.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        // Check if any field is empty
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || retypePassword.isEmpty()) {
            showErrorMessage("Please fill in all fields.");
            return;
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showErrorMessage("Invalid email format.");
            return;
        }

        // Validate name format
        if (!name.matches(".+")) {
            showErrorMessage("Please enter a valid name.");
            return;
        }

        // Validate password length
        if (password.length() < 6) {
            showErrorMessage("Password should be at least 6 characters long.");
            return;
        }

        // Validate password match
        if (!password.equals(retypePassword)) {
            showErrorMessage("Passwords do not match.");
            return;
        }

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign up success, save user info to Firestore
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            saveUserInfoToFirestore(userId, name, email, role);
                            // Send email verification
                            sendEmailVerification(user);
                            // Navigate to login page after successful sign-up
                            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                            finish();
                        }
                    } else {
                        // Sign up failed
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            String errorMessage = task.getException().getMessage();
                            if (errorMessage.contains("email address is already in use")) {
                                showErrorMessage("This email is already in use, try again.");
                            } else {
                                showErrorMessage("Sign up failed. Please try again.");
                            }
                        } else {
                            showErrorMessage("Sign up failed. Please try again.");
                        }
                    }
                });
    }

    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignUpActivity.this, "Email verification sent!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Email verification failed
                        Toast.makeText(SignUpActivity.this, "Failed to send email verification.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void saveUserInfoToFirestore(String userId, String name, String email, String role) {
        User user = new User(name, email, role);

        db.collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    // User info saved successfully
                    Toast.makeText(SignUpActivity.this, "Sign up successful! An email verification has been sent.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Error saving user info
                    showErrorMessage("Error saving user info. Please try again.");
                });
    }

    private void showErrorMessage(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle("Error")
                .setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
