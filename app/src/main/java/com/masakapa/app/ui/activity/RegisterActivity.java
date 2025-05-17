package com.masakapa.app.ui.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.masakapa.app.R;
import com.masakapa.app.viewmodel.AuthViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private EditText firstNameEdit, lastNameEdit, emailEdit, birthDateEdit, passwordEdit, confirmPasswordEdit;
    private Button registerBtn;
    private TextView loginLink;

    private final Calendar calendar = Calendar.getInstance();
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Bind views
        firstNameEdit = findViewById(R.id.first_name);
        lastNameEdit = findViewById(R.id.last_name);
        emailEdit = findViewById(R.id.email);
        birthDateEdit = findViewById(R.id.birth_date);
        passwordEdit = findViewById(R.id.password);
        confirmPasswordEdit = findViewById(R.id.confirm_password);
        registerBtn = findViewById(R.id.register_btn);
        loginLink = findViewById(R.id.link_login); // Add to layout

        // Listeners
        birthDateEdit.setOnClickListener(v -> showDatePicker());
        registerBtn.setOnClickListener(v -> attemptRegister());
        loginLink.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));

        observeViewModel();
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
    }

    private void clearFields() {
        firstNameEdit.setText("");
        lastNameEdit.setText("");
        emailEdit.setText("");
        birthDateEdit.setText("");
        passwordEdit.setText("");
        confirmPasswordEdit.setText("");
    }
}
