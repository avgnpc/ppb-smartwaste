package com.smartwaste.app.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.smartwaste.app.R;
import com.smartwaste.app.model.Capture;
import com.smartwaste.app.viewmodel.CaptureDetailViewModel;

public class CaptureDetailFragment extends Fragment {

    private CaptureDetailViewModel viewModel;
    private ProgressBar progressBar;
    private TextView rawText;
    private Button buttonMarkCleaned;

    private String captureId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_capture_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CaptureDetailViewModel.class);

        progressBar = view.findViewById(R.id.detailProgressBar);
        rawText = view.findViewById(R.id.textRawJson);
        buttonMarkCleaned = view.findViewById(R.id.buttonMarkCleaned);

        captureId = getArguments() != null ? getArguments().getString("captureId") : null;

        if (captureId == null) {
            Toast.makeText(getContext(), "Capture ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        setupObservers();
        viewModel.fetchCaptureById(captureId);

        buttonMarkCleaned.setOnClickListener(v -> {
            viewModel.markCaptureAsDibersihkan(captureId);
        });
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getCaptureLiveData().observe(getViewLifecycleOwner(), capture -> {
            if (capture != null && capture.getSnapshot() != null) {
                rawText.setText(capture.getSnapshot().getData().toString());

                // Hide button if already marked
                if (Boolean.TRUE.equals(capture.getSnapshot().getBoolean("dibersihkan"))) {
                    buttonMarkCleaned.setEnabled(false);
                    buttonMarkCleaned.setText("Already Cleaned");
                } else {
                    buttonMarkCleaned.setEnabled(true);
                    buttonMarkCleaned.setText("Mark as Dibersihkan");
                }
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getUpdateSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(getContext(), "Marked as dibersihkan!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
