package com.example.petanikita;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;

    private OkHttpClient client;

    TextView txtregister;

    private static final String LOGIN_URL = "http://10.0.2.2:5000/api/Auth/login";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        client = new OkHttpClient();

        etUsername = findViewById(R.id.editTextTextUsernameLogin);
        etPassword = findViewById(R.id.editTextTextPasswordLogin);
        btnLogin = findViewById(R.id.btn_login);
        txtregister = findViewById(R.id.textView6);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Username dan Password tidak boleh kosong", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnLogin.setEnabled(false);
                btnLogin.setText("Loading...");

                performLogin(username, password);

            }
        });
        txtregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
    private void performLogin(String username, String password) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("username", username);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(LOGIN_URL)
                .post(body)
                .addHeader("accept", "*/*")
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("API_ERROR", "Gagal menghubungi server", e);

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Koneksi ke server gagal!", Toast.LENGTH_LONG).show();
                    resetButton();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> {
                    resetButton();

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            String token = jsonResponse.getString("token");
                            String role = jsonResponse.optString("role", "Pembeli");

                            saveToken(token, role);

                            Toast.makeText(MainActivity.this, "Login Berhasil!", Toast.LENGTH_SHORT).show();

                             Intent intent = new Intent(MainActivity.this, MenuActivity.class);
                            startActivity(intent);
                             finish();

                        } catch (JSONException e) {
                            Log.e("JSON_ERROR", "Gagal parsing JSON", e);
                            Toast.makeText(MainActivity.this, "Format data tidak valid", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Username atau password salah!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void resetButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText("LOGIN");
    }

    private void saveToken(String token, String role) {
        SharedPreferences sharedPreferences = getSharedPreferences("PetaniKitaApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("JWT_TOKEN", token);
        editor.putString("USER_ROLE", role);
        editor.apply();
    }
}