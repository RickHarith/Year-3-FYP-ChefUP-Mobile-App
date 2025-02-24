package com.example.fyp_chefup;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class SessionViewer extends AppCompatActivity {

    private static final String TAG = "SessionViewer";
    private ImageView sessionImageView;
    private TextView foodNameTextView, equipmentTextView, ingredientsTextView, addressTextView, contactInfoTextView;
    private Button viewChefProfileButton, viewLearnerProfileButton, acceptSessionButton, finishSessionButton, finishSessionChefButton;
    private ScrollView scrollView;
    private GoogleMap googleMap;
    private FirebaseFirestore db;
    private String currentUserRole;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_viewer);

        initializeViews();
        db = FirebaseFirestore.getInstance();

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this::onMapReady);

        fetchUserRole();
        loadSessionDataFromIntent();
    }

    private void initializeViews() {
        sessionImageView = findViewById(R.id.sessionImageView);
        foodNameTextView = findViewById(R.id.foodNameTextView);
        equipmentTextView = findViewById(R.id.equipmentTextView);
        ingredientsTextView = findViewById(R.id.ingredientsTextView);
        addressTextView = findViewById(R.id.addressTextView);
        contactInfoTextView = findViewById(R.id.contactInfoTextView);
        viewChefProfileButton = findViewById(R.id.viewChefProfileButton);
        viewLearnerProfileButton = findViewById(R.id.viewLearnerProfileButton);
        acceptSessionButton = findViewById(R.id.acceptSessionButton);
        finishSessionButton = findViewById(R.id.finishSessionButton);
        finishSessionChefButton = findViewById(R.id.finishSessionChefButton);
        scrollView = findViewById(R.id.scrollView);
        ImageView transparentImageView = findViewById(R.id.imagetrans);

        transparentImageView.setOnTouchListener(this::handleTouch);
    }

    private boolean handleTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                scrollView.requestDisallowInterceptTouchEvent(true);
                return false;
            case MotionEvent.ACTION_UP:
                scrollView.requestDisallowInterceptTouchEvent(false);
                return true;
            case MotionEvent.ACTION_MOVE:
                scrollView.requestDisallowInterceptTouchEvent(true);
                return false;
            default:
                return true;
        }
    }

    private void fetchUserRole() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentUserRole = documentSnapshot.getString("role");
                    configureButtonsBasedOnRole();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching user role", e));
    }

    private void configureButtonsBasedOnRole() {
        // Fetch session data once role is determined to configure UI correctly.
        db.collection("sessions").document(getIntent().getStringExtra("sessionId"))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Determine if session is accepted
                    String learnerId = documentSnapshot.getString("learnerId");
                    boolean sessionAccepted = learnerId != null && !learnerId.isEmpty();

                    if ("Chef".equals(currentUserRole)) {
                        // Hide 'View Chef Profile' button for chefs
                        viewChefProfileButton.setVisibility(View.GONE);
                        // Show 'View Learner Profile' button if session is accepted
                        viewLearnerProfileButton.setVisibility(sessionAccepted ? View.VISIBLE : View.GONE);
                        // Show 'Finish Session' button for chef if session is accepted
                        finishSessionChefButton.setVisibility(sessionAccepted ? View.VISIBLE : View.GONE);
                        // Hide 'Accept Session' button for chefs
                        acceptSessionButton.setVisibility(View.GONE);
                    } else {
                        // Show 'View Chef Profile' button for learners if session is accepted
                        viewChefProfileButton.setVisibility(sessionAccepted ? View.VISIBLE : View.GONE);
                        // Hide 'View Learner Profile' button for learners
                        viewLearnerProfileButton.setVisibility(View.GONE);
                        // Show 'Finish Session' button for learners if session is accepted
                        finishSessionButton.setVisibility(sessionAccepted ? View.VISIBLE : View.GONE);
                        // Show 'Accept Session' button only if the session has not been accepted
                        acceptSessionButton.setVisibility(!sessionAccepted ? View.VISIBLE : View.GONE);
                        finishSessionChefButton.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching session data", e));

        setupButtonListeners(); // Ensure button listeners are set up after roles are configured
    }


    private void setupButtonListeners() {
        acceptSessionButton.setOnClickListener(v -> {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            db.collection("sessions").document(getIntent().getStringExtra("sessionId"))
                    .update("learnerId", userId)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(SessionViewer.this, "Session accepted successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(SessionViewer.this, "Failed to accept session: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        finishSessionButton.setOnClickListener(v -> finishSession("LearnerFinished"));
        finishSessionChefButton.setOnClickListener(v -> finishSession("ChefFinished"));

        viewChefProfileButton.setOnClickListener(v -> viewProfile(false));
        viewLearnerProfileButton.setOnClickListener(v -> viewProfile(true));
    }

    private void finishSession(String finishField) {
        db.collection("sessions").document(getIntent().getStringExtra("sessionId"))
                .update(finishField, 1)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SessionViewer.this, "Session finished successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SessionViewer.this, finishField.equals("LearnerFinished") ? LearnerHomeActivity.class : ChefHomeActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(SessionViewer.this, "Failed to finish session: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void viewProfile(boolean isLearner) {
        Intent intent = new Intent(SessionViewer.this, isLearner ? ProfileView_L.class : ProfileView_C.class);
        String sessionId = getIntent().getStringExtra("sessionId");
        db.collection("sessions").document(sessionId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String userIdKey = isLearner ? "learnerId" : "chefId";
                    String userId = documentSnapshot.getString(userIdKey);
                    if (userId != null) {
                        intent.putExtra("userId", userId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(SessionViewer.this, "User ID is null", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(SessionViewer.this, "Error retrieving user details: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadSessionDataFromIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("sessionId")) {
            String sessionId = intent.getStringExtra("sessionId");
            db.collection("sessions").document(sessionId)
                    .get()
                    .addOnSuccessListener(this::handleDocumentSnapshot)
                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching session data", e));
        }
    }

    private void handleDocumentSnapshot(DocumentSnapshot document) {
        if (document.exists()) {
            updateUI(document);
            handleButtonVisibility(document);
        } else {
            Toast.makeText(this, "Session not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI(DocumentSnapshot document) {
        foodNameTextView.setText(document.getString("foodName"));
        equipmentTextView.setText(document.getString("equipment"));
        ingredientsTextView.setText(document.getString("ingredients"));
        addressTextView.setText(document.getString("address"));
        contactInfoTextView.setText(document.getString("contactInfo"));
        new ImageLoaderTask(sessionImageView).execute(document.getString("imageUrl"));
        addMarkerOnMap(document.getString("address"));
    }

    private void handleButtonVisibility(DocumentSnapshot document) {
        String learnerId = document.getString("learnerId");
        boolean sessionAccepted = learnerId != null && !learnerId.isEmpty();

        // Ensuring acceptSessionButton is only visible to learners and only if the session has not been accepted yet
        if ("Learner".equals(currentUserRole)) {
            acceptSessionButton.setVisibility(!sessionAccepted ? View.VISIBLE : View.GONE);
        } else {
            acceptSessionButton.setVisibility(View.GONE);
        }

        // Display the finish session button if there's a learnerId, indicating the session has been accepted
        finishSessionButton.setVisibility(sessionAccepted ? View.VISIBLE : View.GONE);

        if ("Chef".equals(currentUserRole)) {
            finishSessionChefButton.setVisibility(sessionAccepted ? View.VISIBLE : View.GONE);
            viewLearnerProfileButton.setVisibility(sessionAccepted ? View.VISIBLE : View.GONE);
            viewChefProfileButton.setVisibility(View.GONE);
        } else {
            viewChefProfileButton.setVisibility(sessionAccepted ? View.VISIBLE : View.GONE);
            viewLearnerProfileButton.setVisibility(View.GONE);
            finishSessionChefButton.setVisibility(View.GONE);
        }
    }

    private void onMapReady(GoogleMap map) {
        googleMap = map;
    }

    private void addMarkerOnMap(String address) {
        new GeocoderTask(googleMap).execute(address);
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
                Log.e(TAG, "Error downloading image", e);
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

    private class GeocoderTask extends AsyncTask<String, Void, LatLng> {
        private final WeakReference<GoogleMap> mapReference;

        public GeocoderTask(GoogleMap map) {
            mapReference = new WeakReference<>(map);
        }

        @Override
        protected LatLng doInBackground(String... addresses) {
            try {
                List<Address> addressList = new Geocoder(SessionViewer.this).getFromLocationName(addresses[0], 1);
                if (!addressList.isEmpty()) {
                    Address address = addressList.get(0);
                    return new LatLng(address.getLatitude(), address.getLongitude());
                }
            } catch (IOException e) {
                Log.e(TAG, "Error finding location", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(LatLng latLng) {
            GoogleMap map = mapReference.get();
            if (map != null && latLng != null) {
                map.addMarker(new MarkerOptions().position(latLng).title("Session Location"));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }
        }
    }
}
