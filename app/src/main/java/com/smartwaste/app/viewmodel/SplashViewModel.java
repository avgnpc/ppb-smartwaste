package com.smartwaste.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.smartwaste.app.repository.AuthRepository;

public class SplashViewModel extends ViewModel {

    private final AuthRepository authRepository;

    public SplashViewModel() {
        authRepository = new AuthRepository();
    }

    public LiveData<FirebaseUser> getUserLiveData() {
        return authRepository.getUserLiveData();
    }

    public FirebaseUser getCurrentUser() {
        return authRepository.getCurrentUser();
    }
}
