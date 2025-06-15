package com.smartwaste.app.ui.fragments;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;
import com.smartwaste.app.R;
import com.smartwaste.app.analysis.RoboflowAnalyzer;
import com.smartwaste.app.ui.views.OverlayView;
import com.smartwaste.app.viewmodel.CameraViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
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
    private RoboflowAnalyzer roboflowAnalyzer;
    private static final int REQUEST_LOCATION_PERMISSION = 1002;
    private volatile boolean isCapturing = false;


    // Roboflow API info (replace with your real MODEL URL and API key)
    private static final String ROBOFLOW_API_KEY = "dK43MuIseHtrwhoCzlyL";
    private static final String ROBOFLOW_MODEL_URL =
            "https://serverless.roboflow.com/plastic-recyclable-detection/2";

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

        Button captureButton = root.findViewById(R.id.btn_capture);
        captureButton.setOnClickListener(v -> captureSnapshot());

        return root;
    }

    private void captureSnapshot() {
        if (isCapturing) return; // Prevent multiple captures
        isCapturing = true;

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            isCapturing = false;
            return;
        }

        if (imageAnalysis == null) {
            isCapturing = false;
            return;
        }

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {

            imageAnalysis.clearAnalyzer();

            // 1) Get current timestamp
            String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    .format(new Date());

            // 2) Get geolocation
            getCurrentLocation((latitude, longitude) -> {
                // 3) Convert image to Bitmap (cropped to square)
                Bitmap bitmap = imageToBitmap(image);  // Implement this helper

                // 4) Get detection results from overlay
                Map<String, Integer> trashCounts = overlayView.getDetectionSummary(); // Implement this method

                // 5) Build JSON
                JSONObject json = new JSONObject();
                try {
                    json.put("timestamp", timestamp);

                    JSONObject locationObj = new JSONObject();
                    locationObj.put("latitude", latitude);
                    locationObj.put("longitude", longitude);
                    json.put("location", locationObj);

                    org.json.JSONArray detectionsArray = new org.json.JSONArray();
                    for (Map.Entry<String, Integer> entry : trashCounts.entrySet()) {
                        JSONObject detectionObj = new JSONObject();
                        detectionObj.put("class", entry.getKey());
                        detectionObj.put("count", entry.getValue());
                        detectionsArray.put(detectionObj);
                    }
                    json.put("detections", detectionsArray);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // 6) Save image and JSON to storage / log it
                saveImage(bitmap, timestamp); // Implement this
                saveJsonToDownloads(json, timestamp);
                Log.d("CaptureData", json.toString());
                isCapturing = false;
                image.close();
            });
        });
    }

    private void getCurrentLocation(LocationCallback callback) {
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Location permission not granted", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                callback.onLocationRetrieved(location.getLatitude(), location.getLongitude());
            } else {
                callback.onLocationRetrieved(0, 0); // fallback
            }
        });
    }

    private void saveImage(Bitmap bitmap, String timestamp) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "capture_" + timestamp.replace(" ", "_") + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SmartWaste");

        ContentResolver resolver = requireContext().getContentResolver();
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try (OutputStream out = resolver.openOutputStream(uri)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            Toast.makeText(requireContext(), "Image saved to gallery", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveJsonToDownloads(JSONObject json, String timestamp) {
        String fileName = "capture_" + timestamp.replace(" ", "_") + ".json";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SmartWaste");

        ContentResolver resolver = requireContext().getContentResolver();
        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }

        try (OutputStream out = resolver.openOutputStream(uri)) {
            out.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
            Toast.makeText(requireContext(), "JSON saved to Downloads", Toast.LENGTH_SHORT).show();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private interface LocationCallback {
        void onLocationRetrieved(double latitude, double longitude);
    }

    private Bitmap imageToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(
                nv21,
                ImageFormat.NV21,
                image.getWidth(),
                image.getHeight(),
                null
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 90, out);
        byte[] imageBytes = out.toByteArray();

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
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
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.onCameraPermissionGranted();
                startCamera();
            } else {
                viewModel.onCameraPermissionDenied();
                Toast.makeText(requireContext(),
                        "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureSnapshot(); // Retry capture after permission granted
            } else {
                Toast.makeText(requireContext(),
                        "Location permission denied", Toast.LENGTH_SHORT).show();
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
