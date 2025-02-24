package com.example.fyp_chefup;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProfileView_L extends AppCompatActivity {

    private ImageView profileImage;
    private TextView learnerNameTextView;
    private TextView learnerName;
    private TextView descriptionTextView;
    private TextView description;
    private TextView equipmentTextView;
    private TextView equipment;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_view_l); // Ensure this line is before initializing views

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        profileImage = findViewById(R.id.profileImage);
        learnerNameTextView = findViewById(R.id.learnerNameTextView);
        learnerName = findViewById(R.id.learnerName);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        description = findViewById(R.id.description);
        equipmentTextView = findViewById(R.id.equipmentTextView);
        equipment = findViewById(R.id.equipment);

        // Retrieve Chef ID from Intent
        String learnerId = getIntent().getStringExtra("userId");

        // Fetch Chef's information from Firestore
        db.collection("users").document(learnerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Extract Chef's information from the document snapshot
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        String name = documentSnapshot.getString("name");
                        String desc = documentSnapshot.getString("description");
                        String equip = documentSnapshot.getString("equipment");

                        // Populate views with Chef's information
                        learnerName.setText(name);
                        description.setText(desc);
                        equipment.setText(equip);

                        // Load profile image
                        new ImageLoaderTask(profileImage).execute(profileImageUrl);

                        // Load certification image
                    } else {
                        // Handle the case where Chef's document does not exist
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure to fetch Chef's information
                });
    }

    private static class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        public ImageLoaderTask(ImageView imageView) {
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String imageUrl = params[0];
            try {
                URL url = new URL(imageUrl);
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
            if (imageViewReference != null && result != null) {
                ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(result);
                }
            }
        }
    }
}
