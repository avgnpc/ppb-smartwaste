package com.smartwaste.app.repository;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.smartwaste.app.utils.LocationUtil;

public class LocationRepository {

    public interface LocationResultCallback {
        void onCityFound(String cityName);
        void onError(String errorMessage);
    }

    private final Context context;
    private final LocationUtil locationUtil;

    public LocationRepository(Context context) {
        this.context = context;
        this.locationUtil = new LocationUtil(context);
    }

    public void fetchUserCity(LocationResultCallback callback) {
        locationUtil.getCity(context, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || locationResult.getLastLocation() == null) {
                    callback.onError("Lokasi tidak tersedia");
                    return;
                }

                Location location = locationResult.getLastLocation();
                String city = LocationUtil.getCityName(context, location);

                if (!"Unknown City".equals(city)) {
                    callback.onCityFound(city);
                } else {
                    callback.onError("Gagal mendapatkan nama kota");
                }
            }
        });
    }
}
