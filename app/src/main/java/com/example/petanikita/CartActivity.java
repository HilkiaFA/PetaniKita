package com.example.petanikita;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvGrandTotal;
    private Button btnBayar;

    private CartAdapter adapter;
    private OkHttpClient client;
    private String token;
    private double currentGrandTotal = 0;
    private static final String BASE_URL = "http://10.0.2.2:5000/api/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cart);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recyleviewcart);
        tvGrandTotal = findViewById(R.id.textViewtotal);
        btnBayar = findViewById(R.id.btn_simpan2);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        client = new OkHttpClient();

        SharedPreferences prefs = getSharedPreferences("PetaniKitaApp", MODE_PRIVATE);
        token = prefs.getString("JWT_TOKEN", "");

        if (token.isEmpty()) {
            Toast.makeText(this, "Anda belum login", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        btnBayar.setOnClickListener(v -> {
            Intent intent = new Intent(CartActivity.this, PaymentActivity.class);
            intent.putExtra("GRAND_TOTAL", currentGrandTotal);
            startActivity(intent);
        });
        adapter = new CartAdapter(new ArrayList<>(), new CartAdapter.OnCartItemInteractionListener() {
            @Override
            public void onUpdateQuantity(CartItem item, int newQuantity) {
                updateQuantityAPI(item.getCartItemId(), newQuantity);
            }

            @Override
            public void onDeleteRequest(CartItem item, int position) {
                showDeleteConfirmationDialog(item, position);
            }
        });
        recyclerView.setAdapter(adapter);

        loadCartData();
    }


    private void loadCartData() {
        Request request = new Request.Builder()
                .url(BASE_URL + "Carts")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(CartActivity.this, "Gagal memuat keranjang", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray dataArray = jsonObject.getJSONArray("data");

                        double grandTotal = jsonObject.getDouble("grandTotal");

                        List<CartItem> cartItems = new ArrayList<>();
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject obj = dataArray.getJSONObject(i);
                            cartItems.add(new CartItem(
                                    obj.getInt("cartItemId"),
                                    obj.getInt("productId"),
                                    obj.getString("productName"),
                                    obj.getDouble("price"),
                                    obj.getInt("quantity"),
                                    obj.getDouble("subTotal"),
                                    obj.getString("farmName")
                            ));
                        }

                        runOnUiThread(() -> {
                            currentGrandTotal = grandTotal;
                            adapter.updateData(cartItems);
                            tvGrandTotal.setText("Rp " + String.format("%,.0f", grandTotal));

                            btnBayar.setEnabled(cartItems.size() > 0);
                        });
                    } catch (JSONException e) {
                        Log.e("CART_API", "Parsing error", e);
                    }
                } else {
                    runOnUiThread(() -> {
                        currentGrandTotal = 0;
                        adapter.updateData(new ArrayList<>());
                        tvGrandTotal.setText("Rp 0");
                        btnBayar.setEnabled(false);
                    });
                }
            }
        });
    }


    private void updateQuantityAPI(int cartItemId, int newQuantity) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("quantity", newQuantity);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "Carts/" + cartItemId)
                .put(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    loadCartData();
                }
            }
        });
    }


    private void showDeleteConfirmationDialog(CartItem item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Produk")
                .setMessage("Yakin ingin menghapus '" + item.getProductName() + "' dari keranjang?")
                .setPositiveButton("Ya", (dialog, which) -> {
                    deleteCartItemAPI(item.getCartItemId());
                })
                .setNegativeButton("Tidak", (dialog, which) -> {
                    adapter.notifyItemChanged(position);
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void deleteCartItemAPI(int cartItemId) {
        Request request = new Request.Builder()
                .url(BASE_URL + "Carts/" + cartItemId)
                .delete()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(CartActivity.this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(CartActivity.this, "Produk dihapus dari keranjang", Toast.LENGTH_SHORT).show();
                        loadCartData();
                    } else {
                        Toast.makeText(CartActivity.this, "Gagal menghapus produk", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}