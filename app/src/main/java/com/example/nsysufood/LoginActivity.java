package com.example.nsysufood;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private TextView tvGuest;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // 檢查是否已登入
        if (mAuth.getCurrentUser() != null) {
            goToMainActivity();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvGuest = findViewById(R.id.tvGuest);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (validateInput(email, password)) {
                loginUser(email, password);
            }
        });

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (validateInput(email, password)) {
                registerUser(email, password);
            }
        });

        tvGuest.setOnClickListener(v -> goToMainActivity());
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("請輸入 Email");
            return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("密碼需至少 6 位數");
            return false;
        }
        return true;
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "登入成功", Toast.LENGTH_SHORT).show();
                        goToMainActivity();
                    } else {
                        // ★★★ 修改這裡：使用中文錯誤訊息 ★★★
                        String errorMsg = getFriendlyErrorMessage(task.getException());
                        Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registerUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "註冊成功，已自動登入", Toast.LENGTH_SHORT).show();
                        goToMainActivity();
                    } else {
                        // ★★★ 修改這裡：使用中文錯誤訊息 ★★★
                        String errorMsg = getFriendlyErrorMessage(task.getException());
                        Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ★★★ 核心功能：錯誤訊息翻譯機 ★★★
    private String getFriendlyErrorMessage(Exception e) {
        if (e == null) return "發生未知錯誤";
        String msg = e.getMessage(); // 取得原始英文錯誤

        if (msg == null) return "連線發生錯誤";

        // 開始比對關鍵字，翻譯成中文
        if (msg.contains("badly formatted")) {
            return "Email 格式不正確 (請檢查有無空白或打錯)";
        } else if (msg.contains("network") || msg.contains("connection")) {
            return "網路連線失敗，請檢查網路設定";
        } else if (msg.contains("already in use")) {
            return "註冊失敗：此 Email 已經被註冊過了";
        } else if (msg.contains("There is no user") || msg.contains("user not found")) {
            return "登入失敗：找不到此帳號，請先註冊";
        } else if (msg.contains("password") || msg.contains("credential")) {
            // 包含 wrong password
            if (msg.contains("6 characters")) return "密碼長度過短，至少需 6 位數";
            return "登入失敗：密碼錯誤";
        }

        // 如果都沒對應到，顯示原始訊息方便除錯
        return "錯誤: " + msg;
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}