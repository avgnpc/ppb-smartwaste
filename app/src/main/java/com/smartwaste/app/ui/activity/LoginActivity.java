package com.smartwaste.app.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.smartwaste.app.R;
import com.smartwaste.app.viewmodel.AuthViewModel;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEdit, passwordEdit;
    private Button loginBtn;
    private TextView registerLink;

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Bind views
        emailEdit = findViewById(R.id.email);
        passwordEdit = findViewById(R.id.password);
        loginBtn = findViewById(R.id.login_btn);
        TextView textRegisterPrompt = findViewById(R.id.text_register_prompt);

        // Atur tampilan teks "Belum Punya Akun? Daftar" dengan format dan klik
        String fullText = "Belum Punya Akun? Daftar";
        SpannableString spannable = new SpannableString(fullText);

        // Span untuk "Belum Punya Akun?" (index 0-18)
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, 18, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.WHITE), 0, 18, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Span untuk "Daftar" (index 19 - end)
        int daftarStart = 19;
        int daftarEnd = fullText.length();
        spannable.setSpan(new StyleSpan(Typeface.BOLD), daftarStart, daftarEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new UnderlineSpan(), daftarStart, daftarEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#00008B")), daftarStart, daftarEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        }, daftarStart, daftarEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        textRegisterPrompt.setText(spannable);
        textRegisterPrompt.setMovementMethod(LinkMovementMethod.getInstance());
        textRegisterPrompt.setHighlightColor(Color.TRANSPARENT);

        // Login button listener
        loginBtn.setOnClickListener(v -> {
            String email = emailEdit.getText().toString();
            String password = passwordEdit.getText().toString();
            authViewModel.loginUser(email, password);
        });

        observeLoginResult();
    }

    private void observeLoginResult() {
        authViewModel.getLoginResult().observe(this, result -> {
            if (result.isSuccess()) {
                Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Login gagal: " + result.getErrorMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
