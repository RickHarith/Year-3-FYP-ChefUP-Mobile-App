package com.example.fyp_chefup;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SessionRater_L extends AppCompatActivity {

    private TextView chefNameTextView;
    private ImageView chefProfileImageView;
    private RatingBar chefRatingBar;
    private Button submitRatingButton;
    private FirebaseFirestore db;
    private String sessionId;
    private String learnerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_rater_l);

        db = FirebaseFirestore.getInstance();

        // Initialize views
        chefNameTextView = findViewById(R.id.chefNameTextView);
        chefProfileImageView = findViewById(R.id.chefProfileImageView);
        chefRatingBar = findViewById(R.id.chefRatingBar);
        submitRatingButton = findViewById(R.id.submitRatingButton);

        // Fetch session and user IDs
        sessionId = getIntent().getStringExtra("sessionId");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            learnerId = currentUser.getUid();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Fetch session details
        loadChefDetails();

        // Setup rating submission
        submitRatingButton.setOnClickListener(v -> submitRating());
    }

    private void loadChefDetails() {
        db.collection("sessions").document(sessionId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String chefId = documentSnapshot.getString("chefId");
                    loadChefProfile(chefId);
                })
                .addOnFailureListener(e -> Log.e("SessionRater_L", "Error loading session details", e));
    }

    private void loadChefProfile(String chefId) {
        db.collection("users").document(chefId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        chefNameTextView.setText(documentSnapshot.getString("name"));
                        new ImageLoaderTask(chefProfileImageView).execute(documentSnapshot.getString("profileImageUrl"));
                    }
                })
                .addOnFailureListener(e -> Log.e("SessionRater_L", "Error loading chef profile", e));
    }

    private void submitRating() {
        db.collection("ratings")
                .whereEqualTo("sessionId", sessionId)
                .whereEqualTo("learnerId", learnerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        updateChefRating();
                    } else {
                        Toast.makeText(this, "You have already rated this session.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateChefRating() {
        DocumentReference sessionRef = db.collection("sessions").document(sessionId);
        sessionRef.get().addOnSuccessListener(documentSnapshot -> {
            String chefId = documentSnapshot.getString("chefId");
            DocumentReference chefRef = db.collection("users").document(chefId);

            float rating = chefRatingBar.getRating();
            Map<String, Object> ratingMap = new HashMap<>();
            ratingMap.put("sessionId", sessionId);
            ratingMap.put("learnerId", learnerId);
            ratingMap.put("rating", rating);

            db.collection("ratings").add(ratingMap).addOnSuccessListener(documentReference -> {
                chefRef.collection("ratings").add(ratingMap).addOnSuccessListener(documentReference1 -> {
                    Toast.makeText(this, "Rating submitted successfully!", Toast.LENGTH_SHORT).show();
                    calculateAverageRating(chefId);
                });
            });
        });
    }

    private void calculateAverageRating(String chefId) {
        db.collection("users").document(chefId).collection("ratings")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        double total = 0;
                        int count = 0;
                        for (DocumentSnapshot document : task.getResult()) {
                            total += document.getDouble("rating");
                            count++;
                        }
                        if (count > 0) {
                            double average = total / count;
                            db.collection("users").document(chefId).update("averageRating", average);
                        }
                    }
                });
    }

    private static class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        public ImageLoaderTask(ImageView imageView) {
            this.imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(urls[0]).openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                Log.e("ImageLoaderTask", "Error loading image", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            ImageView imageView = imageViewReference.get();
            if (imageView != null && result != null) {
                imageView.setImageBitmap(result);
            }
        }
    }
}
