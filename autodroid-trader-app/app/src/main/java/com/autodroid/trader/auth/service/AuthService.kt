package com.autodroid.trader.auth.service

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class AuthService {
    private val client: OkHttpClient
    private val gson: Gson

    init {
        this.client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        this.gson = Gson()
    }

    interface AuthCallback {
        fun onSuccess(response: JsonObject?)
        fun onError(error: String?)
    }

    fun login(email: String?, password: String?, callback: AuthCallback) {
        // Create login request body
        val requestBody = JsonObject()
        requestBody.addProperty("email", email)
        requestBody.addProperty("password", password)

        // Create request
        val request: Request = Request.Builder()
            .url(BASE_URL + "/login")
            .post(gson.toJson(requestBody).toRequestBody(JSON))
            .build()

        // Execute request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: okio.IOException) {
                Log.e(TAG, "Login request failed: " + e.message)
                callback.onError("Login failed: " + e.message)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse =
                        gson.fromJson<JsonObject?>(responseBody, JsonObject::class.java)
                    callback.onSuccess(jsonResponse)
                } else {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Login failed with code " + response.code + ": " + errorBody)
                    callback.onError("Login failed: " + response.message)
                }
            }
        })
    }

    fun register(email: String?, password: String?, callback: AuthCallback) {
        // Create register request body
        val requestBody = JsonObject()
        requestBody.addProperty("email", email)
        requestBody.addProperty("password", password)

        // Create request
        val request: Request = Request.Builder()
            .url(BASE_URL + "/register")
            .post(gson.toJson(requestBody).toRequestBody(JSON))
            .build()

        // Execute request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: okio.IOException) {
                Log.e(TAG, "Register request failed: " + e.message)
                callback.onError("Registration failed: " + e.message)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse =
                        gson.fromJson<JsonObject?>(responseBody, JsonObject::class.java)
                    callback.onSuccess(jsonResponse)
                } else {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Registration failed with code " + response.code + ": " + errorBody)
                    callback.onError("Registration failed: " + response.message)
                }
            }
        })
    }

    fun getCurrentUser(token: String?, callback: AuthCallback) {
        // Create request with authorization header
        val request: Request = Request.Builder()
            .url(BASE_URL + "/me")
            .header("Authorization", "Bearer " + token)
            .get()
            .build()

        // Execute request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: okio.IOException) {
                Log.e(TAG, "Get user request failed: " + e.message)
                callback.onError("Failed to get user info: " + e.message)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse =
                        gson.fromJson<JsonObject?>(responseBody, JsonObject::class.java)
                    callback.onSuccess(jsonResponse)
                } else {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Get user failed with code " + response.code + ": " + errorBody)
                    callback.onError("Failed to get user info: " + response.message)
                }
            }
        })
    }

    companion object {
        private const val TAG = "AuthService"
        private const val BASE_URL = "http://autodroid-server:8000/api/auth"
        private val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
    }
}

private fun Call.enqueue(responseCallback: Any) {}
