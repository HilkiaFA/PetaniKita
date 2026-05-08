package com.example.petanikita;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class OrderDetailAdapter extends RecyclerView.Adapter<OrderDetailAdapter.ViewHolder> {
    private List<OrderDetailItem> list;

    public OrderDetailAdapter(List<OrderDetailItem> list) { this.list = list; }
    public void updateData(List<OrderDetailItem> newList) { this.list = newList; notifyDataSetChanged(); }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderDetailItem item = list.get(position);
        holder.tvName.setText(item.getProductName());

        String qtyPrice = item.getQuantity() + " x Rp " + String.format("%,.0f", item.getPrice());
        holder.tvQtyPrice.setText(qtyPrice);

        holder.tvSubtotal.setText("Rp " + String.format("%,.0f", item.getSubTotal()));
    }

    @Override public int getItemCount() { return list != null ? list.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvQtyPrice, tvSubtotal;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvQtyPrice = itemView.findViewById(R.id.tvItemQtyPrice);
            tvSubtotal = itemView.findViewById(R.id.tvItemSubtotal);
        }
    }
}