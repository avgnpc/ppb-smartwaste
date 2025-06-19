package com.smartwaste.app.ui.activity;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smartwaste.app.R;
import com.smartwaste.app.viewmodel.CameraViewModel;

public class MainActivity extends AppCompatActivity {

    private ImageButton navHome, navClipboard, navProfile;
    private FloatingActionButton fabCamera;
    private CameraViewModel cameraViewModel;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navHome = findViewById(R.id.nav_home);
        navClipboard = findViewById(R.id.nav_clipboard);
        navProfile = findViewById(R.id.nav_profile);
        fabCamera = findViewById(R.id.fab_camera);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Listener untuk update tombol saat fragment berubah (termasuk saat tekan Back)
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                if (id == R.id.homeFragment) {
                    setActiveButton(navHome);
                } else if (id == R.id.exploreFragment) {
                    setActiveButton(navClipboard);
                } else if (id == R.id.profileFragment) {
                    setActiveButton(navProfile);
                } else {
                    setActiveButton(null); // Tidak menyorot tombol apapun
                }
            });
        }

        // FAB ke kamera
        fabCamera.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigate(R.id.cameraFragment);
            }
        });

        // Navigasi tombol manual
        navHome.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigate(R.id.homeFragment);
            }
        });

        navClipboard.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigate(R.id.exploreFragment);
            }
        });

        navProfile.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigate(R.id.profileFragment);
            }
        });

        // ViewModel opsional
        cameraViewModel = new ViewModelProvider(this).get(CameraViewModel.class);

        // Default aktif: Home (opsional, bisa dihapus karena sudah di-handle listener)
        setActiveButton(navHome);
    }

    private void setActiveButton(@Nullable ImageButton activeButton) {
        navHome.setSelected(false);
        navClipboard.setSelected(false);
        navProfile.setSelected(false);

        if (activeButton != null) {
            activeButton.setSelected(true);
        }

        // Terapkan warna selector
        navHome.setImageTintList(ContextCompat.getColorStateList(this, R.color.bottom_nav_color_selector));
        navClipboard.setImageTintList(ContextCompat.getColorStateList(this, R.color.bottom_nav_color_selector));
        navProfile.setImageTintList(ContextCompat.getColorStateList(this, R.color.bottom_nav_color_selector));
    }
}
