# Autodroid 故障排除指南

## Android 模拟器连接服务器问题

### 问题描述
Android 模拟器无法连接到本地服务器，导致 APK 扫描和设备注册功能失败。

### 问题症状
- 应用日志显示："Failed to connect to http://192.168.1.59:8004"
- 网络请求超时或连接被拒绝
- APK 扫描按钮点击后无响应或显示连接错误

### 根本原因
Android 模拟器使用特殊的网络配置：
- 模拟器内部网络地址：`10.0.2.2`（指向主机）
- 物理设备 IP 地址（如 `192.168.1.59`）在模拟器内部无法访问
- 模拟器网络连接未启用或配置错误

### 解决方案

#### 1. 修改 API 客户端配置
将应用中的 API 客户端配置从物理设备 IP 改为模拟器专用 IP：

**修改前：**
```kotlin
// ApiClient.kt
private const val BASE_URL = "http://192.168.1.59:8004"
```

**修改后：**
```kotlin
// ApiClient.kt
private const val BASE_URL = "http://10.0.2.2:8004"
```

#### 2. 启用模拟器网络连接
如果模拟器网络连接未启用，需要重启模拟器并确保网络连接正常：

1. **关闭模拟器**
2. **重新启动模拟器**
3. **检查网络状态**：
   ```bash
   adb shell ping -c 3 10.0.2.2
   adb shell nc -z -w 5 10.0.2.2 8004
   ```

#### 3. 验证网络连接状态
使用以下命令检查模拟器网络配置：

```bash
# 检查设备连接
adb devices

# 检查网络接口
adb shell ifconfig

# 检查路由表
adb shell netstat -rn
adb shell cat /proc/net/route

# 检查网络属性
adb shell "getprop | grep -E '(net\.|dns|wifi|ethernet)'"

# 检查网络连接状态
adb shell dumpsys connectivity
```

#### 4. 测试端口连接
验证模拟器是否可以访问服务器端口：

```bash
# 测试端口连接
adb shell "nc -z -w 5 10.0.2.2 8004 && echo '端口8004可访问' || echo '端口8004不可访问'"

# 测试 API 端点
adb shell "echo '测试设备注册API' && echo 'POST http://10.0.2.2:8004/api/devices/register' | nc -q 5 10.0.2.2 8004"
```

### 网络连接验证步骤

#### 步骤 1：检查模拟器网络接口
```bash
adb shell ifconfig
```
**期望输出：**
- `wlan0` 接口状态为 UP
- 获得 IP 地址（如 `10.0.2.16`）
- 有数据包传输（RX/TX bytes > 0）

#### 步骤 2：验证路由表
```bash
adb shell netstat -rn
```
**期望输出：**
- 有默认路由条目
- 路由指向 `10.0.2.2`

#### 步骤 3：测试主机连接
```bash
adb shell ping -c 3 10.0.2.2
```
**期望结果：** 连接成功，无 "Network is unreachable" 错误

#### 步骤 4：测试服务器端口
```bash
adb shell nc -z -w 5 10.0.2.2 8004
```
**期望结果：** 端口可访问，无连接超时

### APK 信息显示问题

#### 问题描述
APK 扫描 API 调用成功，但 APK 信息没有显示在界面上的 `scan_apks_button` 项目下面。

#### 解决方案
在 DashboardFragment 的初始化方法中添加 APK 信息项的占位符：

**修改 DashboardFragment.kt：**
```kotlin
private fun updateUI() {
    // ... 其他项目初始化 ...
    
    // 4. APK scanner
    dashboardItems.add(DashboardItem.ApkScannerItem(
        scanStatus = "SCAN INSTALLED APKS"
    ))
    
    // 5. 添加空的 APK 信息项作为占位符（扫描 APK 后会更新）
    dashboardItems.add(DashboardItem.ApkInfo(
        packageName = "",
        appName = "",
        version = "",
        versionCode = 0,
        installTime = "",
        updateTime = ""
    ))
    
    // ... 其他项目 ...
}
```

### 服务器配置验证

确保服务器正确绑定到所有网络接口：

**检查 run_server.py：**
```python
# 服务器应该绑定到 0.0.0.0（所有接口）
uvicorn.run(app, host="0.0.0.0", port=8004)
```

### 常见错误及解决方法

#### 错误 1："connect: Network is unreachable"
**原因：** 模拟器网络连接未启用
**解决：** 重启模拟器并检查网络设置

#### 错误 2："Connection refused"
**原因：** 服务器未运行或端口被占用
**解决：** 启动服务器并检查端口占用情况

#### 错误 3：APK 信息不显示
**原因：** 界面初始化时缺少 APK 信息项占位符
**解决：** 在 DashboardFragment 中添加空的 APK 信息项

### 测试流程

1. **启动服务器**
   ```bash
   cd autodroid-container
   python run_server.py
   ```

2. **重新编译安装应用**
   ```bash
   cd autodroid-app
   ./gradlew installDebug
   ```

3. **在模拟器上测试**
   - 打开 Autodroid 应用
   - 点击 "SCAN INSTALLED APKS" 按钮
   - 验证 APK 信息正确显示在界面中

### 日志监控

监控应用日志以诊断连接问题：
```bash
adb logcat | grep -E "(ApiClient|ApkScanner|Network)"
```

**关键日志信息：**
- `Making POST request to: http://10.0.2.2:8004/api/devices/register`
- `Registered 1 APKs for device [设备ID]`
- `Scan completed: 1 APKs ready for display`

### 总结

通过以上步骤，Android 模拟器应该能够成功连接到本地服务器，APK 扫描功能正常工作，APK 信息正确显示在界面上。如果问题仍然存在，请检查防火墙设置或尝试使用物理设备进行测试。