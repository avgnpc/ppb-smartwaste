package com.smartwaste.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.smartwaste.app.model.Capture;
import com.smartwaste.app.repository.CaptureRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CaptureViewModel extends ViewModel {

    private final CaptureRepository repository = new CaptureRepository();

    private final MutableLiveData<List<Capture>> pagedCaptures = new MutableLiveData<>(new ArrayList<>());
    private DocumentSnapshot lastVisible = null;
    private boolean isLoading = false;
    private final LiveData<List<Capture>> userCaptures = repository.getCapturesForCurrentUser();

    private Boolean filterDibersihkan = null;
    private String filterStartDate = null;

    private static final int PAGE_SIZE = 10;

    public LiveData<List<Capture>> getUserCaptures() {
        return repository.getCapturesForCurrentUser();
    }

    public LiveData<List<Capture>> getAllCaptures() {
        return repository.getAllCaptures();
    }

    public LiveData<List<Capture>> getPagedCaptures() {
        return pagedCaptures;
    }

    public void resetPagination() {
        pagedCaptures.setValue(new ArrayList<>());
        lastVisible = null;
    }

    public void setFilter(Boolean dibersihkan, String startDate) {
        this.filterDibersihkan = dibersihkan;
        this.filterStartDate = startDate;
        resetPagination();
        loadNextPage();
    }

    public void loadNextPage() {
        if (isLoading) return;
        isLoading = true;

        repository.getPaginatedCaptures(
                filterDibersihkan,
                filterStartDate,
                lastVisible,
                PAGE_SIZE,
                newCaptures -> {
                    List<Capture> currentList = pagedCaptures.getValue();
                    if (currentList == null) currentList = new ArrayList<>();

                    // Avoid duplicates
                    Set<String> existingIds = new HashSet<>();
                    for (Capture c : currentList) {
                        existingIds.add(c.getId());
                    }

                    for (Capture cap : newCaptures) {
                        if (!existingIds.contains(cap.getId())) {
                            currentList.add(cap);
                        }
                    }

                    pagedCaptures.postValue(currentList);

                    if (!newCaptures.isEmpty()) {
                        lastVisible = newCaptures.get(newCaptures.size() - 1).getSnapshot();
                    }

                    isLoading = false;
                },
                e -> {
                    isLoading = false;
                    // Log or handle error
                }
        );
    }

    public void fetchPaginatedCaptures(Boolean dibersihkanFilter,
                                       String startDate,
                                       DocumentSnapshot lastVisible,
                                       int limit,
                                       OnSuccessListener<List<Capture>> onSuccess,
                                       OnFailureListener onFailure) {
        repository.getPaginatedCaptures(
                dibersihkanFilter,
                startDate,
                lastVisible,
                limit,
                captures -> {
                    if (!captures.isEmpty()) {
                        this.lastVisible = captures.get(captures.size() - 1).getSnapshot();
                    }
                    onSuccess.onSuccess(captures);
                },
                onFailure
        );
    }

    public DocumentSnapshot getLastSnapshot() {
        return lastVisible;
    }

    public void setDibersihkan(String id, boolean value,
                               OnSuccessListener<Void> onSuccess,
                               OnFailureListener onFailure) {
        repository.updateDibersihkan(id, value, onSuccess, onFailure);
    }
}
