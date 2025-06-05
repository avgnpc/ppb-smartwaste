package com.smartwaste.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.smartwaste.app.model.User;
import com.smartwaste.app.repository.UserRepository;

public class AccountViewModel extends ViewModel {

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final UserRepository userRepository = new UserRepository();

    public AccountViewModel() {
        fetchUserData();
    }

    public LiveData<User> getUser() {
        return userLiveData;
    }

    private void fetchUserData() {
        userRepository.getCurrentUser(userLiveData::setValue, e -> {
            // Optional: handle error (e.g. log or show toast)
        });
    }

    public void logout() {
        userRepository.logout();
    }
}
