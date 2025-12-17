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
│   │   │   └── WebDriverClient.kt    # WebDriver客户端
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

### 2. WebDriverClient (WebDriver客户端)
- 封装与UIA2 Server的HTTP通信
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

## 部署流程

### 1. 环境准备
- 确保设备已安装Appium UIA2 Server相关APK
- 准备ADB工具和USB连接线

### 2. 编译应用
```bash
./gradlew assembleDebug
```

### 3. 初始化设备（技术人员执行）
运行初始化脚本：
```bash
init_script.bat
```

脚本将自动完成：
- 安装APK
- 授予系统权限
- 开启无线调试

### 4. 无线连接（可选）
```bash
adb connect <设备IP>:5555
```

### 5. 启动应用
- 手动启动AutoDroid Controller应用
- 授予必要权限
- 点击"启动自动化服务"

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
        "capabilities": {
          "platformName": "Android",
          "appPackage": "com.example.app",
          "appActivity": ".MainActivity",
          "automationName": "UiAutomator2"
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
        "elementId": "${lastElementId}"
      }
    }
  ]
}
```

### 支持的操作类型
- `initSession` - 初始化WebDriver会话
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