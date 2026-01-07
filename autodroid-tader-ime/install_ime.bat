@echo off
REM 安装自定义 IME 和 AccessibilityService

echo 正在构建自定义 IME 应用...

REM 构建项目
cd /d d:\git\autodroid\autodroid-tader-ime
echo 当前目录: %cd%
echo 检查 gradlew.bat 文件...
if not exist "gradlew.bat" (
    echo 错误: gradlew.bat 文件不存在
    pause
    exit /b 1
)

call gradlew.bat assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo 构建失败，请检查错误信息
    pause
    exit /b 1
)

echo 构建完成，正在安装应用...

REM 检查 APK 文件是否存在
if not exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo 错误: APK 文件不存在，请检查构建过程
    pause
    exit /b 1
)

REM 安装 APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% NEQ 0 (
    echo 安装失败，请检查错误信息
    pause
    exit /b 1
)

echo.
echo 安装完成！请按以下步骤启用服务：
echo 1. 启用输入法：设置 -^> 系统和更新 -^> 语言和输入法 -^> 当前输入法 -^> AutoDroid Trader IME
echo 2. 启用辅助功能服务：设置 -^> 辅助功能 -^> AutoDroid Trader Accessibility Service
echo.
echo 完成后，系统将使用此输入法，且无障碍服务将自动获取页面信息。
pause