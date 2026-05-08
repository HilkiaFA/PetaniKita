package com.example.petanikita;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DetailHistoryActivity extends AppCompatActivity {

    private TextView tvStatus, tvDate, tvAddress, tvGrandTotal;
    private RecyclerView rvProducts;

    private OrderDetailAdapter adapter;
    private OkHttpClient client;
    private String token;
    private int orderId;

    private static final String BASE_URL = "http://10.0.2.2:5000/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_history);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvStatus = findViewById(R.id.tvDetailStatus);
        tvDate = findViewById(R.id.tvDetailDate);
        tvAddress = findViewById(R.id.tvDetailAddress);
        tvGrandTotal = findViewById(R.id.tvDetailGrandTotal);
        rvProducts = findViewById(R.id.rvDetailProducts);

        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderDetailAdapter(new ArrayList<>());
        rvProducts.setAdapter(adapter);

        client = new OkHttpClient();
        SharedPreferences prefs = getSharedPreferences("PetaniKitaApp", MODE_PRIVATE);
        token = prefs.getString("JWT_TOKEN", "");

        orderId = getIntent().getIntExtra("ORDER_ID", -1);

        if (orderId == -1 || token.isEmpty()) {
            Toast.makeText(this, "Data pesanan tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadOrderSummary();
        loadOrderProducts();
    }

    private void loadOrderSummary() {
        Request request = new Request.Builder()
                .url(BASE_URL + "Orders/" + orderId)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(DetailHistoryActivity.this, "Gagal memuat detail pesanan", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject obj = new JSONObject(response.body().string());

                        String status = obj.getString("status");
                        double grandTotal = obj.getDouble("totalAmount");
                        String address = obj.getString("shippingAddress");

                        String formattedDate = obj.getString("orderDate");
                        try {
                            String rawDate = formattedDate.split("\\.")[0];
                            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                            Date date = input.parse(rawDate);
                            SimpleDateFormat output = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));
                            if (date != null) formattedDate = output.format(date);
                        } catch (Exception e) {}

                        String finalDate = formattedDate;
                        runOnUiThread(() -> {
                            tvStatus.setText(status);
                            tvDate.setText(finalDate);
                            tvAddress.setText(address);
                            tvGrandTotal.setText("Rp " + String.format("%,.0f", grandTotal));

                            // Warnai teks status
                            if (status.equalsIgnoreCase("Pending")) {
                                tvStatus.setTextColor(Color.parseColor("#FFA500"));
                            } else if (status.equalsIgnoreCase("Completed") || status.equalsIgnoreCase("Selesai")) {
                                tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                            } else {
                                tvStatus.setTextColor(Color.parseColor("#F44336"));
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void loadOrderProducts() {
        Request request = new Request.Builder()
                .url(BASE_URL + "Orders/" + orderId + "/details")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray arr = new JSONArray(response.body().string());
                        List<OrderDetailItem> list = new ArrayList<>();

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            list.add(new OrderDetailItem(
                                    obj.getInt("productId"),
                                    obj.getString("productName"),
                                    obj.getInt("quantity"),
                                    obj.getDouble("price"),
                                    obj.getDouble("subTotal")
                            ));
                        }

                        runOnUiThread(() -> adapter.updateData(list));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}