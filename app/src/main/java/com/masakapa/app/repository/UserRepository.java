package com.masakapa.app.repository;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.masakapa.app.model.User;
import com.masakapa.app.services.FirebaseService;

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

    public void logout() {
        firebaseService.logout();
    }
}
