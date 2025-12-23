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
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS) // Longer timeout for bulk operations
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson: Gson = Gson()

    // For Android emulator: use 10.0.2.2 to access host machine
    // For physical device: use actual host IP address (e.g., 192.168.1.59)
    private var apiEndpoint: String = "http://10.0.2.2:8004/api" // Default API endpoint for Android emulator

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
     * Get device information
     */
    fun getDeviceInfo(deviceSerialNo: String): DeviceInfoResponse {
        val url = buildApiUrl("/devices/$deviceSerialNo")
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
    fun registerDevice(deviceInfo: DeviceCreateRequest): DeviceCreateResponse {
        val url = buildApiUrl("/devices")
        val response = makePostRequest(url, deviceInfo)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to register device: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            throw RuntimeException("Empty response body from device registration endpoint")
        }
        
        try {
            return gson.fromJson(responseBody, DeviceCreateResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing device registration response: ${e.message}")
            Log.e(TAG, "Response body: $responseBody")
            throw RuntimeException("Failed to parse device registration response", e)
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
     * Get server information from a custom API endpoint
     */
    fun getServerInfo(customApiEndpoint: String): ServerInfoResponse {
        // Build the complete URL by appending /server to the custom API endpoint
        val url = if (customApiEndpoint.endsWith("/api")) {
            "$customApiEndpoint/server"
        } else {
            "$customApiEndpoint/api/server"
        }
        
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
     * As Server to Check device debug permissions & installed apps etc.
     */
    fun checkDevice(deviceSerialNo: String): DeviceCheckResponse {
        val url = buildApiUrl("/devices/$deviceSerialNo/check")
        val response = makePostRequest(url, emptyMap<String, Any>())
        
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to check device: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            throw RuntimeException("Empty response body from check endpoint")
        }
        
        try {
            return gson.fromJson(responseBody, DeviceCheckResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing check response: ${e.message}")
            Log.e(TAG, "Response body: $responseBody")
            throw RuntimeException("Failed to parse check response", e)
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
    
    /**
     * Update trade plan status on server
     */
    fun updateTradePlanStatus(id: String, status: String): String {
        val url = buildApiUrl("/tradeplans/$id/status")
        val requestData = mapOf("status" to status)
        val response = makePostRequest(url, requestData)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to update trade plan status: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            throw RuntimeException("Empty response body from trade plan status update endpoint")
        }
        
        try {
            val jsonResponse = gson.fromJson(responseBody, Map::class.java)
            return jsonResponse["message"]?.toString() ?: "Status updated successfully"
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing trade plan status update response: ${e.message}")
            Log.e(TAG, "Response body: $responseBody")
            throw RuntimeException("Failed to parse trade plan status update response", e)
        }
    }
    
    /**
     * Execute trade plan on server
     */
    fun executeTradePlan(id: String): String {
        val url = buildApiUrl("/tradeplans/$id/execute")
        val response = makePostRequest(url, emptyMap<String, Any>())
        
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to execute trade plan: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            throw RuntimeException("Empty response body from trade plan execute endpoint")
        }
        
        try {
            val jsonResponse = gson.fromJson(responseBody, Map::class.java)
            return jsonResponse["message"]?.toString() ?: "Trade plan execution started"
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing trade plan execute response: ${e.message}")
            Log.e(TAG, "Response body: $responseBody")
            throw RuntimeException("Failed to parse trade plan execute response", e)
        }
    }
    
    /**
     * Get all trade plans from server
     */
    fun getAllTradePlans(): List<TradePlan> {
        val url = buildApiUrl("/tradeplans")
        val response = makeGetRequest(url)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to fetch all trade plans: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            throw RuntimeException("Empty response body from all trade plans endpoint")
        }
        
        try {
            val jsonResponse = gson.fromJson(responseBody, Map::class.java)
            val tradeplansJson = jsonResponse["tradeplans"]
            if (tradeplansJson != null) {
                val tradePlanType = object : TypeToken<List<TradePlan>>() {}.type
                return gson.fromJson(gson.toJson(tradeplansJson), tradePlanType)
            }
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing all trade plans response: ${e.message}")
            Log.e(TAG, "Response body: $responseBody")
            throw RuntimeException("Failed to parse all trade plans response", e)
        }
    }
    
    /**
     * Execute all approved trade plans on server
     */
    fun executeApprovedPlans(): String {
        val url = buildApiUrl("/tradeplans/execute-approved")
        val response = makePostRequest(url, emptyMap<String, Any>())
        
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to execute approved plans: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            throw RuntimeException("Empty response body from execute approved plans endpoint")
        }
        
        try {
            val jsonResponse = gson.fromJson(responseBody, Map::class.java)
            return jsonResponse["message"]?.toString() ?: "Approved plans execution started"
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing execute approved plans response: ${e.message}")
            Log.e(TAG, "Response body: $responseBody")
            throw RuntimeException("Failed to parse execute approved plans response", e)
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