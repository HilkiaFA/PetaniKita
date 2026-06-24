package com.example.petanikita;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView; // Tambahan
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide; // Tambahan Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy; // Tambahan Glide

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

public class DetailProdukActivity extends AppCompatActivity {

    private TextView tvName, tvDeskripsi, tvPrice, tvStock;
    private TextView tvMin, tvPlus, tvQuantity;
    private Button btnAddToCart;
    private ImageView ivProductImage; // Variabel untuk ImageView

    private int productId;
    private int currentQuantity = 1;
    private int maxStock = 0;

    private OkHttpClient client;
    private String token;

    private static final String BASE_URL = "http://10.0.2.2:5000/api/";
    private static final String SERVER_URL = "http://10.0.2.2:5000";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_produk);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvName = findViewById(R.id.textViewNameDitailmenu);
        tvDeskripsi = findViewById(R.id.textViewDeskirpsi);
        tvPrice = findViewById(R.id.textViewPrice);
        tvStock = findViewById(R.id.textViewStock);
        tvMin = findViewById(R.id.textViewMin);
        tvPlus = findViewById(R.id.textViewPlus);
        tvQuantity = findViewById(R.id.textViewQuatity);
        btnAddToCart = findViewById(R.id.buttonaddtocart);
        ivProductImage = findViewById(R.id.imageDetailProduk);

        client = new OkHttpClient();

        SharedPreferences prefs = getSharedPreferences("PetaniKitaApp", MODE_PRIVATE);
        token = prefs.getString("JWT_TOKEN", "");

        productId = getIntent().getIntExtra("PRODUCT_ID", -1);
        if (productId != -1) {
            loadProductDetail(productId);
        } else {
            Toast.makeText(this, "Produk tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
        }

        tvMin.setOnClickListener(v -> {
            if (currentQuantity > 1) {
                currentQuantity--;
                tvQuantity.setText(String.valueOf(currentQuantity));
            }
        });

        tvPlus.setOnClickListener(v -> {
            if (currentQuantity < maxStock) {
                currentQuantity++;
                tvQuantity.setText(String.valueOf(currentQuantity));
            } else {
                Toast.makeText(this, "Jumlah mencapai maksimal stok!", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddToCart.setOnClickListener(v -> addToCart());
    }

    private void loadProductDetail(int id) {
        Request request = new Request.Builder()
                .url(BASE_URL + "Products/" + id)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(DetailProdukActivity.this, "Gagal memuat detail produk", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject obj = new JSONObject(response.body().string());

                        String name = obj.getString("productName");
                        String desc = obj.getString("description");
                        double price = obj.getDouble("price");
                        int stock = obj.getInt("stock");

                        String imageUrlPath = obj.optString("imageUrl", "");
                        String fullImageUrl = "";
                        if (!imageUrlPath.isEmpty() && !imageUrlPath.equals("null")) {
                            fullImageUrl = SERVER_URL + imageUrlPath;
                        }

                        final String finalImageUrl = fullImageUrl;

                        runOnUiThread(() -> {
                            tvName.setText(name);
                            tvDeskripsi.setText(desc);
                            tvPrice.setText("Rp " + String.format("%,.0f", price));
                            tvStock.setText(String.valueOf(stock));

                            maxStock = stock;

                            if (!finalImageUrl.isEmpty()) {
                                Glide.with(DetailProdukActivity.this)
                                        .load(finalImageUrl)
                                        .centerCrop()
                                        .skipMemoryCache(true)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .into(ivProductImage);
                            } else {
                                ivProductImage.setImageResource(android.R.color.transparent);
                            }

                            if (maxStock == 0) {
                                currentQuantity = 0;
                                tvQuantity.setText("0");
                                btnAddToCart.setEnabled(false);
                                btnAddToCart.setText("STOK HABIS");
                            }
                        });
                    } catch (JSONException e) {
                        Log.e("API", "Parsing error", e);
                    }
                }
            }
        });
    }

    private void addToCart() {
        if (token.isEmpty()) {
            Toast.makeText(this, "Anda harus login terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAddToCart.setEnabled(false);
        btnAddToCart.setText("Menambahkan...");

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("productId", productId);
            jsonBody.put("quantity", currentQuantity);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(BASE_URL + "Carts")
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(DetailProdukActivity.this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show();
                    btnAddToCart.setEnabled(true);
                    btnAddToCart.setText("TAMBAH KE KERANJANG");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    btnAddToCart.setEnabled(true);
                    btnAddToCart.setText("TAMBAH KE KERANJANG");

                    if (response.isSuccessful()) {
                        Toast.makeText(DetailProdukActivity.this, "Berhasil ditambahkan ke keranjang!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(DetailProdukActivity.this, "Gagal menambahkan produk.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}