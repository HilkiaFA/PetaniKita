package com.example.petanikita;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FarmerProfileActivity extends AppCompatActivity {

    private TextView tvFarmName, tvDescription, tvAddress;
    private Button btnAddProduct, btnMyProducts;

    private OkHttpClient client;
    private String token;
    private static final String BASE_URL = "http://10.0.2.2:5000/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_farmer_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvFarmName = findViewById(R.id.tvFarmName);
        tvDescription = findViewById(R.id.tvDescription);
        tvAddress = findViewById(R.id.tvAddress);
        btnAddProduct = findViewById(R.id.btnAddProduct);
        btnMyProducts = findViewById(R.id.btnMyProducts);

        client = new OkHttpClient();
        SharedPreferences prefs = getSharedPreferences("PetaniKitaApp", MODE_PRIVATE);
        token = prefs.getString("JWT_TOKEN", "");

        if (token.isEmpty()) {
            Toast.makeText(this, "Anda belum login", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadFarmerProfile();

        btnAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(FarmerProfileActivity.this, MyProdukActivity.class);
            startActivity(intent);
        });

        btnMyProducts.setOnClickListener(v -> {
            Intent intent = new Intent(FarmerProfileActivity.this, PenjualanProdukActivity.class);
            startActivity(intent);
        });
    }

    private void loadFarmerProfile() {
        Request request = new Request.Builder()
                .url(BASE_URL + "Farmers/me")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showPopUpError("Koneksi gagal", "Tidak dapat menghubungi server."));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject obj = new JSONObject(response.body().string());

                        String farmName = obj.getString("farmName");
                        String description = obj.getString("description");
                        String detailAddress = obj.getString("address");
                        String district = obj.getString("districtName");
                        String regency = obj.getString("regencyName");
                        String province = obj.getString("provinceName");

                        String fullAddress = detailAddress + ", " + district + ", " + regency + ", " + province;

                        runOnUiThread(() -> {
                            tvFarmName.setText(farmName);
                            tvDescription.setText(description);
                            tvAddress.setText(fullAddress);
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> showPopUpError("Error Data", "Gagal memproses data kebun."));
                    }
                } else {
                    runOnUiThread(() -> showPopUpError("Profil Tidak Ditemukan", "Anda belum memiliki data kebun atau sesi Anda habis."));
                }
            }
        });
    }

    private void showPopUpError(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}