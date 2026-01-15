# Guardian SDK 使用指南

## 概述

Guardian SDK 提供完整的隐秘报警功能，应用只需要注册 SDK 提供的组件即可实现零代码集成。SDK 会自动管理所有后台服务和隐秘功能。

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

在应用的 `AndroidManifest.xml` 中注册 SDK 提供的组件：

```xml
<!-- SDK提供的主界面 -->
<activity
    android:name="com.autodroid.guardiansdk.ui.GuardianActivity"
    android:exported="false"
    android:theme="@style/AppTheme" />

<!-- SDK提供的浮动窗口服务 -->
<service
    android:name="com.autodroid.guardiansdk.service.FloatingWindowService"
    android:process=":guardian"
    android:label="Guardians"
    android:exported="false" />

<!-- SDK提供的紧急服务 -->
<service
    android:name="com.autodroid.guardiansdk.service.EmergencyService"
    android:exported="false"
    android:foregroundServiceType="location" />

<!-- SDK提供的无障碍服务 -->
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
    android:exported="true"
    android:enabled="true">
    <intent-filter android:priority="1000">
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
    </intent-filter>
</receiver>
```

### 3. 初始化 SDK

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

## 零代码集成

应用无需编写任何报警相关代码，SDK 会自动：
- 初始化数据库和默认设置项
- 监听短信开门密语自动激活主界面
- 管理所有报警逻辑和界面切换
- 处理权限申请和服务协调

应用只需专注于实现真实功能（如记事本、计算器等），报警功能由 SDK 完全接管。

## SDK 界面说明

### GuardianActivity（主界面）
SDK 提供的主界面，采用底部双 Tab 导航设计（微信风格）：

- **Tab 1：被监护人**
  - 展示监护人列表
  - 大卡片形式显示监护人信息
  - 显示最后联系时间、最新位置、最新报警信息

- **Tab 2：我的设置**
  - 管理个人报警设置
  - 配置监护人信息
  - 设置报警触发方式
  - 查看报警和查询历史记录

### 设置项列表
我的设置界面包含以下设置项：

1. **我的监护人1-5**：最多可配置5个监护人信息
2. **音量键报警模式**：通过长按音量键触发报警
3. **浮动窗口报警模式**：通过长按浮动窗口按钮触发报警
4. **摇动手机报警模式**：通过摇动手机触发报警
5. **我的位置密码本**：自定义位置信息加密密码本
6. **浮动窗口**：配置浮动窗口显示和位置
7. **擦除报警信息**：紧急擦除所有报警相关数据
8. **短信开门密语**：配置短信开门密语，用于自动激活主界面
9. **测试模式**：练习模式下不发送真实短信，仅模拟报警

### 历史记录
在设置界面底部显示报警和查询历史记录：

- **报警记录**：显示触发时间、触发方式、报警信息和位置
- **查询记录**：显示监护人查询时间、查询内容和回复内容

## 权限要求

SDK 需要以下权限，应用需要在 AndroidManifest.xml 中声明：

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
```

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

在 `res/xml/guardian_accessibility_service_config.xml` 中配置无障碍服务：

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeViewTextChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagIncludeNotImportantViews"
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

在使用相关功能前，确保已经获取了必要的运行时权限。建议在应用启动时请求关键权限：

```kotlin
// 请求短信权限
if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this, 
        arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS),
        REQUEST_SMS_PERMISSION)
}

// 请求位置权限
if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this, 
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
        REQUEST_LOCATION_PERMISSION)
}

// 请求浮动窗口权限
if (!Settings.canDrawOverlays(this)) {
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName"))
    startActivity(intent)
}
```

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

在适当的时机启动和停止服务：

```kotlin
// 应用启动时启动服务
override fun onResume() {
    super.onResume()
    GuardianSdk.getInstance().startFloatingWindowService()
    GuardianSdk.getInstance().startAlarmService()
}

// 应用退出时停止服务
override fun onPause() {
    super.onPause()
    GuardianSdk.getInstance().stopFloatingWindowService()
    GuardianSdk.getInstance().stopAlarmService()
}
```

### 5. 无障碍服务检查

在使用无障碍服务相关功能前，检查服务是否已启用：

```kotlin
if (!GuardianSdk.getInstance().isAccessibilityServiceEnabled()) {
    // 提示用户开启无障碍服务
    AlertDialog.Builder(this)
        .setTitle("需要开启无障碍服务")
        .setMessage("请开启无障碍服务以使用短信开门密语功能")
        .setPositiveButton("去设置") { _, _ ->
            GuardianSdk.getInstance().openAccessibilitySettings()
        }
        .setNegativeButton("取消", null)
        .show()
}
```


## 故障排除

### 常见问题

1. **ActivityNotFoundException**
   - 确保在 AndroidManifest.xml 中正确注册了 `GuardianActivity`
   - 检查 activity 的 exported 属性是否设置为 false

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

6. **短信开门密语不生效**
   - 确保无障碍服务已启用
   - 检查短信开门密语配置是否正确
   - 确保短信内容包含正确的短信开门密语

### 日志调试

SDK 会输出详细的日志，可以通过 Logcat 查看：

```bash
# 查看所有 Guardian SDK 日志
adb logcat | grep "Guardian"

# 查看特定服务的日志
adb logcat | grep "FloatingWindowService"
adb logcat | grep "EmergencyService"
adb logcat | grep "GuardianAccessibilityService"
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

### v1.0.0 (2026-01-15)
- 初始版本发布
- 支持双 Tab 导航界面（被监护人、我的设置）
- 支持 9 种设置项配置
- 支持报警和查询历史记录
- 支持浮动窗口报警
- 支持音量键报警
- 支持摇动手机报警
- 支持短信开门密语
- 支持测试模式
- 支持紧急擦除功能
- 支持位置密码本加密