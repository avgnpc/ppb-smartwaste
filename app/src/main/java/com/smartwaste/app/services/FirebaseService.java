package com.smartwaste.app.services;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.smartwaste.app.model.User;
import com.smartwaste.app.dto.FirestoreUserDTO;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.json.JSONArray;
import java.util.ArrayList;

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

    public void getUserById(String uid,
                            OnSuccessListener<User> success,
                            OnFailureListener failure) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    FirestoreUserDTO dto = documentSnapshot.toObject(FirestoreUserDTO.class);
                    if (dto != null) {
                        success.onSuccess(dto.toUser());
                    } else {
                        failure.onFailure(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(failure);
    }


    public String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            return null;
        }
    }

    public void logout() {
        auth.signOut();
    }

    public void uploadDetection(JSONObject json,
                                OnSuccessListener<Void> onSuccess,
                                OnFailureListener onFailure) {
        Map<String, Object> data = jsonToMap(json);
        db.collection("detections")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    Log.d("FirebaseService", "Detection uploaded: " + documentReference.getId());
                    onSuccess.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseService", "Upload failed", e);
                    onFailure.onFailure(e);
                });
    }

    private Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object value = json.get(key);
                map.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    public void saveCaptureData(JSONObject metadataJson, String imageUrl,
                                OnSuccessListener<Void> onSuccess,
                                OnFailureListener onFailure) {
        String uid = getCurrentUserId();
        if (uid == null) {
            onFailure.onFailure(new Exception("User not logged in"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        try {
            // Timestamp
            data.put("timestamp", metadataJson.getString("timestamp"));

            // Location
            if (metadataJson.has("location")) {
                JSONObject location = metadataJson.getJSONObject("location");
                data.put("location", jsonToMap(location));
            }

            // Detections
            if (metadataJson.has("detections")) {
                JSONArray detections = metadataJson.getJSONArray("detections");
                List<Map<String, Object>> detectionList = new ArrayList<>();
                for (int i = 0; i < detections.length(); i++) {
                    JSONObject detectionObj = detections.getJSONObject(i);
                    detectionList.add(jsonToMap(detectionObj));
                }
                data.put("detections", detectionList);
            }

            // Image URL
            data.put("imageUrl", imageUrl);

            if (metadataJson.has("user_id")) {
                data.put("user_id", metadataJson.getString("user_id"));
            }

            if (metadataJson.has("dibersihkan")) {
                data.put("dibersihkan", metadataJson.getBoolean("dibersihkan"));
            } else {
                data.put("dibersihkan", false); // fallback
            }

        } catch (JSONException e) {
            onFailure.onFailure(e);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("captures")
                .add(data)
                .addOnSuccessListener(documentReference -> onSuccess.onSuccess(null))
                .addOnFailureListener(onFailure);
    }

// In FirebaseService.java

    public void updateCaptureStatus(String captureId, boolean dibersihkan, java.util.function.Consumer<Boolean> callback) {
        FirebaseFirestore.getInstance()
                .collection("captures")
                .document(captureId)
                .update("dibersihkan", dibersihkan)
                .addOnSuccessListener(aVoid -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }
}
