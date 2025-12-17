# UIA2服务器状态验证脚本
# 主动使用adb命令验证UIA2服务器状态

Write-Host "=== UIA2服务器状态验证 ===" -ForegroundColor Green

# 1. 检查设备连接
Write-Host "`n1. 检查设备连接状态..." -ForegroundColor Yellow
$devices = adb devices
Write-Host "设备列表:"
Write-Host $devices

# 2. 检查UIA2进程状态
Write-Host "`n2. 检查UIA2进程状态..." -ForegroundColor Yellow
$uia2Process = adb shell "ps | grep uiautomator"
if ($uia2Process -match "uiautomator") {
    Write-Host "✅ UIA2服务器正在运行" -ForegroundColor Green
    Write-Host "进程信息: $uia2Process"
} else {
    Write-Host "❌ UIA2服务器未运行" -ForegroundColor Red
    
    # 尝试启动UIA2服务器
    Write-Host "尝试启动UIA2服务器..." -ForegroundColor Yellow
    $startOutput = adb shell "am instrument -w -r -e debug false -e class 'io.appium.uiautomator2.server.test.AppiumUiAutomator2ServerAndroidTest' io.appium.uiautomator2.server.test/androidx.test.runner.AndroidJUnitRunner"
    Write-Host "启动结果: $startOutput"
}

# 3. 检查可访问性服务状态
Write-Host "`n3. 检查可访问性服务状态..." -ForegroundColor Yellow
$accessibilityServices = adb shell "settings get secure enabled_accessibility_services"
if ($accessibilityServices -match "uiautomator") {
    Write-Host "✅ UIA2可访问性服务已启用" -ForegroundColor Green
} else {
    Write-Host "❌ UIA2可访问性服务未启用" -ForegroundColor Red
    Write-Host "当前启用的服务: $accessibilityServices"
}

# 4. 检查应用安装状态
Write-Host "`n4. 检查明佣宝应用安装状态..." -ForegroundColor Yellow
$mingyongbao = adb shell "pm list packages | grep tdx"
if ($mingyongbao -match "com.tdx.androidCCZQ") {
    Write-Host "✅ 明佣宝应用已安装" -ForegroundColor Green
} else {
    Write-Host "❌ 明佣宝应用未安装" -ForegroundColor Red
}

# 5. 检查网络连接
Write-Host "`n5. 检查网络连接状态..." -ForegroundColor Yellow
$network = adb shell "netstat -tuln | grep 6790"
if ($network -match "6790") {
    Write-Host "✅ UIA2服务器端口6790正在监听" -ForegroundColor Green
} else {
    Write-Host "❌ UIA2服务器端口6790未监听" -ForegroundColor Red
}

# 6. 测试直接连接
Write-Host "`n6. 测试直接连接UIA2服务器..." -ForegroundColor Yellow
$testConnection = adb shell "curl -s http://127.0.0.1:6790/wd/hub/status"
if ($testConnection -match "sessionId" -or $testConnection -match "status") {
    Write-Host "✅ UIA2服务器响应正常" -ForegroundColor Green
    Write-Host "服务器状态: $testConnection"
} else {
    Write-Host "❌ UIA2服务器无响应" -ForegroundColor Red
}

Write-Host "`n=== 诊断完成 ===" -ForegroundColor Green
Write-Host "如果UIA2服务器未运行，请手动执行以下步骤:" -ForegroundColor Yellow
Write-Host "1. 在设备设置中启用UIA2服务器的可访问性服务"
Write-Host "2. 手动启动UIA2服务器应用"
Write-Host "3. 确保设备已授予必要的权限"