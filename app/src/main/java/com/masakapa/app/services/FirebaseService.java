package com.masakapa.app.services;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.masakapa.app.model.User;

public class FirebaseService {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void registerUser(String email, String password, User user,
                             OnSuccessListener<Void> onSuccess,
                             OnFailureListener onFailure) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = auth.getCurrentUser().getUid();
                    user.setUid(uid);
                    db.collection("users").document(uid).set(user)
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    public void loginUser(String email, String password,
                          OnSuccessListener<AuthResult> onSuccess,
                          OnFailureListener onFailure) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void logout() {
        auth.signOut();
    }
}
