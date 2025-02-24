package com.example.fyp_chefup;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;

public class ProfileFragment_C extends Fragment {
    private static final int REQUEST_IMAGE_PICKER = 1;
    private static final int REQUEST_CERT_IMAGE_PICKER = 2;
    private ImageView profileImage, certImage;
    private EditText editTextEquipment, editTextDescription, editTextPassword, editTextRetypePassword;
    private AppCompatButton buttonSave, buttonDeleteAccount;
    private RatingBar averageRatingBar;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public ProfileFragment_C() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_c, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileImage = view.findViewById(R.id.profileImage);
        certImage = view.findViewById(R.id.certImage);
        editTextEquipment = view.findViewById(R.id.editTextEquipment);
        editTextDescription = view.findViewById(R.id.editTextDescription);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        editTextRetypePassword = view.findViewById(R.id.editTextRetypePassword);
        buttonSave = view.findViewById(R.id.buttonSave);
        buttonDeleteAccount = view.findViewById(R.id.buttonDeleteAccount);
        averageRatingBar = view.findViewById(R.id.averageRatingBar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        profileImage.setOnClickListener(v -> openImagePicker());
        certImage.setOnClickListener(v -> openCertImagePicker());
        buttonSave.setOnClickListener(v -> saveChanges());
        buttonDeleteAccount.setOnClickListener(v -> deleteAccount());

        loadUserData();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICKER);
    }

    private void openCertImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CERT_IMAGE_PICKER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICKER) {
                if (data != null && data.getData() != null) {
                    Uri selectedImageUri = data.getData();
                    profileImage.setImageURI(selectedImageUri);
                    profileImage.setTag(selectedImageUri); // Set the URI as a tag to identify it later
                }
            } else if (requestCode == REQUEST_CERT_IMAGE_PICKER) {
                if (data != null && data.getData() != null) {
                    Uri selectedCertImageUri = data.getData();
                    certImage.setImageURI(selectedCertImageUri);
                    certImage.setTag(selectedCertImageUri); // Set the URI as a tag to identify it later
                }
            }
        }
    }

    private void saveChanges() {
        String equipment = editTextEquipment.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String retypePassword = editTextRetypePassword.getText().toString().trim();

        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(userId);

        // Update equipment only if it's not empty
        if (!TextUtils.isEmpty(equipment)) {
            userRef.update("equipment", equipment)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Equipment updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Failed to update equipment", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        // Update description only if it's not empty
        if (!TextUtils.isEmpty(description)) {
            userRef.update("description", description)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Description updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Failed to update description", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        if (!TextUtils.isEmpty(password) && !TextUtils.isEmpty(retypePassword) && password.equals(retypePassword)) {
            mAuth.getCurrentUser().updatePassword(password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Failed to change password", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        uploadImageToStorage(profileImage, "profile_pictures");
        uploadImageToStorage(certImage, "cert_images");
    }

    private void uploadImageToStorage(ImageView imageView, String storageFolder) {
        if (imageView.getTag() != null) {
            Uri imageUri = (Uri) imageView.getTag();
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(storageFolder).child(imageUri.getLastPathSegment());

            storageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            String userId = mAuth.getCurrentUser().getUid();
                            db.collection("users").document(userId)
                                    .update(storageFolder.equals("profile_pictures") ? "profileImageUrl" : "certImageUrl", imageUrl)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(requireContext(), "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadUserData() {
        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String equipment = documentSnapshot.getString("equipment");
                String description = documentSnapshot.getString("description");
                Number averageRatingNumber = documentSnapshot.getDouble("averageRating"); // Get as Number to handle null safely

                if (averageRatingNumber != null) {
                    float rating = averageRatingNumber.floatValue();
                    averageRatingBar.setRating(rating);
                } else {
                    // Handle the case where there is no rating yet, for example by setting to 0 or leaving it untouched
                    averageRatingBar.setRating(0); // Or some default behavior
                }

                editTextEquipment.setHint(equipment);
                editTextDescription.setHint(description);

                // Load profile and cert images from Firebase Storage using URLs
                loadProfileImageFromStorage(documentSnapshot.getString("profileImageUrl"));
                loadCertImageFromStorage(documentSnapshot.getString("certImageUrl"));
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadProfileImageFromStorage(String profileImageUrl) {
        if (profileImageUrl != null) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(profileImageUrl);
            try {
                final File localFile = File.createTempFile("images", "jpg");
                storageRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                    Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    profileImage.setImageBitmap(bitmap);
                }).addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load profile image", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadCertImageFromStorage(String certImageUrl) {
        if (certImageUrl != null) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(certImageUrl);
            try {
                final File localFile = File.createTempFile("images", "jpg");
                storageRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                    Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    certImage.setImageBitmap(bitmap);
                }).addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load cert image", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteAccount() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you wish to delete your account?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        String userId = user.getUid();
                        DocumentReference userRef = db.collection("users").document(userId);

                        // Retrieve URLs of profile picture and certificate image
                        userRef.get().addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                                String certImageUrl = documentSnapshot.getString("certImageUrl");

                                // Delete account from Firestore
                                userRef.delete()
                                        .addOnSuccessListener(aVoid -> {
                                            // Delete profile picture from storage
                                            if (profileImageUrl != null) {
                                                StorageReference profileImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(profileImageUrl);
                                                profileImageRef.delete().addOnSuccessListener(aVoid1 -> {
                                                    // Profile picture deleted successfully
                                                }).addOnFailureListener(e -> {
                                                    // Failed to delete profile picture
                                                });
                                            }

                                            // Delete certificate image from storage
                                            if (certImageUrl != null) {
                                                StorageReference certImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(certImageUrl);
                                                certImageRef.delete().addOnSuccessListener(aVoid1 -> {
                                                    // Certificate image deleted successfully
                                                }).addOnFailureListener(e -> {
                                                    // Failed to delete certificate image
                                                });
                                            }

                                            // Delete account from authentication
                                            user.delete().addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    // Sign out the user
                                                    mAuth.signOut();

                                                    // Transfer back to login activity
                                                    startActivity(new Intent(requireContext(), LoginActivity.class));
                                                    requireActivity().finish();
                                                } else {
                                                    // Failed to delete account from authentication
                                                    Toast.makeText(requireContext(), "Failed to delete account", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            // Failed to delete account from Firestore
                                            Toast.makeText(requireContext(), "Failed to delete account", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        }).addOnFailureListener(e -> {
                            // Failed to retrieve user data
                            Toast.makeText(requireContext(), "Failed to delete account", Toast.LENGTH_SHORT).show();
                        });
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
}
