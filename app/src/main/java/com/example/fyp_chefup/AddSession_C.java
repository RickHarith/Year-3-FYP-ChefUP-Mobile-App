package com.example.fyp_chefup;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AddSession_C extends AppCompatActivity implements OnMapReadyCallback {

    private FirebaseFirestore db;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private EditText editTextFoodName, editTextEquipment, editTextIngredients, editTextContactInfo, editTextAddress;
    private Handler handler = new Handler();
    private Runnable mapUpdateRunnable;
    private String currentAddress;
    private ImageView imageViewFood;
    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    private ScrollView scrollView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_session_c);

        editTextFoodName = findViewById(R.id.editTextFoodName);
        editTextEquipment = findViewById(R.id.editTextEquipment);
        editTextIngredients = findViewById(R.id.editTextIngredients);
        editTextContactInfo = findViewById(R.id.editTextContactInfo);
        editTextAddress = findViewById(R.id.editTextAddress);
        imageViewFood = findViewById(R.id.imageViewFood);
        imageViewFood.setOnClickListener(v -> openImagePicker());
        scrollView = findViewById(R.id.scrollView);
        ImageView transparentImageView = findViewById(R.id.imagetrans);

        db = FirebaseFirestore.getInstance();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

        Button buttonSubmit = findViewById(R.id.buttonSubmit);
        buttonSubmit.setOnClickListener(v -> saveSessionToFirestore());

        transparentImageView.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // Disallow ScrollView to intercept touch events.
                    scrollView.requestDisallowInterceptTouchEvent(true);
                    return false;

                case MotionEvent.ACTION_UP:
                    // Allow ScrollView to intercept touch events.
                    scrollView.requestDisallowInterceptTouchEvent(false);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    scrollView.requestDisallowInterceptTouchEvent(true);
                    return false;

                default:
                    return true;
            }
        });

        editTextAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                handler.removeCallbacks(mapUpdateRunnable);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                mapUpdateRunnable = () -> updateMapLocation(editable.toString());
                handler.postDelayed(mapUpdateRunnable, 500); // 3 seconds delay
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Handle the result of image selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            imageViewFood.setImageURI(selectedImageUri);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng));
            getAddressFromLocation(latLng);
        });
    }

    private void enableMyLocation() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
        locationTask.addOnSuccessListener(location -> {
            if (location != null) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
            }
        });
    }

    private void getAddressFromLocation(LatLng latLng) {
        Geocoder geocoder = new Geocoder(AddSession_C.this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                currentAddress = address.getAddressLine(0);
                editTextAddress.setText(currentAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateMapLocation(String address) {
        Geocoder geocoder = new Geocoder(AddSession_C.this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                currentAddress = address;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSessionToFirestore() {
        String foodName = editTextFoodName.getText().toString().trim();
        String equipment = editTextEquipment.getText().toString().trim();
        String ingredients = editTextIngredients.getText().toString().trim();
        String contactInfo = editTextContactInfo.getText().toString().trim();

        if (foodName.isEmpty() || equipment.isEmpty() || ingredients.isEmpty() || contactInfo.isEmpty()) {
            Toast.makeText(AddSession_C.this, "Please fill in all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(AddSession_C.this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            return;
        }
        String chefId = currentUser.getUid();

        if (currentAddress == null || currentAddress.isEmpty()) {
            Toast.makeText(AddSession_C.this, "Address not specified!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if an image is selected
        if (selectedImageUri == null) {
            Toast.makeText(AddSession_C.this, "Please select an image!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Upload image to Firestore Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("images/" + UUID.randomUUID().toString());
        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Image uploaded successfully, get the download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Create a Session object with the image URL
                        String imageUrl = uri.toString();
                        Session session = new Session(foodName, equipment, ingredients, contactInfo, currentAddress, chefId, imageUrl);

                        // Save the session to Firestore
                        db.collection("sessions").add(session)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(AddSession_C.this, "Session added successfully!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(AddSession_C.this, ChefHomeActivity.class));
                                })
                                .addOnFailureListener(e -> Toast.makeText(AddSession_C.this, "Failed to add session!", Toast.LENGTH_SHORT).show());
                    });
                })
                .addOnFailureListener(e -> {
                    // Handle errors
                    Toast.makeText(AddSession_C.this, "Failed to upload image!", Toast.LENGTH_SHORT).show();
                });
    }

}
