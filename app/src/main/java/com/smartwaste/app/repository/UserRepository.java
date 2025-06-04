package com.smartwaste.app.repository;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.firebase.auth.AuthResult;
import com.smartwaste.app.model.User;
import com.smartwaste.app.services.FirebaseService;

public class UserRepository {
    private final FirebaseService firebaseService = new FirebaseService();

    public void getUserById(String uid,
                            OnSuccessListener<User> success,
                            OnFailureListener failure) {
        firebaseService.getUserById(uid, success, failure);
    }

    public void logout() {
        firebaseService.logout();
    }
}
