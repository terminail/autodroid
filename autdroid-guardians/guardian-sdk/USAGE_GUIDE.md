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
<!-- SDK提供的隐秘设置界面 -->
<activity
    android:name="com.autodroid.guardiansdk.ui.SettingActivity"
    android:exported="false"
    android:theme="@android:style/Theme.DeviceDefault.Dialog" />

<!-- SDK提供的浮动窗口服务 -->
<service
    android:name="com.autodroid.guardiansdk.service.FloatingWindowService"
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
        android:resource="@xml/accessibility_service_config" />
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
        
        // 初始化 SDK - SDK会自动启动所有必要服务
        GuardianSdk.initialize(this)
    }
}
```

## 零代码集成

应用无需编写任何报警相关代码，SDK 会自动：
- 启动后台服务（浮动窗口、报警监听等）
- 监听短信密语自动激活隐秘设置界面
- 管理所有报警逻辑和界面切换
- 处理权限申请和服务协调

应用只需专注于实现真实功能（如记事本、计算器等），报警功能由 SDK 完全接管。

## 权限要求

SDK 需要以下权限，应用需要在 AndroidManifest.xml 中声明：

```xml
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

1. **GuardianDatabase** - 数据库
   - 基于 Room 的数据存储
   - 支持设置项、联系人等数据

2. **SettingRepository** - 设置项仓库
   - 类似 SharedPreferences 的接口
   - 支持类型安全的设置项管理

## 配置说明

### 权限配置

应用需要在 `AndroidManifest.xml` 中声明以下权限：

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
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
```

### 主题配置

SDK 界面使用应用的主题，确保应用定义了以下主题：

```xml
<!-- 对话框主题 -->
<style name="Theme.AutoDroidGuardian.Dialog" parent="Theme.Material3.Dialog">
    <item name="colorPrimary">@color/primary</item>
    <item name="colorSurface">@color/surface</item>
</style>
```

## 最佳实践

### 1. 初始化时机

在 Application 的 `onCreate()` 中尽早初始化 SDK，确保所有功能可用。

### 2. 权限请求

在使用相关功能前，确保已经获取了必要的运行时权限。

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

### 4. 生命周期管理

在适当的时机启动和停止服务：

```kotlin
// 应用启动时启动服务
override fun onResume() {
    super.onResume()
    GuardianSdk.getInstance().startAlarmService()
}

// 应用退出时停止服务
override fun onPause() {
    super.onPause()
    GuardianSdk.getInstance().stopAlarmService()
}
```

## 扩展功能

### 自定义设置项

应用可以扩展 SDK 的设置项：

```kotlin
val settingRepository = SettingRepository(database.settingDao())

// 添加自定义设置项
settingRepository.putString("custom_setting", "value", "自定义设置", "custom")

// 读取自定义设置项
val value = settingRepository.getString("custom_setting")
```

### 界面定制

SDK 的界面组件支持主题定制，应用可以通过修改主题来改变界面外观。

## 故障排除

### 常见问题

1. **ActivityNotFoundException**
   - 确保在 AndroidManifest.xml 中正确注册了 SDK 的 Activity

2. **IllegalStateException: GuardianSdk not initialized**
   - 在调用 SDK 功能前先调用 `GuardianSdk.initialize(context)`

3. **权限被拒绝**
   - 确保应用已经获取了必要的运行时权限

### 日志调试

SDK 会输出详细的日志，可以通过 Logcat 查看：

```bash
adb logcat | grep "GuardianSdk"
```

## 版本信息

- **当前版本**: 1.0.0
- **最低 Android 版本**: API 21 (Android 5.0)
- **依赖库**: Room, AndroidX, Material Design

## 技术支持

如有问题，请参考源码或联系开发团队。