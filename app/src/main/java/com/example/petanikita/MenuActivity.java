package com.example.petanikita;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
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
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MenuActivity extends AppCompatActivity {

    private Spinner spinnerProvince, spinnerRegency;
    private RecyclerView recyclerViewMenu;
    private EditText etSearch;
    private ImageView imageProfile;
    private ImageView imageCart;
    private ProductAdapter productAdapter;
    private OkHttpClient client;

    private static final String BASE_URL = "http://10.0.2.2:5000/api/";

    private int selectedProvinceId = 0;
    private int selectedRegencyId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        client = new OkHttpClient();
        spinnerProvince = findViewById(R.id.spinner);
        spinnerRegency = findViewById(R.id.spinner2);
        recyclerViewMenu = findViewById(R.id.recyclerViewmenu);
        etSearch = findViewById(R.id.editTextTextSearch);
        imageProfile = findViewById(R.id.imageViewProfile);
        imageCart = findViewById(R.id.imageViewcart);

        recyclerViewMenu.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductAdapter(new ArrayList<>(), new ProductAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Product product) {
                Intent intent = new Intent(MenuActivity.this, DetailProdukActivity.class);
                intent.putExtra("PRODUCT_ID", product.getId());
                startActivity(intent);
            }
        });
        recyclerViewMenu.setAdapter(productAdapter);

        loadProvinces();
        loadFilteredProducts();

        imageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        imageCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, CartActivity.class);
                startActivity(intent);
            }
        });
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                loadFilteredProducts();
            }
        });

        spinnerProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LocationItem selected = (LocationItem) parent.getSelectedItem();
                selectedProvinceId = selected.getId();

                if (selectedProvinceId == 0) {
                    selectedRegencyId = 0;
                    spinnerRegency.setAdapter(null);
                } else {
                    loadRegencies(selectedProvinceId);
                }

                loadFilteredProducts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerRegency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LocationItem selected = (LocationItem) parent.getSelectedItem();
                selectedRegencyId = selected.getId();

                loadFilteredProducts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadFilteredProducts() {
        String query = etSearch.getText().toString().trim();
        String url;

        if (query.isEmpty()) {
            url = BASE_URL + "Products?";
        } else {
            url = BASE_URL + "Products/search?q=" + query + "&";
        }

        if (selectedProvinceId > 0) {
            url += "provinceId=" + selectedProvinceId + "&";
        }

        if (selectedRegencyId > 0) {
            url += "regencyId=" + selectedRegencyId;
        }

        if (url.endsWith("?") || url.endsWith("&")) {
            url = url.substring(0, url.length() - 1);
        }

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MenuActivity.this, "Gagal memuat produk", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<Product> products = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            products.add(new Product(
                                    obj.getInt("productId"),
                                    obj.getString("productName"),
                                    obj.getString("farmName"),
                                    obj.getDouble("price"),
                                    obj.getInt("stock")
                            ));
                        }

                        runOnUiThread(() -> productAdapter.updateData(products));
                    } catch (Exception e) {
                        Log.e("API", "Parsing error", e);
                    }
                } else {
                    runOnUiThread(() -> productAdapter.updateData(new ArrayList<>()));
                }
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
                        List<LocationItem> provinceList = new ArrayList<>();
                        provinceList.add(new LocationItem(0, "Semua Provinsi"));

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            provinceList.add(new LocationItem(obj.getInt("provinceId"), obj.getString("provinceName")));
                        }

                        runOnUiThread(() -> {
                            ArrayAdapter<LocationItem> adapter = new ArrayAdapter<>(MenuActivity.this, android.R.layout.simple_spinner_dropdown_item, provinceList);
                            spinnerProvince.setAdapter(adapter);
                        });
                    } catch (Exception e) {}
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
                        List<LocationItem> regencyList = new ArrayList<>();
                        regencyList.add(new LocationItem(0, "Semua Kabupaten"));

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            regencyList.add(new LocationItem(obj.getInt("regencyId"), obj.getString("regencyName")));
                        }

                        runOnUiThread(() -> {
                            ArrayAdapter<LocationItem> adapter = new ArrayAdapter<>(MenuActivity.this, android.R.layout.simple_spinner_dropdown_item, regencyList);
                            spinnerRegency.setAdapter(adapter);
                        });
                    } catch (Exception e) {}
                }
            }
        });
    }
}