# UIA2服务启动脚本
# 这个脚本会启动UIA2服务并保持运行状态

Write-Host "正在启动UIA2服务..." -ForegroundColor Green

# 检查设备连接
$devices = adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "device" }
if ($devices.Count -eq 0) {
    Write-Host "错误：没有找到连接的Android设备" -ForegroundColor Red
    exit 1
}

Write-Host "设备已连接: $($devices[0])" -ForegroundColor Yellow

# 检查UIA2服务是否已经在运行
Write-Host "检查UIA2服务状态..." -ForegroundColor Yellow
$uia2Process = adb shell "ps | grep uiautomator"
if ($LASTEXITCODE -eq 0) {
    Write-Host "UIA2服务已经在运行" -ForegroundColor Green
} else {
    Write-Host "启动UIA2服务..." -ForegroundColor Yellow
    
    # 启动UIA2服务（后台运行）
    adb shell "nohup am instrument -w io.appium.uiautomator2.server.test/androidx.test.runner.AndroidJUnitRunner > /dev/null 2>&1 &"
    
    # 等待服务启动
    Write-Host "等待UIA2服务启动..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
    
    # 检查服务是否启动成功
    $uia2Process = adb shell "ps | grep uiautomator"
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ UIA2服务启动成功" -ForegroundColor Green
    } else {
        Write-Host "❌ UIA2服务启动失败" -ForegroundColor Red
        exit 1
    }
}

# 设置端口转发
Write-Host "设置端口转发 (8200 -> 6790)..." -ForegroundColor Yellow
adb forward tcp:8200 tcp:6790

# 验证服务状态
Write-Host "验证UIA2服务状态..." -ForegroundColor Yellow
$status = curl -s http://127.0.0.1:8200/status
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ UIA2服务状态正常" -ForegroundColor Green
    Write-Host "服务响应: $status" -ForegroundColor Cyan
} else {
    Write-Host "❌ UIA2服务状态检查失败" -ForegroundColor Red
}

Write-Host ""
Write-Host "UIA2服务已准备就绪" -ForegroundColor Green
Write-Host "- 服务端口: 6790 (设备本地)" -ForegroundColor White
Write-Host "- 转发端口: 8200 (电脑本地)" -ForegroundColor White
Write-Host "- 测试应用可以连接: http://127.0.0.1:6790" -ForegroundColor White