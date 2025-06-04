package com.smartwaste.app.viewmodel;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.smartwaste.app.model.RegisterResult;
import com.smartwaste.app.model.LoginResult;
import com.smartwaste.app.repository.AuthRepository;
import com.smartwaste.app.utils.PrefKeys;

public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private SharedPreferences sharedPreferences;

    private final MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();

    private final MutableLiveData<RegisterResult> registerResult = new MutableLiveData<>();


    public AuthViewModel() {
        authRepository = new AuthRepository();
    }

    public void setSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void register(String firstName, String lastName, String email, String password, String birthDate) {
        authRepository.registerUser(firstName, lastName, email, password, birthDate,
                () -> registerResult.postValue(new RegisterResult(true, null)),
                error -> registerResult.postValue(new RegisterResult(false, error.getMessage()))
        );
    }

    public LiveData<RegisterResult> getRegisterResult() {
        return registerResult;
    }

    public void loginUser(String email, String password) {
        authRepository.loginUser(email, password, () -> {
            loginResult.postValue(new LoginResult(true, null));
        }, error -> {
            loginResult.postValue(new LoginResult(false, error.getMessage()));
        });
    }

    public LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void rememberCredentials(String email, String password) {
        if (sharedPreferences != null) {
            sharedPreferences.edit()
                    .putString(PrefKeys.EMAIL, email)
                    .putString(PrefKeys.PASSWORD, password)
                    .apply();
        }
    }

    public void clearCredentials() {
        if (sharedPreferences != null) {
            sharedPreferences.edit().clear().apply();
        }
    }

    public void loadRememberedCredentials(MutableLiveData<String> email, MutableLiveData<String> password) {
        if (sharedPreferences != null) {
            email.postValue(sharedPreferences.getString(PrefKeys.EMAIL, ""));
            password.postValue(sharedPreferences.getString(PrefKeys.PASSWORD, ""));
        }
    }

    public FirebaseUser getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    // Optional existing observables if still needed elsewhere
    public LiveData<FirebaseUser> getUser() {
        return authRepository.getUserLiveData();
    }

    public LiveData<Boolean> getRegisterSuccess() {
        return authRepository.getRegisterSuccess();
    }

    public LiveData<String> getError() {
        return authRepository.getErrorMessage();
    }
}
