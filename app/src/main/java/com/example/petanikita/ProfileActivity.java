package com.example.petanikita;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

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

public class ProfileActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPhone;
    private ImageView btnHistory;
    private Button btnEdit, btnSimpan, btnPetani, btnAdress;

    private OkHttpClient client;
    private String token;

    private static final String PROFILE_URL = "http://10.0.2.2:5000/api/Users/profile";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etFullName = findViewById(R.id.editTextTextFullname);
        etEmail = findViewById(R.id.editTextTextemail);
        etPhone = findViewById(R.id.editTextText_Phone);
        btnEdit = findViewById(R.id.btn_edit);
        btnSimpan = findViewById(R.id.btn_simpan);
        btnAdress = findViewById(R.id.btn_alamat);
        btnPetani = findViewById(R.id.btn_petani);
        btnHistory = findViewById(R.id.imageViewHistory);

        client = new OkHttpClient();
        btnPetani.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, FormPenjualActivity.class);
                startActivity(intent);
            }
        });

        btnAdress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, AddressActivity.class);
                startActivity(intent);
            }
        });
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, HistoryPaymentActivity.class);
                startActivity(intent);
            }
        });

        SharedPreferences prefs = getSharedPreferences("PetaniKitaApp", MODE_PRIVATE);
        token = prefs.getString("JWT_TOKEN", "");

        if (token.isEmpty()) {
            // Gunakan true agar Activity di-finish setelah tombol OK ditekan
            showAlertDialog("Peringatan", "Sesi Anda telah habis, silakan login kembali.", true);
            return;
        }

        setEditMode(false);

        loadProfileData();

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEditMode(true);
                etFullName.requestFocus();
            }
        });

        btnSimpan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileData();
            }
        });
    }

    private void setEditMode(boolean isEditable) {
        etFullName.setEnabled(isEditable);
        etEmail.setEnabled(isEditable);
        etPhone.setEnabled(isEditable);

        btnEdit.setEnabled(!isEditable);
        btnSimpan.setEnabled(isEditable);
    }

    private void loadProfileData() {
        Request request = new Request.Builder()
                .url(PROFILE_URL)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showAlertDialog("Error", "Gagal memuat profil, periksa koneksi internet.", false));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);

                        String fullName = jsonObject.optString("fullName", "");
                        String email = jsonObject.optString("email", "");
                        String phone = jsonObject.optString("phone", "");

                        runOnUiThread(() -> {
                            etFullName.setText(fullName);
                            etEmail.setText(email);
                            etPhone.setText(phone);
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    runOnUiThread(() -> showAlertDialog("Error", "Gagal mendapatkan data dari server.", false));
                }
            }
        });
    }

    private void saveProfileData() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            showAlertDialog("Peringatan", "Semua kolom data diri harus diisi!", false);
            return;
        }

        btnSimpan.setText("Menyimpan...");
        btnSimpan.setEnabled(false);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("fullName", fullName);
            jsonBody.put("email", email);
            jsonBody.put("phone", phone);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(PROFILE_URL)
                .put(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showAlertDialog("Error", "Gagal menyimpan data ke server.", false);
                    btnSimpan.setText("Simpan");
                    btnSimpan.setEnabled(true);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    btnSimpan.setText("Simpan");

                    if (response.isSuccessful()) {
                        showAlertDialog("Sukses", "Profil berhasil diperbarui!", false);
                        setEditMode(false);
                    } else {
                        showAlertDialog("Error", "Gagal memperbarui profil. Periksa format data.", false);
                        btnSimpan.setEnabled(true);
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
}