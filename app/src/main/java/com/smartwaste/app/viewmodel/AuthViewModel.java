package com.smartwaste.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseUser;
import com.smartwaste.app.model.LoginResult;
import com.smartwaste.app.model.RegisterResult;
import com.smartwaste.app.repository.AuthRepository;
import com.smartwaste.app.repository.LocationRepository;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final LocationRepository locationRepository;

    private final MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private final MutableLiveData<RegisterResult> registerResult = new MutableLiveData<>();
    private final MutableLiveData<String> userCity = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository();
        locationRepository = new LocationRepository(application.getApplicationContext());
    }

    public void register(String firstName, String lastName, String email, String password, String birthDate) {
        String city = userCity.getValue() != null ? userCity.getValue() : "Unknown";

        authRepository.registerUser(firstName, lastName, email, password, birthDate, city,
                () -> registerResult.postValue(new RegisterResult(true, null)),
                error -> registerResult.postValue(new RegisterResult(false, error.getMessage()))
        );
    }

    public LiveData<RegisterResult> getRegisterResult() {
        return registerResult;
    }

    public void loginUser(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            loginResult.postValue(new LoginResult(false, "Email dan password wajib diisi"));
            return;
        }

        authRepository.loginUser(email, password,
                () -> loginResult.postValue(new LoginResult(true, null)),
                error -> loginResult.postValue(new LoginResult(false, error.getMessage()))
        );
    }

    public LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public FirebaseUser getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    public LiveData<FirebaseUser> getUser() {
        return authRepository.getUserLiveData();
    }

    public LiveData<Boolean> getRegisterSuccess() {
        return authRepository.getRegisterSuccess();
    }

    public LiveData<String> getError() {
        return authRepository.getErrorMessage();
    }

    public void fetchUserCity() {
        locationRepository.fetchUserCity(new LocationRepository.LocationResultCallback() {
            @Override
            public void onCityFound(String cityName) {
                userCity.postValue(cityName);
            }

            @Override
            public void onError(String errorMessage) {
                userCity.postValue("Unknown");
            }
        });
    }

    public LiveData<String> getUserCity() {
        return userCity;
    }
}
