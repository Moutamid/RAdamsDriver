package com.moutamid.radamsdriver;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fxn.stash.Stash;
import com.google.gson.JsonObject;
import com.moutamid.radamsdriver.databinding.ActivityLoginBinding;

import org.json.JSONObject;

import java.util.Arrays;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding b;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading...");

        b.loginBtn.setOnClickListener(v -> {
            if (b.nameTextviewCreateProfile.getText().toString().isEmpty())
                return;

            if (b.passwordTextviewCreateProfile.getText().toString().isEmpty())
                return;

            progressDialog.show();

            new Thread(() -> {
                OkHttpClient client = new OkHttpClient().newBuilder().build();
                MediaType mediaType = MediaType.parse("application/json");
//            RequestBody body = RequestBody.create(mediaType, "{\r\n    \"userName\":\"admin\",\r\n    \"password\":\"admin\"\r\n}");
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("userName", b.nameTextviewCreateProfile.getText().toString().trim());
                    jsonObject.put("password", b.passwordTextviewCreateProfile.getText().toString().trim());

                    RequestBody body = RequestBody.create(mediaType, jsonObject.toString());

                    Request request = new Request.Builder()
                            .url("https://app.ra-app.co.uk/api/login")
                            .method("POST", body)
                            .addHeader("Content-Type", "application/json")
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();

                    Log.d(TAG, "onCreate/68: response : " + responseData);
                    JSONObject responseObject = new JSONObject(responseData);
                    Log.d(TAG, "onCreate/68: response : " + responseObject);
                    Log.d(TAG, "onCreate/73:  : ");

                    String permission = responseObject.getJSONObject("user").getString("permission");

//                    if (!responseData.contains("\"token\":")) {
                    if (permission.equals("Admin")) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(LoginActivity.this, "Login failed!", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    Stash.put(Constants.TOKEN, responseObject.getString("token"));
                    Stash.put(Constants.FULL_NAME,
                            responseObject.getJSONObject("user").getString("firstName")
                                    +" "+ responseObject.getJSONObject("user").getString("lastName"));

                    Stash.put(Constants.VEHICLE, responseObject.getJSONObject("user").getString("vehicle"));

                    Stash.put(Constants.IS_LOGGED_IN, true);

                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
                        finish();
                        startActivity(new Intent(LoginActivity.this, WelcomeActivity.class));
                    });

                } catch (Exception e) {
                    Log.d(TAG, "onCreate/49: error: " + e.getMessage());
                    Log.d(TAG, "onCreate/49: error: " + e.getLocalizedMessage());
                    Log.d(TAG, "onCreate/49: error: " + Arrays.toString(e.getStackTrace()));
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                    });
                }
            }).start();
        });
    }
}
