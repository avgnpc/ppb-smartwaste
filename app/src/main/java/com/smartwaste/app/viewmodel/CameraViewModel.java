package com.smartwaste.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class CameraViewModel extends AndroidViewModel {
    public MutableLiveData<String> message = new MutableLiveData<>();

    public CameraViewModel(@NonNull Application application) {
        super(application);
    }

    public void onCameraPermissionGranted() {
        message.setValue("Camera permission granted");
    }

    public void onCameraPermissionDenied() {
        message.setValue("Camera permission denied");
    }
}
