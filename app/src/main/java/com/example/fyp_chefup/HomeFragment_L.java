package com.example.fyp_chefup;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class HomeFragment_L extends Fragment {

    private View rootView;
    private FirebaseFirestore db;
    private LinearLayout nearbySessionsLayout; // LinearLayout to hold nearby sessions
    private LinearLayout acceptedSessionsLayout; // LinearLayout to hold accepted sessions
    private FusedLocationProviderClient fusedLocationClient;
    private Geocoder geocoder;

    // Request code for location permission
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home_l, container, false);
        nearbySessionsLayout = rootView.findViewById(R.id.nearbySessionsLayout);
        acceptedSessionsLayout = rootView.findViewById(R.id.acceptedSessionsLayout);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        geocoder = new Geocoder(requireContext(), Locale.getDefault());

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permission
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, fetch sessions
            fetchSessionsForLearner();
        }

        return rootView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch sessions
                fetchSessionsForLearner();
            } else {
                // Permission denied, show a message or handle accordingly
                Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchSessionsForLearner() {
        // Check if location permission is granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Permission is granted, proceed with getting last location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // Fetch sessions based on nearby addresses
                            double userLat = location.getLatitude();
                            double userLng = location.getLongitude();

                            db.collection("sessions")
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        List<DocumentSnapshot> sessions = queryDocumentSnapshots.getDocuments();
                                        nearbySessionsLayout.removeAllViews(); // Clear existing views
                                        acceptedSessionsLayout.removeAllViews(); // Clear existing views
                                        displaySessionsForLearner(sessions, userLat, userLng);
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to fetch sessions: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }
                });
    }

    private void displaySessionsForLearner(List<DocumentSnapshot> sessions, double userLat, double userLng) {
        for (DocumentSnapshot session : sessions) {
            // Extract session data
            String sessionId = session.getId(); // Get the session UID
            String foodName = session.getString("foodName");
            String address = session.getString("address");
            String contactInfo = session.getString("contactInfo");
            String equipment = session.getString("equipment");
            String imageUrl = session.getString("imageUrl");
            String chefId = session.getString("chefId");
            String learnerId = session.getString("learnerId");
            int learnerFinished = session.getLong("LearnerFinished") != null ? session.getLong("LearnerFinished").intValue() : 0;

            // Fetch chef name based on chefId
            db.collection("users").document(chefId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String chefName = documentSnapshot.getString("name");

                        try {
                            // Get latitude and longitude from address
                            List<Address> addresses = geocoder.getFromLocationName(address, 1);
                            if (!addresses.isEmpty()) {
                                Address targetAddress = addresses.get(0);
                                double sessionLat = targetAddress.getLatitude();
                                double sessionLng = targetAddress.getLongitude();

                                // Calculate distance between user and session location
                                float[] results = new float[1];
                                Location.distanceBetween(userLat, userLng, sessionLat, sessionLng, results);
                                float distance = results[0] / 1000; // Convert meters to kilometers

                                // Check if the session is within 25 km radius and not finished by learner
                                if (distance <= 25 && learnerFinished == 0) {
                                    // Display session details
                                    String sessionTitle = "Chef: " + chefName + "\n" + foodName;
                                    View sessionView = LayoutInflater.from(getContext()).inflate(R.layout.session_item_2, null);
                                    ImageView sessionImage = sessionView.findViewById(R.id.sessionImage);
                                    TextView sessionFoodName = sessionView.findViewById(R.id.sessionFoodName);
                                    TextView sessionAddress = sessionView.findViewById(R.id.sessionAddress);
                                    TextView sessionContactInfo = sessionView.findViewById(R.id.sessionContactInfo);
                                    TextView sessionEquipment = sessionView.findViewById(R.id.sessionEquipment);

                                    float scale = getResources().getDisplayMetrics().density;
                                    int margin16dp = (int) (8 * scale + 0.5f);

                                    // Set session details
                                    sessionFoodName.setText(sessionTitle);
                                    sessionAddress.setText(address);
                                    sessionContactInfo.setText(contactInfo);
                                    sessionEquipment.setText(equipment);

                                    // Load image using URL
                                    new ImageLoaderTask(sessionImage).execute(imageUrl);

                                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    );
                                    layoutParams.setMargins(0, 0, 0, margin16dp);
                                    sessionView.setLayoutParams(layoutParams);

                                    // Add click listener to the session item
                                    sessionView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            // Open SessionViewer activity and pass the session UID
                                            Intent intent = new Intent(getContext(), SessionViewer.class);
                                            intent.putExtra("sessionId", sessionId);
                                            startActivity(intent);
                                        }
                                    });

                                    // Determine whether to add session to nearby or accepted sessions layout
                                    if (learnerId == null || learnerId.isEmpty()) {
                                        // Nearby session (unclaimed)
                                        nearbySessionsLayout.addView(sessionView);
                                    } else if (learnerId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                        // Accepted session by the current learner
                                        acceptedSessionsLayout.addView(sessionView);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to fetch chef details: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
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
