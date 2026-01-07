@echo off
REM 安装自定义 IME 和 AccessibilityService

echo 正在构建自定义 IME 应用...

REM 构建项目
cd /d d:\git\autodroid\autodroid-trader-ime
gradlew.bat assembleDebug

echo 构建完成，正在安装应用...

REM 安装 APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

echo 安装完成，请手动在设备上启用辅助功能服务：
echo 设置 -^> 辅助功能 -^> AutoDroid Trader Accessibility Service
echo 并启用输入法：
echo 设置 -^> 系统和更新 -^> 语言和输入法 -^> 当前输入法 -^> AutoDroid Trader IME