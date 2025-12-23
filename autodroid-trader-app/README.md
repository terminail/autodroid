# Autodroid App

Autodroid App 是 Autodroid Android 自动化系统的客户端应用，用于发现服务器、注册测试设备和管理测试工作脚本。

## 功能特性

- **服务器发现**：通过Manual Set or Scan QRCode to find Autodroid服务器
- **设备注册**：将测试设备信息注册到服务器
- **工作脚本管理**：查看和管理测试工作脚本
- **测试报告**：查看测试执行报告
- **实时监控**：监控测试设备状态

## 开发环境

- **Android Studio**：最新稳定版
- **Kotlin**：1.9.0+
- **Gradle**：8.0+
- **Android SDK**：API Level 30+

## 构建项目

### 1. 打开项目

使用 Android Studio 打开 `autodroid-app` 目录。

### 2. 同步 Gradle

等待 Android Studio 自动同步 Gradle 依赖。如果遇到问题，可以手动点击 "Sync Project with Gradle Files" 按钮。

### 3. 构建 APK

```bash
# 构建 Debug 版本
 cd 'd:/git/autodroid/autodroid-trader-app';./gradlew assembleDebug

# 构建 Release 版本
 cd 'd:/git/autodroid/autodroid-trader-app';./gradlew assembleRelease
```

## 运行应用

### 1. 连接设备

使用 USB 数据线连接 Android 设备到开发机，并确保已启用 USB 调试模式。

### 2. 安装应用

```bash
# 安装 Debug 版本
cd 'd:/git/autodroid/autodroid-trader-app'; ./gradlew installDebug

# 或使用 adb 命令
cd 'd:/git/autodroid/autodroid-trader-app'; adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. 启动应用

在设备上找到 Autodroid App 图标，点击启动应用。

## 运行测试

### 运行网络测试

网络测试是一个集成测试，用于测试应用连接服务器的功能。

#### 步骤 1：准备测试环境

1. 确保已安装 Android Studio
2. 确保已连接测试设备或启动模拟器
3. 确保设备已启用开发者选项和 USB 调试

### 运行单元测试

```bash
# 运行所有单元测试
./gradlew test
```

## 测试依赖

- **JUnit 4**：单元测试框架
- **AndroidX Test**：集成测试框架
- **Espresso**：UI 测试框架

## 项目结构

```
autodroid-app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/autodroid/manager/
│   │   │   │   ├── auth/          # 认证相关代码
│   │   │   │   ├── managers/      # 业务管理器
│   │   │   │   ├── model/        # 数据模型
│   │   │   │   ├── service/      # 服务类
│   │   │   │   ├── ui/           # UI 组件
│   │   │   │   ├── utils/        # 工具类
│   │   │   │   └── viewmodel/     # ViewModel 类
│   │   │   ├── res/             # 资源文件
│   │   │   └── AndroidManifest.xml  # 应用配置
│   │   └── androidTest/         # 集成测试
│   │       └── java/com/autodroid/manager/  # 集成测试代码
│   └── build.gradle.kts         # 模块配置
├── build.gradle.kts             # 项目配置
└── settings.gradle.kts          # 项目设置
```

## 核心组件

### 1. NetworkService

处理与服务器的网络通信。

### 3. DeviceManager

管理设备信息和状态。

### 4. WorkScriptManager

管理测试工作脚本。

## API 接口

应用通过 RESTful API 与 Autodroid 服务器通信，主要接口包括：

- `/api/health`：健康检查
- `/api/server`：获取服务器信息
- `/api/devices/register`：注册设备
- `/api/workscripts`：获取工作脚本列表
- `/api/plans`：获取测试计划

## 常见问题

### 1. 无法连接服务器

- 确保设备和服务器在同一网络
- 确保服务器已启动
- 检查设备的网络权限

### 2. 设备注册失败

- 检查设备网络连接
- 确保服务器 API 正常
- 检查设备信息格式

### 3. 测试运行失败

- 确保设备已正确连接
- 确保已启用 USB 调试
- 检查测试代码和依赖

## 贡献指南

1. Fork 仓库
2. 创建特性分支
3. 提交代码
4. 推送分支
5. 创建 Pull Request

## 许可证

MIT License
