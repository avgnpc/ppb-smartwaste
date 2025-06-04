package com.smartwaste.app.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smartwaste.app.model.User;

public class AuthRepository {

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    private final MutableLiveData<FirebaseUser> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> registerSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AuthRepository() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userLiveData.postValue(mAuth.getCurrentUser());
        }
    }

    public void registerUser(String firstName, String lastName, String email, String password, String birthDate,
                             Runnable onSuccess, OnFailureListener onFailure) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        userLiveData.setValue(firebaseUser);

                        // Create a user object
                        User user = new User(
                                firebaseUser.getUid(),
                                firstName,
                                lastName,
                                email,
                                birthDate,
                                System.currentTimeMillis()
                        );

                        // Save user to Firestore
                        db.collection("users")
                                .document(firebaseUser.getUid())
                                .set(user)
                                .addOnSuccessListener(unused -> {
                                    registerSuccess.setValue(true);
                                    onSuccess.run();
                                })
                                .addOnFailureListener(e -> {
                                    errorMessage.setValue(e.getMessage());
                                    registerSuccess.setValue(false);
                                    onFailure.onFailure(e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue(e.getMessage());
                    registerSuccess.setValue(false);
                    onFailure.onFailure(e);
                });
    }

    public void loginUser(String email, String password, Runnable onSuccess, OnFailureListener onFailure) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    userLiveData.setValue(mAuth.getCurrentUser());
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue(e.getMessage());
                    onFailure.onFailure(e);
                });
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Boolean> getRegisterSuccess() {
        return registerSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}
