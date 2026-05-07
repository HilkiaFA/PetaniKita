package com.example.petanikita;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartList;
    private OnCartItemInteractionListener listener;

    public interface OnCartItemInteractionListener {
        void onUpdateQuantity(CartItem item, int newQuantity);
        void onDeleteRequest(CartItem item, int position);
    }

    public CartAdapter(List<CartItem> cartList, OnCartItemInteractionListener listener) {
        this.cartList = cartList;
        this.listener = listener;
    }

    public void updateData(List<CartItem> newCartList) {
        this.cartList = newCartList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartList.get(position);

        holder.tvName.setText(item.getProductName());
        holder.tvFarmName.setText(item.getFarmName());
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        holder.tvSubtotal.setText("Rp " + String.format("%,.0f", item.getSubTotal()));

        holder.tvPlus.setOnClickListener(v -> {
            int newQty = item.getQuantity() + 1;
            if (listener != null) listener.onUpdateQuantity(item, newQty);
        });

        holder.tvMin.setOnClickListener(v -> {
            int newQty = item.getQuantity() - 1;
            if (newQty <= 0) {
                if (listener != null) listener.onDeleteRequest(item, position);
            } else {
                if (listener != null) listener.onUpdateQuantity(item, newQty);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartList != null ? cartList.size() : 0;
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvFarmName, tvSubtotal, tvQuantity, tvPlus, tvMin;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.textViewNameCart);
            tvFarmName = itemView.findViewById(R.id.textViewNamaKebun);
            tvSubtotal = itemView.findViewById(R.id.textViewPricecart);
            tvQuantity = itemView.findViewById(R.id.textViewQuatityCart);
            tvPlus = itemView.findViewById(R.id.textViewPlusCart);
            tvMin = itemView.findViewById(R.id.textViewMinCart);
        }
    }
}