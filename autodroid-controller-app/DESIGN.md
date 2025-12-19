#

# Q: 只有一次设置这个无所谓，因为公司有技术人员 ==路径一：依赖ADB授权（需要电脑，您想避免的）
# A: 您抓住了关键点。如果技术人员能完成**一次性初始化**，那么**路径一（ADB授权）是最稳定、最彻底的专业方案**。这不仅解决了权限问题，更打开了构建稳定测试系统的大门。

### ✅ 基于ADB授权的专业方案详解

**核心优势**：授权是一次性的、稳定的。授予的权限不会被系统自动回收，无需用户后续干预，这是与“无障碍服务”路径最本质的区别。

#### **如何实施（技术人员操作核心）**

理想情况下，技术人员只需对每台手机进行一次连接操作。具体流程和所需权限如下表所示：

| 步骤 | 操作命令示例（在连接手机的电脑上执行） | 授予的权限与作用 |
| :--- | :--- | :--- |
| **1. 基础调试权** | `adb devices` (授权) | 允许电脑通过ADB与手机通信。**这是所有操作的基础**。 |
| **2. 无线调试权** | `adb tcpip 5555` | 开启手机的无线调试端口，后续可脱离USB线，通过网络连接控制。 |
| **3. 关键系统权限** | `adb shell pm grant com.your.controller android.permission.WRITE_SECURE_SETTINGS` | 允许您的APP修改系统安全设置，是自动化稳定的关键。 |
| **4. 安装与管理APK** | `adb shell appops set com.your.controller REQUEST_INSTALL_PACKAGES allow` | 允许您的APP静默安装/更新其他APK（如UIA2 Server）。 |
| **5. 悬浮窗权限** | `adb shell appops set com.your.controller SYSTEM_ALERT_WINDOW allow` | 允许显示调试信息悬浮窗（非必需，但调试时有用）。 |

执行完毕后，您的控制器APP就拥有了极高的自动化权限，可以脱离电脑，长期稳定运行。

#### **将“一次性设置”产品化**

为了让技术人员高效地完成成百上千台设备的初始化，强烈建议将其脚本化、产品化：

1.  **开发一个“初始化工具”**：
    *   一个简单的桌面程序（如用Python的Tkinter或Go编写）。
    *   技术人员运行后，界面会引导：`1. 用USB连接手机 -> 2. 点击“一键授权” -> 3. 拔掉USB线`。
    *   工具后台自动执行上述所有`adb`命令，并验证结果。

2.  **网络架构回归经典模式**：
    *   初始化后，手机和控制器APP完全独立。推荐回归到之前讨论过的最稳定架构：**在家庭网络中放置一个中心节点**（如员工的旧电脑、树莓派或路由器旁的路由器）。
    *   该中心节点运行一个轻量级服务，负责：
        *   通过Wi-Fi与手机保持`adb connect`连接。
        *   从公司服务器拉取测试任务和脚本。
        *   将任务分发给手机（通过ADB启动控制器APP或直接调用UIA2 Server）。

**这个“中心节点”的角色，正是最初方案中“家庭电脑”的轻量化版本。** 因为它只运行一个简单的代理服务，部署复杂度远低于完整的STF。

### ⚖️ 最终决策对比：ADB授权 vs. 无障碍服务

为了帮助您做出最适合的决策，以下是两种路径的最终对比：

| 特性 | **路径一：ADB授权 + 中心节点** | **路径二：纯手机APP（无障碍服务）** |
| :--- | :--- | :--- |
| **首次部署** | 需要技术人员通过USB连接电脑，执行一次性脚本。**过程可控、可批量**。 | 用户自行在手机设置中开启，依赖图文指引，受手机品牌影响大。 |
| **长期稳定性** | **极高**。系统级授权，一劳永逸，除非刷机。 | **中**。可能被系统或安全软件关闭，需要用户重新开启。 |
| **测试能力** | **完整**。可静默安装APK、修改系统设置，实现深度自动化。 | **受限制**。能力取决于无障碍服务API，某些操作（如静默安装）无法实现。 |
| **维护成本** | 低。初始化后基本无需维护。 | 高。需持续处理用户反馈，应对不同手机品牌的兼容性问题。 |
| **网络架构** | 需要家庭网络中存在一个**中心代理节点**（如树莓派）。 | 完全不需要额外硬件，手机直连公司服务器。 |
| **适合场景** | **追求长期稳定、大规模、深度自动化**的企业测试集群。 | **测试规模较小、用户配合度高、对部署硬件零要求**的众测或试用场景。 |

### 🎯 给您的明确建议

**如果您拥有技术人员资源，且目标是构建一个长期稳定、可大规模扩展的企业级测试网络，请毫不犹豫地选择路径一（ADB授权）。**

**接下来的行动路线：**
1.  **开发控制器APP**：确保其具备通过HTTP接收并执行任务的能力。
2.  **制作初始化工具**：编写一个脚本或桌面工具，自动执行上表的ADB命令。
3.  **设计中心节点服务**：开发一个轻量级代理程序，可部署在树莓派上，负责任务中转和设备连接维持。

这个方案在初期投入一些技术人力完成标准化初始化后，长期运维成本极低，系统稳定性最高，最能满足企业级测试的需求。


## Q: 我在手机上已经安装了Appium Settings、io.appium.uiautomator2.server, io.appium.uiautomator2.server.test 三个应用

## A: 您已经安装了三个核心APK，这说明您对Appium的底层架构理解非常到位。让我们先理清这三个应用的角色，然后规划如何利用它们构建一个完全基于手机的自动化测试节点。

### 🔍 三个APK的作用

*   **`io.appium.uiautomator2.server`**：这是**核心服务**，相当于自动化指令的“执行引擎”。它会监听一个本地端口（默认为`8200`），接收WebDriver协议命令（如点击、滑动），并通过UIAutomator2框架操作系统界面。
*   **`io.appium.uiautomator2.server.test`**：这是Server的**配套测试APK**。其主要作用是在Appium测试初始化时，帮助**注入并启动Server进程**。它本身不直接执行您的业务测试用例。
*   **`Appium Settings`**：这是一个**系统辅助工具**，用于快速修改手机的开发设置（如开启/关闭Wi-Fi、定位服务、修改系统语言等），以使测试环境保持一致。

现在，您手机上已经具备了执行自动化测试的“**肌肉**”（Server）和“**神经**”（Settings），但还缺少一个能接收公司指令并指挥肌肉运动的“**大脑**”。

### 🧠 方案选择：为系统添加“大脑”

您需要开发或部署一个“**大脑**”——即一个能接收任务并控制本地Server的应用程序。结合您公司有技术人员的优势，主要有以下两种实现路径：

| 方案 | 核心思路 | 所需关键权限 | 技术人员一次性操作 | 优点 | 注意事项 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **方案A：自研轻量Controller APP** | 开发一个Android应用，通过**HttpProxyProvider Content Provider**从公司服务器拉取测试任务（JSON格式），并转发给本地的UIA2 Server执行。通过**悬浮窗实时监控**测试进度。 | **ADB授权的高权限** + **SYSTEM_ALERT_WINDOW** | 1. 安装自研Controller APP。<br>2. 通过USB连接，使用 `adb shell pm grant` 命令授予APP系统权限。<br>3. 通过 `adb shell appops set` 授予悬浮窗权限。 | 1. **部署极简**：员工家庭只需保持手机在线。<br>2. **纯手机端**：无需其他硬件。<br>3. **架构清晰**：Controller作为中枢，易于维护。<br>4. **无需无障碍服务**：避免用户手动开启和权限回收问题。<br>5. **实时监控**：悬浮窗显示测试进度和状态。<br>6. **沙盒绕过**：通过Content Provider通信，绕过Android沙盒HTTP限制。 | 1. 需技术人员对每台手机进行一次USB初始连接，但一劳永逸。<br>2. **不再需要无障碍服务配置**：简化了AndroidManifest.xml配置。 |
| **方案B：使用现有开源Agent (STF Minicap)** | 不自己写APP，而是在手机上运行一个轻量的守护进程（如STF的 `minicap`、`minitouch` 服务），并通过公司服务器直接发送ADB命令来控制手机。 | **USB调试 (ADB)** 权限 | 1. 通过USB对手机进行一次授权。<br>2. 在手机或家庭网关（如树莓派）上部署一个连接守护进程。 | 1. **无需开发**：直接利用成熟组件。<br>2. **功能强大**：可获取实时屏幕流、直接注入触摸事件，不依赖UIA2 Server。 | 1. **仍需一个家庭内网节点**（可以是旧手机或路由器）来运行守护进程并保持与公司服务器的连接。<br>2. 直接ADB命令不如WebDriver协议易读易维护。 |

### 🎯 新架构优势：悬浮窗监控 + HttpProxyProvider

#### 架构演进：从无障碍服务到悬浮窗监控

**传统方案的问题：**
- 无障碍服务需要用户手动开启，体验不佳
- 权限可能被系统或安全软件自动回收
- 不同手机品牌的无障碍服务配置差异大
- AndroidManifest.xml需要配置复杂的无障碍服务声明

**新架构的优势：**
- **悬浮窗实时监控**：通过SYSTEM_ALERT_WINDOW权限显示测试进度、状态和日志
- **HttpProxyProvider通信**：使用Content Provider绕过Android沙盒HTTP限制
- **简化配置**：不再需要无障碍服务配置，简化AndroidManifest.xml
- **稳定授权**：通过ADB一次性授权，权限长期稳定
- **用户体验**：无需用户手动开启任何服务

#### 技术实现要点

1. **悬浮窗监控实现**：
   ```kotlin
   // 创建悬浮窗显示测试进度
   val overlayView = OverlayView(context)
   val params = WindowManager.LayoutParams(
       WindowManager.LayoutParams.WRAP_CONTENT,
       WindowManager.LayoutParams.WRAP_CONTENT,
       WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
       WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
       PixelFormat.TRANSLUCENT
   )
   windowManager.addView(overlayView, params)
   ```

2. **HttpProxyProvider通信**：
   ```kotlin
   // 使用Content Provider与UIA2 Server通信
   val uri = Uri.parse("content://io.appium.uiautomator2.server.provider/http/session")
   val contentValues = ContentValues().apply {
       put("X", "POST")  // 方法
       put("d", capabilitiesJson)  // 数据
       put("H", "application/json")  // Content-Type
   }
   val resultUri = contentResolver.insert(uri, contentValues)
   ```

3. **权限简化**：
   - 不再需要`BIND_ACCESSIBILITY_SERVICE`权限
   - 不再需要无障碍服务配置声明
   - 仅需`SYSTEM_ALERT_WINDOW`悬浮窗权限

### 🚀 下一步行动建议

考虑到您已经安装了Appium UIA2 Server，并且希望利用现有技术栈，**方案A（自研Controller APP）是更直接、更符合您现有架构的选择**。以下是具体步骤：

1.  **第一步：验证基础环境**
    在已安装三个APK的手机上，通过电脑执行一次测试，确保底层服务正常。
    ```bash
    # 在电脑上操作
    adb devices # 确保手机已连接
    # 启动UIA2 Server服务（通常安装后会自动运行，也可手动启动）
    adb shell am start -n io.appium.uiautomator2.server/.Service
    # 使用curl或Postman向手机本地端口发送一个简单的WebDriver命令试试
    ```

2.  **第二步：开发Controller APP (核心)**
    *   **功能1 - 通信**：实现与公司服务器API的定时同步，拉取测试任务（任务可定义为JSON，如 `{"action": "launchApp", "packageName": "com.example.app"}`）。
    *   **功能2 - 执行**：将任务转换为标准的WebDriver API请求，使用**HttpProxyProvider Content Provider**发送给UIA2 Server，绕过Android沙盒HTTP限制。
    *   **功能3 - 悬浮窗监控**：通过SYSTEM_ALERT_WINDOW权限显示实时测试进度、状态和日志信息。
    *   **功能4 - 上报**：将测试执行结果（成功/失败、日志）回传给服务器。

3.  **第三步：一键式初始化（技术人员执行）**
    制作一个脚本，技术人员用USB连接手机后，运行脚本自动完成：
    *   安装您开发的Controller APP。
    *   通过 `adb shell pm grant` 授予其必要的权限（如 `WRITE_SECURE_SETTINGS`）。
    *   可选：配置Wi-Fi ADB，以便后续可无线维护。
    *   启动Controller APP。

完成以上步骤后，手机就成为一个独立的、可接收远程指令的自动化测试节点。员工只需要确保手机充电和联网，公司的调度系统就可以随时向其下发对任意第三方APK的测试任务。

如果您选择方案A并开始开发Controller APP，在将JSON任务转换为WebDriver请求，或处理UIA2 Server的会话管理等具体实现上需要更详细的代码示例，我可以继续提供帮助。

## Q: 选择方案A并开始开发Controller APP，在将JSON任务转换为WebDriver请求代码示例
## A: 我将为您提供一个完整的、分层的代码示例，展示如何将JSON任务转换为WebDriver请求。这个示例基于您选择的方案A，采用模块化设计，便于理解和扩展。

### 📋 1. 定义JSON任务格式

首先，定义清晰的任务数据结构。这是整个系统的通信协议基础。

```json
// 示例任务 (task.json)
{
  "taskId": "test_20230520_001",
  "deviceId": "emulator-5554",
  "actions": [
    {
      "action": "initSession",
      "params": {
        "capabilities": {
          "platformName": "Android",
          "appPackage": "com.example.targetapp",
          "appActivity": ".MainActivity",
          "automationName": "UiAutomator2",
          "noReset": true
        }
      }
    },
    {
      "action": "findElement",
      "params": {
        "strategy": "accessibility id",
        "selector": "LoginButton"
      }
    },
    {
      "action": "click",
      "params": {
        "elementId": "${lastElementId}" // 引用上一步找到的元素
      }
    },
    {
      "action": "takeScreenshot",
      "params": {
        "savePath": "/sdcard/screenshot_login.png"
      }
    }
  ]
}
```

```kotlin
// 对应的Kotlin数据类 (TaskModel.kt)
data class AutomationTask(
    val taskId: String,
    val deviceId: String,
    val actions: List<Action>
)

data class Action(
    val action: String, // "initSession", "findElement", "click", "swipe", etc.
    val params: Map<String, Any> = emptyMap()
)

// WebDriver会话状态管理
object SessionManager {
    var currentSessionId: String? = null
    var currentElementId: String? = null
    val context = mutableMapOf<String, Any>() // 存储变量，如${lastElementId}
}
```

### 🔧 2. 核心WebDriver客户端（使用Content Provider + 悬浮窗监控）

这是与UIA2 Server通信的核心模块，使用HttpProxyProvider Content Provider而不是HTTP请求，避免Android沙盒限制。通过悬浮窗实时监控自动化测试进度，无需依赖无障碍服务。

#### 架构优势：
- **悬浮窗监控**：通过SYSTEM_ALERT_WINDOW权限显示实时测试进度和状态
- **Content Provider通信**：使用HttpProxyProvider绕过Android沙盒限制，直接与UIA2 Server通信
- **无需无障碍服务**：避免了用户手动开启和权限回收问题
- **ADB授权稳定性**：一次性ADB授权确保系统级权限的长期稳定

```kotlin
// ContentProviderClient.kt
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import org.json.JSONObject

class ContentProviderClient(
    private val context: android.content.Context
) {
    private val AUTHORITY = "io.appium.uiautomator2.server.provider"
    private val BASE_CONTENT_URI = "content://$AUTHORITY/http"
    
    // 发送WebDriver命令的核心方法（使用Content Provider）
    fun executeCommand(endpoint: String, method: String = "POST", data: JSONObject? = null): String {
        val uri = Uri.parse("$BASE_CONTENT_URI$endpoint")
        val contentValues = ContentValues()
        
        when (method.uppercase()) {
            "POST", "PUT" -> {
                contentValues.put("X", method) // 使用简化的参数名
                if (data != null) {
                    contentValues.put("d", data.toString()) // 使用简化的参数名
                }
                contentValues.put("H", "application/json") // Content-Type
                
                val resultUri = context.contentResolver.insert(uri, contentValues)
                return "Command executed via Content Provider"
            }
            "GET" -> {
                val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
                return try {
                    cursor?.use { 
                        if (it.moveToFirst()) {
                            val response = it.getString(it.getColumnIndex("response"))
                            response ?: "{}"
                        } else {
                            "{}"
                        }
                    } ?: "{}"
                } catch (e: Exception) {
                    Log.e("WebDriverClient", "Query failed", e)
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
    
    // 初始化会话
    fun initSession(capabilities: Map<String, Any>): String {
        val data = JSONObject().apply {
            put("capabilities", JSONObject(capabilities))
            put("desiredCapabilities", JSONObject(capabilities))
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
    
    // 查找元素
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
    
    // 点击元素
    fun clickElement(elementId: String) {
        val sessionId = SessionManager.currentSessionId 
            ?: throw IllegalStateException("No active session")
        
        val response = executeCommand("/session/$sessionId/element/$elementId/click", "POST")
        
        if (!response.isSuccessful) {
            throw RuntimeException("Click failed: ${response.body?.string()}")
        }
    }
    
    // 通用的动作执行入口
    fun executeAction(action: Action): Any {
        return when (action.action) {
            "initSession" -> {
                val caps = action.params["capabilities"] as? Map<String, Any>
                    ?: throw IllegalArgumentException("Missing capabilities")
                initSession(caps)
            }
            "findElement" -> {
                val strategy = action.params["strategy"] as? String
                    ?: throw IllegalArgumentException("Missing strategy")
                val selector = action.params["selector"] as? String
                    ?: throw IllegalArgumentException("Missing selector")
                findElement(strategy, selector)
            }
            "click" -> {
                var elementId = action.params["elementId"] as? String
                // 支持变量引用，如${lastElementId}
                if (elementId?.startsWith("\${") == true) {
                    val key = elementId.removePrefix("\${").removeSuffix("}")
                    elementId = SessionManager.context[key] as? String
                }
                elementId ?: SessionManager.currentElementId 
                    ?: throw IllegalArgumentException("No element specified")
                clickElement(elementId)
                "click_success"
            }
            "takeScreenshot" -> {
                takeScreenshot(action.params["savePath"] as? String)
            }
            else -> throw UnsupportedOperationException("Unknown action: ${action.action}")
        }
    }
    
    private fun takeScreenshot(savePath: String?): String {
        val sessionId = SessionManager.currentSessionId 
            ?: throw IllegalStateException("No active session")
        
        val response = executeCommand("/session/$sessionId/screenshot", "GET")
        val responseBody = response.body?.string() ?: ""
        val jsonResponse = JSONObject(responseBody)
        
        if (!response.isSuccessful) {
            throw RuntimeException("Screenshot failed: $responseBody")
        }
        
        val base64Image = jsonResponse.getString("value")
        // 这里可以添加保存图片的逻辑
        return "screenshot_taken"
    }
}
```

### 📱 3. 主控制器Service

这是Android Service，负责后台运行、获取任务并执行。

```kotlin
// AutomationService.kt
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class AutomationService : Service() {
    private val TAG = "AutomationService"
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var webDriverClient: WebDriverClient
    
    // 从服务器获取任务的模拟方法（需根据您的API调整）
    private suspend fun fetchTaskFromServer(): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://your-company-server.com/api/task") // 替换为您的服务器地址
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Device-ID", getDeviceId())
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
    
    // 上报结果给服务器
    private suspend fun reportResult(taskId: String, success: Boolean, log: String) {
        // 实现上报逻辑
        Log.d(TAG, "Task $taskId completed: $success")
    }
    
    // 任务执行器
    private fun executeTask(taskJson: String) {
        try {
            // 解析JSON任务
            val jsonObject = JSONObject(taskJson)
            val task = parseTask(jsonObject) // 需要实现parseTask函数
            
            Log.i(TAG, "Starting task: ${task.taskId}")
            
            // 执行每个动作
            task.actions.forEach { action ->
                try {
                    Log.d(TAG, "Executing action: ${action.action}")
                    val result = webDriverClient.executeAction(action)
                    Log.d(TAG, "Action result: $result")
                    
                    // 每个动作后可以短暂暂停
                    Thread.sleep(500)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Action failed: ${action.action}", e)
                    throw e // 或者根据需求继续执行下一个动作
                }
            }
            
            // 任务成功完成
            scope.launch {
                reportResult(task.taskId, true, "Task completed successfully")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Task execution failed", e)
            scope.launch {
                reportResult(JSONObject(taskJson).getString("taskId"), false, e.message ?: "Unknown error")
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service creating")
        webDriverClient = WebDriverClient()
        
        // 启动定时任务检查
        scope.launch {
            while (isActive) {
                try {
                    val taskJson = fetchTaskFromServer()
                    if (taskJson != null) {
                        executeTask(taskJson)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Task loop error", e)
                }
                
                // 每30秒检查一次新任务
                delay(30000)
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY // 系统会重启被杀死服务
    }
    
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        Log.d(TAG, "Service destroyed")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun getDeviceId(): String {
        // 获取设备唯一标识
        return android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }
}
```

### 📝 4. AndroidManifest.xml配置要点

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/xml">
    
    <!-- 存储权限（用于保存截图） -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
                     android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    
    <!-- Android 10+ 的文件访问 -->
    <application
        android:requestLegacyExternalStorage="true"
        ...>
        
        <!-- 后台服务 -->
        <service
            android:name=".AutomationService"
            android:enabled="true"
            android:exported="false" />
        
        <!-- 启动Activity（用于引导用户） -->
        <activity
            android:name=".MainActivity"
            android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
    </application>
</manifest>
```

### 🚀 5. 快速启动指南

1.  **创建Android项目**：使用Android Studio新建项目，添加上述代码文件。
2.  **添加依赖**：在`build.gradle`中添加：
    ```gradle
    dependencies {
        implementation 'org.json:json:20231013'
        implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    }
    ```
    **注意**：不再需要OkHttp依赖，因为使用Content Provider通信而不是HTTP请求。
3.  **初始化设备**：
    ```bash
    # 技术人员用USB连接手机后执行
    adb shell pm grant com.your.package android.permission.WRITE_SECURE_SETTINGS
    adb shell appops set com.your.package android:write_settings allow
    # 安装APK
    adb install app-debug.apk
    ```
4.  **启动服务**：在您的Controller APP中，添加启动`AutomationService`的代码。

### ⚠️ 关键注意事项

1.  **Content Provider通信**：确保UIA2 Server的HttpProxyProvider已正确安装并运行，Controller APP能通过Content Provider与Server通信。
2.  **会话管理**：示例中的`SessionManager`是简化版，生产环境需要更健壮的会话恢复机制。
3.  **错误处理**：考虑UIA2 Server崩溃等情况，添加重试和状态检查。
4.  **任务队列**：当前示例是拉取模式，您可以根据服务器API改为推送模式（如WebSocket）。
5.  **资源清理**：长时间运行后，可能需要重启UIA2 Server进程。
6.  **安全考虑**：与公司服务器的通信应使用HTTPS，并考虑设备身份验证。
7.  **悬浮窗权限**：确保通过ADB正确授予SYSTEM_ALERT_WINDOW权限，悬浮窗才能正常显示。

这个框架为您提供了一个坚实的起点。在实际开发中，您可能还需要添加更多动作类型（如`inputText`、`swipe`、`back`等）和更完善的错误处理。如果您在实现特定功能时遇到问题，我可以提供更具体的指导。

