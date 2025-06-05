package com.smartwaste.app.ui.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.smartwaste.app.databinding.FragmentAccountBinding;
import com.smartwaste.app.ui.activity.LoginActivity;
import com.smartwaste.app.viewmodel.AccountViewModel;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private AccountViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        observeUser();
        initClickListeners();

        return binding.getRoot();
    }

    @SuppressLint("SetTextI18n")
    private void observeUser() {
        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            binding.textName.setText(user.getName());
            binding.textLocation.setText("Lokasi : " + user.getLocation());
            // Load image using Glide/Picasso if needed
        });
    }

    private void initClickListeners() {
        binding.buttonEditProfile.setOnClickListener(v -> {
            // Navigate to EditProfileActivity or Fragment
        });

        binding.buttonLogout.setOnClickListener(v -> {
            viewModel.logout();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
