# AutoDroid 项目指南

## 项目概述
AutoDroid 是一个自动化设备管理和测试平台，包含客户端应用（Android）和服务端（Python FastAPI）。

## 项目结构
- `autodroid-trader-app/`: Android 客户端应用
- `autodroid-trader-server/`: Python FastAPI 服务端

## 常用命令

### 重要：命令行环境说明
**所有命令必须在 Git Bash 环境中执行，不要使用 PowerShell！**
- 使用路径格式：`/d/git/autodroid` 而不是 `d:\git\autodroid`
- 使用正斜杠 `/` 而不是反斜杠 `\`
- 使用 `cd /d/git/autodroid` 而不是 `cd d:\git\autodroid`

### 客户端构建和安装
```bash
cd /d/git/autodroid/autodroid-trader-app
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 服务端启动
```bash
cd /d/git/autodroid/autodroid-trader-server
conda activate autodroid
python run_server.py
```

或者使用 uvicorn:
```bash
cd /d/git/autodroid/autodroid-trader-server
conda activate autodroid
python -m uvicorn api.main:app --host 0.0.0.0 --port 8000 --reload
```

## 代码规范

### 禁止使用的模式
1. **不要使用 `hasattr` 和 `getattr`**：如果字段不存在，应该直接添加到模型中，而不是使用这些变通方法。
2. **不要添加"垃圾代码"**：避免为了兼容性而添加复杂的兼容逻辑，应该修复根本问题。
3. **不要创建不必要的文档文件**：除非明确要求，否则不要创建 *.md 或 README 文件。

### 推荐的做法
1. **Local-First 设计理念**：客户端应该优先使用本地数据，只在必要时同步服务器数据。
2. **保留现有信息**：在同步设备信息时，应该保留现有的设备信息（如设备名称），只更新服务器返回的字段。
3. **直接访问字段**：直接使用模型字段，如 `device.ip` 而不是 `device.ip_address`。

## 关键文件

### 客户端
- `autodroid-trader-app/app/src/main/java/com/autodroid/trader/data/repository/DeviceRepository.kt`: 设备数据仓库，负责与服务器同步数据
- `autodroid-trader-app/app/src/main/java/com/autodroid/trader/managers/DeviceManager.kt`: 设备管理器
- `autodroid-trader-app/app/src/main/java/com/autodroid/trader/ui/dashboard/ItemDeviceManager.kt`: 设备UI管理器

### 服务端
- `autodroid-trader-server/api/devices.py`: 设备API端点
- `autodroid-trader-server/core/device/service.py`: 设备服务逻辑
- `autodroid-trader-server/core/device/database.py`: 设备数据库操作
- `autodroid-trader-server/core/database/models.py`: 数据库模型定义

## 常见问题和解决方案

### 设备信息重置问题
问题：设备重启后，设备名称和USB调试状态等信息重置为默认值。
解决方案：
1. 确保服务端返回完整的设备信息
2. 客户端在同步时保留现有设备信息，只更新服务器返回的字段

### 已安装应用显示"无"
问题：尽管服务器返回了应用信息，但客户端显示"无"。
解决方案：
1. 确保服务端和客户端的应用数据格式一致
2. 检查 AppInfo 类和服务器响应格式的匹配

## 数据模型

### Device 模型字段（服务端）
- `serialno`: 设备序列号（主键）
- `name`: 设备名称
- `ip`: IP地址（注意：不是 ip_address）
- `usb_debug_enabled`: USB调试是否开启
- `wifi_debug_enabled`: WiFi调试是否开启
- 其他字段...

### DeviceEntity 模型字段（客户端）
- `serialNo`: 设备序列号（注意：驼峰命名）
- `name`: 设备名称
- `usbDebugEnabled`: USB调试是否开启（注意：驼峰命名）
- `wifiDebugEnabled`: WiFi调试是否开启（注意：驼峰命名）
- 其他字段...

## 注意事项
1. 客户端使用驼峰命名（camelCase），服务端使用下划线命名（snake_case）
2. 在修改数据模型时，确保客户端和服务端保持一致
3. 在同步设备信息时，应该优先保留本地数据，避免覆盖有用信息