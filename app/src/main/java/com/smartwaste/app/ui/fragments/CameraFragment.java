package com.smartwaste.app.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.ListenableFuture;
import com.smartwaste.app.R;
import com.smartwaste.app.analysis.RoboflowAnalyzer;
import com.smartwaste.app.ui.views.OverlayView;
import com.smartwaste.app.viewmodel.CameraViewModel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {
    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private PreviewView previewView;
    private OverlayView overlayView;
    private ExecutorService cameraExecutor;
    private ImageAnalysis imageAnalysis;
    private ProcessCameraProvider cameraProvider;
    private CameraViewModel viewModel;

    // Roboflow API info (replace with your real MODEL URL and API key)
    private static final String ROBOFLOW_API_KEY = "dK43MuIseHtrwhoCzlyL";
    private static final String ROBOFLOW_MODEL_URL =
            "https://serverless.roboflow.com/trash-detection-on-ocean-surface/15";

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_camera, container, false);
        previewView = root.findViewById(R.id.preview_view);
        overlayView = root.findViewById(R.id.overlay_view);

        // 1) Tell overlay the JPEG resolution that RoboflowAnalyzer will send
        overlayView.setImageSize(1088, 1088);

        // 2) Once PreviewView is laid out, capture its actual on-screen size
        previewView.post(() -> {
            int pvW = previewView.getWidth();
            int pvH = previewView.getHeight();
            Log.d("CameraFragment", "PreviewView size: " + pvW + "×" + pvH);

            // 3) Pass that to overlay, so it can compute scale + letterbox
            overlayView.setViewSize(pvW, pvH);
        });

        // Close button: pop back to previous screen
        root.findViewById(R.id.btn_close_camera).setOnClickListener(v ->
                requireActivity().onBackPressed()
        );

        // Init ViewModel for permission messages
        viewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 1) Request camera permission if needed
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera(); // Permission already granted
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Shutdown the background executor
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    // 2) Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.onCameraPermissionGranted();
                startCamera();
            } else {
                viewModel.onCameraPermissionDenied();
                Toast.makeText(requireContext(),
                        "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 3) Start CameraX preview + ImageAnalysis
    private void startCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor();

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // 3.1) Preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 3.1a) Ensure the preview is letterboxed (no cropping):
                previewView.setImplementationMode(
                        PreviewView.ImplementationMode.COMPATIBLE
                );

                // 3.2) ImageAnalysis use case
                //     Set target resolution to 1088×1088 so frames are square
                imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1088, 1088))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // 3.3) Assign our custom analyzer
                imageAnalysis.setAnalyzer(
                        cameraExecutor,
                        new RoboflowAnalyzer(
                                requireContext(),
                                overlayView,
                                ROBOFLOW_MODEL_URL,
                                ROBOFLOW_API_KEY
                        )
                );

                // 3.4) Select back camera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // 3.5) Bind to lifecycle
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        cameraSelector,
                        preview,
                        imageAnalysis
                );
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }
}
