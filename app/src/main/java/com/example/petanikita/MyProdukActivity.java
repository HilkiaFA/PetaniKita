package com.example.petanikita;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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

public class MyProdukActivity extends AppCompatActivity {

    private RecyclerView rvMyProducts;
    private Button btnAddProduct;
    private ProductAdapter productAdapter;
    private OkHttpClient client;
    private String token;

    private static final String BASE_URL = "http://10.0.2.2:5000/api/";
    private static final String SERVER_URL = "http://10.0.2.2:5000";

    private int selectedProvIdEdit = 0;
    private int selectedRegIdEdit = 0;
    private int selectedDistIdEdit = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_produk);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvMyProducts = findViewById(R.id.rvMyProducts);
        btnAddProduct = findViewById(R.id.btnAddProduct);

        client = new OkHttpClient();
        SharedPreferences prefs = getSharedPreferences("PetaniKitaApp", MODE_PRIVATE);
        token = prefs.getString("JWT_TOKEN", "");

        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi Anda telah habis. Silakan login kembali.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvMyProducts.setLayoutManager(new LinearLayoutManager(this));

        productAdapter = new ProductAdapter(new ArrayList<>(), product -> {
            fetchProductDetailAndShowDialog(product.getId());
        });
        rvMyProducts.setAdapter(productAdapter);

        btnAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(MyProdukActivity.this, TambahProdukActivity.class);
            startActivity(intent);
        });

        loadMyProducts();
    }

    private void loadMyProducts() {
        Request request = new Request.Builder()
                .url(BASE_URL + "Products/my")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MyProdukActivity.this, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<Product> products = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);

                            String imageUrlPath = obj.optString("imageUrl", "");
                            String fullImageUrl = "";
                            if (!imageUrlPath.isEmpty() && !imageUrlPath.equals("null")) {
                                fullImageUrl = SERVER_URL + imageUrlPath;
                            }

                            products.add(new Product(
                                    obj.getInt("productId"),
                                    obj.getString("productName"),
                                    obj.getString("farmName"),
                                    obj.getDouble("price"),
                                    obj.getInt("stock"),
                                    fullImageUrl
                            ));
                        }

                        runOnUiThread(() -> {
                            productAdapter.updateData(products);
                            if (products.isEmpty()) {
                                Toast.makeText(MyProdukActivity.this, "Anda belum memiliki produk yang dijual.", Toast.LENGTH_LONG).show();
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void fetchProductDetailAndShowDialog(int productId) {
        Request request = new Request.Builder()
                .url(BASE_URL + "Products/" + productId)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MyProdukActivity.this, "Gagal memuat detail", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject productJson = new JSONObject(response.body().string());
                        runOnUiThread(() -> showEditDialog(productJson, productId));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void showEditDialog(JSONObject productData, int productId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_product, null);
        builder.setView(view);

        EditText etName = view.findViewById(R.id.etEditProductName);
        EditText etDesc = view.findViewById(R.id.etEditDescription);
        EditText etPrice = view.findViewById(R.id.etEditPrice);
        EditText etStock = view.findViewById(R.id.etEditStock);
        Button btnSubmit = view.findViewById(R.id.btnSubmitEdit);
        Button btnCancel = view.findViewById(R.id.btnCancelEdit);

        Spinner spinProv = view.findViewById(R.id.spinnerEditProvinsi);
        Spinner spinReg = view.findViewById(R.id.spinnerEditKabupaten);
        Spinner spinDist = view.findViewById(R.id.spinnerEditDaerah);

        AlertDialog dialog = builder.create();

        try {
            etName.setText(productData.getString("productName"));
            etDesc.setText(productData.optString("description", ""));
            etPrice.setText(String.valueOf(productData.getInt("price")));
            etStock.setText(String.valueOf(productData.getInt("stock")));

            int defaultProv = productData.optInt("provinceId", 0);
            int defaultReg = productData.optInt("regencyId", 0);
            int defaultDist = productData.optInt("districtId", 0);

            loadEditProvinces(spinProv, spinReg, spinDist, defaultProv, defaultReg, defaultDist);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            int price = etPrice.getText().toString().isEmpty() ? 0 : Integer.parseInt(etPrice.getText().toString());
            int stock = etStock.getText().toString().isEmpty() ? 0 : Integer.parseInt(etStock.getText().toString());

            if (name.isEmpty() || price == 0 || stock == 0) {
                Toast.makeText(this, "Nama, Harga, dan Stok tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedProvIdEdit == 0 || selectedRegIdEdit == 0 || selectedDistIdEdit == 0) {
                Toast.makeText(this, "Harap pilih Provinsi, Kabupaten, dan Daerah yang valid", Toast.LENGTH_SHORT).show();
                return;
            }

            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("ProductName", name)
                    .addFormDataPart("Description", desc)
                    .addFormDataPart("Price", String.valueOf(price))
                    .addFormDataPart("Stock", String.valueOf(stock))
                    .addFormDataPart("ProvinceId", String.valueOf(selectedProvIdEdit))
                    .addFormDataPart("RegencyId", String.valueOf(selectedRegIdEdit))
                    .addFormDataPart("DistrictId", String.valueOf(selectedDistIdEdit));

            RequestBody requestBody = multipartBuilder.build();
            updateProductApi(productId, requestBody, dialog);
        });

        dialog.show();
    }

    private void updateProductApi(int productId, RequestBody body, AlertDialog dialog) {
        Request request = new Request.Builder()
                .url(BASE_URL + "Products/" + productId)
                .put(body) // Body sekarang menggunakan MultipartBody
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MyProdukActivity.this, "Gagal mengupdate produk", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(MyProdukActivity.this, "Produk berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadMyProducts();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(MyProdukActivity.this, "Gagal: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void loadEditProvinces(Spinner spinProv, Spinner spinReg, Spinner spinDist, int defProv, int defReg, int defDist) {
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

                        int targetIndex = 0;
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            int id = obj.getInt("provinceId");
                            list.add(new LocationItem(id, obj.getString("provinceName")));
                            if (id == defProv) targetIndex = i + 1;
                        }

                        final int finalTargetIndex = targetIndex;
                        runOnUiThread(() -> {
                            ArrayAdapter<LocationItem> adapter = new ArrayAdapter<>(MyProdukActivity.this, android.R.layout.simple_spinner_dropdown_item, list);
                            spinProv.setAdapter(adapter);
                            spinProv.setSelection(finalTargetIndex);

                            spinProv.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    LocationItem selected = (LocationItem) parent.getSelectedItem();
                                    selectedProvIdEdit = selected.getId();
                                    if (selectedProvIdEdit > 0) {
                                        int nextDefReg = (selectedProvIdEdit == defProv) ? defReg : 0;
                                        int nextDefDist = (selectedProvIdEdit == defProv) ? defDist : 0;
                                        loadEditRegencies(selectedProvIdEdit, spinReg, spinDist, nextDefReg, nextDefDist);
                                    } else {
                                        spinReg.setAdapter(null);
                                        spinDist.setAdapter(null);
                                        selectedRegIdEdit = 0;
                                        selectedDistIdEdit = 0;
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

    private void loadEditRegencies(int provId, Spinner spinReg, Spinner spinDist, int defReg, int defDist) {
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
                        list.add(new LocationItem(0, "Pilih Kabupaten"));

                        int targetIndex = 0;
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            int id = obj.getInt("regencyId");
                            list.add(new LocationItem(id, obj.getString("regencyName")));
                            if (id == defReg) targetIndex = i + 1;
                        }

                        final int finalTargetIndex = targetIndex;
                        runOnUiThread(() -> {
                            ArrayAdapter<LocationItem> adapter = new ArrayAdapter<>(MyProdukActivity.this, android.R.layout.simple_spinner_dropdown_item, list);
                            spinReg.setAdapter(adapter);
                            spinReg.setSelection(finalTargetIndex);

                            spinReg.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    LocationItem selected = (LocationItem) parent.getSelectedItem();
                                    selectedRegIdEdit = selected.getId();
                                    if (selectedRegIdEdit > 0) {
                                        int nextDefDist = (selectedRegIdEdit == defReg) ? defDist : 0;
                                        loadEditDistricts(selectedRegIdEdit, spinDist, nextDefDist);
                                    } else {
                                        spinDist.setAdapter(null);
                                        selectedDistIdEdit = 0;
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

    private void loadEditDistricts(int regId, Spinner spinDist, int defDist) {
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
                        list.add(new LocationItem(0, "Pilih Daerah/Kecamatan"));

                        int targetIndex = 0;
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            int id = obj.optInt("districtId", obj.optInt("id"));
                            String name = obj.optString("districtName", obj.optString("name"));
                            list.add(new LocationItem(id, name));
                            if (id == defDist) targetIndex = i + 1;
                        }

                        final int finalTargetIndex = targetIndex;
                        runOnUiThread(() -> {
                            ArrayAdapter<LocationItem> adapter = new ArrayAdapter<>(MyProdukActivity.this, android.R.layout.simple_spinner_dropdown_item, list);
                            spinDist.setAdapter(adapter);
                            spinDist.setSelection(finalTargetIndex);

                            spinDist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    LocationItem selected = (LocationItem) parent.getSelectedItem();
                                    selectedDistIdEdit = selected.getId();
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