import requests
import json

# Appium服务器地址
base_url = "http://127.0.0.1:6790"

# 测试capabilities
capabilities = {
    "capabilities": {
        "alwaysMatch": {
            "platformName": "Android",
            "appium:automationName": "UiAutomator2",
            "appium:udid": "TDCDU17905004388",
            "appium:appPackage": "com.tdx.androidCCZQ",
            "appium:noReset": False,
            "appium:autoGrantPermissions": True,
            "appium:skipServerInstallation": True,
            "appium:remoteAppsCacheLimit": 0,
            "appium:dontStopAppOnReset": True
        }
    },
    "desiredCapabilities": {
        "platformName": "Android",
        "appium:automationName": "UiAutomator2",
        "appium:udid": "TDCDU17905004388",
        "appium:appPackage": "com.tdx.androidCCZQ",
        "appium:noReset": False,
        "appium:autoGrantPermissions": True,
        "appium:skipServerInstallation": True,
        "appium:remoteAppsCacheLimit": 0,
        "appium:dontStopAppOnReset": True
    }
}

print("测试Appium服务器连接...")
print(f"服务器地址: {base_url}")
print(f"Capabilities: {json.dumps(capabilities, indent=2)}")

try:
    # 首先检查服务器状态
    status_url = f"{base_url}/wd/hub/status"
    print(f"\n检查服务器状态: {status_url}")
    
    response = requests.get(status_url, timeout=10)
    print(f"状态响应: {response.status_code}")
    if response.status_code == 200:
        print("服务器状态正常")
        print(f"响应内容: {response.text}")
    else:
        print(f"服务器状态异常: {response.text}")
        
except Exception as e:
    print(f"检查服务器状态失败: {e}")

try:
    # 尝试创建会话
    session_url = f"{base_url}/wd/hub/session"
    print(f"\n尝试创建会话: {session_url}")
    
    response = requests.post(session_url, json=capabilities, timeout=30)
    print(f"会话创建响应: {response.status_code}")
    
    if response.status_code == 200:
        print("会话创建成功！")
        result = response.json()
        print(f"会话ID: {result.get('value', {}).get('sessionId', '未知')}")
        print(f"完整响应: {json.dumps(result, indent=2)}")
    else:
        print(f"会话创建失败: {response.text}")
        
except Exception as e:
    print(f"创建会话失败: {e}")