package com.autodroid.manager.network

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * ApiClient for communicating with the Autodroid container server
 */
class ApiClient private constructor() {
    companion object {
        private const val TAG = "ApiClient"
        private const val BASE_URL = "http://192.168.1.59:8004" // Default server URL
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
    private var serverBaseUrl: String = BASE_URL
    
    init {
        this.client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS) // Longer timeout for bulk operations
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        this.gson = Gson()
    }
    
    /**
     * Set the server base URL (for dynamic server discovery)
     */
    fun setServerBaseUrl(url: String) {
        serverBaseUrl = url
        Log.d(TAG, "Server base URL set to: $url")
    }
    
    /**
     * Register a single APK for a device
     */
    fun registerApkForDevice(deviceUdid: String, apkData: Map<String, Any>): Response {
        val url = "$serverBaseUrl/api/devices/$deviceUdid/apks"
        return makePostRequest(url, apkData)
    }
    
    /**
     * Register multiple APKs for a device in bulk
     */
    fun registerApksBulkForDevice(deviceUdid: String, apkList: List<Map<String, Any>>): Response {
        val url = "$serverBaseUrl/api/devices/$deviceUdid/apks/bulk"
        return makePostRequest(url, apkList)
    }
    
    /**
     * Get device information
     */
    fun getDeviceInfo(deviceUdid: String): Response {
        val url = "$serverBaseUrl/api/devices/$deviceUdid"
        return makeGetRequest(url)
    }
    
    /**
     * Register a device with the server
     */
    fun registerDevice(deviceInfo: Map<String, Any>): Response {
        val url = "$serverBaseUrl/api/devices"
        return makePostRequest(url, deviceInfo)
    }
    
    /**
     * Health check for the server
     */
    fun healthCheck(): Response {
        val url = "$serverBaseUrl/api/health"
        return makeGetRequest(url)
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
     */
    val Response.isSuccessful: Boolean
        get() = this.isSuccessful
}