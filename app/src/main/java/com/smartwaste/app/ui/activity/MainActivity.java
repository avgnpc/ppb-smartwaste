package com.smartwaste.app.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smartwaste.app.R;
import com.smartwaste.app.viewmodel.CameraViewModel;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private CameraViewModel cameraViewModel;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    cameraViewModel.onCameraPermissionGranted();
                } else {
                    cameraViewModel.onCameraPermissionDenied();
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    cameraViewModel.onCameraResultOk();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);

        // Navigation setup
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        // ViewModel
        cameraViewModel = new ViewModelProvider(this).get(CameraViewModel.class);

        // FAB click tells ViewModel
        findViewById(R.id.fab_camera).setOnClickListener(v -> {
            cameraViewModel.onCameraFabClicked();
        });

        // Observe ViewModel
        observeCameraState();
    }

    private void observeCameraState() {
        cameraViewModel.requestCameraPermission.observe(this, shouldRequest -> {
            if (Boolean.TRUE.equals(shouldRequest)) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    cameraViewModel.onCameraPermissionGranted();
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA);
                }
            }
        });

        cameraViewModel.launchCameraIntent.observe(this, intent -> {
            cameraLauncher.launch(intent);
        });

        cameraViewModel.message.observe(this, msg -> {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }
}
