package com.example.fyp_chefup;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class HomeFragment_C extends Fragment {

    private View rootView;
    private FirebaseFirestore db;
    private LinearLayout sessionsLayout; // LinearLayout to hold the sessions

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home_c, container, false);
        sessionsLayout = rootView.findViewById(R.id.sessionsLayout);

        db = FirebaseFirestore.getInstance();
        fetchSessions();

        // Find the button
        Button addSessionButton = rootView.findViewById(R.id.addSessionButton);

        // Set OnClickListener
        addSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the AddSession_C activity
                startActivity(new Intent(getActivity(), AddSession_C.class));
            }
        });

        return rootView;
    }

    private void fetchSessions() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not authenticated!", Toast.LENGTH_SHORT).show();
            return;
        }

        String chefId = currentUser.getUid();

        db.collection("sessions")
                .whereEqualTo("chefId", chefId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    sessionsLayout.removeAllViews(); // Clear existing views

                    // Get the density of the display for converting dp to pixels
                    float scale = getResources().getDisplayMetrics().density;
                    int margin16dp = (int) (8 * scale + 0.5f); // Convert dp to pixels

                    // Add label for Accepted Sessions
                    TextView acceptedLabel = new TextView(getContext());
                    acceptedLabel.setText("Accepted Sessions");
                    acceptedLabel.setTextSize(20);
                    acceptedLabel.setPadding(16, 16, 16, 16);
                    sessionsLayout.addView(acceptedLabel);

                    // Add Accepted Sessions
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        String learnerId = documentSnapshot.getString("learnerId");
                        Integer chefFinished = documentSnapshot.getLong("ChefFinished") != null ? documentSnapshot.getLong("ChefFinished").intValue() : null;
                        if (learnerId != null && !learnerId.isEmpty() && (chefFinished == null || chefFinished != 1)) {
                            addSessionView(documentSnapshot);
                        }
                    }

                    // Add label for Your Sessions
                    TextView yourSessionsLabel = new TextView(getContext());
                    yourSessionsLabel.setText("Your Sessions");
                    yourSessionsLabel.setTextSize(20);
                    yourSessionsLabel.setPadding(16, 16, 16, 16);
                    sessionsLayout.addView(yourSessionsLabel);

                    // Add Your Sessions
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        String learnerId = documentSnapshot.getString("learnerId");
                        Integer chefFinished = documentSnapshot.getLong("ChefFinished") != null ? documentSnapshot.getLong("ChefFinished").intValue() : null;
                        if ((learnerId == null || learnerId.isEmpty()) && (chefFinished == null || chefFinished != 1)) {
                            addSessionView(documentSnapshot);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to fetch sessions: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addSessionView(DocumentSnapshot documentSnapshot) {
        String foodName = documentSnapshot.getString("foodName");
        String address = documentSnapshot.getString("address");
        String contactInfo = documentSnapshot.getString("contactInfo");
        String equipment = documentSnapshot.getString("equipment");
        String imageUrl = documentSnapshot.getString("imageUrl");
        String sessionId = documentSnapshot.getId();

        // Create session view
        View sessionView = LayoutInflater.from(getContext()).inflate(R.layout.session_item, null);

        // Find views within session view
        ImageView sessionImage = sessionView.findViewById(R.id.sessionImage);
        TextView sessionFoodName = sessionView.findViewById(R.id.sessionFoodName);
        TextView sessionAddress = sessionView.findViewById(R.id.sessionAddress);
        TextView sessionContactInfo = sessionView.findViewById(R.id.sessionContactInfo);
        TextView sessionEquipment = sessionView.findViewById(R.id.sessionEquipment);
        ImageView deleteSessionButton = sessionView.findViewById(R.id.deleteSessionButton);

        // Set session details
        sessionFoodName.setText(foodName);
        sessionAddress.setText(address);
        sessionContactInfo.setText(contactInfo);
        sessionEquipment.setText(equipment);

        // Load image using URL (no Glide or Picasso)
        new ImageLoaderTask(sessionImage).execute(imageUrl);

        // Set margins for session view
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        float scale = getResources().getDisplayMetrics().density;
        int margin16dp = (int) (8 * scale + 0.5f);
        layoutParams.setMargins(0, 0, 0, margin16dp);
        sessionView.setLayoutParams(layoutParams);

        // Add click listener to delete button
        deleteSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSession(sessionId, sessionView);
            }
        });

        // Add click listener to session view
        sessionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open SessionViewer activity and pass the session UID
                Intent intent = new Intent(getContext(), SessionViewer.class);
                intent.putExtra("sessionId", sessionId);
                startActivity(intent);
            }
        });

        // Add session view to sessionsLayout
        sessionsLayout.addView(sessionView);
    }

    private void deleteSession(String sessionId, View sessionView) {
        db.collection("sessions")
                .document(sessionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove session view from layout
                    sessionsLayout.removeView(sessionView);
                    Toast.makeText(getContext(), "Session deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete session: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // AsyncTask to load images from URLs in the background
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
