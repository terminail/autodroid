package com.autodroid.controller.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.autodroid.controller.model.Action
import com.autodroid.controller.model.ActionType
import com.autodroid.controller.model.AppiumStatus
import com.autodroid.controller.model.AutomationTask
import com.autodroid.controller.util.NotificationHelper
import com.autodroid.controller.webdriver.ContentProviderClient
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

class AutoDroidControllerService : Service() {
    private val TAG = "AutoDroidControllerService"
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var webDriverClient: ContentProviderClient
    
    companion object {
        const val ACTION_START = "com.autodroid.controller.START"
        const val ACTION_STOP = "com.autodroid.controller.STOP"
        const val ACTION_EXECUTE_TASK = "com.autodroid.controller.EXECUTE_TASK"
        const val EXTRA_TASK_JSON = "task_json"
        const val ACTION_START_APPIUM_SERVER = "com.autodroid.controller.START_APPIUM_SERVER"
        const val ACTION_STOP_APPIUM_SERVER = "com.autodroid.controller.STOP_APPIUM_SERVER"
        const val ACTION_APPIUM_STATUS_UPDATE = "com.autodroid.controller.APPIUM_STATUS_UPDATE"
        const val EXTRA_APPIUM_STATUS = "appium_status"
        const val ACTION_SERVICE_STATUS_UPDATE = "com.autodroid.controller.SERVICE_STATUS_UPDATE"
        const val EXTRA_SERVICE_STATUS = "service_status"
        const val EXTRA_LAST_CHECK_TIME = "last_check_time"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "AutomationService created")
        webDriverClient = ContentProviderClient(this)
        
        // 注意：不在这里启动定期任务检查，而是在服务真正启动时启动
        // schedulePeriodicTaskCheck() 移动到 onStartCommand 中
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 启动服务
        startService()
        
        when (intent?.action) {
            ACTION_START -> {
                Log.i(TAG, "Starting automation service")
                // 立即广播服务启动状态（只广播服务状态，不广播Appium状态）
                broadcastServiceStatus(com.autodroid.controller.model.ServiceStatus.STARTING)
                // 启动定期任务检查
                schedulePeriodicTaskCheck()
                // 在后台启动任务循环
                scope.launch {
                    startTaskLoop()
                }
                // 服务启动后立即广播运行状态（只广播服务状态，不广播Appium状态）
                broadcastServiceStatus(com.autodroid.controller.model.ServiceStatus.RUNNING)
            }
            ACTION_STOP -> {
                Log.i(TAG, "Stopping automation service")
                // 只广播服务停止状态，不影响Appium服务器状态
                broadcastServiceStatus(com.autodroid.controller.model.ServiceStatus.STOPPING)
                stopSelf()
                // 服务停止后只广播服务停止状态
                broadcastServiceStatus(com.autodroid.controller.model.ServiceStatus.STOPPED)
                // 注意：不广播Appium状态，服务停止不应该影响Appium服务器
            }
            ACTION_EXECUTE_TASK -> {
                val taskJson = intent.getStringExtra(EXTRA_TASK_JSON)
                if (taskJson != null) {
                    scope.launch {
                        executeTask(taskJson)
                    }
                }
            }
            ACTION_START_APPIUM_SERVER -> {
                Log.i(TAG, "Starting Appium UIA2 Server")
                scope.launch {
                    startAppiumServer()
                }
            }
            ACTION_STOP_APPIUM_SERVER -> {
                Log.i(TAG, "Stopping Appium UIA2 Server")
                scope.launch {
                    stopAppiumServer()
                }
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "AutomationService destroyed")
        job.cancel()
        webDriverClient.closeSession()
        
        // 服务销毁时只广播服务停止状态，不影响Appium服务器状态
        broadcastServiceStatus(com.autodroid.controller.model.ServiceStatus.STOPPED)
        
        // 注意：这里不广播Appium状态，因为服务停止不应该影响Appium服务器的运行状态
        // Appium服务器应该独立于服务运行，只有在明确停止Appium时才更新其状态
    }
    
    private suspend fun startAppiumServer() {
        Log.i(TAG, "Starting Appium UIA2 Server")
        
        // 立即更新状态为启动中
        broadcastAppiumStatus(AppiumStatus.STARTING)
        
        try {
            // 检查网络连接状态
            val isNetworkAvailable = checkNetworkConnectivity()
            Log.i(TAG, "Network connectivity check: $isNetworkAvailable")
            
            // 如果网络不可用，直接返回失败状态
            if (!isNetworkAvailable) {
                Log.e(TAG, "Network not available, cannot start Appium server")
                broadcastAppiumStatus(AppiumStatus.STOPPED)
                return
            }
            
            // 主要方法：启动Appium Settings应用来管理UIA2服务器
            val settingsIntent = packageManager.getLaunchIntentForPackage("io.appium.settings")
            if (settingsIntent != null) {
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(settingsIntent)
                Log.i(TAG, "Appium Settings launched successfully")
                
                // 等待一段时间让用户操作Appium Settings
                delay(3000)
                
                // 检查服务器状态
                if (isAppiumServerRunning()) {
                    Log.i(TAG, "Appium UIA2 Server is now running")
                    broadcastAppiumStatus(AppiumStatus.RUNNING)
                    return
                } else {
                    Log.w(TAG, "Appium UIA2 Server not running after launching Appium Settings")
                    // 服务器可能需要在Appium Settings中手动启动
                    // 延迟一段时间再检查，给服务器启动时间
                    delay(5000)
                    if (isAppiumServerRunning()) {
                        Log.i(TAG, "Appium UIA2 Server started after delay")
                        broadcastAppiumStatus(AppiumStatus.RUNNING)
                        return
                    } else {
                        Log.w(TAG, "Appium UIA2 Server still not running - manual startup required")
                        broadcastAppiumStatus(AppiumStatus.STOPPED)
                        return
                    }
                }
            } else {
                Log.e(TAG, "Appium Settings app not found")
                
                // 备用方法：尝试直接启动UIA2服务器（可能失败）
                try {
                    val startIntent = Intent()
                    startIntent.setClassName(
                        "io.appium.uiautomator2.server",
                        "io.appium.uiautomator2.server.Service"
                    )
                    startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(startIntent)
                    Log.i(TAG, "Attempted to start Appium UIA2 Server directly")
                    
                    delay(5000)
                    
                    if (isAppiumServerRunning()) {
                        Log.i(TAG, "Appium UIA2 Server is now running")
                        broadcastAppiumStatus(AppiumStatus.RUNNING)
                        return
                    } else {
                        Log.w(TAG, "Appium UIA2 Server may not be running properly")
                        broadcastAppiumStatus(AppiumStatus.STOPPED)
                        return
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start Appium server directly: ${e.message}")
                    broadcastAppiumStatus(AppiumStatus.STOPPED)
                    return
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Appium UIA2 Server", e)
            broadcastAppiumStatus(AppiumStatus.STOPPED)
        }
    }
    
    private suspend fun stopAppiumServer() {
        try {
            Log.i(TAG, "Stopping Appium UIA2 Server")
            broadcastAppiumStatus(AppiumStatus.STOPPING)
            
            // 尝试通过shell命令停止服务器（不使用su）
            Runtime.getRuntime().exec(arrayOf(
                "am", "force-stop", "io.appium.uiautomator2.server"
            ))
            
            delay(2000)
            
            if (!isAppiumServerRunning()) {
                Log.i(TAG, "Appium UIA2 Server stopped successfully")
                broadcastAppiumStatus(AppiumStatus.STOPPED)
            } else {
                Log.w(TAG, "Appium UIA2 Server may still be running")
                broadcastAppiumStatus(AppiumStatus.RUNNING)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Appium UIA2 Server", e)
            broadcastAppiumStatus(AppiumStatus.RUNNING)
        }
    }
    
    private fun checkNetworkConnectivity(): Boolean {
        try {
            // 检查本地回环接口是否可用
            val networkInterface = java.net.NetworkInterface.getByName("lo")
            return networkInterface != null && networkInterface.isUp
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check network connectivity", e)
            return false
        }
    }
    
    private fun getDeviceIdentifier(): String {
        return try {
            // 获取ANDROID_ID作为设备标识符
            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            androidId ?: "unknown_device"
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get device identifier, using default", e)
            "unknown_device"
        }
    }
    
    private suspend fun isAppiumServerRunning(): Boolean = withContext(Dispatchers.IO) {
        // 增加重试机制，最多尝试3次
        for (attempt in 1..3) {
            try {
                Log.d(TAG, "Appium server status check attempt $attempt/3")
                // 尝试连接到服务器端口来验证是否运行
                val url = URL("http://127.0.0.1:6790/wd/hub/status")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000  // 增加连接超时时间
                connection.readTimeout = 5000    // 增加读取超时时间
                connection.requestMethod = "GET"
                
                val responseCode = connection.responseCode
                connection.disconnect()
                
                Log.i(TAG, "Appium server status check: response code = $responseCode")
                if (responseCode == 200) {
                    return@withContext true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Appium server status check failed on attempt $attempt: ${e.message}")
                // 如果不是最后一次尝试，等待一段时间再重试
                if (attempt < 3) {
                    delay(1000)
                }
            }
        }
        return@withContext false
    }
    
    private fun broadcastAppiumStatus(status: AppiumStatus) {
        val intent = Intent(ACTION_APPIUM_STATUS_UPDATE).apply {
            putExtra(EXTRA_APPIUM_STATUS, status.name)
            setPackage(packageName) // 明确指定包名，避免隐式Intent问题
        }
        sendBroadcast(intent)
        Log.d(TAG, "Broadcasted Appium status: $status")
    }
    
    private fun broadcastServiceStatus(status: com.autodroid.controller.model.ServiceStatus) {
        val intent = Intent(ACTION_SERVICE_STATUS_UPDATE).apply {
            putExtra(EXTRA_SERVICE_STATUS, status.name)
            setPackage(packageName) // 明确指定包名，避免隐式Intent问题
        }
        sendBroadcast(intent)
        Log.d(TAG, "Broadcasted Service status: $status")
    }
    
    private fun startService() {
        Log.i(TAG, "Service started successfully")
    }
    
    private fun schedulePeriodicTaskCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val taskCheckWork = PeriodicWorkRequestBuilder<TaskCheckWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints)
         .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "task_check_work",
            ExistingPeriodicWorkPolicy.KEEP,
            taskCheckWork
        )
    }
    
    private suspend fun startTaskLoop() {
        // 暂时禁用任务获取功能，避免不必要的网络请求和错误日志
        Log.i(TAG, "Task loop started but task fetching is disabled")
        
        // 只广播服务运行状态，不进行任何定期检查
        broadcastServiceStatus(com.autodroid.controller.model.ServiceStatus.RUNNING)
        
        // 保持服务运行但不执行任何定期任务
        while (scope.isActive) {
            delay(60000) // 每分钟检查一次服务是否仍在运行
        }
    }
    
    private suspend fun fetchTaskFromServer(): String? = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceIdentifier()
            val url = URL("https://your-company-server.com/api/task?deviceId=$deviceId")
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Device-ID", deviceId)
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            
            if (connection.responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch task", e)
            null
        }
    }
    
    private suspend fun reportResult(taskId: String, success: Boolean, log: String) {
        withContext(Dispatchers.IO) {
            try {
                val deviceId = getDeviceIdentifier()
                val url = URL("https://your-company-server.com/api/task/result")
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Device-ID", deviceId)
                
                val result = JSONObject().apply {
                    put("taskId", taskId)
                    put("deviceId", deviceId)
                    put("success", success)
                    put("log", log)
                    put("timestamp", System.currentTimeMillis())
                }
                
                connection.outputStream.bufferedWriter().use { writer ->
                    writer.write(result.toString())
                }
                
                if (connection.responseCode == 200) {
                    Log.d(TAG, "Result reported successfully for task: $taskId")
                } else {
                    Log.e(TAG, "Failed to report result: ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report result", e)
            }
        }
    }
    
    private suspend fun executeTask(taskJson: String) {
        val logBuilder = StringBuilder()
        var success = true
        
        try {
            logBuilder.append("开始执行任务...\n")
            Log.i(TAG, "开始执行任务...")
            
            // 在执行任务前确保Appium UIA2 Server正在运行
            if (!isAppiumServerRunning()) {
                val errorMsg = "Appium UIA2 Server未运行，无法执行任务。请手动启动Appium服务器。"
                logBuilder.append("$errorMsg\n")
                Log.w(TAG, errorMsg)
                throw RuntimeException(errorMsg)
            } else {
                logBuilder.append("Appium UIA2 Server已在运行中\n")
                Log.i(TAG, "Appium UIA2 Server已在运行中")
            }
            
            val jsonObject = JSONObject(taskJson)
            val task = parseTask(jsonObject)
            
            logBuilder.append("开始执行任务: ${task.taskId}\n")
            Log.i(TAG, "开始执行任务: ${task.taskId}")
            
            task.actions.forEachIndexed { index, action ->
                try {
                    logBuilder.append("执行动作 ${index + 1}/${task.actions.size}: ${action.action}\n")
                    Log.d(TAG, "执行动作: ${action.action}")
                    
                    val result = webDriverClient.executeAction(action)
                    logBuilder.append("动作 ${index + 1} 执行完成: $result\n")
                    Log.d(TAG, "动作 ${index + 1} 执行完成: $result")
                    
                } catch (e: Exception) {
                    logBuilder.append("动作 ${index + 1} 执行失败: ${e.message}\n")
                    Log.e(TAG, "动作执行失败: ${action.action}", e)
                    success = false
                    throw e // 停止执行后续动作
                }
            }
            
            logBuilder.append("任务执行完成\n")
            Log.i(TAG, "任务执行完成: ${task.taskId}")
            
        } catch (e: Exception) {
            logBuilder.append("任务执行失败: ${e.message}\n")
            Log.e(TAG, "任务执行失败", e)
            success = false
        } finally {
            try {
                webDriverClient.closeSession()
                logBuilder.append("WebDriver会话已关闭\n")
                Log.d(TAG, "WebDriver会话已关闭")
            } catch (e: Exception) {
                logBuilder.append("关闭WebDriver会话时发生错误: ${e.message}\n")
                Log.w(TAG, "关闭WebDriver会话时发生错误", e)
            }
            
            reportResult(
                JSONObject(taskJson).optString("taskId", "unknown"),
                success,
                logBuilder.toString()
            )
        }
    }
    
    private fun parseTask(jsonObject: JSONObject): AutomationTask {
        val actionsArray = jsonObject.getJSONArray("actions")
        val actions = mutableListOf<Action>()
        
        for (i in 0 until actionsArray.length()) {
            val actionObj = actionsArray.getJSONObject(i)
            val paramsObj = actionObj.optJSONObject("params")
            val params = mutableMapOf<String, Any>()
            
            if (paramsObj != null) {
                val keys = paramsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = paramsObj.get(key)
                    params[key] = when (value) {
                        is JSONObject -> jsonObjectToMap(value)
                        is JSONArray -> jsonArrayToList(value)
                        else -> value
                    }
                }
            }
            
            val actionString = actionObj.getString("action")
            val actionType = when (actionString) {
                "initSession" -> ActionType.INIT_SESSION
                "findElement" -> ActionType.FIND_ELEMENT
                "click" -> ActionType.CLICK
                "sendKeys" -> ActionType.SEND_KEYS
                "swipe" -> ActionType.SWIPE
                "takeScreenshot" -> ActionType.TAKE_SCREENSHOT
                "closeSession" -> ActionType.CLOSE_SESSION
                "getPageSource" -> ActionType.GET_PAGE_SOURCE
                else -> throw IllegalArgumentException("Unknown action type: $actionString")
            }
            
            actions.add(Action(
                action = actionType,
                params = params
            ))
        }
        
        return AutomationTask(
            taskId = jsonObject.getString("taskId"),
            deviceId = jsonObject.getString("deviceId"),
            actions = actions
        )
    }
    
    private fun jsonObjectToMap(jsonObject: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)
            map[key] = when (value) {
                is JSONObject -> jsonObjectToMap(value)
                is JSONArray -> jsonArrayToList(value)
                else -> value
            }
        }
        return map
    }
    
    private fun jsonArrayToList(jsonArray: JSONArray): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until jsonArray.length()) {
            val value = jsonArray.get(i)
            list.add(when (value) {
                is JSONObject -> jsonObjectToMap(value)
                is JSONArray -> jsonArrayToList(value)
                else -> value
            })
        }
        return list
    }
    
    
}

class TaskCheckWorker(context: android.content.Context, params: WorkerParameters) 
    : Worker(context, params) {
    
    override fun doWork(): Result {
        Log.d("TaskCheckWorker", "Checking for new tasks")
        
        val intent = Intent(applicationContext, AutoDroidControllerService::class.java).apply {
            action = AutoDroidControllerService.ACTION_START
        }
        
        applicationContext.startService(intent)
        
        return Result.success()
    }
}