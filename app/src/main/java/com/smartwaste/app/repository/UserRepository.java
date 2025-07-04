package com.smartwaste.app.repository;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.smartwaste.app.model.User;
import com.smartwaste.app.services.FirebaseService;

public class UserRepository {
    private final FirebaseService firebaseService = new FirebaseService();

    public void register(String email, String password, User user,
                         OnSuccessListener<Void> success,
                         OnFailureListener failure) {
        firebaseService.registerUser(email, password, user, success, failure);
    }

    public void login(String email, String password,
                      OnSuccessListener<AuthResult> success,
                      OnFailureListener failure) {
        firebaseService.loginUser(email, password, success, failure);
    }

    public void getUserById(String uid,
                            OnSuccessListener<User> success,
                            OnFailureListener failure) {
        firebaseService.getUserById(uid, success, failure);
    }

    public void getCurrentUser(OnSuccessListener<User> success,
                               OnFailureListener failure) {
        String uid = firebaseService.getCurrentUserId();
        if (uid != null) {
            firebaseService.getUserById(uid, success, failure);
        } else {
            failure.onFailure(new Exception("User not logged in"));
        }
    }

    public void logout() {
        firebaseService.logout();
    }
}
