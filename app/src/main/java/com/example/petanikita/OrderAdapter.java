package com.example.petanikita;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<OrderItem> orderList;
    private OnItemClickListener listener; // 1. Tambahkan listener

    // 2. Buat Interface
    public interface OnItemClickListener {
        void onItemClick(OrderItem item);
    }

    // 3. Update Constructor
    public OrderAdapter(List<OrderItem> orderList, OnItemClickListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    public void updateData(List<OrderItem> newOrderList) {
        this.orderList = newOrderList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderItem item = orderList.get(position);

        holder.tvTotal.setText("Rp " + String.format("%,.0f", item.getTotalAmount()));
        holder.tvAddress.setText(item.getShippingAddress());

        String status = item.getStatus();
        holder.tvStatus.setText(status);
        if (status.equalsIgnoreCase("Pending")) {
            holder.tvStatus.setTextColor(Color.parseColor("#FFA500"));
        } else if (status.equalsIgnoreCase("Completed") || status.equalsIgnoreCase("Selesai")) {
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
        }

        String formattedDate = item.getOrderDate();
        try {
            String rawDate = item.getOrderDate().split("\\.")[0];
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(rawDate);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));
            if (date != null) formattedDate = outputFormat.format(date);
        } catch (Exception e) {}

        holder.tvDate.setText(formattedDate);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() { return orderList != null ? orderList.size() : 0; }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvStatus, tvTotal, tvAddress;
        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvOrderDate);
            tvStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvAddress = itemView.findViewById(R.id.tvOrderAddress);
        }
    }
}