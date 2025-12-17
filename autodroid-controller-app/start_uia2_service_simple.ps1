# UIA2服务启动脚本
Write-Host "正在启动UIA2服务..." -ForegroundColor Green

# 检查设备连接
adb devices

# 启动UIA2服务
Write-Host "启动UIA2服务..." -ForegroundColor Yellow
adb shell "am instrument -w io.appium.uiautomator2.server.test/androidx.test.runner.AndroidJUnitRunner"

Write-Host "UIA2服务已启动" -ForegroundColor Green