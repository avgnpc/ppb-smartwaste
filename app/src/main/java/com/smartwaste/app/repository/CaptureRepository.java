package com.smartwaste.app.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.smartwaste.app.model.Capture;

import java.util.ArrayList;
import java.util.List;

public class CaptureRepository {

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CollectionReference capturesRef = firestore.collection("captures");

    public LiveData<List<Capture>> getCapturesForCurrentUser() {
        MutableLiveData<List<Capture>> result = new MutableLiveData<>();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        capturesRef.whereEqualTo("user_id", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        result.setValue(new ArrayList<>());
                        return;
                    }

                    List<Capture> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Capture capture = doc.toObject(Capture.class);
                        if (capture != null) {
                            capture.setId(doc.getId());
                            capture.setSnapshot(doc); // Add snapshot for pagination
                            list.add(capture);
                        }
                    }
                    result.setValue(list);
                });

        return result;
    }

    public LiveData<List<Capture>> getAllCaptures() {
        MutableLiveData<List<Capture>> result = new MutableLiveData<>();

        capturesRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        result.setValue(new ArrayList<>());
                        return;
                    }

                    List<Capture> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Capture capture = doc.toObject(Capture.class);
                        if (capture != null) {
                            capture.setId(doc.getId());
                            capture.setSnapshot(doc);
                            list.add(capture);
                        }
                    }
                    result.setValue(list);
                });

        return result;
    }

    public void updateDibersihkan(String captureId, boolean dibersihkan,
                                  OnSuccessListener<Void> onSuccess,
                                  OnFailureListener onFailure) {
        capturesRef.document(captureId)
                .update("dibersihkan", dibersihkan)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void getPaginatedCaptures(Boolean dibersihkanFilter,
                                     String startDate,
                                     DocumentSnapshot lastVisible,
                                     int limit,
                                     OnSuccessListener<List<Capture>> onSuccess,
                                     OnFailureListener onFailure) {

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId == null) {
            onSuccess.onSuccess(new ArrayList<>());
            return;
        }

        Query query = capturesRef
                .whereEqualTo("user_id", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit);

        if (dibersihkanFilter != null) {
            query = query.whereEqualTo("dibersihkan", dibersihkanFilter);
        }

        if (startDate != null && !startDate.isEmpty()) {
            query = query.whereGreaterThanOrEqualTo("timestamp", startDate);
        }

        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.get()
                .addOnSuccessListener(snapshot -> {
                    List<Capture> result = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Capture cap = doc.toObject(Capture.class);
                        if (cap != null) {
                            cap.setId(doc.getId());
                            cap.setSnapshot(doc);
                            result.add(cap);
                        }
                    }
                    onSuccess.onSuccess(result);
                })
                .addOnFailureListener(onFailure);
    }

    public void getCaptureById(String captureId,
                               OnSuccessListener<Capture> onSuccess,
                               OnFailureListener onFailure) {
        if (captureId == null || captureId.isEmpty()) {
            onFailure.onFailure(new IllegalArgumentException("Capture ID is null or empty"));
            return;
        }

        capturesRef.document(captureId)
                .get(Source.SERVER) // ensure it always fetches from Firestore, not local cache
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Capture capture = doc.toObject(Capture.class);
                        if (capture != null) {
                            capture.setId(doc.getId());
                            capture.setSnapshot(doc);
                            onSuccess.onSuccess(capture);
                        } else {
                            onFailure.onFailure(new Exception("Document exists but couldn't parse Capture"));
                        }
                    } else {
                        onFailure.onFailure(new Exception("Capture not found for ID: " + captureId));
                    }
                })
                .addOnFailureListener(onFailure);
    }


}
