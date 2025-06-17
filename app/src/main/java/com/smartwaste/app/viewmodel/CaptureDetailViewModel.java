package com.smartwaste.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.smartwaste.app.model.Capture;
import com.smartwaste.app.repository.CaptureRepository;

public class CaptureDetailViewModel extends ViewModel {

    private final CaptureRepository repository = new CaptureRepository();

    private final MutableLiveData<Capture> captureLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> updateSuccess = new MutableLiveData<>();

    public LiveData<Capture> getCaptureLiveData() {
        return captureLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getUpdateSuccess() {
        return updateSuccess;
    }

    public void fetchCaptureById(String captureId) {
        if (captureId == null || captureId.isEmpty()) {
            errorMessage.setValue("Invalid capture ID");
            return;
        }

        isLoading.setValue(true);

        repository.getCaptureById(
                captureId,
                capture -> {
                    isLoading.setValue(false);
                    captureLiveData.setValue(capture);
                },
                e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("Failed to load capture: " + e.getMessage());
                }
        );
    }

    public void markCaptureAsDibersihkan(String captureId) {
        isLoading.setValue(true);
        repository.updateDibersihkan(
                captureId,
                true,
                unused -> {
                    isLoading.setValue(false);
                    updateSuccess.setValue(true);
                    fetchCaptureById(captureId); // Refresh capture data after update
                },
                e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("Failed to update: " + e.getMessage());
                }
        );
    }
}
