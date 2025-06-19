package com.smartwaste.app.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.smartwaste.app.R;
import com.smartwaste.app.model.Capture;
import com.smartwaste.app.ui.adapter.CaptureAdapter;
import com.smartwaste.app.viewmodel.AccountViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements CaptureAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private CaptureAdapter captureAdapter;
    private AccountViewModel viewModel;

    private TextView tvUserName;
    private TextView tvLocation;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        recyclerView = view.findViewById(R.id.rvCaptures);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvLocation = view.findViewById(R.id.tvLocation);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        captureAdapter = new CaptureAdapter();
        captureAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(captureAdapter);

        loadCapturesFromFirestore();
        observeUser();
    }

    private void observeUser() {
        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                tvUserName.setText("Halo " + user.getName()+"!");
                tvLocation.setText(user.getLocation());
            }
        });
    }

    private void loadCapturesFromFirestore() {
        FirebaseFirestore.getInstance()
                .collection("captures")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Capture> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Capture capture = doc.toObject(Capture.class);
                        if (capture != null) {
                            capture.setId(doc.getId());
                            capture.setSnapshot(doc);
                            list.add(capture);
                        }
                    }

                    captureAdapter.submitList(list);
                    Log.d("HomeFragment", "Jumlah data: " + list.size());
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Gagal ambil data", e);
                    Toast.makeText(getContext(), "Gagal ambil data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onItemClick(Capture capture) {
        if (capture != null && capture.getId() != null) {
            Bundle bundle = new Bundle();
            bundle.putString("captureId", capture.getId());
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_homeFragment_to_captureDetailFragment, bundle);
        }
    }
}
