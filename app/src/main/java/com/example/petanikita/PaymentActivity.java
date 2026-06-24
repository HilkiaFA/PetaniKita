package com.example.petanikita;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

public class PaymentActivity extends AppCompatActivity {

    private Button btnBayar;
    private Spinner spinnerLokasi, spinnerPayment;
    private TextView tvHargaTotal;

    private OkHttpClient client;
    private String token;

    private String loggedInUserName = "";
    private String loggedInUserPhone = "";

    private int selectedLocationId = 0;
    private int selectedProvinceId = 0;
    private int selectedRegencyId = 0;

    private String selectedPaymentMethod = "";

    private static final String BASE_URL = "http://10.0.2.2:5000/api/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static class DeliveryLocation {
        int locationId;
        int provinceId;
        int regencyId;
        String displayInfo;

        DeliveryLocation(int locId, int provId, int regId, String display) {
            this.locationId = locId;
            this.provinceId = provId;
            this.regencyId = regId;
            this.displayInfo = display;
        }

        @Override
        public String toString() {
            return displayInfo;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnBayar = findViewById(R.id.btn_bayar);
        spinnerLokasi = findViewById(R.id.spinnerLokasi);
        spinnerPayment = findViewById(R.id.spinnerjenispembayaran);
        tvHargaTotal = findViewById(R.id.textViewharga);

        client = new OkHttpClient();

        SharedPreferences prefs = getSharedPreferences("PetaniKitaApp", MODE_PRIVATE);
        token = prefs.getString("JWT_TOKEN", "");

        if (token.isEmpty()) {
            showAlertDialog("Peringatan", "Anda belum login!", true, null);
            return;
        }

        double grandTotal = getIntent().getDoubleExtra("GRAND_TOTAL", 0);
        tvHargaTotal.setText("Rp " + String.format("%,.0f", grandTotal));

        setupPaymentSpinner();
        loadUserProfileAndLocations();

        btnBayar.setOnClickListener(v -> showConfirmationDialog());
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi Pesanan")
                .setMessage("Apakah pesanan Anda sudah benar?")
                .setPositiveButton("Ya", (dialog, which) -> {
                    // Jika user memilih "Ya", jalankan proses checkout
                    processCheckout();
                })
                .setNegativeButton("Tidak", (dialog, which) -> {
                    // Jika user memilih "Tidak", tutup dialog saja
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void setupPaymentSpinner() {
        String[] paymentOptions = {"Pilih Jenis Pembayaran", "COD", "QRIS"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, paymentOptions);
        spinnerPayment.setAdapter(adapter);

        spinnerPayment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = paymentOptions[position];

                if (selected.equals("QRIS")) {
                    new AlertDialog.Builder(PaymentActivity.this)
                            .setTitle("Maaf")
                            .setMessage("QRIS Sedang error. Silakan gunakan metode pembayaran lain.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                spinnerPayment.setSelection(0);
                                selectedPaymentMethod = "";
                            })
                            .setCancelable(false)
                            .show();
                } else if (selected.equals("COD")) {
                    selectedPaymentMethod = "COD";
                } else {
                    selectedPaymentMethod = "";
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadUserProfileAndLocations() {
        Request request = new Request.Builder()
                .url(BASE_URL + "Users/profile")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showAlertDialog("Error", "Gagal memuat profil", false, null));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject obj = new JSONObject(response.body().string());
                        loggedInUserName = obj.getString("fullName");
                        loggedInUserPhone = obj.getString("phone");

                        loadLocationsForSpinner();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void loadLocationsForSpinner() {
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
                        JSONArray arr = new JSONArray(response.body().string());

                        if (arr.length() == 0) {
                            runOnUiThread(() -> {
                                List<DeliveryLocation> emptyList = new ArrayList<>();
                                emptyList.add(new DeliveryLocation(0, 0, 0, "Alamat Kosong (Ketuk untuk tambah)"));

                                ArrayAdapter<DeliveryLocation> adapter = new ArrayAdapter<>(PaymentActivity.this, android.R.layout.simple_spinner_dropdown_item, emptyList);
                                spinnerLokasi.setAdapter(adapter);

                                spinnerLokasi.setOnTouchListener((v, event) -> {
                                    if (event.getAction() == MotionEvent.ACTION_UP) {
                                        Intent intent = new Intent(PaymentActivity.this, AddressActivity.class);
                                        showAlertDialog("Peringatan", "Silakan tambah alamat Anda terlebih dahulu.", true, intent);
                                    }
                                    return true;
                                });
                            });
                            return;
                        }

                        List<DeliveryLocation> locationList = new ArrayList<>();
                        locationList.add(new DeliveryLocation(0, 0, 0, "Pilih Alamat Pengiriman"));

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            int locId = obj.getInt("locationId");
                            int provId = obj.getInt("provinceId");
                            int regId = obj.getInt("regencyId");

                            String address = obj.getString("address");
                            String prov = obj.getString("provinceName");
                            String reg = obj.getString("regencyName");
                            String dist = obj.getString("districtName");

                            String fullDetail = loggedInUserName + " (" + loggedInUserPhone + ")\n"
                                    + address + ", " + dist + ", " + reg + " - " + prov;

                            locationList.add(new DeliveryLocation(locId, provId, regId, fullDetail));
                        }

                        runOnUiThread(() -> {
                            spinnerLokasi.setOnTouchListener(null);

                            ArrayAdapter<DeliveryLocation> adapter = new ArrayAdapter<>(PaymentActivity.this, android.R.layout.simple_spinner_dropdown_item, locationList);
                            spinnerLokasi.setAdapter(adapter);

                            spinnerLokasi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    DeliveryLocation item = (DeliveryLocation) parent.getSelectedItem();
                                    selectedLocationId = item.locationId;
                                    selectedProvinceId = item.provinceId;
                                    selectedRegencyId = item.regencyId;
                                }
                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void processCheckout() {
        if (selectedLocationId == 0) {
            showAlertDialog("Peringatan", "Harap pilih alamat pengiriman!", false, null);
            return;
        }

        if (selectedPaymentMethod.isEmpty()) {
            showAlertDialog("Peringatan", "Harap pilih metode pembayaran yang valid!", false, null);
            return;
        }

        btnBayar.setEnabled(false);
        btnBayar.setText("Memvalidasi Lokasi...");

        validateCartLocation();
    }

    private void validateCartLocation() {
        Request request = new Request.Builder()
                .url(BASE_URL + "Carts")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showAlertDialog("Error", "Gagal mengecek keranjang", false, null);
                    resetButton();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray dataArray = jsonObject.getJSONArray("data");

                        if (dataArray.length() == 0) {
                            runOnUiThread(() -> {
                                showAlertDialog("Peringatan", "Keranjang Anda kosong!", false, null);
                                resetButton();
                            });
                            return;
                        }

                        int productId = dataArray.getJSONObject(0).getInt("productId");
                        checkProductLocation(productId);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            showAlertDialog("Error", "Gagal memproses data keranjang", false, null);
                            resetButton();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        showAlertDialog("Error", "Gagal mengambil keranjang", false, null);
                        resetButton();
                    });
                }
            }
        });
    }

    private void checkProductLocation(int productId) {
        Request request = new Request.Builder()
                .url(BASE_URL + "Products/" + productId)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showAlertDialog("Error", "Gagal mengecek detail produk", false, null);
                    resetButton();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject productObj = new JSONObject(response.body().string());
                        int productProvId = productObj.getInt("provinceId");
                        int productRegId = productObj.getInt("regencyId");

                        if (productProvId == selectedProvinceId && productRegId == selectedRegencyId) {
                            runOnUiThread(() -> {
                                btnBayar.setText("Memproses Pesanan...");
                                executeOrder();
                            });
                        } else {
                            runOnUiThread(() -> {
                                showAlertDialog("Lokasi Tidak Sesuai", "Maaf, alamat pengiriman Anda harus berada di Provinsi dan Kabupaten yang sama dengan lokasi produk yang dibeli.", false, null);
                                resetButton();
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            showAlertDialog("Error", "Gagal membaca lokasi produk", false, null);
                            resetButton();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        showAlertDialog("Error", "Gagal mengambil data produk", false, null);
                        resetButton();
                    });
                }
            }
        });
    }

    private void executeOrder() {
        JSONObject orderBody = new JSONObject();
        try {
            orderBody.put("locationId", selectedLocationId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody reqBody = RequestBody.create(orderBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "Orders")
                .post(reqBody)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showAlertDialog("Error", "Gagal membuat pesanan", false, null);
                    resetButton();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject resObj = new JSONObject(response.body().string());
                        int newOrderId = resObj.getInt("orderId");

                        processPayment(newOrderId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            showAlertDialog("Error", "Gagal memproses ID Pesanan", false, null);
                            resetButton();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        showAlertDialog("Error", "Keranjang kosong atau gagal checkout", false, null);
                        resetButton();
                    });
                }
            }
        });
    }

    private void processPayment(int orderId) {
        JSONObject paymentBody = new JSONObject();
        try {
            paymentBody.put("orderId", orderId);
            paymentBody.put("paymentMethod", selectedPaymentMethod);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody reqBody = RequestBody.create(paymentBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "Payments")
                .post(reqBody)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showAlertDialog("Error", "Gagal memproses pembayaran", false, null);
                    resetButton();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    resetButton();
                    if (response.isSuccessful()) {
                        Intent intent = new Intent(PaymentActivity.this, MenuActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        showAlertDialog("Sukses!", "Selamat pembayaran berhasil. Silahkan menunggu barang yang anda beli.", true, intent);
                    } else {
                        showAlertDialog("Error", "Pembayaran ditolak server", false, null);
                    }
                });
            }
        });
    }

    private void resetButton() {
        btnBayar.setEnabled(true);
        btnBayar.setText("Bayar");
    }

    private void showAlertDialog(String title, String message, boolean finishOnOk, Intent navigateIntent) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    if (navigateIntent != null) {
                        startActivity(navigateIntent);
                    }
                    if (finishOnOk) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }
}