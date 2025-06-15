package com.smartwaste.app.ui.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smartwaste.app.R;
import com.smartwaste.app.viewmodel.CameraViewModel;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FloatingActionButton fabCamera;
    private CameraViewModel cameraViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        fabCamera = findViewById(R.id.fab_camera);

        // 1) Set up the NavController
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);

            // 2) FAB click now simply navigates to CameraFragment
            fabCamera.setOnClickListener(v ->
                    navController.navigate(R.id.cameraFragment)
            );
        }

        // 3) (Optional) Instantiate the ViewModel if you want to show toasts/messages later
        cameraViewModel = new ViewModelProvider(this).get(CameraViewModel.class);
        // Note: We are not observing any LiveData in MainActivity now,
        // but the fragment can still post to cameraViewModel.message if needed.
    }
}
