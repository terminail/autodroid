@echo off
echo ========================================
echo AutoDroid Controller 初始化脚本
echo ========================================
echo.

echo 步骤1: 检查ADB连接
echo.
adb devices
if %errorlevel% neq 0 (
    echo 错误: 请确保ADB已正确安装并配置
    pause
    exit /b 1
)

echo.
echo 步骤2: 安装AutoDroid Controller APP
echo.
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %errorlevel% neq 0 (
    echo 错误: APK安装失败
    pause
    exit /b 1
)

echo.
echo 步骤3: 授予系统权限
echo.

:: 授予WRITE_SECURE_SETTINGS权限
adb shell pm grant com.autodroid.controller android.permission.WRITE_SECURE_SETTINGS

:: 授予安装APK权限
adb shell appops set com.autodroid.controller REQUEST_INSTALL_PACKAGES allow

:: 授予悬浮窗权限
adb shell appops set com.autodroid.controller SYSTEM_ALERT_WINDOW allow

echo.
echo 步骤4: 开启无线调试
echo.
adb tcpip 5555

echo.
echo 步骤5: 获取设备IP地址
echo.
adb shell ip addr show wlan0 | grep "inet " | awk '{print $2}' | cut -d/ -f1

echo.
echo ========================================
echo 初始化完成！
echo ========================================
echo.
echo 设备信息：
echo - 包名: com.autodroid.controller
echo - 无线调试端口: 5555
echo - 请记录上述IP地址用于后续连接
echo.
echo 下一步操作：
echo 1. 断开USB线
echo 2. 通过Wi-Fi连接设备: adb connect <设备IP>:5555
echo 3. 启动AutoDroid Controller应用
echo.
pause