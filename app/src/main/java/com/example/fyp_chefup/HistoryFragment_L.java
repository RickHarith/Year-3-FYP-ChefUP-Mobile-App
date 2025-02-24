package com.example.fyp_chefup;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class HistoryFragment_L extends Fragment {

    private LinearLayout historySessionsLayout;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_history_l, container, false);
        historySessionsLayout = rootView.findViewById(R.id.historySessionsLayout);
        db = FirebaseFirestore.getInstance();
        fetchCompletedSessions();
        return rootView;
    }

    private void fetchCompletedSessions() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("sessions")
                .whereEqualTo("learnerId", userId)
                .whereEqualTo("LearnerFinished", 1)
                .whereEqualTo("ChefFinished", 1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> completedSessions = queryDocumentSnapshots.getDocuments();
                    displayCompletedSessions(completedSessions);
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to fetch completed sessions: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void displayCompletedSessions(List<DocumentSnapshot> completedSessions) {
        if (getContext() == null) {
            return;
        }

        historySessionsLayout.removeAllViews(); // Clear existing views
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (DocumentSnapshot session : completedSessions) {
            String sessionId = session.getId(); // Get the session UID
            String foodName = session.getString("foodName");
            String address = session.getString("address");
            String contactInfo = session.getString("contactInfo");
            String equipment = session.getString("equipment");
            String imageUrl = session.getString("imageUrl");

            View sessionView = inflater.inflate(R.layout.session_item_2, historySessionsLayout, false);
            ImageView sessionImage = sessionView.findViewById(R.id.sessionImage);
            TextView sessionFoodName = sessionView.findViewById(R.id.sessionFoodName);
            TextView sessionAddress = sessionView.findViewById(R.id.sessionAddress);
            TextView sessionContactInfo = sessionView.findViewById(R.id.sessionContactInfo);
            TextView sessionEquipment = sessionView.findViewById(R.id.sessionEquipment);

            // Set session details
            sessionFoodName.setText(foodName);
            sessionAddress.setText(address);
            sessionContactInfo.setText(contactInfo);
            sessionEquipment.setText(equipment);

            // Load image using URL
            new ImageLoaderTask(sessionImage).execute(imageUrl);

            // Apply card styling
            float scale = getResources().getDisplayMetrics().density;
            int margin16dp = (int) (8 * scale + 0.5f);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(margin16dp, margin16dp, margin16dp, margin16dp);
            sessionView.setLayoutParams(layoutParams);

            // Add click listener to the session item
            sessionView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Open SessionRater_L activity and pass the session UID
                    Intent intent = new Intent(getContext(), SessionRater_L.class);
                    intent.putExtra("sessionId", sessionId);
                    startActivity(intent);
                }
            });

            // Add session to layout
            historySessionsLayout.addView(sessionView);
        }
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
