package com.example.petanikita;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {

    private List<AddressItem> addressList;
    private OnItemActionClickListener listener;

    public interface OnItemActionClickListener {
        void onUpdateClick(AddressItem item);
        void onDeleteClick(AddressItem item);
    }

    public AddressAdapter(List<AddressItem> addressList, OnItemActionClickListener listener) {
        this.addressList = addressList;
        this.listener = listener;
    }

    public void updateData(List<AddressItem> newAddresses) {
        this.addressList = newAddresses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        AddressItem item = addressList.get(position);

        holder.tvName.setText(item.getUserName());
        holder.tvPhone.setText(item.getUserPhone());
        holder.tvAddress.setText(item.getAddress());

        String detailWilayah = item.getProvinceName() + " - " + item.getRegencyName() + " - " + item.getDistrictName();
        holder.tvProvinsi.setText(detailWilayah);

        holder.btnUbah.setOnClickListener(v -> {
            if (listener != null) listener.onUpdateClick(item);
        });

        holder.btnHapus.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return addressList != null ? addressList.size() : 0;
    }

    public static class AddressViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvAddress, tvProvinsi;
        Button btnUbah, btnHapus;

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.textViewNamaUser);
            tvPhone = itemView.findViewById(R.id.textViewPhoneUser);
            tvAddress = itemView.findViewById(R.id.textViewAlamatUser);
            tvProvinsi = itemView.findViewById(R.id.textViewProvinsiUser);
            btnUbah = itemView.findViewById(R.id.btn_ubah);
            btnHapus = itemView.findViewById(R.id.btn_hapus);
        }
    }
}