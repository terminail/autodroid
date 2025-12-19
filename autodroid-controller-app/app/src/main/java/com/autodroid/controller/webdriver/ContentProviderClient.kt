package com.autodroid.controller.webdriver

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.autodroid.controller.model.Action
import com.autodroid.controller.model.ActionType
import com.autodroid.controller.model.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ContentProviderClient(
    private val context: Context
) {
    companion object {
        private const val TAG = "ContentProviderClient"
        private const val AUTHORITY = "io.appium.uiautomator2.server.provider"
        private const val BASE_CONTENT_URI = "content://$AUTHORITY/http"
        
        // 简化的参数名（根据HttpProxyProvider设计）
        private const val PARAM_METHOD = "X"
        private const val PARAM_DATA = "d"
        private const val PARAM_HEADER = "H"
        
        /**
         * 检查UIA2 Server状态（通过Content Provider）
         */
        fun checkUIA2ServerStatus(context: Context): JSONObject {
            return executeCommand(context, "/status", "GET")
        }
        
        /**
         * 检查UIA2 Server是否正常运行
         */
        fun isUIA2ServerRunning(context: Context): Boolean {
            val result = checkUIA2ServerStatus(context)
            if (result.getBoolean("success")) {
                try {
                    val response = JSONObject(result.getString("response"))
                    val value = response.getJSONObject("value")
                    return value.getBoolean("ready")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse server status response", e)
                }
            }
            return false
        }
        
        /**
         * 获取UIA2 Server详细信息
         */
        fun getUIA2ServerInfo(context: Context): JSONObject {
            val result = checkUIA2ServerStatus(context)
            return if (result.getBoolean("success")) {
                try {
                    val response = JSONObject(result.getString("response"))
                    val value = response.getJSONObject("value")
                    val build = value.getJSONObject("build")
                    
                    JSONObject().apply {
                        put("success", true)
                        put("ready", value.getBoolean("ready"))
                        put("version", build.getString("version"))
                        put("message", value.getString("message"))
                        put("sessionId", response.getString("sessionId"))
                    }
                } catch (e: Exception) {
                    JSONObject().apply {
                        put("success", false)
                        put("message", "Failed to parse server info: ${e.message}")
                    }
                }
            } else {
                result
            }
        }
        
        /**
         * 通过Content Provider发送HTTP请求
         */
        private fun executeCommand(
            context: Context,
            endpoint: String,
            method: String = "GET",
            data: JSONObject? = null
        ): JSONObject {
            val uri = Uri.parse("$BASE_CONTENT_URI$endpoint")
            val contentValues = ContentValues()
            
            try {
                when (method.uppercase()) {
                    "POST", "PUT" -> {
                        contentValues.put(PARAM_METHOD, method)
                        if (data != null) {
                            contentValues.put(PARAM_DATA, data.toString())
                        }
                        contentValues.put(PARAM_HEADER, "application/json")
                        
                        val resultUri = context.contentResolver.insert(uri, contentValues)
                        return JSONObject().apply {
                            put("success", true)
                            put("message", "Command executed via Content Provider")
                            put("resultUri", resultUri?.toString() ?: "")
                        }
                    }
                    "GET" -> {
                        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
                        return try {
                            cursor?.use { 
                                if (it.moveToFirst()) {
                                    val responseColumnIndex = it.getColumnIndex("response")
                                    val statusCodeColumnIndex = it.getColumnIndex("status_code")
                                    
                                    val response = if (responseColumnIndex >= 0) it.getString(responseColumnIndex) else null
                                    val statusCode = if (statusCodeColumnIndex >= 0) it.getInt(statusCodeColumnIndex) else -1
                                    
                                    JSONObject().apply {
                                        put("success", statusCode in 200..299)
                                        put("statusCode", statusCode)
                                        put("response", response ?: "{}")
                                    }
                                } else {
                                    JSONObject().apply {
                                        put("success", false)
                                        put("message", "No data returned from Content Provider")
                                    }
                                }
                            } ?: JSONObject().apply {
                                put("success", false)
                                put("message", "Cursor is null")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Query failed", e)
                            JSONObject().apply {
                                put("success", false)
                                put("message", "Query failed: ${e.message}")
                            }
                        }
                    }
                    "DELETE" -> {
                        val rowsDeleted = context.contentResolver.delete(uri, null, null)
                        return JSONObject().apply {
                            put("success", rowsDeleted > 0)
                            put("deleted", rowsDeleted)
                        }
                    }
                    else -> {
                        throw IllegalArgumentException("Unsupported method: $method")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Content Provider operation failed", e)
                return JSONObject().apply {
                    put("success", false)
                    put("message", "Content Provider operation failed: ${e.message}")
                }
            }
        }
    }
    
    // 发送WebDriver命令的核心方法（使用Content Provider）
    fun executeCommand(endpoint: String, method: String = "POST", data: JSONObject? = null): String {
        val uri = Uri.parse("$BASE_CONTENT_URI$endpoint")
        val contentValues = ContentValues()
        
        when (method.uppercase()) {
            "POST", "PUT" -> {
                contentValues.put(PARAM_METHOD, method)
                if (data != null) {
                    contentValues.put(PARAM_DATA, data.toString())
                }
                contentValues.put(PARAM_HEADER, "application/json")
                
                val resultUri = context.contentResolver.insert(uri, contentValues)
                return "Command executed via Content Provider"
            }
            "GET" -> {
                val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
                return try {
                    cursor?.use { 
                        if (it.moveToFirst()) {
                            val columnIndex = it.getColumnIndex("response")
                            if (columnIndex >= 0) {
                                val response = it.getString(columnIndex)
                                response ?: "{}"
                            } else {
                                "{}"
                            }
                        } else {
                            "{}"
                        }
                    } ?: "{}"
                } catch (e: Exception) {
                    Log.e("ContentProviderClient", "Query failed", e)
                    "{}"
                }
            }
            "DELETE" -> {
                val rowsDeleted = context.contentResolver.delete(uri, null, null)
                return "{\"deleted\": $rowsDeleted}"
            }
            else -> throw IllegalArgumentException("Unsupported method: $method")
        }
    }
    
    private fun getCurrentDeviceUdId(): String {
        // 对于UIA2 Server，通常使用固定的设备标识符
        // 在实际设备上，可以使用 "emulator-5554" 或通过ADB获取真实设备ID
        // 这里返回一个固定的值，因为UIA2 Server在同一设备上运行
        return "TDCDU17905004388"
    }
    
    fun initSession(appPackage: String, appActivity: String? = null): String {
        // 动态获取当前设备的UDID
        val udid = getCurrentDeviceUdId()

        
        val caps = JSONObject().apply {
            // 硬编码的配置参数
            put("platformName", "Android")
            put("automationName", "UiAutomator2")
            put("udid", udid)
            put("appPackage", appPackage)
            put("noReset", false)
            put("autoGrantPermissions", true)
            put("skipServerInstallation", true)
            put("remoteAppsCacheLimit", 0)
            put("dontStopAppOnReset", true)
            
            // 如果有appActivity参数，则添加
            appActivity?.let { put("appActivity", it) }
        }
        
        val data = JSONObject().apply {
            put("capabilities", JSONObject().apply {
                put("alwaysMatch", caps)
                put("firstMatch", JSONArray())
            })
            put("desiredCapabilities", caps)
        }
        
        val response = executeCommand("/session", "POST", data)
        val jsonResponse = JSONObject(response)
        
        if (jsonResponse.has("error")) {
            throw RuntimeException("Failed to init session: $response")
        }
        
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
        val jsonResponse = JSONObject(response)
        
        if (jsonResponse.has("error")) {
            throw RuntimeException("Find element failed: $response")
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
        val jsonResponse = JSONObject(response)
        
        if (jsonResponse.has("error")) {
            throw RuntimeException("Click failed: $response")
        }
    }
    
    fun sendKeys(elementId: String, text: String) {
        val sessionId = SessionManager.currentSessionId 
            ?: throw IllegalStateException("No active session")
        
        val data = JSONObject().apply {
            put("text", text)
        }
        
        val response = executeCommand("/session/$sessionId/element/$elementId/value", "POST", data)
        val jsonResponse = JSONObject(response)
        
        if (jsonResponse.has("error")) {
            throw RuntimeException("Send keys failed: $response")
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
        val jsonResponse = JSONObject(response)
        
        if (jsonResponse.has("error")) {
            throw RuntimeException("Swipe failed: $response")
        }
    }
    
    fun takeScreenshot(): String {
        val sessionId = SessionManager.currentSessionId 
            ?: throw IllegalStateException("No active session")
        
        val response = executeCommand("/session/$sessionId/screenshot", "GET")
        val jsonResponse = JSONObject(response)
        
        if (jsonResponse.has("error")) {
            throw RuntimeException("Screenshot failed: $response")
        }
        
        val base64Image = jsonResponse.getString("value")
        
        // 保存为PNG文件
        saveScreenshotToFile(base64Image)
        
        return base64Image
    }
    
    private fun saveScreenshotToFile(base64Image: String) {
        try {
            // 解码base64图片数据
            val imageBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            // 创建文件名
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "screenshot_${timestamp}.png"
            
            // 保存到外部存储的Pictures目录
            val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val screenshotFile = File(picturesDir, fileName)
            
            // 保存为PNG文件
            FileOutputStream(screenshotFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }
            
            // 记录保存路径
            Log.d("ContentProviderClient", "Screenshot saved to: ${screenshotFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.e("ContentProviderClient", "Failed to save screenshot: ${e.message}")
        }
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
        val jsonResponse = JSONObject(response)
        
        if (jsonResponse.has("error")) {
            throw RuntimeException("Get page source failed: $response")
        }
        
        return jsonResponse.getString("value")
    }
    
    fun executeAction(action: Action): Any {
        return when (action.action) {
            ActionType.INIT_SESSION -> {
                val appPackage = action.params["appPackage"] as? String
                    ?: throw IllegalArgumentException("Missing appPackage")
                val appActivity = action.params["appActivity"] as? String
                initSession(appPackage, appActivity)
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
                "session_closed"
            }
            else -> throw IllegalArgumentException("Unsupported action: ${action.action}")
        }
    }
}