#!/usr/bin/env python3
"""
测试受保护页面访问流程 - 详细调试版本
验证导航拦截逻辑和点击事件处理
"""

import subprocess
import time
import sys

def run_command(cmd):
    """执行命令并返回输出"""
    try:
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        return result.stdout, result.stderr, result.returncode
    except Exception as e:
        return "", str(e), 1

def clear_logcat():
    """清除日志缓存"""
    print("清除日志缓存...")
    stdout, stderr, code = run_command("adb logcat -c")
    if code == 0:
        print("✓ 日志缓存已清除")
    else:
        print("✗ 清除日志缓存失败")

def capture_logcat(tag="", lines=500):
    """捕获日志"""
    if tag:
        print(f"捕获 {tag} 日志...")
        stdout, stderr, code = run_command(f"adb logcat -d -t {lines} | grep {tag}")
    else:
        print(f"捕获所有日志...")
        stdout, stderr, code = run_command(f"adb logcat -d -t {lines}")
    return stdout

def start_app():
    """启动应用"""
    print("启动应用...")
    stdout, stderr, code = run_command("adb shell am start -n com.autodroid.manager/.MainActivity")
    if code == 0:
        print("✓ 应用启动成功")
    else:
        print("✗ 应用启动失败")
    time.sleep(3)  # 等待应用完全启动

def stop_app():
    """停止应用"""
    print("停止应用...")
    stdout, stderr, code = run_command("adb shell am force-stop com.autodroid.manager")
    if code == 0:
        print("✓ 应用停止成功")
    else:
        print("✗ 应用停止失败")

def get_ui_hierarchy():
    """获取UI层次结构"""
    stdout, stderr, code = run_command("adb shell uiautomator dump /sdcard/window_dump.xml")
    if code == 0:
        stdout, stderr, code = run_command("adb shell cat /sdcard/window_dump.xml")
        return stdout
    return ""

def get_bottom_navigation_info():
    """获取底部导航栏信息"""
    print("获取底部导航栏信息...")
    
    # 获取UI层次结构
    ui_hierarchy = get_ui_hierarchy()
    
    # 查找底部导航栏相关元素
    if "bottom_navigation" in ui_hierarchy:
        print("✓ 找到底部导航栏")
        
        # 提取导航项信息
        if "nav_dashboard" in ui_hierarchy:
            print("✓ 找到Dashboard导航项")
        if "nav_workflows" in ui_hierarchy:
            print("✓ 找到Workflows导航项")
        if "nav_reports" in ui_hierarchy:
            print("✓ 找到Reports导航项")
        if "nav_orders" in ui_hierarchy:
            print("✓ 找到Orders导航项")
        if "nav_my" in ui_hierarchy:
            print("✓ 找到My导航项")
    else:
        print("✗ 未找到底部导航栏")
    
    return ui_hierarchy

def test_navigation_setup():
    """测试导航设置"""
    print("\n=== 测试导航设置 ===")
    
    # 重启应用
    stop_app()
    clear_logcat()
    start_app()
    
    # 捕获启动日志
    logs = capture_logcat("", 300)
    
    # 检查关键组件
    checks = {
        "MainActivity创建": "MainActivity onCreate" in logs,
        "导航初始化": "initializeNavigation" in logs,
        "导航拦截器设置": "setupNavigationInterception" in logs,
        "服务器连接观察器": "Setting up server connection observer" in logs,
        "DiscoveryStatusManager": "DiscoveryStatusManager" in logs,
        "服务器状态": "Server disconnected" in logs or "Server connected" in logs,
    }
    
    for check, status in checks.items():
        print(f"{'✓' if status else '✗'} {check}")
    
    # 显示相关日志
    print("\n关键启动日志:")
    for line in logs.split('\n'):
        if any(keyword in line for keyword in ["onCreate", "initializeNavigation", "setupNavigationInterception", "observer", "DiscoveryStatusManager", "Server"]):
            print(f"  {line}")
    
    return sum(checks.values()) >= len(checks) - 1

def test_click_events():
    """测试点击事件"""
    print("\n=== 测试点击事件 ===")
    
    # 获取UI信息
    ui_hierarchy = get_bottom_navigation_info()
    
    # 测试不同点击方法
    print("\n测试点击方法:")
    
    # 方法1: 使用UI自动化工具点击
    print("方法1: 使用UI自动化点击Workflows导航项")
    clear_logcat()
    
    # 尝试使用uiautomator点击
    stdout, stderr, code = run_command("adb shell uiautomator events")
    if code == 0:
        print("✓ UI自动化工具可用")
    
    # 尝试点击Workflows导航项
    stdout, stderr, code = run_command("adb shell input text 'KEYCODE_DPAD_RIGHT'")
    time.sleep(1)
    stdout, stderr, code = run_command("adb shell input keyevent KEYCODE_DPAD_RIGHT")
    time.sleep(1)
    stdout, stderr, code = run_command("adb shell input keyevent KEYCODE_ENTER")
    time.sleep(2)
    
    # 捕获日志
    logs = capture_logcat("MainActivity", 200)
    
    if "Navigation interception" in logs:
        print("✓ 导航拦截器被触发")
        for line in logs.split('\n'):
            if "Navigation interception" in line:
                print(f"  日志: {line}")
    else:
        print("✗ 导航拦截器未被触发")
    
    # 方法2: 使用精确坐标点击
    print("\n方法2: 使用精确坐标点击")
    
    # 获取屏幕分辨率
    stdout, stderr, code = run_command("adb shell wm size")
    print(f"屏幕分辨率: {stdout.strip()}")
    
    # 测试不同坐标
    coordinates = [
        (540, 1800),  # 中心底部
        (200, 1800),  # 左侧底部
        (900, 1800),  # 右侧底部
        (540, 1750),  # 稍高
        (540, 1850),  # 稍低
    ]
    
    for i, (x, y) in enumerate(coordinates, 1):
        print(f"\n测试坐标 {i}: ({x}, {y})")
        clear_logcat()
        
        stdout, stderr, code = run_command(f"adb shell input tap {x} {y}")
        time.sleep(2)
        
        logs = capture_logcat("MainActivity", 100)
        
        if "Navigation interception" in logs:
            print("✓ 导航拦截器被触发")
            for line in logs.split('\n'):
                if "Navigation interception" in line:
                    print(f"  日志: {line}")
            break
        else:
            print("✗ 导航拦截器未被触发")

def test_navigation_interception_logic():
    """测试导航拦截逻辑"""
    print("\n=== 测试导航拦截逻辑 ===")
    
    # 重启应用
    stop_app()
    clear_logcat()
    start_app()
    
    # 等待应用完全启动
    time.sleep(5)
    
    # 捕获当前状态
    logs = capture_logcat("MainActivity", 300)
    
    # 检查服务器状态
    if "Server disconnected" in logs:
        print("✓ 服务器状态: 未连接")
        
        # 测试导航拦截
        print("\n测试导航拦截:")
        
        # 使用ADB命令模拟导航到Workflows
        print("尝试导航到Workflows页面...")
        clear_logcat()
        
        # 使用不同的导航方法
        methods = [
            ("ADB导航命令", "adb shell am start -a android.intent.action.VIEW -d 'autodroid://workflows'"),
            ("按键导航", "adb shell input keyevent KEYCODE_DPAD_RIGHT && adb shell input keyevent KEYCODE_ENTER"),
        ]
        
        for method_name, cmd in methods:
            print(f"\n方法: {method_name}")
            stdout, stderr, code = run_command(cmd)
            time.sleep(3)
            
            logs = capture_logcat("MainActivity", 200)
            
            # 检查导航拦截日志
            if "Navigation interception" in logs:
                print("✓ 导航拦截器被触发")
                
                # 检查具体拦截逻辑
                if "Server not connected" in logs:
                    print("✓ 服务器未连接，导航被正确拦截")
                if "User not authenticated" in logs:
                    print("✓ 用户未认证，导航被正确拦截")
                
                # 显示相关日志
                for line in logs.split('\n'):
                    if "Navigation interception" in line or "Server not connected" in line or "User not authenticated" in line:
                        print(f"  日志: {line}")
                
                break
            else:
                print("✗ 导航拦截器未被触发")
    else:
        print("✗ 无法确定服务器状态")

def main():
    """主测试函数"""
    print("=== 受保护页面访问流程测试 - 详细调试版本 ===")
    
    # 检查设备连接
    stdout, stderr, code = run_command("adb devices")
    if "device" not in stdout:
        print("✗ 未检测到连接的Android设备")
        return 1
    
    print("✓ Android设备已连接")
    
    # 运行测试
    setup_ok = test_navigation_setup()
    
    if setup_ok:
        print("\n✓ 导航设置正常")
        test_click_events()
        test_navigation_interception_logic()
    else:
        print("\n✗ 导航设置存在问题")
    
    # 检查应用状态
    print("\n=== 检查应用状态 ===")
    stdout, stderr, code = run_command("adb shell dumpsys activity activities | grep -A 20 com.autodroid.manager")
    if "com.autodroid.manager" in stdout:
        print("✓ 应用仍在运行")
    else:
        print("✗ 应用可能已崩溃")
    
    print("\n=== 测试完成 ===")
    return 0

if __name__ == "__main__":
    sys.exit(main())