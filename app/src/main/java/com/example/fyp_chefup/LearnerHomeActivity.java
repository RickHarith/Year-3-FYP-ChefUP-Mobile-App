package com.example.fyp_chefup;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class LearnerHomeActivity extends AppCompatActivity {

    com.example.fyp_chefup.databinding.ActivityLearnerHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = com.example.fyp_chefup.databinding.ActivityLearnerHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Call replaceFragment method with HomeFragment_C
        replaceFragment(new HomeFragment_L());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.home) {
                replaceFragment(new HomeFragment_L());
            } else if (item.getItemId() == R.id.search) {
                replaceFragment(new HistoryFragment_L());
            } else if (item.getItemId() == R.id.profile) {
                replaceFragment(new ProfileFragment_L());
            } else if (item.getItemId() == R.id.settings) {
                replaceFragment(new SettingsFragment());
            }

            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }
}