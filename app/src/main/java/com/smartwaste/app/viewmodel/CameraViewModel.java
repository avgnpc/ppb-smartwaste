package com.smartwaste.app.viewmodel;

import android.app.Application;
import android.content.Intent;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class CameraViewModel extends AndroidViewModel {

    public MutableLiveData<Boolean> requestCameraPermission = new MutableLiveData<>();
    public MutableLiveData<Intent> launchCameraIntent = new MutableLiveData<>();
    public MutableLiveData<String> message = new MutableLiveData<>();

    public CameraViewModel(@NonNull Application application) {
        super(application);
    }

    public void onCameraFabClicked() {
        requestCameraPermission.setValue(true);
    }

    public void onCameraPermissionGranted() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        launchCameraIntent.setValue(intent);
    }

    public void onCameraPermissionDenied() {
        message.setValue("Camera permission denied");
    }

    public void onCameraResultOk() {
        message.setValue("Photo captured successfully!");
    }
}
