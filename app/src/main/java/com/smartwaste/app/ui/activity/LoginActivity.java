package com.smartwaste.app.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.smartwaste.app.R;
import com.smartwaste.app.viewmodel.AuthViewModel;
import com.smartwaste.app.utils.PrefKeys;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEdit, passwordEdit;
    private CheckBox rememberCheck;
    private Button loginBtn;
    private TextView registerLink;

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Redirect if already logged in
        if (authViewModel.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // Bind views
        emailEdit = findViewById(R.id.email);
        passwordEdit = findViewById(R.id.password);
        rememberCheck = findViewById(R.id.checkbox_remember);
        loginBtn = findViewById(R.id.login_btn);
        registerLink = findViewById(R.id.link_register);

        // Init shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences(PrefKeys.PREF_NAME, MODE_PRIVATE);
        authViewModel.setSharedPreferences(sharedPreferences);

        observeLoginResult();
        loadRememberedCredentials();

        loginBtn.setOnClickListener(v -> loginUser());
        registerLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        });
    }

    private void loginUser() {
        String email = emailEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Email dan password wajib diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.loginUser(email, password);
    }

    private void observeLoginResult() {
        authViewModel.getLoginResult().observe(this, result -> {
            if (result.isSuccess()) {
                if (rememberCheck.isChecked()) {
                    authViewModel.rememberCredentials(
                            emailEdit.getText().toString(),
                            passwordEdit.getText().toString()
                    );
                } else {
                    authViewModel.clearCredentials();
                }

                Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Login gagal: " + result.getErrorMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadRememberedCredentials() {
        MutableLiveData<String> savedEmail = new MutableLiveData<>();
        MutableLiveData<String> savedPassword = new MutableLiveData<>();

        savedEmail.observe(this, email -> emailEdit.setText(email));
        savedPassword.observe(this, password -> passwordEdit.setText(password));

        authViewModel.loadRememberedCredentials(savedEmail, savedPassword);

        // You might want to store checkbox state too if needed
        rememberCheck.setChecked(true);
    }
}
