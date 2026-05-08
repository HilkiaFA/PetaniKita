package com.example.petanikita;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etFullName, etEmail, etPassword, etPhone;
    private Button btnRegister;
    private TextView tvLoginHere;

    private OkHttpClient client;

    private static final String REGISTER_URL = "http://10.0.2.2:5000/api/Auth/register";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        client = new OkHttpClient();

        etUsername = findViewById(R.id.editTextTextUsernameregister);
        etFullName = findViewById(R.id.editTextTextfullname);
        etEmail = findViewById(R.id.editTextTextEmailAddress);
        etPassword = findViewById(R.id.editTextTextPasswordRegister);
        etPhone = findViewById(R.id.editTextTextPhone);
        btnRegister = findViewById(R.id.button2);
        tvLoginHere = findViewById(R.id.textView18);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String fullName = etFullName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();

                if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
                    showAlertDialog("Peringatan", "Semua kolom harus diisi!", false);
                    return;
                }

                btnRegister.setEnabled(false);
                btnRegister.setText("Loading...");

                performRegister(username, fullName, email, password, phone);
            }
        });

        tvLoginHere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void performRegister(String username, String fullName, String email, String password, String phone) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("username", username);
            jsonBody.put("password", password);
            jsonBody.put("fullName", fullName);
            jsonBody.put("email", email);
            jsonBody.put("phone", phone);
            jsonBody.put("roleId", 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(REGISTER_URL)
                .post(body)
                .addHeader("accept", "*/*")
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("API_ERROR", "Gagal menghubungi server", e);

                runOnUiThread(() -> {
                    showAlertDialog("Error", "Koneksi ke server gagal!", false);
                    resetButton();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> {
                    resetButton();

                    if (response.isSuccessful()) {
                        showAlertDialog("Sukses", "Registrasi Berhasil! Silakan Login.", true);
                    } else {
                        try {
                            showAlertDialog("Registrasi Gagal", responseData, false);
                        } catch (Exception e) {
                            showAlertDialog("Registrasi Gagal", "Periksa kembali data Anda.", false);
                        }
                    }
                });
            }
        });
    }

    private void showAlertDialog(String title, String message, boolean finishOnOk) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    if (finishOnOk) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void resetButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText("SIGN UP");
    }
}