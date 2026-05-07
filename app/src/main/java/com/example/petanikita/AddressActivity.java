package com.example.petanikita;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

public class AddressActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AddressAdapter adapter;
    private OkHttpClient client;
    private String token;

    private String loggedInUserName = "User";
    private String loggedInUserPhone = "-";

    private static final String BASE_URL = "http://10.0.2.2:5000/api/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_address);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        client = new OkHttpClient();

        SharedPreferences prefs = getSharedPreferences("PetaniKitaApp", MODE_PRIVATE);
        token = prefs.getString("JWT_TOKEN", "");

        if (token.isEmpty()) {
            Toast.makeText(this, "Anda belum login", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new AddressAdapter(new ArrayList<>(), new AddressAdapter.OnItemActionClickListener() {
            @Override
            public void onUpdateClick(AddressItem item) {
                showUpdateDialog(item);
            }

            @Override
            public void onDeleteClick(AddressItem item) {
                showDeleteConfirmationDialog(item);
            }
        });
        recyclerView.setAdapter(adapter);

        loadUserProfile();
    }

    private void loadUserProfile() {
        Request request = new Request.Builder()
                .url(BASE_URL + "Users/profile")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(AddressActivity.this, "Gagal mengambil data user", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject obj = new JSONObject(response.body().string());
                        loggedInUserName = obj.getString("fullName");
                        loggedInUserPhone = obj.getString("phone");

                        loadLocations();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void loadLocations() {
        Request request = new Request.Builder()
                .url(BASE_URL + "Locations")
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
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<AddressItem> list = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);

                            AddressItem item = new AddressItem(
                                    obj.getInt("locationId"),
                                    obj.getInt("provinceId"),
                                    obj.getInt("regencyId"),
                                    obj.getInt("districtId"),
                                    obj.optString("provinceName", ""),
                                    obj.optString("regencyName", ""),
                                    obj.optString("districtName", ""),
                                    obj.getString("address")
                            );
                            item.setUserInfo(loggedInUserName, loggedInUserPhone);
                            list.add(item);
                        }

                        runOnUiThread(() -> adapter.updateData(list));
                    } catch (Exception e) {
                        Log.e("API", "Parsing error", e);
                    }
                }
            }
        });
    }


    private void showUpdateDialog(AddressItem item) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_update_address);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Spinner spProvinsi = dialog.findViewById(R.id.spinnerDialogProvinsi);
        Spinner spKabupaten = dialog.findViewById(R.id.spinnerDialogKabupaten);
        Spinner spDaerah = dialog.findViewById(R.id.spinnerDialogDaerah);
        EditText etAlamat = dialog.findViewById(R.id.etDialogAlamat);
        Button btnSimpan = dialog.findViewById(R.id.btnDialogUbah);

        final int[] selectedIds = {item.getProvinceId(), item.getRegencyId(), item.getDistrictId()};

        etAlamat.setText(item.getAddress());

        loadSpinnerProvinces(spProvinsi, selectedIds);

        spProvinsi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LocationItem loc = (LocationItem) parent.getSelectedItem();
                selectedIds[0] = loc.getId();
                spKabupaten.setAdapter(null);
                spDaerah.setAdapter(null);
                if (loc.getId() > 0) {
                    loadSpinnerRegencies(loc.getId(), spKabupaten, selectedIds);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spKabupaten.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LocationItem loc = (LocationItem) parent.getSelectedItem();
                selectedIds[1] = loc.getId();
                spDaerah.setAdapter(null);
                if (loc.getId() > 0) {
                    loadSpinnerDistricts(loc.getId(), spDaerah, selectedIds);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spDaerah.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LocationItem loc = (LocationItem) parent.getSelectedItem();
                selectedIds[2] = loc.getId();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSimpan.setOnClickListener(v -> {
            String newAddress = etAlamat.getText().toString().trim();
            if (newAddress.isEmpty() || selectedIds[0] == 0 || selectedIds[1] == 0 || selectedIds[2] == 0) {
                Toast.makeText(this, "Harap lengkapi semua data wilayah dan alamat!", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSimpan.setEnabled(false);
            btnSimpan.setText("Menyimpan...");
            updateLocationToApi(item.getLocationId(), selectedIds[0], selectedIds[1], selectedIds[2], newAddress, dialog);
        });

        dialog.show();
    }

    private void updateLocationToApi(int locationId, int provId, int regId, int distId, String address, Dialog dialog) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("provinceId", provId);
            jsonBody.put("regencyId", regId);
            jsonBody.put("districtId", distId);
            jsonBody.put("address", address);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "Locations/" + locationId)
                .put(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(AddressActivity.this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    if (response.isSuccessful()) {
                        Toast.makeText(AddressActivity.this, "Alamat berhasil diperbarui", Toast.LENGTH_SHORT).show();
                        loadLocations();
                    } else {
                        Toast.makeText(AddressActivity.this, "Gagal memperbarui alamat", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }


    private void showDeleteConfirmationDialog(AddressItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi Hapus")
                .setMessage("Yakin ingin menghapus alamat ini?")
                .setPositiveButton("Ya", (dialog, which) -> {
                    deleteLocationFromApi(item.getLocationId());
                })
                .setNegativeButton("Tidak", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void deleteLocationFromApi(int locationId) {
        Request request = new Request.Builder()
                .url(BASE_URL + "Locations/" + locationId)
                .delete() // Menggunakan method DELETE
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(AddressActivity.this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddressActivity.this, "Alamat berhasil dihapus", Toast.LENGTH_SHORT).show();
                        loadLocations();
                    } else {
                        Toast.makeText(AddressActivity.this, "Gagal menghapus alamat", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void loadSpinnerProvinces(Spinner spinner, int[] selectedIds) {
        Request request = new Request.Builder().url(BASE_URL + "Wilayah/provinces").build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray arr = new JSONArray(response.body().string());
                        List<LocationItem> list = new ArrayList<>();
                        list.add(new LocationItem(0, "Pilih Provinsi"));

                        int selectedPosition = 0;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            int id = obj.getInt("provinceId");
                            list.add(new LocationItem(id, obj.getString("provinceName")));
                            if (id == selectedIds[0]) selectedPosition = i + 1;
                        }

                        int finalPos = selectedPosition;
                        runOnUiThread(() -> {
                            ArrayAdapter<LocationItem> adapter = new ArrayAdapter<>(AddressActivity.this, android.R.layout.simple_spinner_dropdown_item, list);
                            spinner.setAdapter(adapter);
                            spinner.setSelection(finalPos);
                        });
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private void loadSpinnerRegencies(int provinceId, Spinner spinner, int[] selectedIds) {
        Request request = new Request.Builder().url(BASE_URL + "Wilayah/regencies?provinceId=" + provinceId).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray arr = new JSONArray(response.body().string());
                        List<LocationItem> list = new ArrayList<>();
                        list.add(new LocationItem(0, "Pilih Kabupaten"));

                        int selectedPosition = 0;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            int id = obj.getInt("regencyId");
                            list.add(new LocationItem(id, obj.getString("regencyName")));
                            if (id == selectedIds[1]) selectedPosition = i + 1;
                        }

                        int finalPos = selectedPosition;
                        runOnUiThread(() -> {
                            ArrayAdapter<LocationItem> adapter = new ArrayAdapter<>(AddressActivity.this, android.R.layout.simple_spinner_dropdown_item, list);
                            spinner.setAdapter(adapter);
                            if (finalPos > 0) spinner.setSelection(finalPos);
                        });
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private void loadSpinnerDistricts(int regencyId, Spinner spinner, int[] selectedIds) {
        Request request = new Request.Builder().url(BASE_URL + "Wilayah/districts?regencyId=" + regencyId).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray arr = new JSONArray(response.body().string());
                        List<LocationItem> list = new ArrayList<>();
                        list.add(new LocationItem(0, "Pilih Daerah"));

                        int selectedPosition = 0;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            int id = obj.getInt("districtId");
                            list.add(new LocationItem(id, obj.getString("districtName")));
                            if (id == selectedIds[2]) selectedPosition = i + 1;
                        }

                        int finalPos = selectedPosition;
                        runOnUiThread(() -> {
                            ArrayAdapter<LocationItem> adapter = new ArrayAdapter<>(AddressActivity.this, android.R.layout.simple_spinner_dropdown_item, list);
                            spinner.setAdapter(adapter);
                            if (finalPos > 0) spinner.setSelection(finalPos);
                        });
                    } catch (Exception ignored) {}
                }
            }
        });
    }
}