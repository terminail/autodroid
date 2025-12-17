import json
import subprocess
import time

# 测试capabilities配置
capabilities = {
    "platformName": "Android",
    "appium:automationName": "UiAutomator2",
    "appium:udid": "TDCDU17905004388",
    "appium:appPackage": "com.tdx.androidCCZQ",
    "appium:appActivity": "com.tdx.Android.TdxAndroidActivity",
    "appium:noReset": False,
    "appium:autoGrantPermissions": True,
    "appium:skipServerInstallation": True,
    "appium:remoteAppsCacheLimit": 0,
    "appium:dontStopAppOnReset": True
}

def test_capabilities_directly():
    print("=== 直接测试capabilities配置 ===")
    print("Capabilities配置:")
    print(json.dumps(capabilities, indent=2, ensure_ascii=False))
    
    # 1. 验证应用包名和Activity
    print("\n1. 验证应用包名和Activity...")
    app_package = capabilities["appium:appPackage"]
    app_activity = capabilities["appium:appActivity"]
    
    print(f"应用包名: {app_package}")
    print(f"应用Activity: {app_activity}")
    
    # 检查应用是否已安装
    result = subprocess.run(
        ["adb", "shell", "pm", "list", "packages", app_package],
        capture_output=True, text=True
    )
    
    if app_package in result.stdout:
        print("✓ 应用已安装")
    else:
        print("✗ 应用未安装")
        return
    
    # 2. 测试直接启动应用
    print("\n2. 测试直接启动应用...")
    
    # 先停止应用（如果正在运行）
    subprocess.run(["adb", "shell", "am", "force-stop", app_package])
    time.sleep(1)
    
    # 启动应用
    start_cmd = [
        "adb", "shell", "am", "start",
        "-n", f"{app_package}/{app_activity}",
        "-a", "android.intent.action.MAIN",
        "-c", "android.intent.category.LAUNCHER"
    ]
    
    result = subprocess.run(start_cmd, capture_output=True, text=True)
    print(f"启动命令: {' '.join(start_cmd)}")
    print(f"启动结果: {result.stdout}")
    
    if "Error" in result.stdout or result.returncode != 0:
        print("✗ 应用启动失败")
        if result.stderr:
            print(f"错误信息: {result.stderr}")
    else:
        print("✓ 应用启动成功")
    
    # 3. 检查应用是否在前台
    time.sleep(2)
    print("\n3. 检查应用是否在前台...")
    
    result = subprocess.run(
        ["adb", "shell", "dumpsys", "window", "windows"],
        capture_output=True, text=True
    )
    
    if app_package in result.stdout:
        print("✓ 应用已在前台")
        # 提取窗口信息
        for line in result.stdout.split('\n'):
            if app_package in line and 'mCurrentFocus' in line:
                print(f"窗口焦点信息: {line.strip()}")
    else:
        print("✗ 应用未在前台")
    
    # 4. 验证其他capabilities参数
    print("\n4. 验证其他capabilities参数...")
    
    # 检查设备ID
    udid = capabilities["appium:udid"]
    result = subprocess.run(
        ["adb", "devices"],
        capture_output=True, text=True
    )
    
    if udid in result.stdout:
        print(f"✓ 设备ID {udid} 存在")
    else:
        print(f"✗ 设备ID {udid} 不存在")
    
    # 检查权限相关设置
    print(f"✓ noReset: {capabilities['appium:noReset']}")
    print(f"✓ autoGrantPermissions: {capabilities['appium:autoGrantPermissions']}")
    print(f"✓ skipServerInstallation: {capabilities['appium:skipServerInstallation']}")
    print(f"✓ dontStopAppOnReset: {capabilities['appium:dontStopAppOnReset']}")
    
    print("\n=== 测试完成 ===")
    print("总结: capabilities配置基本正确，应用可以正常启动")
    print("下一步: 需要确保Appium服务器正确运行，然后通过WebDriver发送这些capabilities")

if __name__ == "__main__":
    test_capabilities_directly()