# AutoDroid Controller APP

基于Appium UIA2 Server的Android自动化测试控制器应用。

## 项目概述

这是一个Android应用，用于接收远程测试任务并通过本地UIA2 Server执行自动化测试。应用采用方案A（自研Controller APP）架构，具备完整的WebDriver协议支持。

## 功能特性

- ✅ 通过HTTP与公司服务器通信，拉取测试任务
- ✅ 将JSON任务转换为WebDriver协议命令
- ✅ 与本地UIA2 Server（端口8200）通信执行自动化操作
- ✅ 支持多种UI操作：点击、输入、滑动、截图等
- ✅ 后台服务持续运行，定期检查新任务
- ✅ 支持变量引用和会话管理
- ✅ 通过ADB授权获得系统级权限

## 项目结构

```
autodroid-controller-app/
├── app/
│   ├── src/main/java/com/autodroid/controller/
│   │   ├── model/
│   │   │   └── TaskModel.kt          # 任务数据模型
│   │   ├── webdriver/
│   │   │   └── ContentProviderClient.kt    # Content Provider客户端
│   │   ├── service/
│   │   │   └── AutomationService.kt  # 自动化服务
│   │   ├── util/
│   │   │   └── NotificationHelper.kt # 通知工具
│   │   └── MainActivity.kt           # 主界面
│   ├── src/main/res/                 # 资源文件
│   └── build.gradle.kts              # 模块配置
├── build.gradle.kts                   # 项目配置
├── settings.gradle.kts                # 设置文件
├── init_script.bat                    # 初始化脚本
└── README.md                          # 说明文档
```

## 核心模块

### 1. TaskModel (任务模型)
- 定义`AutomationTask`和`Action`数据结构
- 管理WebDriver会话状态
- 支持变量引用（如`${lastElementId}`）

### 2. ContentProviderClient (Content Provider客户端)
- 使用HttpProxyProvider Content Provider与UIA2 Server通信
- 避免Android沙盒限制，无需网络权限
- 支持完整的WebDriver协议操作
- 错误处理和重试机制

### 3. AutomationService (自动化服务)
- 后台服务，定期检查新任务
- 任务执行和结果上报
- 使用WorkManager进行定期调度

### 4. MainActivity (主界面)
- 服务启动/停止控制
- 权限管理和设置跳转
- 测试任务执行

## 快速开始

### 1. 环境准备
- 确保设备已安装Appium UIA2 Server相关APK
- 准备ADB工具和USB连接线

### 2. 安装UIA2 Server组件
```bash
# 卸载旧版本（如有）
adb -s TDCDU17905004388 uninstall io.appium.uiautomator2.server
adb -s TDCDU17905004388 uninstall io.appium.uiautomator2.server.test
adb -s TDCDU17905004388 uninstall io.appium.settings

# 安装UIA2 Server核心组件
# 请将以下路径替换为你的实际路径
adb -s TDCDU17905004388 install "C:\\Users\\Administrator\\.appium\\node_modules\\appium-uiautomator2-driver\\node_modules\\appium-uiautomator2-server\\apks\\appium-uiautomator2-server-v9.9.0.apk"
adb -s TDCDU17905004388 install "C:\\Users\\Administrator\\.appium\\node_modules\\appium-uiautomator2-driver\\node_modules\\appium-uiautomator2-server\\apks\\appium-uiautomator2-server-debug-androidTest.apk"
adb -s TDCDU17905004388 install "C:\\Users\\Administrator\\.appium\\node_modules\\appium-uiautomator2-driver\\node_modules\\io.appium.settings\\apks\\settings_apk-debug.apk"
```

### 3. 安装Controller-app
```bash
# 编译应用
./gradlew assembleDebug

# 安装APK
adb -s TDCDU17905004388 install -r app/build/outputs/apk/debug/app-debug.apk

# 授予系统权限（技术人员执行）
adb -s TDCDU17905004388 shell pm grant com.autodroid.controller android.permission.WRITE_SECURE_SETTINGS
adb -s TDCDU17905004388 shell appops set com.autodroid.controller android:write_settings allow
adb -s TDCDU17905004388 shell appops set com.autodroid.controller REQUEST_INSTALL_PACKAGES allow
adb -s TDCDU17905004388 shell appops set com.autodroid.controller SYSTEM_ALERT_WINDOW allow
```

### 4. 初始化设备（技术人员执行）
运行初始化脚本：
```bash
init_script.bat
```

脚本将自动完成：
- 授予系统权限
- 开启无线调试

### 5. 无线连接（可选）
```bash
adb connect <设备IP>:5555
```

### 6. 启动服务

#### 启动UIA2 Server
```bash
# 停止/重新启动ADB服务
adb kill-server; adb start-server 

# 启动UIA2 Server（后台运行）
adb -s TDCDU17905004388 shell am instrument -w -e disableAnalytics true io.appium.uiautomator2.server.test/androidx.test.runner.AndroidJUnitRunner

# 设置端口转发
adb -s TDCDU17905004388 forward tcp:8200 tcp:6790


adb shell pm list packages | grep -E "(autodroid|appium)"
```

#### 启动Controller-app
```bash
# 方式1：手动启动（推荐用于测试）
adb -s TDCDU17905004388 shell am start -n com.autodroid.controller/.MainActivity

# 方式2：通过Appium自动化启动（推荐用于生产环境）
# 在Capabilities中配置：
{
  "appium:appPackage": "com.autodroid.controller",
  "appium:appActivity": ".MainActivity",
  "appium:noReset": false
}
```

### 7. 验证服务状态

#### 检查UIA2 Server状态
```bash
# 验证UIA2 Server是否正常运行
adb -s TDCDU17905004388 shell "ps | grep uiautomator"

# 检查端口是否监听
adb -s TDCDU17905004388 shell "netstat -tuln | grep 6790"

# 通过HTTP接口验证服务状态
curl localhost:8200/status

# 期望响应：
{"sessionId":"None","value":{"build":{"version":"9.9.0","versionCode":244},"message":"UiAutomator2 Server is ready to accept commands","ready":true}}
```

#### 检查Controller-app状态
```bash
# 检查应用是否在运行
adb -s TDCDU17905004388 shell "ps | grep autodroid"

# 检查应用进程状态
adb -s TDCDU17905004388 shell "dumpsys activity services com.autodroid.controller"
```

### 8. 停止服务
```bash
# 停止AutoDroid Controller应用
adb -s TDCDU17905004388 shell am force-stop com.autodroid.controller

# 停止UIA2 Server
adb -s TDCDU17905004388 shell am force-stop io.appium.uiautomator2.server
adb -s TDCDU17905004388 shell am force-stop io.appium.uiautomator2.server.test

# 清除端口转发
adb -s TDCDU17905004388 forward --remove tcp:8200

# 清除应用数据（可选）
adb -s TDCDU17905004388 shell pm clear com.autodroid.controller
```

## 权限配置

通过ADB授予的系统权限：
- `WRITE_SECURE_SETTINGS` - 修改系统安全设置
- `REQUEST_INSTALL_PACKAGES` - 静默安装APK
- `SYSTEM_ALERT_WINDOW` - 显示悬浮窗

## 任务格式

### JSON任务示例
```json
{
  "taskId": "test_20230520_001",
  "deviceId": "emulator-5554",
  "actions": [
    {
      "action": "initSession",
      "params": {
        "appPackage": "com.example.app",
        "appActivity": ".MainActivity"
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
        "elementId": "${lastElementId}"
      }
    }
  ]
}
```

### 支持的操作类型
- `initSession` - 初始化WebDriver会话（仅需appPackage和appActivity参数）
- `findElement` - 查找元素
- `click` - 点击元素
- `sendKeys` - 输入文本
- `swipe` - 滑动操作
- `takeScreenshot` - 截图
- `closeSession` - 关闭会话

## 服务器接口

### 获取任务
```
GET /api/task?deviceId=<设备ID>
```

### 上报结果
```
POST /api/task/result
Content-Type: application/json

{
  "taskId": "test_001",
  "deviceId": "device_123",
  "success": true,
  "log": "执行日志...",
  "timestamp": 1672531200000
}
```

## 技术栈

- **语言**: Kotlin
- **框架**: Android SDK
- **网络**: OkHttp3
- **异步**: Kotlin Coroutines
- **调度**: WorkManager
- **构建**: Gradle

## 注意事项

1. 确保UIA2 Server已正确安装并运行在端口8200
2. 应用需要网络权限与服务器通信
3. 建议通过ADB授权获得系统权限以确保稳定性
4. 后台服务会持续运行，定期检查新任务
5. 支持Android 8.0+ (API 24+) 设备

## 故障排除

### 常见问题
1. **权限不足**: 重新运行初始化脚本
2. **网络连接失败**: 检查设备网络和服务器地址
3. **UIA2 Server未响应**: 确认Server是否正常运行
4. **任务执行失败**: 检查任务JSON格式和参数

### UIA2 Server状态检查
```bash
# 检查UIA2 Server进程状态
adb -s TDCDU17905004388 shell "ps | grep uiautomator"

# 检查端口监听状态
adb -s TDCDU17905004388 shell "netstat -tuln | grep 6790"

# 检查服务是否可访问
curl localhost:8200/status

# 重启UIA2 Server
adb -s TDCDU17905004388 shell am force-stop io.appium.uiautomator2.server
adb -s TDCDU17905004388 shell am force-stop io.appium.uiautomator2.server.test
adb -s TDCDU17905004388 shell am instrument -w -e disableAnalytics true io.appium.uiautomator2.server.test/androidx.test.runner.AndroidJUnitRunner

# 重新建立端口转发
adb -s TDCDU17905004388 forward tcp:8200 tcp:6790
```

### 日志查看
```bash
adb logcat | grep "AutoDroid"
```

## 开发说明

### 扩展新操作
1. 在`WebDriverClient`中添加新方法
2. 在`executeAction`中添加对应case
3. 更新任务JSON格式说明

### 自定义配置
- 修改`WebDriverClient`中的服务器地址
- 调整`AutomationService`中的检查间隔
- 自定义通知样式和权限要求

## 许可证

本项目仅供企业内部使用。