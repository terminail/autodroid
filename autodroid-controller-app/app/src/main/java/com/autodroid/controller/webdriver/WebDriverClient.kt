package com.autodroid.controller.webdriver

import com.autodroid.controller.model.Action
import com.autodroid.controller.model.ActionType
import com.autodroid.controller.model.SessionManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebDriverClient(
    private val baseUrl: String = "http://127.0.0.1:6790"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    
    fun executeCommand(endpoint: String, method: String = "POST", data: JSONObject? = null): Response {
        val url = if (endpoint.startsWith("/")) {
            "$baseUrl$endpoint"
        } else {
            "$baseUrl/$endpoint"
        }
        
        val requestBuilder = Request.Builder().url(url)
        
        when (method.uppercase()) {
            "POST" -> {
                val body = data?.toString()?.toRequestBody(jsonMediaType) 
                    ?: "{}".toRequestBody(jsonMediaType)
                requestBuilder.post(body)
            }
            "GET" -> requestBuilder.get()
            "DELETE" -> requestBuilder.delete()
            else -> throw IllegalArgumentException("Unsupported method: $method")
        }
        
        val request = requestBuilder.build()
        return client.newCall(request).execute()
    }
    
    fun initSession(capabilities: Map<*, *>): String {
        val caps = JSONObject().apply {
            put("platformName", "Android")
            put("appium:automationName", "UiAutomator2")
            
            // 使用传入的capabilities参数，如果没有则使用默认值
            val udid = capabilities["appium:udid"] ?: "TDCDU17905004388"
            val appPackage = capabilities["appium:appPackage"] ?: "com.tdx.androidCCZQ"
            val noReset = capabilities["appium:noReset"] ?: false
            val autoGrantPermissions = capabilities["appium:autoGrantPermissions"] ?: true
            val skipServerInstallation = capabilities["appium:skipServerInstallation"] ?: true
            val remoteAppsCacheLimit = capabilities["appium:remoteAppsCacheLimit"] ?: 0
            val dontStopAppOnReset = capabilities["appium:dontStopAppOnReset"] ?: true
            
            put("appium:udid", udid)
            put("appium:appPackage", appPackage)
            put("appium:noReset", noReset)
            put("appium:autoGrantPermissions", autoGrantPermissions)
            put("appium:skipServerInstallation", skipServerInstallation)
            put("appium:remoteAppsCacheLimit", remoteAppsCacheLimit)
            put("appium:dontStopAppOnReset", dontStopAppOnReset)
        }
        
        val data = JSONObject().apply {
            put("capabilities", JSONObject().apply {
                put("alwaysMatch", caps)
                put("firstMatch", JSONArray())
            })
            put("desiredCapabilities", caps)
        }
        
        val response = executeCommand("/session", "POST", data)
        val responseBody = response.body?.string() ?: ""
        
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to init session: $responseBody")
        }
        
        val jsonResponse = JSONObject(responseBody)
        val sessionId = jsonResponse.getJSONObject("value").getString("sessionId")
        SessionManager.currentSessionId = sessionId
        return sessionId
    }
    
    fun findElement(strategy: String, selector: String): String {
        val sessionId = SessionManager.currentSessionId 
            ?: throw IllegalStateException("No active session")
        
        val data = JSONObject().apply {
            put("using", strategy)
            put("value", selector)
        }
        
        val response = executeCommand("/session/$sessionId/element", "POST", data)
        val responseBody = response.body?.string() ?: ""
        val jsonResponse = JSONObject(responseBody)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Find element failed: $responseBody")
        }
        
        val elementId = jsonResponse.getJSONObject("value").getString("ELEMENT")
        SessionManager.currentElementId = elementId
        SessionManager.context["lastElementId"] = elementId
        return elementId
    }
    
    fun clickElement(elementId: String) {
        val sessionId = SessionManager.currentSessionId 
            ?: throw IllegalStateException("No active session")
        
        val response = executeCommand("/session/$sessionId/element/$elementId/click", "POST")
        
        if (!response.isSuccessful) {
            throw RuntimeException("Click failed: ${response.body?.string()}")
        }
    }
    
    fun sendKeys(elementId: String, text: String) {
        val sessionId = SessionManager.currentSessionId 
            ?: throw IllegalStateException("No active session")
        
        val data = JSONObject().apply {
            put("text", text)
        }
        
        val response = executeCommand("/session/$sessionId/element/$elementId/value", "POST", data)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Send keys failed: ${response.body?.string()}")
        }
    }
    
    fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Int = 500) {
        val sessionId = SessionManager.currentSessionId 
            ?: throw IllegalStateException("No active session")
        
        val data = JSONObject().apply {
            put("startX", startX)
            put("startY", startY)
            put("endX", endX)
            put("endY", endY)
            put("duration", duration)
        }
        
        val response = executeCommand("/session/$sessionId/touch/perform", "POST", data)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Swipe failed: ${response.body?.string()}")
        }
    }
    
    fun takeScreenshot(): String {
        val sessionId = SessionManager.currentSessionId 
            ?: throw IllegalStateException("No active session")
        
        val response = executeCommand("/session/$sessionId/screenshot", "GET")
        val responseBody = response.body?.string() ?: ""
        val jsonResponse = JSONObject(responseBody)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Screenshot failed: $responseBody")
        }
        
        return jsonResponse.getString("value")
    }
    
    fun closeSession() {
        val sessionId = SessionManager.currentSessionId ?: return
        
        try {
            executeCommand("/session/$sessionId", "DELETE")
        } catch (e: Exception) {
            // Ignore errors when closing session
        } finally {
            SessionManager.clear()
        }
    }
    
    fun getPageSource(): String {
        val sessionId = SessionManager.currentSessionId 
            ?: throw IllegalStateException("No active session")
        
        val response = executeCommand("/session/$sessionId/source", "GET")
        val responseBody = response.body?.string() ?: ""
        val jsonResponse = JSONObject(responseBody)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Get page source failed: $responseBody")
        }
        
        return jsonResponse.getString("value")
    }
    
    fun executeAction(action: Action): Any {
        return when (action.action) {
            ActionType.INIT_SESSION -> {
                val caps = action.params["capabilities"] as? Map<String, Any>
                    ?: throw IllegalArgumentException("Missing capabilities")
                initSession(caps)
            }
            ActionType.FIND_ELEMENT -> {
                val strategy = action.params["strategy"] as? String
                    ?: throw IllegalArgumentException("Missing strategy")
                val selector = action.params["selector"] as? String
                    ?: throw IllegalArgumentException("Missing selector")
                findElement(strategy, selector)
            }
            ActionType.CLICK -> {
                var elementId = action.params["elementId"] as? String
                elementId = SessionManager.resolveVariable(elementId ?: "") ?: SessionManager.currentElementId
                    ?: throw IllegalArgumentException("No element specified")
                clickElement(elementId)
                "click_success"
            }
            ActionType.SEND_KEYS -> {
                var elementId = action.params["elementId"] as? String
                val text = action.params["text"] as? String
                    ?: throw IllegalArgumentException("Missing text")
                elementId = SessionManager.resolveVariable(elementId ?: "") ?: SessionManager.currentElementId
                    ?: throw IllegalArgumentException("No element specified")
                sendKeys(elementId, text)
                "sendKeys_success"
            }
            ActionType.SWIPE -> {
                val startX = action.params["startX"] as? Int
                    ?: throw IllegalArgumentException("Missing startX")
                val startY = action.params["startY"] as? Int
                    ?: throw IllegalArgumentException("Missing startY")
                val endX = action.params["endX"] as? Int
                    ?: throw IllegalArgumentException("Missing endX")
                val endY = action.params["endY"] as? Int
                    ?: throw IllegalArgumentException("Missing endY")
                val duration = action.params["duration"] as? Int ?: 500
                swipe(startX, startY, endX, endY, duration)
                "swipe_success"
            }
            ActionType.TAKE_SCREENSHOT -> {
                takeScreenshot()
            }
            ActionType.GET_PAGE_SOURCE -> {
                getPageSource()
            }
            ActionType.CLOSE_SESSION -> {
                closeSession()
                "closeSession_success"
            }
        }
    }
}