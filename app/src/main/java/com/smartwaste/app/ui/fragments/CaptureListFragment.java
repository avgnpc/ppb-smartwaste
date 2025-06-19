package com.smartwaste.app.ui.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.smartwaste.app.R;
import com.smartwaste.app.model.Capture;
import com.smartwaste.app.ui.adapter.CaptureAdapter;
import com.smartwaste.app.viewmodel.CaptureViewModel;

import java.util.ArrayList;
import java.util.List;

public class CaptureListFragment extends Fragment {

    private CaptureViewModel viewModel;
    private CaptureAdapter adapter;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private boolean isFirstLoad = true;

    private DocumentSnapshot lastVisibleSnapshot = null;

    private ProgressBar progressBar;
    private Spinner filterSpinner;
    private RecyclerView recyclerView;

    private Boolean dibersihkanFilter = null;
    private final List<Capture> allCaptures = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_capture_list, container, false);

        viewModel = new ViewModelProvider(this).get(CaptureViewModel.class);

        recyclerView = view.findViewById(R.id.captureRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
//        filterSpinner = view.findViewById(R.id.spinnerFilter);

        adapter = new CaptureAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(capture -> {
            NavController navController = NavHostFragment.findNavController(this);
            Bundle bundle = new Bundle();
            bundle.putString("captureId", capture.getId());
            navController.navigate(R.id.action_exploreFragment_to_captureDetailFragment, bundle);
        });

        // Observe only the current user's captures
        viewModel.getUserCaptures().observe(getViewLifecycleOwner(), captures -> {
            adapter.submitList(captures != null ? new ArrayList<>(captures) : new ArrayList<>());
            progressBar.setVisibility(View.GONE);
        });

        MaterialButton buttonFilter = view.findViewById(R.id.buttonFilter);
        buttonFilter.setOnClickListener(v -> {
            // Show filter dialog or menu here
            // Example: show a simple dialog to select filter
            String[] options = {"Semua", "Dibersihkan", "Belum Dibersihkan"};
            new AlertDialog.Builder(requireContext())
                    .setTitle("Filter")
                    .setItems(options, (dialog, which) -> {
                        Boolean filter = null;
                        if (which == 1) filter = true;
                        else if (which == 2) filter = false;
                        if (!filterEquals(dibersihkanFilter, filter) || isFirstLoad) {
                            dibersihkanFilter = filter;
                            resetPagination();
                            loadNextPage();
                            isFirstLoad = false;
                        }
                    })
                    .show();
        });

//        setupFilterSpinner();

        setupRecyclerView();

        return view;
    }

    private void setupFilterSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.filter_dibersihkan,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);

        filterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Boolean filter = null;
                if (position == 1) filter = true;
                else if (position == 2) filter = false;

                // Only reload if filter actually changed
                if (!filterEquals(dibersihkanFilter, filter) || isFirstLoad) {
                    dibersihkanFilter = filter;
                    resetPagination();
                    loadNextPage();
                    isFirstLoad = false;
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private boolean filterEquals(Boolean a, Boolean b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    private void setupRecyclerView() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;

                int totalItemCount = lm.getItemCount();
                int lastVisibleItem = lm.findLastVisibleItemPosition();

                if (!isLoading && !isLastPage && totalItemCount <= (lastVisibleItem + 2)) {
                    loadNextPage();
                }
            }
        });
    }

    private void resetPagination() {
        allCaptures.clear();
        lastVisibleSnapshot = null;
        isLastPage = false;
        isLoading = false;
        adapter.submitList(new ArrayList<>());
    }

    private void loadNextPage() {
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        viewModel.fetchPaginatedCaptures(dibersihkanFilter, null, lastVisibleSnapshot, 10,
                captures -> {
                    if (captures.isEmpty()) {
                        isLastPage = true;
                    } else {
                        lastVisibleSnapshot = viewModel.getLastSnapshot();

                        for (Capture newCap : captures) {
                            boolean exists = false;
                            for (Capture existing : allCaptures) {
                                if (existing.getId() != null && existing.getId().equals(newCap.getId())) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                allCaptures.add(newCap);
                            }
                        }

                        adapter.submitList(new ArrayList<>(allCaptures));
                    }
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                },
                e -> {
                    Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                });
    }
}
