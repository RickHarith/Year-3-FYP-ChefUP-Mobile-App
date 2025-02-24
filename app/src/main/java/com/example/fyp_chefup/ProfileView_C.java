package com.example.fyp_chefup;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProfileView_C extends AppCompatActivity {

    private ImageView profileImage;
    private ImageView certImage;
    private TextView chefNameTextView;
    private TextView chefName;
    private TextView descriptionTextView;
    private TextView description;
    private TextView equipmentTextView;
    private TextView equipment;
    private RatingBar averageRatingBar;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_view_c);

        db = FirebaseFirestore.getInstance();

        profileImage = findViewById(R.id.profileImage);
        certImage = findViewById(R.id.certImage);
        chefNameTextView = findViewById(R.id.chefNameTextView);
        chefName = findViewById(R.id.chefName);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        description = findViewById(R.id.description);
        equipmentTextView = findViewById(R.id.equipmentTextView);
        equipment = findViewById(R.id.equipment);
        averageRatingBar = findViewById(R.id.averageRatingBar);

        String chefId = getIntent().getStringExtra("userId");

        db.collection("users").document(chefId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        chefName.setText(documentSnapshot.getString("name"));
                        description.setText(documentSnapshot.getString("description"));
                        equipment.setText(documentSnapshot.getString("equipment"));

                        if (documentSnapshot.contains("averageRating")) {
                            double averageRating = documentSnapshot.getDouble("averageRating");
                            averageRatingBar.setRating((float) averageRating);
                        } else {
                            averageRatingBar.setRating(0); // Set to 0 or keep unset
                        }

                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        if (profileImageUrl != null) {
                            new ImageLoaderTask(profileImage).execute(profileImageUrl);
                        }

                        String certImageUrl = documentSnapshot.getString("certImageUrl");
                        if (certImageUrl != null) {
                            new ImageLoaderTask(certImage).execute(certImageUrl);
                        }
                    } else {
                        Toast.makeText(this, "Chef's data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch chef's information: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private static class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        public ImageLoaderTask(ImageView imageView) {
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                e.printStackTrace();
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
