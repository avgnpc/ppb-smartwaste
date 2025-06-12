package com.smartwaste.app.ui.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.smartwaste.app.R;
import com.smartwaste.app.viewmodel.AuthViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private EditText firstNameEdit, lastNameEdit, emailEdit, birthDateEdit, passwordEdit, confirmPasswordEdit;
    private Button registerBtn;
    private TextView loginLink;
    private ImageButton backButton;

    private final Calendar calendar = Calendar.getInstance();
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // ViewModel
        authViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(AuthViewModel.class);

        // View Binding
        firstNameEdit = findViewById(R.id.first_name);
        lastNameEdit = findViewById(R.id.last_name);
        emailEdit = findViewById(R.id.email);
        birthDateEdit = findViewById(R.id.birth_date);
        passwordEdit = findViewById(R.id.password);
        confirmPasswordEdit = findViewById(R.id.confirm_password);
        registerBtn = findViewById(R.id.register_btn);
        loginLink = findViewById(R.id.link_login);
        backButton = findViewById(R.id.back_button);

        // Actions
        birthDateEdit.setOnClickListener(v -> showDatePicker());
        registerBtn.setOnClickListener(v -> attemptRegister());
        loginLink.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        backButton.setOnClickListener(v -> {
            // Back button behavior: go to LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        observeViewModel();
        checkLocationPermissionOrFetchCity();
    }

    private void showDatePicker() {
        new DatePickerDialog(
                this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    String formattedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(calendar.getTime());
                    birthDateEdit.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void attemptRegister() {
        String firstName = firstNameEdit.getText().toString().trim();
        String lastName = lastNameEdit.getText().toString().trim();
        String email = emailEdit.getText().toString().trim();
        String birthDate = birthDateEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();
        String confirmPassword = confirmPasswordEdit.getText().toString().trim();

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) ||
                TextUtils.isEmpty(email) || TextUtils.isEmpty(birthDate) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.register(firstName, lastName, email, password, birthDate);
    }

    private void observeViewModel() {
        authViewModel.getRegisterResult().observe(this, result -> {
            if (result == null) return;

            if (result.isSuccess()) {
                Toast.makeText(this, "Pendaftaran berhasil", Toast.LENGTH_SHORT).show();
                clearFields();
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Gagal daftar: " + result.getErrorMessage(), Toast.LENGTH_LONG).show();
            }
        });

        authViewModel.getUserCity().observe(this, city -> {
            // Optional: show user city for debugging or logging
        });
    }

    private void clearFields() {
        firstNameEdit.setText("");
        lastNameEdit.setText("");
        emailEdit.setText("");
        birthDateEdit.setText("");
        passwordEdit.setText("");
        confirmPasswordEdit.setText("");
    }

    private void checkLocationPermissionOrFetchCity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            authViewModel.fetchUserCity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Izin lokasi diberikan", Toast.LENGTH_SHORT).show();
                authViewModel.fetchUserCity();
            } else {
                Toast.makeText(this, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show();
                // We can still register user with "Unknown" city
            }
        }
    }
}
