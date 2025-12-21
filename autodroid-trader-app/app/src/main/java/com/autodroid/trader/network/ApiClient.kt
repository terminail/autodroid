package com.autodroid.trader.network

import android.util.Log
import com.autodroid.trader.model.TradePlan
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * ApiClient for communicating with the Autodroid container server
 */
class ApiClient private constructor() {
    companion object {
        private const val TAG = "ApiClient"
        private var instance: ApiClient? = null
        
        fun getInstance(): ApiClient {
            if (instance == null) {
                instance = ApiClient()
            }
            return instance!!
        }
    }
    
    private val client: OkHttpClient
    private val gson: Gson
    // For Android emulator: use 10.0.2.2 to access host machine
    // For physical device: use actual host IP address (e.g., 192.168.1.59)
    private var apiEndpoint: String = "http://10.0.2.2:8004/api" // Default API endpoint for Android emulator
    
    init {
        this.client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS) // Longer timeout for bulk operations
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        this.gson = Gson()
    }
    
    /**
     * Set the API endpoint (for dynamic server discovery)
     */
    fun setApiEndpoint(url: String) : ApiClient{
        // Validate API endpoint format
        if (!isValidApiEndpoint(url)) {
            throw IllegalArgumentException("Invalid API endpoint format: $url")
        }
        
        // Use user input as-is
        apiEndpoint = url.trim()
        Log.d(TAG, "API endpoint set to: $apiEndpoint")

        return this
    }
    
    /**
     * Validate API endpoint format
     */
    private fun isValidApiEndpoint(endpoint: String): Boolean {
        return endpoint.startsWith("http://") || endpoint.startsWith("https://")
    }
    
    /**
     * Build API URL with consistent path handling
     */
    private fun buildApiUrl(path: String): String {
        return "$apiEndpoint$path"
    }
    
    /**
     * Register APKs for a device (supports both single APK and list of APKs)
     */
    fun registerApksForDevice(deviceUdid: String, apkData: Any): ApkRegistrationResponse {
        val url = buildApiUrl("/devices/$deviceUdid/apks")
        val response = makePostRequest(url, apkData)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to register APKs: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            throw RuntimeException("Empty response body from APK registration endpoint")
        }
        
        try {
            return gson.fromJson(responseBody, ApkRegistrationResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing APK registration response: ${e.message}")
            Log.e(TAG, "Response body: $responseBody")
            throw RuntimeException("Failed to parse APK registration response", e)
        }
    }
    
    /**
     * Get device information
     */
    fun getDeviceInfo(deviceUdid: String): DeviceInfoResponse {
        val url = buildApiUrl("/devices/$deviceUdid")
        val response = makeGetRequest(url)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to get device info: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            throw RuntimeException("Empty response body from device info endpoint")
        }
        
        try {
            return gson.fromJson(responseBody, DeviceInfoResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing device info response: ${e.message}")
            Log.e(TAG, "Response body: $responseBody")
            throw RuntimeException("Failed to parse device info response", e)
        }
    }
    
    /**
     * Register a device with the server
     */
    fun registerDevice(deviceInfo: DeviceRegistrationRequest): DeviceRegistrationResponse {
        val url = buildApiUrl("/devices/register")
        val response = makePostRequest(url, deviceInfo)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to register device: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            throw RuntimeException("Empty response body from device registration endpoint")
        }
        
        try {
            return gson.fromJson(responseBody, DeviceRegistrationResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing device registration response: ${e.message}")
            Log.e(TAG, "Response body: $responseBody")
            throw RuntimeException("Failed to parse device registration response", e)
        }
    }
    
    /**
     * Health check for the server
     */
    fun healthCheck(): HealthCheckResponse {
        val url = buildApiUrl("/health")
        val response = makeGetRequest(url)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to perform health check: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            throw RuntimeException("Empty response body from health check endpoint")
        }
        
        try {
            return gson.fromJson(responseBody, HealthCheckResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing health check response: ${e.message}")
            Log.e(TAG, "Response body: $responseBody")
            throw RuntimeException("Failed to parse health check response", e)
        }
    }

    /**
     * Get server information from FastAPI
     */
    fun getServerInfo(): ServerInfoResponse? {
        // Build the complete URL by appending /server to the base API endpoint
        val url = buildApiUrl("/server")
        
        val response = makeGetRequest(url)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to fetch server info: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            throw RuntimeException("Empty response body from server info endpoint")
        }
        
        try {
            return gson.fromJson(responseBody, ServerInfoResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server info response: ${e.message}")
            Log.e(TAG, "Response body: $responseBody")
            throw RuntimeException("Failed to parse server info response", e)
        }
    }
    
    /**
     * Get trade plans from server
     */
    fun getTradePlans(): List<TradePlan> {
        val url = buildApiUrl("/tradeplans")
        val response = makeGetRequest(url)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to fetch trade plans: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            throw RuntimeException("Empty response body from trade plans endpoint")
        }
        
        try {
            val tradePlanType = object : TypeToken<List<TradePlan>>() {}.type
            return gson.fromJson(responseBody, tradePlanType)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing trade plans response: ${e.message}")
            Log.e(TAG, "Response body: $responseBody")
            throw RuntimeException("Failed to parse trade plans response", e)
        }
    }
    
    private fun makePostRequest(url: String, data: Any): Response {
        try {
            val json = gson.toJson(data)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = json.toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            
            Log.d(TAG, "Making POST request to: $url")
            Log.d(TAG, "Request data: $json")
            
            return client.newCall(request).execute()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error making POST request to $url: ${e.message}")
            throw RuntimeException("Network request failed", e)
        }
    }
    
    private fun makeGetRequest(url: String): Response {
        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            Log.d(TAG, "Making GET request to: $url")
            
            return client.newCall(request).execute()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error making GET request to $url: ${e.message}")
            throw RuntimeException("Network request failed", e)
        }
    }
    
    /**
     * Response extension to check if request was successful
     * Note: This extension is redundant as OkHttp Response already has isSuccessful property
     */
    // val Response.isSuccessful: Boolean
    //     get() = this.isSuccessful
}