package com.example.petanikita;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DetailPenjualanActivity extends AppCompatActivity {

    private RecyclerView rvPembeli;
    private TextView tvProductName;
    private PembeliAdapter adapter;
    private OkHttpClient client;
    private String token;
    private int productId;

    private static final String BASE_URL = "http://10.0.2.2:5000/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_penjualan);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.textViewDetailTitle), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        tvProductName = findViewById(R.id.textViewProductName);
        rvPembeli = findViewById(R.id.rvPembeli);

        productId = getIntent().getIntExtra("PRODUCT_ID", -1);
        String productName = getIntent().getStringExtra("PRODUCT_NAME");
        tvProductName.setText("Produk: " + (productName != null ? productName : ""));

        rvPembeli.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PembeliAdapter(new ArrayList<>());
        rvPembeli.setAdapter(adapter);

        client = new OkHttpClient();
        SharedPreferences prefs = getSharedPreferences("PetaniKitaApp", MODE_PRIVATE);
        token = prefs.getString("JWT_TOKEN", "");

        if (productId != -1) {
            loadBuyersData();
        }
    }

    private void loadBuyersData() {
        String url = BASE_URL + "Orders/product/" + productId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(DetailPenjualanActivity.this, "Gagal memuat data", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<PembeliItem> list = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            // ASUMSI FIELD JSON: Sesuaikan field getString() ini dengan response JSON aslimu
                            list.add(new PembeliItem(
                                    obj.optString("buyerName", "Hamba Allah"),
                                    obj.optInt("quantity", 1),
                                    obj.optDouble("price", 0),
                                    obj.optDouble("subTotal", 0),
                                    obj.optString("status", "Pending"),
                                    obj.optString("orderDate", "Baru saja")
                            ));
                        }

                        runOnUiThread(() -> adapter.updateData(list));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(DetailPenjualanActivity.this, "Belum ada pesanan", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }



    public static class PembeliItem {
        String buyerName, status, orderDate;
        int qty;
        double price, subTotal;

        public PembeliItem(String buyerName, int qty, double price, double subTotal, String status, String orderDate) {
            this.buyerName = buyerName; this.qty = qty; this.price = price;
            this.subTotal = subTotal; this.status = status; this.orderDate = orderDate;
        }
    }

    public static class PembeliAdapter extends RecyclerView.Adapter<PembeliAdapter.ViewHolder> {
        private List<PembeliItem> list;

        public PembeliAdapter(List<PembeliItem> list) { this.list = list; }
        public void updateData(List<PembeliItem> newList) { this.list = newList; notifyDataSetChanged(); }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pembeli, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PembeliItem item = list.get(position);
            holder.tvName.setText(item.buyerName);
            holder.tvDetails.setText("Membeli: " + item.qty + " item x Rp " + String.format("%,.0f", item.price));
            holder.tvTotal.setText("Total: Rp " + String.format("%,.0f", item.subTotal));

            holder.tvDate.setText(item.orderDate.split("T")[0]);

            holder.tvStatus.setText(item.status);
            if (item.status.equalsIgnoreCase("Pending")) {
                holder.tvStatus.setTextColor(Color.parseColor("#FFA500"));
            } else if (item.status.equalsIgnoreCase("Completed") || item.status.equalsIgnoreCase("Selesai")) {
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
            }
        }

        @Override public int getItemCount() { return list != null ? list.size() : 0; }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvStatus, tvDetails, tvTotal, tvDate;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvBuyerName);
                tvStatus = itemView.findViewById(R.id.tvOrderStatus);
                tvDetails = itemView.findViewById(R.id.tvOrderDetails);
                tvTotal = itemView.findViewById(R.id.tvOrderTotal);
                tvDate = itemView.findViewById(R.id.tvOrderDate);
            }
        }
    }
}