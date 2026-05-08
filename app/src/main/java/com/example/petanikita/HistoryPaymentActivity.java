package com.example.petanikita;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HistoryPaymentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OrderAdapter adapter;
    private OkHttpClient client;
    private String token;

    private static final String BASE_URL = "http://10.0.2.2:5000/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history_payment);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.reycleviewhistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        client = new OkHttpClient();
        SharedPreferences prefs = getSharedPreferences("PetaniKitaApp", MODE_PRIVATE);
        token = prefs.getString("JWT_TOKEN", "");

        adapter = new OrderAdapter(new ArrayList<>(), item -> {
            Intent intent = new Intent(HistoryPaymentActivity.this, DetailHistoryActivity.class);
            intent.putExtra("ORDER_ID", item.getOrderId());
            startActivity(intent);
        });        recyclerView.setAdapter(adapter);

        if (token.isEmpty()) {
            Toast.makeText(this, "Anda belum login!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadOrderHistory();
    }

    private void loadOrderHistory() {
        Request request = new Request.Builder()
                .url(BASE_URL + "Orders")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(HistoryPaymentActivity.this, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<OrderItem> orderList = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);

                            orderList.add(new OrderItem(
                                    obj.getInt("orderId"),
                                    obj.getString("orderDate"),
                                    obj.getString("status"),
                                    obj.getDouble("totalAmount"),
                                    obj.getString("shippingAddress")
                            ));
                        }

                        runOnUiThread(() -> adapter.updateData(orderList));

                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(HistoryPaymentActivity.this, "Gagal memproses data", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(HistoryPaymentActivity.this, "Gagal mengambil riwayat pesanan", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}