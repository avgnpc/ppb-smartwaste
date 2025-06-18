package com.smartwaste.app.ui.fragments;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.smartwaste.app.R;
import com.smartwaste.app.model.Capture;
import com.smartwaste.app.services.FirebaseService;
import com.smartwaste.app.viewmodel.CaptureDetailViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import android.graphics.Typeface;

public class CaptureDetailFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private ImageView imageView;
    private TextView textTimestamp, textUserId, textStatus;
    private CaptureDetailViewModel viewModel;
    private Capture capture;
    private Button buttonBersihkan;
    private LinearLayout layoutDetections;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_capture_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapView = view.findViewById(R.id.mapView);
        imageView = view.findViewById(R.id.imageView);
        textTimestamp = view.findViewById(R.id.textTimestamp);
//        textUserId = view.findViewById(R.id.textUserId);
        textStatus = view.findViewById(R.id.textStatus);
        buttonBersihkan = view.findViewById(R.id.buttonBersihkan);
        layoutDetections = view.findViewById(R.id.layoutDetections);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        viewModel = new ViewModelProvider(this).get(CaptureDetailViewModel.class);

        String captureId = getArguments() != null ? getArguments().getString("captureId") : null;
        if (captureId != null) {
            viewModel.getCaptureLiveData().observe(getViewLifecycleOwner(), cap -> {
                if (cap != null) {
                    capture = cap;
                    bindData();
                }
            });
            viewModel.fetchCaptureById(captureId);
        }
    }

    private void bindData() {
        if (capture == null) return;

        Glide.with(this).load(capture.getImageUrl()).into(imageView);

        // Format timestamp
        String formattedTime = capture.getTimestamp();
        try {
            // Example: if timestamp is ISO 8601 or millis, adjust parsing accordingly
            long millis = Long.parseLong(capture.getTimestamp());
            Date date = new Date(millis);
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            formattedTime = sdf.format(date);
        } catch (Exception ignored) {}

        textTimestamp.setText("Waktu laporan: " + formattedTime);

        // Status color
        boolean sudah = capture.isDibersihkan();
        textStatus.setText("Status Sampah: " + (sudah ? "Dibersihkan" : "Belum Dibersihkan"));
        int bgRes = sudah ? R.drawable.status_bg_green : R.drawable.status_bg_red;
        textStatus.setBackground(ContextCompat.getDrawable(requireContext(), bgRes));

        // Show/hide and handle Bersihkan button
        if (!capture.isDibersihkan()) {
            buttonBersihkan.setVisibility(View.VISIBLE);
            buttonBersihkan.setOnClickListener(v -> markAsCleaned());
        } else {
            buttonBersihkan.setVisibility(View.GONE);
        }

        // Populate detected objects table
        layoutDetections.removeAllViews();

        // Header row
        TableRow header = new TableRow(requireContext());
        TextView th1 = new TextView(requireContext());
        th1.setText("Jenis Sampah");
        th1.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        th1.setGravity(Gravity.CENTER);
        th1.setPadding(16, 8, 16, 8);
        th1.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.table_header_bg));
        th1.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));

        TextView th2 = new TextView(requireContext());
        th2.setText("Jumlah");
        th2.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        th2.setGravity(Gravity.CENTER);
        th2.setPadding(16, 8, 16, 8);
        th2.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.table_header_bg));
        th2.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));

        header.addView(th1);
        header.addView(th2);
        layoutDetections.addView(header);

        if (capture.getDetections() != null && !capture.getDetections().isEmpty()) {
            int i = 0;
            for (Map<String, Object> detection : capture.getDetections()) {
                String className = String.valueOf(detection.get("class"));
                int count = detection.get("count") != null ? ((Number) detection.get("count")).intValue() : 1;

                TableRow row = new TableRow(requireContext());

                int rowColor = (i % 2 == 0)
                        ? ContextCompat.getColor(requireContext(), R.color.table_row_bg)
                        : ContextCompat.getColor(requireContext(), R.color.table_row_alt_bg);

                TextView tvClass = new TextView(requireContext());
                tvClass.setText(className);
                tvClass.setGravity(Gravity.CENTER);
                tvClass.setPadding(16, 8, 16, 8);
                tvClass.setBackgroundColor(rowColor);

                TextView tvCount = new TextView(requireContext());
                tvCount.setText(String.valueOf(count));
                tvCount.setGravity(Gravity.CENTER);
                tvCount.setPadding(16, 8, 16, 8);
                tvCount.setBackgroundColor(rowColor);

                row.addView(tvClass);
                row.addView(tvCount);
                layoutDetections.addView(row);
                i++;
            }
        } else {
            TableRow row = new TableRow(requireContext());
            TextView tv = new TextView(requireContext());
            tv.setText("Tidak ada deteksi.");
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(16, 8, 16, 8);
            tv.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.table_row_bg));
            row.addView(tv);
            layoutDetections.addView(row);
        }

        if (googleMap != null) setMapLocation();
    }

    private void markAsCleaned() {
        buttonBersihkan.setEnabled(false);
        new FirebaseService().updateCaptureStatus(capture.getId(), true, success -> {
            if (success) {
                Toast.makeText(requireContext(), "Status updated!", Toast.LENGTH_SHORT).show();
                capture.setDibersihkan(true);
                bindData();
            } else {
                Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show();
                buttonBersihkan.setEnabled(true);
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        setMapLocation();
    }

    private void setMapLocation() {
        if (capture == null || capture.getLocation() == null) return;
        Map<String, Object> loc = capture.getLocation();
        if (loc.containsKey("latitude") && loc.containsKey("longitude")) {
            double lat = ((Number) loc.get("latitude")).doubleValue();
            double lng = ((Number) loc.get("longitude")).doubleValue();
            LatLng latLng = new LatLng(lat, lng);
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(latLng).title("Capture Location"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
        }
    }

    // MapView lifecycle methods
    @Override public void onResume() { super.onResume(); mapView.onResume(); }
    @Override public void onPause() { super.onPause(); mapView.onPause(); }
    @Override public void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}