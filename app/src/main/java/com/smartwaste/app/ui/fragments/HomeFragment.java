package com.smartwaste.app.ui.fragments;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.smartwaste.app.R;
import com.smartwaste.app.model.Capture;
import com.smartwaste.app.ui.adapter.CaptureAdapter;
import com.smartwaste.app.utils.AreaData;
import com.smartwaste.app.utils.LocationUtil;
import com.smartwaste.app.viewmodel.AccountViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.clustering.*;
import com.google.maps.android.clustering.view.*;
import com.google.maps.android.heatmaps.*;


public class HomeFragment extends Fragment implements CaptureAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private CaptureAdapter captureAdapter;
    private AccountViewModel viewModel;

    private TextView tvUserName;
    private TextView tvLocation;

    private MapView mapView;
    private GoogleMap googleMap;
    private ClusterManager<TrashClusterItem> clusterManager;
    private Map<String, AreaData> areaDataMap = new HashMap<>();
    private final Map<Circle, AreaData> circleAreaDataMap = new HashMap<>();

    public HomeFragment() {
        // Required empty public constructor
    }

    public static class TrashClusterRenderer extends DefaultClusterRenderer<TrashClusterItem> {
        public TrashClusterRenderer(Context context, GoogleMap map, ClusterManager<TrashClusterItem> clusterManager) {
            super(context, map, clusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(TrashClusterItem item, MarkerOptions markerOptions) {
            markerOptions.title(item.getTitle()).snippet(item.getSnippet());
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        recyclerView = view.findViewById(R.id.rvCaptures);
        recyclerView = view.findViewById(R.id.rvCaptures);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvLocation = view.findViewById(R.id.tvLocation);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        captureAdapter = new CaptureAdapter();
        captureAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(captureAdapter);

        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this::onMapReady);

        loadCapturesFromFirestore();
        observeUser();
    }

    private void observeUser() {
        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                tvUserName.setText("Halo " + user.getName() + "!");
                tvLocation.setText(user.getLocation());
            }
        });
    }

    private void loadCapturesFromFirestore() {
        FirebaseFirestore.getInstance()
                .collection("captures")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Capture> list = new ArrayList<>();
                    Map<String, Integer> subLocalityCounts = new HashMap<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        Capture capture = doc.toObject(Capture.class);
                        if (capture != null) {
                            capture.setId(doc.getId());
                            capture.setSnapshot(doc);
                            list.add(capture);

                            // Aggregate subLocality
                            Map<String, Object> locMap = (Map<String, Object>) doc.get("location");
                            if (locMap != null && locMap.get("latitude") != null && locMap.get("longitude") != null) {
                                double lat = (double) locMap.get("latitude");
                                double lon = (double) locMap.get("longitude");

                                Location location = new Location("");
                                location.setLatitude(lat);
                                location.setLongitude(lon);

                                String subLocality = LocationUtil.getCityName(requireContext(), location);
                                if (subLocality != null) {
                                    subLocalityCounts.put(subLocality, subLocalityCounts.getOrDefault(subLocality, 0) + 1);
                                }
                            }
                        }
                    }

                    captureAdapter.submitList(list);

                    // Log aggregated locations
                    for (Map.Entry<String, Integer> entry : subLocalityCounts.entrySet()) {
                        Log.d("SubLocalityAgg", entry.getKey() + " = " + entry.getValue());
                    }

                    // 1. Collect all coordinates for each sublocality
                    Map<String, List<LatLng>> areaCoordinates = new HashMap<>();

                    // After areaCoordinates is filled
                    List<LatLng> allPoints = new ArrayList<>();
                    for (List<LatLng> coords : areaCoordinates.values()) {
                        allPoints.addAll(coords);
                    }
                    if (googleMap != null && !allPoints.isEmpty()) {
                        showHeatmap(allPoints);
                    }

                    for (DocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> locMap = (Map<String, Object>) doc.get("location");
                        if (locMap != null && locMap.get("latitude") != null && locMap.get("longitude") != null) {
                            double lat = (double) locMap.get("latitude");
                            double lon = (double) locMap.get("longitude");
                            Location location = new Location("");
                            location.setLatitude(lat);
                            location.setLongitude(lon);
                            String subLocality = LocationUtil.getCityName(requireContext(), location);
                            if (subLocality != null) {
                                areaCoordinates.computeIfAbsent(subLocality, k -> new ArrayList<>()).add(new LatLng(lat, lon));
                            }
                        }
                    }

                    // 2. For each area, use the average coordinate for the cluster
                    areaDataMap.clear();
                    for (Map.Entry<String, List<LatLng>> entry : areaCoordinates.entrySet()) {
                        String areaName = entry.getKey();
                        List<LatLng> coords = entry.getValue();
                        int trashCount = coords.size();
                        double avgLat = 0, avgLng = 0;
                        for (LatLng ll : coords) {
                            avgLat += ll.latitude;
                            avgLng += ll.longitude;
                        }
                        avgLat /= trashCount;
                        avgLng /= trashCount;
                        areaDataMap.put(areaName, new AreaData(avgLat, avgLng, areaName, trashCount));
                    }

                    if (googleMap != null && clusterManager != null) {
                        showTrashDensityCircles(areaDataMap);
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Gagal ambil data", e);
                    Toast.makeText(getContext(), "Gagal ambil data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void onMapReady(GoogleMap map) {
        googleMap = map;
        LatLng surabaya = new LatLng(-7.2575, 112.7521);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(surabaya, 12f));
        setUpClusterer();
    }

    private void setUpClusterer() {
        clusterManager = new ClusterManager<>(getContext(), googleMap);
        clusterManager.setRenderer(new TrashClusterRenderer(getContext(), googleMap, clusterManager));
        googleMap.setOnCameraIdleListener(clusterManager);
        googleMap.setOnMarkerClickListener(clusterManager);

        clusterManager.setOnClusterItemInfoWindowClickListener(item -> {
            // Optional: handle info window click
        });

        googleMap.setInfoWindowAdapter(clusterManager.getMarkerManager());
        clusterManager.getMarkerCollection().setInfoWindowAdapter(new CustomInfoWindowAdapter());

        // Add aggregate data to the cluster manager
        if (areaDataMap != null) {
            clusterManager.clearItems();
            for (AreaData data : areaDataMap.values()) {
                clusterManager.addItem(new TrashClusterItem(data.lat, data.lng, data.areaName, data.trashCount));
            }
            clusterManager.cluster();
        }
    }

    // Call this after you have your aggregate data
    private void showTrashClusters(Map<String, AreaData> areaDataMap) {
        clusterManager.clearItems();
        for (AreaData data : areaDataMap.values()) {
            clusterManager.addItem(new TrashClusterItem(data.lat, data.lng, data.areaName, data.trashCount));
        }
        clusterManager.cluster();
    }

    // Custom cluster item
    public static class TrashClusterItem implements ClusterItem {
        private final LatLng position;
        private final String areaName;
        private final int trashCount;

        public TrashClusterItem(double lat, double lng, String areaName, int trashCount) {
            this.position = new LatLng(lat, lng);
            this.areaName = areaName;
            this.trashCount = trashCount;
        }

        @NonNull
        @Override
        public LatLng getPosition() { return position; }
        @Nullable
        @Override
        public String getTitle() { return areaName; }
        @Nullable
        @Override
        public String getSnippet() { return "Trash count: " + trashCount; }
    }

    // Custom info window
    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        @Override
        public View getInfoWindow(Marker marker) { return null; }
        @Override
        public View getInfoContents(Marker marker) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.info_window_trash, null);
            ((TextView) view.findViewById(R.id.tvAreaName)).setText(marker.getTitle());
            ((TextView) view.findViewById(R.id.tvTrashCount)).setText(marker.getSnippet());
            return view;
        }
    }

    // Optionally, add a heatmap overlay
    private void showHeatmap(List<LatLng> points) {
        HeatmapTileProvider provider = new HeatmapTileProvider.Builder().data(points).build();
        googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
    }

    @Override
    public void onItemClick(Capture capture) {
        if (capture != null && capture.getId() != null) {
            Bundle bundle = new Bundle();
            bundle.putString("captureId", capture.getId());
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_homeFragment_to_captureDetailFragment, bundle);
        }
    }

    private void showTrashDensityCircles(Map<String, AreaData> areaDataMap) {
        if (googleMap == null) return;

        googleMap.clear();
        circleAreaDataMap.clear();

        int maxTrash = 1;
        for (AreaData data : areaDataMap.values()) {
            if (data.trashCount > maxTrash) maxTrash = data.trashCount;
        }

        for (AreaData data : areaDataMap.values()) {
            int radius = 400 + (data.trashCount * 120);
            float density = (float) data.trashCount / maxTrash;
            int fillColor = Color.argb((int)(80 + 175 * density), 255, 87, 34);

            Circle circle = googleMap.addCircle(new CircleOptions()
                    .center(new LatLng(data.lat, data.lng))
                    .radius(radius)
                    .strokeColor(Color.argb(180, 255, 87, 34))
                    .strokeWidth(3f)
                    .fillColor(fillColor)
                    .clickable(true) // Enable click
            );
            circleAreaDataMap.put(circle, data);
        }

        googleMap.setOnCircleClickListener(clickedCircle -> {
            AreaData data = circleAreaDataMap.get(clickedCircle);
            if (data != null) {
                // Show a dialog with area/trash details
                showAreaDetailDialog(data);
            }
        });
    }

    //show a simple dialog with area details
    private void showAreaDetailDialog(AreaData data) {
        View infoView = LayoutInflater.from(requireContext()).inflate(R.layout.info_window_trash, null);
        ((TextView) infoView.findViewById(R.id.tvAreaName)).setText(data.areaName);
        ((TextView) infoView.findViewById(R.id.tvTrashCount)).setText("Jumlah Sampah: " + data.trashCount);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(infoView)
                .setPositiveButton("OK", null)
                .show();
    }
}
