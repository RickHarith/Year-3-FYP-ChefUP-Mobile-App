package com.example.fyp_chefup;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fyp_chefup.databinding.ActivityChefHomeBinding;

public class MainActivity extends AppCompatActivity {

    ActivityChefHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChefHomeBinding.inflate(getLayoutInflater());
        setContentView(R.layout.activity_main);
    }
}