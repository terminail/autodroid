# Guardian SDK 使用指南

## 概述

Guardian SDK 提供完整的隐秘报警功能和短信守卫功能，应用只需要注册 SDK 提供的组件即可实现零代码集成。SDK 会自动管理所有后台服务和隐秘功能。

**核心特性**：
- **零代码集成**：应用只需注册 SDK 组件，无需编写任何报警相关代码
- **短信守卫**：自动监控和响应短信指令，提供隐秘的远程控制能力
- **智能自动化**：所有报警功能自动管理和触发
- **多应用协调**：自动处理多个应用同时使用 SDK 的冲突
- **高度隐蔽**：后台静默运行，无任何视觉提示

## 快速开始

### 1. 添加依赖

在应用的 `build.gradle.kts` 中添加 SDK 依赖：

```kotlin
dependencies {
    implementation(project(":guardian-sdk"))
    // 其他依赖...
}
```

### 2. 注册 SDK 组件

在应用的 `AndroidManifest.xml` 中注册 SDK 提供的组件。SDK 已经包含了所有必要的服务、Activity 和接收器，应用只需声明即可：

```xml
<!-- SDK提供的主界面 -->
<activity
    android:name="com.autodroid.guardiansdk.ui.GuardianActivity"
    android:exported="true"
    android:theme="@style/AppTheme" />

<!-- SDK提供的紧急服务 -->
<service
    android:name="com.autodroid.guardiansdk.service.EmergencyService"
    android:exported="false" />

<!-- SDK提供的浮动窗口服务 -->
<service
    android:name="com.autodroid.guardiansdk.service.FloatingWindowService"
    android:exported="false" />

<!-- SDK提供的无障碍服务（短信守卫核心） -->
<service
    android:name="com.autodroid.guardiansdk.service.GuardianAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/guardian_accessibility_service_config" />
</service>

<!-- SDK提供的短信接收器 -->
<receiver
    android:name="com.autodroid.guardiansdk.receiver.SmsReceiver"
    android:exported="true">
    <intent-filter android:priority="1000">
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
    </intent-filter>
</receiver>

<!-- SDK提供的音频录音服务 -->
<service
    android:name="com.autodroid.guardiansdk.service.AudioRecordingService"
    android:exported="false"
    android:enabled="true" />
```

### 3. 声明权限

SDK 已经在 `guardian-sdk/src/main/AndroidManifest.xml` 中声明了所有必要的权限，应用无需重复声明。但如果应用需要单独使用某些功能，可以参考以下权限列表：

```xml
<!-- 短信权限 -->
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />

<!-- 位置权限 -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- 浮动窗口权限 -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- 后台服务权限 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- 其他权限 -->
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 4. 初始化 SDK

在应用的 Application 类中初始化 SDK：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 SDK - SDK会自动初始化数据库和默认设置
        GuardianSdk.initialize(this)
    }
}
```

### 5. 完成！

就这么简单！应用现在已经集成了完整的报警功能和短信守卫功能。

## 零代码集成

应用无需编写任何报警相关代码，SDK 会自动：
- 初始化数据库和默认设置项
- 监听短信开门密语自动激活主界面
- 管理所有报警逻辑和界面切换
- 处理权限申请和服务协调
- 响应短信指令（开门、查询、报警、录音、状态等）

应用只需专注于实现真实功能（如记事本、计算器等），报警功能和短信守卫功能由 SDK 完全接管。

## 短信守卫功能

### 功能概述

短信守卫是 Guardian SDK 的核心功能之一，为所有集成 SDK 的应用提供隐秘的短信远程控制能力。

**核心特性**：
- **自动集成**：任何应用集成 SDK 后自动获得短信守卫功能
- **隐秘运行**：后台静默监听短信，无任何界面提示
- **指令响应**：支持多种短信指令，实现远程控制
- **安全验证**：通过密码本和密语验证确保安全性
- **多应用兼容**：多个应用同时使用 SDK 时自动协调

### 短信指令系统

SDK 支持以下短信指令：

#### 1. 开门指令
- **功能**：打开隐秘主界面
- **格式**：`[开门密语]`
- **示例**：`开门`、`open`、`help`
- **触发条件**：只有自己的手机发给自己的短信才会触发
- **配置**：可在"我的设置"中自定义开门密语

#### 2. 查询指令
- **功能**：查询被监护人位置信息
- **格式**：`[查询密语]`
- **示例**：`在哪里`、`位置`、`location`
- **响应**：自动回复加密的位置信息

#### 3. 报警指令
- **功能**：远程触发报警
- **格式**：`[报警密语]`
- **示例**：`报警`、`sos`、`help`
- **响应**：立即发送报警短信给所有监护人

#### 4. 录音指令
- **功能**：远程启动录音
- **格式**：`[录音密语]`
- **示例**：`录音`、`record`
- **响应**：开始录音并通过邮件发送

#### 5. 状态指令
- **功能**：查询设备状态
- **格式**：`[状态密语]`
- **示例**：`状态`、`status`
- **响应**：回复电池、网络、位置等状态信息

### 短信守卫工作流程

```
监护人手机 → 发送短信指令 → 短信系统 → GuardianAccessibilityService
                                              ↓
                                          指令验证器
                                              ↓
                                          验证发送者
                                              ↓
                                          验证密语
                                              ↓
                                          验证密码本
                                              ↓
                                          指令处理器
                                              ↓
                                          执行指令
                                              ↓
                                          发送响应短信
                                              ↓
                                        监护人手机
```

### 安全机制

**多层验证**：
1. **发送者验证**：检查发送者手机号是否在联系人列表中
2. **密语匹配**：验证短信内容是否匹配配置的密语
3. **密码本验证**：使用密码本加密验证，防止伪造
4. **时间窗口**：限制指令的有效时间窗口

**隐私保护**：
- 指令短信不显示在收件箱（可选）
- 响应短信使用加密格式
- 敏感信息使用密码本加密存储
- 支持指令日志记录和审计

## SDK 界面说明

### GuardianActivity（主界面）
SDK 提供的主界面，采用底部三 Tab 导航设计（Material Design 风格）：

- **Tab 1：联系人**
  - 展示监护人、被监护人列表
  - 大卡片形式显示监护人、被监护人信息
  - 显示最后联系时间、最新位置、最新报警信息

- **Tab 2：WHY**
  - SDK 功能介绍
  - 使用说明
  - 安全特性说明
  - 分享信息给监护人
  - 导入被监护人信息

- **Tab 3：我的设置**
  - 管理个人报警设置
  - 配置监护人信息
  - 设置报警触发方式
  - 配置短信守卫功能

### 设置项列表
我的设置界面包含以下设置项：

1. **监护人1-5**：最多可配置5个监护人信息
2. **报警触发模式-音量键**：通过长按音量键触发报警
3. **报警触发模式-浮动窗口**：通过长按浮动窗口按钮触发报警
4. **报警触发模式-摇动手机**：通过摇动手机触发报警
5. **报警触发模式-长时间未使用**：长时间未使用手机自动报警
6. **报警信息密码**：自定义位置信息加密密码本
7. **录音模式**：报警时自动启动录音，通过邮件发送
8. **邮件配置**：配置发送录音的邮箱信息
9. **Ping响应设置**：配置Ping响应功能
10. **隐秘界面设置**：配置短信开门密语和自动关闭界面
11. **测试模式**：练习模式下不发送真实短信，仅模拟报警

### 历史记录
在设置界面底部显示报警和查询历史记录：

- **报警记录**：显示触发时间、触发方式、报警信息和位置
- **查询记录**：显示监护人查询时间、查询内容和回复内容

## API 接口

### GuardianSdk

SDK 主入口类，提供以下公共方法：

```kotlin
/**
 * 初始化SDK
 * @param context 应用上下文
 */
fun initialize(context: Context): GuardianSdk

/**
 * 获取SDK实例
 * @return GuardianSdk 实例
 */
fun getInstance(): GuardianSdk

/**
 * 启动设置界面（GuardianActivity）
 */
fun startSettingActivity()

/**
 * 启动浮动窗口服务
 */
fun startFloatingWindowService()

/**
 * 停止浮动窗口服务
 */
fun stopFloatingWindowService()

/**
 * 启动报警服务
 */
fun startAlarmService()

/**
 * 停止报警服务
 */
fun stopAlarmService()

/**
 * 触发紧急报警
 * @param alarmType 报警类型
 */
fun triggerEmergencyAlarm(alarmType: AlarmType)

/**
 * 检查无障碍服务是否已启用
 * @return 是否启用
 */
fun isAccessibilityServiceEnabled(): Boolean

/**
 * 打开无障碍服务设置页面
 */
fun openAccessibilitySettings()

/**
 * 获取短信仓库
 */
fun getSmsRepository(): SmsRepository

/**
 * 发送短信
 * @param address 收件人手机号
 * @param body 短信内容
 * @return 是否发送成功
 */
suspend fun sendSms(address: String, body: String): Boolean

/**
 * 获取所有会话
 * @return Flow<List<Conversation>> 会话列表
 */
fun getAllConversations()

/**
 * 获取指定会话的消息
 * @param threadId 会话ID
 * @return Flow<List<SmsMessage>> 消息列表
 */
fun getMessagesByThread(threadId: Long)

/**
 * 根据地址获取消息
 * @param address 手机号
 * @return Flow<List<SmsMessage>> 消息列表
 */
fun getMessagesByAddress(address: String)
```

### AlarmType（报警类型）

```kotlin
enum class AlarmType {
    URGENT_RESCUE,    // 紧急求救
    FOLLOWED,         // 被跟踪
    NEED_HELP,        // 需要帮助
    MEDICAL_EMERGENCY // 医疗紧急
}
```

## 配置说明

### 主题配置

SDK 界面使用应用的主题，确保应用定义了以下主题：

```xml
<style name="AppTheme" parent="Theme.Material3.DayNight">
    <item name="colorPrimary">@color/primary</item>
    <item name="colorSurface">@color/surface</item>
    <item name="android:statusBarColor">@color/primary</item>
</style>
```

### 无障碍服务配置

SDK 已经提供了无障碍服务配置文件 `res/xml/guardian_accessibility_service_config.xml`，应用无需额外配置。

如果需要自定义配置，可以在应用的 `res/xml` 目录下创建同名文件覆盖 SDK 的配置：

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeNotificationStateChanged|typeAllMask"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagRequestTouchExplorationMode|flagReportViewIds"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100" />
```

## 最佳实践

### 1. 初始化时机

在 Application 的 `onCreate()` 中尽早初始化 SDK，确保所有功能可用：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GuardianSdk.initialize(this)
    }
}
```

### 2. 权限请求

SDK 提供了统一的权限检查和请求接口，应用只需调用一个方法即可。建议在应用启动时引导用户授予关键权限：

```kotlin
// 一键检查并请求所有必要权限
GuardianSdk.getInstance().checkAndRequestPermissions(this)
```

SDK 会智能地循环检查所有权限，引导用户逐个授权，直到所有权限都授予。

**工作流程**：
1. SDK 检查所有权限状态
2. 如果所有权限已授予，返回 true
3. 如果有权限未授予，显示对话框引导用户授权第一个缺失的权限
4. 用户授权后，应用再次调用 `checkAndRequestPermissions()` 检查下一个权限
5. 重复步骤 1-4，直到所有权限都授予

**应用示例**：

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化 SDK
        GuardianSdk.initialize(this)
        
        // 检查并请求所有权限
        if (GuardianSdk.getInstance().checkAndRequestPermissions(this)) {
            // 所有权限已授予，可以使用所有功能
            startApp()
        } else {
            // 有权限需要用户授权，等待用户授权后再次调用
            // 可以在 onResume() 中再次检查
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 用户从设置页面返回后，再次检查权限
        if (GuardianSdk.getInstance().checkAndRequestPermissions(this)) {
            startApp()
        }
    }
    
    private fun startApp() {
        // 启动应用主功能
    }
}
```

**优势**：
- ✅ 极简 API：应用只需调用一个方法
- ✅ 智能引导：自动按顺序引导用户授权
- ✅ 统一管理：所有权限的检查逻辑、提示文案都在 SDK 中统一管理
- ✅ 易于维护：修改提示文案或逻辑只需要改 SDK 代码
- ✅ 一致性：所有使用 SDK 的应用都有统一的体验

### 3. 错误处理

```kotlin
try {
    GuardianSdk.getInstance().startSettingActivity()
} catch (e: IllegalStateException) {
    // SDK 未初始化
    GuardianSdk.initialize(context)
    GuardianSdk.getInstance().startSettingActivity()
}
```

### 4. 服务生命周期管理

SDK 会自动管理服务生命周期，应用无需手动启动和停止服务。但如果需要，可以手动控制：

```kotlin
// 应用启动时启动服务（可选）
override fun onResume() {
    super.onResume()
    GuardianSdk.getInstance().startFloatingWindowService()
    GuardianSdk.getInstance().startAlarmService()
}

// 应用退出时停止服务（可选）
override fun onPause() {
    super.onPause()
    GuardianSdk.getInstance().stopFloatingWindowService()
    GuardianSdk.getInstance().stopAlarmService()
}
```

### 5. 无障碍服务检查

无障碍服务检查已经集成到 `checkAndRequestPermissions()` 中，应用无需单独检查。

如果需要单独检查无障碍服务状态（不显示对话框），可以使用：

```kotlin
// 检查无障碍服务是否已启用
if (GuardianSdk.getInstance().isAccessibilityServiceEnabled()) {
    // 服务已启用
} else {
    // 服务未启用
}
```

### 6. 权限检查和请求

SDK 提供了统一的权限检查和请求接口，应用只需调用一个方法即可。

#### 检查并请求所有权限（推荐）

```kotlin
// 一键检查并请求所有必要权限
GuardianSdk.getInstance().checkAndRequestPermissions(this)
```

SDK 会智能地循环检查所有权限，引导用户逐个授权，直到所有权限都授予。

**工作流程**：
1. SDK 检查所有权限状态
2. 如果所有权限已授予，返回 true
3. 如果有权限未授予，显示对话框引导用户授权第一个缺失的权限
4. 用户授权后，应用再次调用 `checkAndRequestPermissions()` 检查下一个权限
5. 重复步骤 1-4，直到所有权限都授予

**应用示例**：

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化 SDK
        GuardianSdk.initialize(this)
        
        // 检查并请求所有权限
        if (GuardianSdk.getInstance().checkAndRequestPermissions(this)) {
            // 所有权限已授予，可以使用所有功能
            startApp()
        } else {
            // 有权限需要用户授权，等待用户授权后再次调用
            // 可以在 onResume() 中再次检查
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 用户从设置页面返回后，再次检查权限
        if (GuardianSdk.getInstance().checkAndRequestPermissions(this)) {
            startApp()
        }
    }
    
    private fun startApp() {
        // 启动应用主功能
    }
}
```

**优势**：
- ✅ 极简 API：应用只需调用一个方法
- ✅ 智能引导：自动按顺序引导用户授权
- ✅ 统一管理：所有权限的检查逻辑、提示文案都在 SDK 中统一管理
- ✅ 易于维护：修改提示文案或逻辑只需要改 SDK 代码
- ✅ 一致性：所有使用 SDK 的应用都有统一的体验

## 应用场景示例

### 记事本守卫
- 用户使用记事本应用记录日常信息
- 集成 Guardian SDK 后，记事本变成"记事本守卫"
- 用户可以通过短信远程控制记事本应用

### 计算器守卫
- 用户使用计算器应用进行日常计算
- 集成 Guardian SDK 后，计算器变成"计算器守卫"
- 计算器应用看起来完全正常，但具有隐秘的安全功能

### 短信守卫（增强版短信应用）
- 开发一个专门的短信应用
- 集成 Guardian SDK 后，短信应用变成"短信守卫"
- 提供完整的短信功能 + 短信守卫功能

### 日历守卫
- 用户使用日历应用管理日程
- 集成 Guardian SDK 后，日历变成"日历守卫"
- 日历应用看起来完全正常，但具有隐秘的安全功能

## 故障排除

### 常见问题

1. **ActivityNotFoundException**
   - 确保在 AndroidManifest.xml 中正确注册了 `GuardianActivity`
   - 检查 activity 的 exported 属性是否设置为 true

2. **IllegalStateException: GuardianSdk not initialized**
   - 在调用 SDK 功能前先调用 `GuardianSdk.initialize(context)`
   - 确保在 Application 的 onCreate() 中初始化

3. **权限被拒绝**
   - 确保应用已经获取了必要的运行时权限
   - 检查 AndroidManifest.xml 中是否声明了所有权限
   - 在 Android 6.0+ 上需要动态请求权限

4. **无障碍服务未启用**
   - 使用 `isAccessibilityServiceEnabled()` 检查服务状态
   - 使用 `openAccessibilitySettings()` 引导用户开启服务
   - 确保在 AndroidManifest.xml 中正确配置了无障碍服务

5. **浮动窗口无法显示**
   - 确保已授予 SYSTEM_ALERT_WINDOW 权限
   - 在 Android 6.0+ 上需要用户手动授权
   - 检查应用是否在后台运行

6. **短信守卫不生效**
   - 确保无障碍服务已启用
   - 检查短信开门密语配置是否正确
   - 确保短信内容包含正确的密语
   - 检查发送者手机号是否在联系人列表中

7. **短信指令无响应**
   - 确保短信接收器已正确注册
   - 检查短信接收权限是否已授予
   - 检查短信指令格式是否正确
   - 查看日志了解详细信息

### 日志调试

SDK 会输出详细的日志，可以通过 Logcat 查看：

```bash
# 查看所有 Guardian SDK 日志
adb logcat | grep "Guardian"

# 查看特定服务的日志
adb logcat | grep "FloatingWindowService"
adb logcat | grep "EmergencyService"
adb logcat | grep "GuardianAccessibilityService"
adb logcat | grep "SmsReceiver"
```

### 数据库调试

查看数据库中的设置项：

```kotlin
val database = GuardianDatabase.getDatabase(context)
val settings = database.settingDao().getAllSettings()
settings.forEach { setting ->
    Log.d("GuardianDb", "Key: ${setting.key}, Value: ${setting.value}")
}
```

## 版本信息

- **当前版本**: 1.0.0
- **最低 Android 版本**: API 21 (Android 5.0)
- **推荐 Android 版本**: API 24 (Android 7.0) 及以上
- **依赖库**: Room, AndroidX, Material Design

## 技术支持

如有问题，请参考以下资源：

- **设计文档**: 查看 [DESIGN.md](DESIGN.md) 了解详细设计
- **源码**: 查看源码了解实现细节
- **示例应用**: 参考 note-app 模块了解集成示例

## 更新日志

### v1.0.0 (2026-01-18)
- 初始版本发布
- 支持三 Tab 导航界面（联系人、WHY、我的设置）
- 支持 11 种设置项配置
- 支持报警和查询历史记录
- 支持浮动窗口报警
- 支持音量键报警
- 支持摇动手机报警
- 支持短信开门密语
- 支持短信守卫功能（开门、查询、报警、录音、状态指令）
- 支持测试模式
- 支持位置密码本加密
- 支持录音模式和邮件发送
- 支持Ping响应功能
- 支持隐秘界面设置
- 零代码集成，应用只需注册SDK组件
