package com.autodroid.proxy.auth.service;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthService {
    private static final String TAG = "AuthService";
    private static final String BASE_URL = "http://autodroid-server:8000/api/auth";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private OkHttpClient client;
    private Gson gson;
    
    public AuthService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        
        this.gson = new Gson();
    }
    
    public interface AuthCallback {
        void onSuccess(JsonObject response);
        void onError(String error);
    }
    
    public void login(String email, String password, AuthCallback callback) {
        // Create login request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);
        requestBody.addProperty("password", password);
        
        // Create request
        Request request = new Request.Builder()
                .url(BASE_URL + "/login")
                .post(RequestBody.create(gson.toJson(requestBody), JSON))
                .build();
        
        // Execute request asynchronously
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "Login request failed: " + e.getMessage());
                callback.onError("Login failed: " + e.getMessage());
            }
            
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    callback.onSuccess(jsonResponse);
                } else {
                    String errorBody = response.body().string();
                    Log.e(TAG, "Login failed with code " + response.code() + ": " + errorBody);
                    callback.onError("Login failed: " + response.message());
                }
            }
        });
    }
    
    public void register(String email, String password, AuthCallback callback) {
        // Create register request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);
        requestBody.addProperty("password", password);
        
        // Create request
        Request request = new Request.Builder()
                .url(BASE_URL + "/register")
                .post(RequestBody.create(gson.toJson(requestBody), JSON))
                .build();
        
        // Execute request asynchronously
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "Register request failed: " + e.getMessage());
                callback.onError("Registration failed: " + e.getMessage());
            }
            
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    callback.onSuccess(jsonResponse);
                } else {
                    String errorBody = response.body().string();
                    Log.e(TAG, "Registration failed with code " + response.code() + ": " + errorBody);
                    callback.onError("Registration failed: " + response.message());
                }
            }
        });
    }
    
    public void getCurrentUser(String token, AuthCallback callback) {
        // Create request with authorization header
        Request request = new Request.Builder()
                .url(BASE_URL + "/me")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();
        
        // Execute request asynchronously
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "Get user request failed: " + e.getMessage());
                callback.onError("Failed to get user info: " + e.getMessage());
            }
            
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    callback.onSuccess(jsonResponse);
                } else {
                    String errorBody = response.body().string();
                    Log.e(TAG, "Get user failed with code " + response.code() + ": " + errorBody);
                    callback.onError("Failed to get user info: " + response.message());
                }
            }
        });
    }
}
