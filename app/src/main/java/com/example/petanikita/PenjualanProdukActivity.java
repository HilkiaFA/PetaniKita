package com.example.petanikita;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

public class PenjualanProdukActivity extends AppCompatActivity {

    private RecyclerView rvPenjualan;
    private ProductAdapter productAdapter; // Kita gunakan ulang ProductAdapter
    private OkHttpClient client;
    private String token;

    private static final String BASE_URL = "http://10.0.2.2:5000/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_penjualan_produk);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvPenjualan = findViewById(R.id.rvPenjualanProduk);
        rvPenjualan.setLayoutManager(new LinearLayoutManager(this));

        productAdapter = new ProductAdapter(new ArrayList<>(), product -> {
            Intent intent = new Intent(PenjualanProdukActivity.this, DetailPenjualanActivity.class);
            intent.putExtra("PRODUCT_ID", product.getId());
            intent.putExtra("PRODUCT_NAME", product.getName()); // <--- Cukup seperti ini
            startActivity(intent);
        });
        rvPenjualan.setAdapter(productAdapter);

        client = new OkHttpClient();
        SharedPreferences prefs = getSharedPreferences("PetaniKitaApp", MODE_PRIVATE);
        token = prefs.getString("JWT_TOKEN", "");

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
                runOnUiThread(() -> Toast.makeText(PenjualanProdukActivity.this, "Gagal koneksi", Toast.LENGTH_SHORT).show());
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
                }
            }
        });
    }
}