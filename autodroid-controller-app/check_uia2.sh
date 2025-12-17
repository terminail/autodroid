#!/bin/bash

echo "=== UIA2服务器状态验证 ==="

echo ""
echo "1. 检查设备连接状态..."
adb devices

echo ""
echo "2. 检查UIA2进程状态..."
adb shell "ps | grep uiautomator"

echo ""
echo "3. 检查可访问性服务状态..."
adb shell "settings get secure enabled_accessibility_services"

echo ""
echo "4. 检查明佣宝应用安装状态..."
adb shell "pm list packages | grep tdx"

echo ""
echo "5. 检查网络连接状态..."
adb shell "netstat -tuln | grep 6790"

echo ""
echo "=== 诊断完成 ==="
echo "如果UIA2服务器未运行，请手动执行以下步骤:"
echo "1. 在设备设置中启用UIA2服务器的可访问性服务"
echo "2. 手动启动UIA2服务器应用"
echo "3. 确保设备已授予必要的权限"