import requests
import json
import time

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
            "appium:appActivity": "com.tdx.Android.TdxAndroidActivity",
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
        "appium:appActivity": "com.tdx.Android.TdxAndroidActivity",
        "appium:noReset": False,
        "appium:autoGrantPermissions": True,
        "appium:skipServerInstallation": True,
        "appium:remoteAppsCacheLimit": 0,
        "appium:dontStopAppOnReset": True
    }
}

def test_appium_capabilities():
    print("=== 开始测试Appium UIA2 Server ===")
    
    # 1. 检查服务器状态
    print("\n1. 检查Appium服务器状态...")
    status_url = f"{base_url}/wd/hub/status"
    try:
        response = requests.get(status_url, timeout=10)
        print(f"服务器状态响应: {response.status_code}")
        if response.status_code == 200:
            print("服务器状态正常")
            print(f"响应内容: {response.text}")
        else:
            print(f"服务器状态异常: {response.text}")
            return
    except Exception as e:
        print(f"检查服务器状态失败: {e}")
        return
    
    # 2. 创建会话
    print("\n2. 尝试创建Appium会话...")
    session_url = f"{base_url}/wd/hub/session"
    try:
        response = requests.post(session_url, json=capabilities, timeout=30)
        print(f"创建会话响应: {response.status_code}")
        
        if response.status_code == 200:
            session_data = response.json()
            session_id = session_data["value"]["sessionId"]
            print(f"会话创建成功! Session ID: {session_id}")
            print(f"响应详情: {json.dumps(session_data, indent=2, ensure_ascii=False)}")
            
            # 3. 发送命令启动应用
            print("\n3. 发送命令启动CQZQ应用...")
            
            # 方法1: 使用startActivity命令
            activity_url = f"{base_url}/wd/hub/session/{session_id}/appium/device/start_activity"
            activity_data = {
                "appPackage": "com.tdx.androidCCZQ",
                "appActivity": "com.tdx.Android.TdxAndroidActivity"
            }
            
            print("尝试启动Activity...")
            response = requests.post(activity_url, json=activity_data, timeout=10)
            print(f"启动Activity响应: {response.status_code}")
            if response.status_code == 200:
                print("Activity启动成功!")
            else:
                print(f"启动Activity失败: {response.text}")
            
            # 等待应用启动
            time.sleep(3)
            
            # 方法2: 尝试查找元素并点击（如果应用已启动）
            print("\n4. 尝试查找应用元素...")
            elements_url = f"{base_url}/wd/hub/session/{session_id}/elements"
            
            # 使用XPath查找可能的元素
            element_data = {
                "using": "xpath",
                "value": "//*[@text]"
            }
            
            response = requests.post(elements_url, json=element_data, timeout=10)
            print(f"查找元素响应: {response.status_code}")
            if response.status_code == 200:
                elements = response.json()["value"]
                print(f"找到 {len(elements)} 个元素")
                
                # 尝试点击第一个元素
                if len(elements) > 0:
                    element_id = elements[0]["ELEMENT"]
                    click_url = f"{base_url}/wd/hub/session/{session_id}/element/{element_id}/click"
                    response = requests.post(click_url, json={}, timeout=10)
                    print(f"点击元素响应: {response.status_code}")
                    if response.status_code == 200:
                        print("点击成功!")
            
            # 4. 获取当前应用信息
            print("\n5. 获取当前应用信息...")
            current_app_url = f"{base_url}/wd/hub/session/{session_id}/appium/device/current_package"
            response = requests.get(current_app_url, timeout=10)
            print(f"当前应用包名: {response.text}")
            
            # 5. 获取屏幕截图
            print("\n6. 获取屏幕截图...")
            screenshot_url = f"{base_url}/wd/hub/session/{session_id}/screenshot"
            response = requests.get(screenshot_url, timeout=10)
            if response.status_code == 200:
                print("截图获取成功!")
                screenshot_data = response.json()["value"]
                print(f"截图数据长度: {len(screenshot_data)}")
            
            # 6. 关闭会话
            print("\n7. 关闭会话...")
            delete_url = f"{base_url}/wd/hub/session/{session_id}"
            response = requests.delete(delete_url, timeout=10)
            print(f"关闭会话响应: {response.status_code}")
            
        else:
            print(f"创建会话失败: {response.text}")
            
    except Exception as e:
        print(f"创建会话过程中出错: {e}")

if __name__ == "__main__":
    test_appium_capabilities()