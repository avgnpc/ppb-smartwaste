package com.smartwaste.app.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationUtil {
    private final FusedLocationProviderClient locationClient;

    public LocationUtil(Context context) {
        locationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void getCity(Context context, LocationCallback callback) {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1)
                .build();

        locationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper());
    }

    public static String getCityName(Context context, Location location) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);

                // Force Surabaya-style city level
                String city = address.getSubAdminArea(); // <-- Prefer this
                if (city == null || city.isEmpty()) {
                    city = address.getLocality(); // fallback to district
                }
                if (city == null || city.isEmpty()) {
                    city = address.getAdminArea(); // fallback to province
                }
                if (city == null || city.isEmpty()) {
                    city = "Unknown City";
                }

                return city;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown City";
    }


}
