package com.example.petanikita;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FormPenjualActivity extends AppCompatActivity {

    private EditText etNamaKebun, etDeskripsi, etAlamat;
    private Spinner spinnerProvinsi, spinnerKabupaten, spinnerDaerah;
    private Button btnPenjual;
    private ProgressDialog loadingDialog;

    private OkHttpClient client;
    private String token;

    private static final String BASE_URL = "http://10.0.2.2:5000/api/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private int selectedProvinceId = 0;
    private int selectedRegencyId = 0;
    private int selectedDistrictId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_form_penjual);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etNamaKebun = findViewById(R.id.editTextTextnamakebun);
        etDeskripsi = findViewById(R.id.editTextTextdeskripsi);
        etAlamat = findViewById(R.id.editTextText_Alamat);
        spinnerProvinsi = findViewById(R.id.spinner3);
        spinnerKabupaten = findViewById(R.id.spinner4);
        spinnerDaerah = findViewById(R.id.spinner5);
        btnPenjual = findViewById(R.id.btn_penjual);

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Sedang memproses pendaftaran...");
        loadingDialog.setCancelable(false);

        client = new OkHttpClient();

        SharedPreferences prefs = getSharedPreferences("PetaniKitaApp", MODE_PRIVATE);
        token = prefs.getString("JWT_TOKEN", "");

        if (token.isEmpty()) {
            Toast.makeText(this, "Anda belum login!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadProvinces();

        spinnerProvinsi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LocationItem selected = (LocationItem) parent.getSelectedItem();
                selectedProvinceId = selected.getId();

                selectedRegencyId = 0;
                selectedDistrictId = 0;
                spinnerKabupaten.setAdapter(null);
                spinnerDaerah.setAdapter(null);

                if (selectedProvinceId > 0) {
                    loadRegencies(selectedProvinceId);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerKabupaten.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LocationItem selected = (LocationItem) parent.getSelectedItem();
                selectedRegencyId = selected.getId();

                selectedDistrictId = 0;
                spinnerDaerah.setAdapter(null);

                if (selectedRegencyId > 0) {
                    loadDistricts(selectedRegencyId);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerDaerah.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LocationItem selected = (LocationItem) parent.getSelectedItem();
                selectedDistrictId = selected.getId();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnPenjual.setOnClickListener(v -> submitFormPenjual());
    }

    private void loadProvinces() {
        Request request = new Request.Builder().url(BASE_URL + "Wilayah/provinces").build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<LocationItem> list = new ArrayList<>();
                        list.add(new LocationItem(0, "Pilih Provinsi"));

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            list.add(new LocationItem(obj.getInt("provinceId"), obj.getString("provinceName")));
                        }

                        runOnUiThread(() -> {
                            ArrayAdapter<LocationItem> adapter = new ArrayAdapter<>(FormPenjualActivity.this, android.R.layout.simple_spinner_dropdown_item, list);
                            spinnerProvinsi.setAdapter(adapter);
                        });
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private void loadRegencies(int provinceId) {
        Request request = new Request.Builder().url(BASE_URL + "Wilayah/regencies?provinceId=" + provinceId).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<LocationItem> list = new ArrayList<>();
                        list.add(new LocationItem(0, "Pilih Kabupaten"));

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            list.add(new LocationItem(obj.getInt("regencyId"), obj.getString("regencyName")));
                        }

                        runOnUiThread(() -> {
                            ArrayAdapter<LocationItem> adapter = new ArrayAdapter<>(FormPenjualActivity.this, android.R.layout.simple_spinner_dropdown_item, list);
                            spinnerKabupaten.setAdapter(adapter);
                        });
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private void loadDistricts(int regencyId) {
        Request request = new Request.Builder().url(BASE_URL + "Wilayah/districts?regencyId=" + regencyId).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<LocationItem> list = new ArrayList<>();
                        list.add(new LocationItem(0, "Pilih Daerah"));

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            list.add(new LocationItem(obj.getInt("districtId"), obj.getString("districtName")));
                        }

                        runOnUiThread(() -> {
                            ArrayAdapter<LocationItem> adapter = new ArrayAdapter<>(FormPenjualActivity.this, android.R.layout.simple_spinner_dropdown_item, list);
                            spinnerDaerah.setAdapter(adapter);
                        });
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private void submitFormPenjual() {
        String farmName = etNamaKebun.getText().toString().trim();
        String description = etDeskripsi.getText().toString().trim();
        String address = etAlamat.getText().toString().trim();

        if (farmName.isEmpty() || description.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Harap isi semua kolom teks!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedProvinceId == 0 || selectedRegencyId == 0 || selectedDistrictId == 0) {
            Toast.makeText(this, "Harap pilih wilayah dengan lengkap!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPenjual.setEnabled(false);
        loadingDialog.show();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("farmName", farmName);
            jsonBody.put("description", description);
            jsonBody.put("provinceId", selectedProvinceId);
            jsonBody.put("regencyId", selectedRegencyId);
            jsonBody.put("districtId", selectedDistrictId);
            jsonBody.put("address", address);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(BASE_URL + "Users/become-farmer")
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(FormPenjualActivity.this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show();
                    btnPenjual.setEnabled(true);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    btnPenjual.setEnabled(true);

                    if (response.isSuccessful()) {
                        Toast.makeText(FormPenjualActivity.this, "Berhasil menjadi penjual! Silakan login ulang.", Toast.LENGTH_LONG).show();

                        SharedPreferences prefs = getSharedPreferences("PetaniKitaApp", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.clear();
                        editor.apply();

                        Intent intent = new Intent(FormPenjualActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(FormPenjualActivity.this, "Pendaftaran gagal. Anda mungkin sudah menjadi penjual.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}