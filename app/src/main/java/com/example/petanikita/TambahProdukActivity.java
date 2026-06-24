package com.example.petanikita;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TambahProdukActivity extends AppCompatActivity {

    private EditText etName, etDesc, etPrice, etStock;
    private Spinner spinProv, spinReg, spinDist;
    private Button btnSubmit;

    private CardView cardAddImage;
    private ImageView ivProductImage;
    private LinearLayout layoutImageHint;
    private Uri selectedImageUri = null;

    private OkHttpClient client;
    private String token;
    private static final String BASE_URL = "http://10.0.2.2:5000/api/";

    private int selectedProvId = 0;
    private int selectedRegId = 0;
    private int selectedDistId = 0;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivProductImage.setImageURI(selectedImageUri);
                    layoutImageHint.setVisibility(View.GONE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tambah_produk);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etName = findViewById(R.id.etAddProductName);
        etDesc = findViewById(R.id.etAddDescription);
        etPrice = findViewById(R.id.etAddPrice);
        etStock = findViewById(R.id.etAddStock);
        spinProv = findViewById(R.id.spinnerAddProvinsi);
        spinReg = findViewById(R.id.spinnerAddKabupaten);
        spinDist = findViewById(R.id.spinnerAddDaerah);
        btnSubmit = findViewById(R.id.btnSubmitAddProduct);

        cardAddImage = findViewById(R.id.cardAddImage);
        ivProductImage = findViewById(R.id.ivProductImage);
        layoutImageHint = findViewById(R.id.layoutImageHint);

        client = new OkHttpClient();
        SharedPreferences prefs = getSharedPreferences("PetaniKitaApp", MODE_PRIVATE);
        token = prefs.getString("JWT_TOKEN", "");

        if (token.isEmpty()) {
            Toast.makeText(this, "Anda harus login terlebih dahulu", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadProvinces();

        cardAddImage.setOnClickListener(v -> openGallery());

        btnSubmit.setOnClickListener(v -> submitProductData());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("product_img_", ".jpg", getCacheDir());
            FileOutputStream out = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            if (inputStream != null) {
                inputStream.close();
            }
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void submitProductData() {
        String name = etName.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();

        if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
            Toast.makeText(this, "Harap lengkapi semua data produk", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedProvId == 0 || selectedRegId == 0 || selectedDistId == 0) {
            Toast.makeText(this, "Harap pilih wilayah dengan lengkap", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Harap pilih foto produk", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("MENYIMPAN...");

        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("ProductName", name)
                .addFormDataPart("Description", desc)
                .addFormDataPart("Price", priceStr)
                .addFormDataPart("Stock", stockStr)
                .addFormDataPart("ProvinceId", String.valueOf(selectedProvId))
                .addFormDataPart("RegencyId", String.valueOf(selectedRegId))
                .addFormDataPart("DistrictId", String.valueOf(selectedDistId));

        File imageFile = getFileFromUri(selectedImageUri);
        if (imageFile != null) {
            multipartBuilder.addFormDataPart(
                    "Image",
                    imageFile.getName(),
                    RequestBody.create(imageFile, MediaType.parse("image/*"))
            );
        } else {
            Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true);
            btnSubmit.setText("SIMPAN PRODUK");
            return;
        }

        RequestBody requestBody = multipartBuilder.build();

        Request request = new Request.Builder()
                .url(BASE_URL + "Products")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(TambahProdukActivity.this, "Gagal koneksi ke server", Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("SIMPAN PRODUK");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("SIMPAN PRODUK");

                    if (response.isSuccessful()) {
                        Toast.makeText(TambahProdukActivity.this, "Produk berhasil ditambahkan!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(TambahProdukActivity.this, "Gagal menambahkan produk. Code: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
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
                        list.add(new LocationItem(0, "Pilih Provinsi..."));

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            list.add(new LocationItem(obj.getInt("provinceId"), obj.getString("provinceName")));
                        }

                        runOnUiThread(() -> {
                            ArrayAdapter<LocationItem> adapter = new ArrayAdapter<>(TambahProdukActivity.this, android.R.layout.simple_spinner_dropdown_item, list);
                            spinProv.setAdapter(adapter);

                            spinProv.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    LocationItem selected = (LocationItem) parent.getSelectedItem();
                                    selectedProvId = selected.getId();

                                    if (selectedProvId > 0) {
                                        loadRegencies(selectedProvId);
                                    } else {
                                        spinReg.setAdapter(null);
                                        spinDist.setAdapter(null);
                                        selectedRegId = 0;
                                        selectedDistId = 0;
                                    }
                                }
                                @Override public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        });
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }

    private void loadRegencies(int provId) {
        Request request = new Request.Builder().url(BASE_URL + "Wilayah/regencies?provinceId=" + provId).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<LocationItem> list = new ArrayList<>();
                        list.add(new LocationItem(0, "Pilih Kabupaten..."));

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            list.add(new LocationItem(obj.getInt("regencyId"), obj.getString("regencyName")));
                        }

                        runOnUiThread(() -> {
                            ArrayAdapter<LocationItem> adapter = new ArrayAdapter<>(TambahProdukActivity.this, android.R.layout.simple_spinner_dropdown_item, list);
                            spinReg.setAdapter(adapter);

                            spinReg.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    LocationItem selected = (LocationItem) parent.getSelectedItem();
                                    selectedRegId = selected.getId();

                                    if (selectedRegId > 0) {
                                        loadDistricts(selectedRegId);
                                    } else {
                                        spinDist.setAdapter(null);
                                        selectedDistId = 0;
                                    }
                                }
                                @Override public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        });
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }

    private void loadDistricts(int regId) {
        Request request = new Request.Builder().url(BASE_URL + "Wilayah/districts?regencyId=" + regId).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<LocationItem> list = new ArrayList<>();
                        list.add(new LocationItem(0, "Pilih Daerah/Kecamatan..."));

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            int id = obj.optInt("districtId", obj.optInt("id"));
                            String name = obj.optString("districtName", obj.optString("name"));
                            list.add(new LocationItem(id, name));
                        }

                        runOnUiThread(() -> {
                            ArrayAdapter<LocationItem> adapter = new ArrayAdapter<>(TambahProdukActivity.this, android.R.layout.simple_spinner_dropdown_item, list);
                            spinDist.setAdapter(adapter);

                            spinDist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    LocationItem selected = (LocationItem) parent.getSelectedItem();
                                    selectedDistId = selected.getId();
                                }
                                @Override public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        });
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }
}