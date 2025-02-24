package com.example.fyp_chefup;

import android.app.AlertDialog;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ProfileFragment_L extends Fragment {
    private static final int REQUEST_IMAGE_PICKER = 1;
    private ImageView profileImage;
    private EditText editTextEquipment, editTextDescription, editTextPassword, editTextRetypePassword;
    private AppCompatButton buttonSave, buttonDeleteAccount;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Uri selectedImageUri;

    public ProfileFragment_L() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_l, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileImage = view.findViewById(R.id.profileImage);
        editTextEquipment = view.findViewById(R.id.editTextEquipment);
        editTextDescription = view.findViewById(R.id.editTextDescription);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        editTextRetypePassword = view.findViewById(R.id.editTextRetypePassword);
        buttonSave = view.findViewById(R.id.buttonSave);
        buttonDeleteAccount = view.findViewById(R.id.buttonDeleteAccount);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        profileImage.setOnClickListener(v -> openImagePicker());
        buttonSave.setOnClickListener(v -> saveChanges());
        buttonDeleteAccount.setOnClickListener(v -> deleteAccount());

        loadUserData();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICKER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICKER && resultCode == getActivity().RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            profileImage.setImageURI(selectedImageUri);
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

        // Upload image to Firebase Storage if an image is selected
        if (selectedImageUri != null) {
            uploadImage(selectedImageUri, userRef);
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
    }

    private void loadUserData() {
        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String equipment = documentSnapshot.getString("equipment");
                String description = documentSnapshot.getString("description");

                editTextEquipment.setHint(equipment);
                editTextDescription.setHint(description);

                // Load profile image from Firebase Storage using URL
                loadProfileImageFromStorage(documentSnapshot.getString("profileImageUrl"));
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

    private void uploadImage(Uri imageUri, DocumentReference userRef) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("profile_images").child(mAuth.getCurrentUser().getUid() + ".jpg");
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();

        UploadTask uploadTask = storageRef.putBytes(imageData);
        uploadTask.continueWithTask((Continuation<UploadTask.TaskSnapshot, Task<Uri>>) task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            // Continue with the task to get the download URL
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                userRef.update("profileImageUrl", downloadUri.toString());
                Toast.makeText(requireContext(), "Image uploaded successfully", Toast.LENGTH_SHORT).show();
            } else {
                // Handle failures
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
            }
        });
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

                        // Retrieve URL of profile picture
                        userRef.get().addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String profileImageUrl = documentSnapshot.getString("profileImageUrl");

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
