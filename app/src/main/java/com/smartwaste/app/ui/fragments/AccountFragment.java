package com.smartwaste.app.ui.fragments;

import androidx.appcompat.widget.AppCompatImageView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.smartwaste.app.R;
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
            if (user != null) {
                binding.textName.setText(user.getName());
                binding.textLocation.setText("Lokasi : " + user.getLocation());

                Glide.with(this)
                        .load(user.getProfileImage())
                        .placeholder(R.drawable.ic_profile)
                        .circleCrop()
                        .into(binding.imageProfile);
            }
        });
    }

    private void initClickListeners() {
        View menuScan = binding.getRoot().findViewById(R.id.menu_scan);
        View menuLogout = binding.getRoot().findViewById(R.id.menu_logout);

        // Ubah teks dan ikon Riwayat Scan
        TextView textScan = menuScan.findViewById(R.id.menu_text);
        AppCompatImageView iconScan = menuScan.findViewById(R.id.menu_icon);
        textScan.setText("Riwayat Scan");
        iconScan.setImageResource(R.drawable.ic_history);

        // Ubah teks dan ikon Keluar
        TextView textLogout = menuLogout.findViewById(R.id.menu_text);
        AppCompatImageView iconLogout = menuLogout.findViewById(R.id.menu_icon);
        textLogout.setText("Keluar");
        iconLogout.setImageResource(R.drawable.ic_logout);

        // Aksi klik
        menuScan.setOnClickListener(v -> {
            // TODO: Navigasi ke RiwayatScanActivity
        });

        menuLogout.setOnClickListener(v -> {
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
