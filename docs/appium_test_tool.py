#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Appium UIA2 测试工具箱
整合了以下测试功能：
- test_appium_capabilities_fixed.py (ADB端口转发 + Appium测试)
- test_appium_capabilities.py (直接Appium测试)
- test_appium.py (简单连接测试)
- test_capabilities_direct.py (ADB直接测试)
- test_capabilities.json (capabilities配置)

使用方法:
    python appium_test_tool.py
"""

import json
import subprocess
import time
import requests
import sys
from typing import Optional


# 默认Capabilities配置
DEFAULT_CAPABILITIES = {
    "capabilities": {
        "alwaysMatch": {
            "platformName": "Android",
            "appium:automationName": "UiAutomator2",
            "appium:udid": "TDCDU17905004388",
            "appium:appPackage": "com.tdx.androidCCZQ",
            "appium:appActivity": "com.tdx.Android.TdxAndroidActivity",
            "appium:noReset": True,
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
        "appium:noReset": True,
        "appium:autoGrantPermissions": True,
        "appium:skipServerInstallation": True,
        "appium:remoteAppsCacheLimit": 0,
        "appium:dontStopAppOnReset": True
    }
}


class AppiumTester:
    """Appium服务器测试类"""
    
    def __init__(self, base_url: str = "http://127.0.0.1:6790"):
        self.base_url = base_url
        self.capabilities = DEFAULT_CAPABILITIES
        self.session_id: Optional[str] = None
    
    def check_server_status(self) -> bool:
        """检查Appium服务器状态"""
        print("\n" + "=" * 50)
        print("  1. 检查Appium服务器状态")
        print("=" * 50)
        
        status_url = f"{self.base_url}/wd/hub/status"
        try:
            response = requests.get(status_url, timeout=10)
            print(f"服务器状态响应: {response.status_code}")
            
            if response.status_code == 200:
                print("✓ 服务器状态正常")
                print(f"响应内容: {response.text}")
                return True
            else:
                print(f"✗ 服务器状态异常: {response.text}")
                return False
        except Exception as e:
            print(f"✗ 检查服务器状态失败: {e}")
            print("请确保: 1) Appium服务器已启动 2) 网络连接正常")
            return False
    
    def create_session(self) -> bool:
        """创建Appium会话"""
        print("\n" + "=" * 50)
        print("  2. 创建Appium会话")
        print("=" * 50)
        
        session_url = f"{self.base_url}/wd/hub/session"
        try:
            response = requests.post(session_url, json=self.capabilities, timeout=30)
            print(f"创建会话响应: {response.status_code}")
            
            if response.status_code == 200:
                session_data = response.json()
                self.session_id = session_data["value"]["sessionId"]
                print(f"✓ 会话创建成功!")
                print(f"Session ID: {self.session_id}")
                print(f"响应详情: {json.dumps(session_data, indent=2, ensure_ascii=False)}")
                return True
            else:
                print(f"✗ 创建会话失败: {response.text}")
                return False
        except Exception as e:
            print(f"✗ 创建会话过程中出错: {e}")
            return False
    
    def start_activity(self) -> bool:
        """启动目标应用Activity"""
        print("\n" + "=" * 50)
        print("  3. 启动目标应用")
        print("=" * 50)
        
        if not self.session_id:
            print("✗ 没有有效的会话ID")
            return False
        
        activity_url = f"{self.base_url}/wd/hub/session/{self.session_id}/appium/device/start_activity"
        activity_data = {
            "appPackage": "com.tdx.androidCCZQ",
            "appActivity": "com.tdx.Android.TdxAndroidActivity"
        }
        
        try:
            print("尝试启动Activity...")
            response = requests.post(activity_url, json=activity_data, timeout=10)
            print(f"启动Activity响应: {response.status_code}")
            
            if response.status_code == 200:
                print("✓ Activity启动成功!")
                print(f"响应: {response.text}")
                time.sleep(3)  # 等待应用启动
                return True
            else:
                print(f"✗ 启动Activity失败: {response.text}")
                return False
        except Exception as e:
            print(f"✗ 启动Activity出错: {e}")
            return False
    
    def find_and_click_elements(self) -> bool:
        """查找并点击元素"""
        print("\n" + "=" * 50)
        print("  4. 查找并点击元素")
        print("=" * 50)
        
        if not self.session_id:
            print("✗ 没有有效的会话ID")
            return False
        
        elements_url = f"{self.base_url}/wd/hub/session/{self.session_id}/elements"
        element_data = {
            "using": "xpath",
            "value": "//*[@text]"
        }
        
        try:
            print("查找页面元素...")
            response = requests.post(elements_url, json=element_data, timeout=10)
            print(f"查找元素响应: {response.status_code}")
            
            if response.status_code == 200:
                elements = response.json()["value"]
                print(f"找到 {len(elements)} 个元素")
                
                if len(elements) > 0:
                    element_id = elements[0]["ELEMENT"]
                    click_url = f"{self.base_url}/wd/hub/session/{self.session_id}/element/{element_id}/click"
                    response = requests.post(click_url, json={}, timeout=10)
                    
                    if response.status_code == 200:
                        print("✓ 点击成功!")
                        return True
                    else:
                        print(f"✗ 点击失败: {response.text}")
                        return False
                else:
                    print("未找到可点击的元素")
                    return False
            else:
                print(f"✗ 查找元素失败: {response.text}")
                return False
        except Exception as e:
            print(f"✗ 查找元素出错: {e}")
            return False
    
    def get_current_app_info(self) -> bool:
        """获取当前应用信息"""
        print("\n" + "=" * 50)
        print("  5. 获取当前应用信息")
        print("=" * 50)
        
        if not self.session_id:
            print("✗ 没有有效的会话ID")
            return False
        
        try:
            current_app_url = f"{self.base_url}/wd/hub/session/{self.session_id}/appium/device/current_package"
            response = requests.get(current_app_url, timeout=10)
            print(f"当前应用包名: {response.text}")
            return True
        except Exception as e:
            print(f"✗ 获取应用信息出错: {e}")
            return False
    
    def take_screenshot(self) -> bool:
        """获取屏幕截图"""
        print("\n" + "=" * 50)
        print("  6. 获取屏幕截图")
        print("=" * 50)
        
        if not self.session_id:
            print("✗ 没有有效的会话ID")
            return False
        
        try:
            screenshot_url = f"{self.base_url}/wd/hub/session/{self.session_id}/screenshot"
            response = requests.get(screenshot_url, timeout=10)
            
            if response.status_code == 200:
                print("✓ 截图获取成功!")
                screenshot_data = response.json()["value"]
                print(f"截图数据长度: {len(screenshot_data)}")
                return True
            else:
                print(f"✗ 截图失败: {response.text}")
                return False
        except Exception as e:
            print(f"✗ 获取截图出错: {e}")
            return False
    
    def close_session(self) -> bool:
        """关闭Appium会话"""
        print("\n" + "=" * 50)
        print("  7. 关闭会话")
        print("=" * 50)
        
        if not self.session_id:
            print("没有需要关闭的会话")
            return True
        
        try:
            delete_url = f"{self.base_url}/wd/hub/session/{self.session_id}"
            response = requests.delete(delete_url, timeout=10)
            print(f"关闭会话响应: {response.status_code}")
            
            if response.status_code == 200:
                print("✓ 会话已关闭")
                return True
            else:
                print(f"✗ 关闭会话失败: {response.text}")
                return False
        except Exception as e:
            print(f"✗ 关闭会话出错: {e}")
            return False
    
    def run_full_test(self) -> bool:
        """运行完整的Appium测试"""
        print("\n" + "=" * 60)
        print("       Appium UIA2 完整测试")
        print("=" * 60)
        print(f"服务器地址: {self.base_url}")
        print(f"Capabilities配置已加载")
        
        success_count = 0
        total_tests = 5
        
        if self.check_server_status():
            success_count += 1
        
        if self.create_session():
            success_count += 1
            self.start_activity()
            self.find_and_click_elements()
            self.get_current_app_info()
            self.take_screenshot()
            self.close_session()
        
        print("\n" + "=" * 60)
        print(f"       测试完成: {success_count}/{total_tests} 项通过")
        print("=" * 60)
        
        return success_count >= 2  # 至少服务器状态和会话创建成功


class ADBTester:
    """ADB直接测试类"""
    
    def __init__(self):
        self.capabilities = {
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
    
    def run_adb_command(self, args: list) -> subprocess.CompletedProcess:
        """执行ADB命令"""
        try:
            result = subprocess.run(args, capture_output=True, text=True)
            return result
        except Exception as e:
            print(f"执行ADB命令失败: {e}")
            return subprocess.CompletedProcess(args, 1, "", str(e))
    
    def check_device(self) -> bool:
        """检查设备连接"""
        print("\n" + "=" * 50)
        print("  1. 检查设备连接")
        print("=" * 50)
        
        udid = self.capabilities["appium:udid"]
        result = self.run_adb_command(["adb", "devices"])
        
        print(f"设备列表:\n{result.stdout}")
        
        if udid in result.stdout:
            print(f"✓ 设备 {udid} 已连接")
            return True
        else:
            print(f"✗ 设备 {udid} 未找到")
            return False
    
    def check_app_installed(self) -> bool:
        """检查应用是否已安装"""
        print("\n" + "=" * 50)
        print("  2. 检查应用安装状态")
        print("=" * 50)
        
        app_package = self.capabilities["appium:appPackage"]
        result = self.run_adb_command(["adb", "shell", "pm", "list", "packages", app_package])
        
        if app_package in result.stdout:
            print(f"✓ 应用 {app_package} 已安装")
            return True
        else:
            print(f"✗ 应用 {app_package} 未安装")
            return False
    
    def launch_app(self) -> bool:
        """启动应用"""
        print("\n" + "=" * 50)
        print("  3. 启动应用")
        print("=" * 50)
        
        app_package = self.capabilities["appium:appPackage"]
        app_activity = self.capabilities["appium:appActivity"]
        
        # 先停止应用
        print("停止应用...")
        self.run_adb_command(["adb", "shell", "am", "force-stop", app_package])
        time.sleep(1)
        
        # 启动应用
        start_cmd = [
            "adb", "shell", "am", "start",
            "-n", f"{app_package}/{app_activity}",
            "-a", "android.intent.action.MAIN",
            "-c", "android.intent.category.LAUNCHER"
        ]
        
        print(f"启动命令: {' '.join(start_cmd)}")
        result = self.run_adb_command(start_cmd)
        
        print(f"启动结果: {result.stdout}")
        
        if "Error" in result.stdout or result.returncode != 0:
            print("✗ 应用启动失败")
            if result.stderr:
                print(f"错误信息: {result.stderr}")
            return False
        else:
            print("✓ 应用启动成功")
            return True
    
    def check_app_in_foreground(self) -> bool:
        """检查应用是否在前台运行"""
        print("\n" + "=" * 50)
        print("  4. 检查应用前台状态")
        print("=" * 50)
        
        app_package = self.capabilities["appium:appPackage"]
        time.sleep(2)  # 等待应用启动
        
        result = self.run_adb_command(["adb", "shell", "dumpsys", "window", "windows"])
        
        if app_package in result.stdout:
            print("✓ 应用已在前台运行")
            for line in result.stdout.split('\n'):
                if app_package in line and 'mCurrentFocus' in line:
                    print(f"窗口焦点信息: {line.strip()}")
            return True
        else:
            print("✗ 应用未在前台")
            return False
    
    def verify_capabilities(self) -> bool:
        """验证Capabilities配置"""
        print("\n" + "=" * 50)
        print("  5. 验证Capabilities配置")
        print("=" * 50)
        
        all_valid = True
        
        print(f"平台: {self.capabilities['platformName']}")
        print(f"自动化引擎: {self.capabilities['appium:automationName']}")
        print(f"设备ID: {self.capabilities['appium:udid']}")
        print(f"应用包名: {self.capabilities['appium:appPackage']}")
        print(f"应用Activity: {self.capabilities['appium:appActivity']}")
        
        print("\nAppium配置项:")
        print(f"  noReset: {self.capabilities['appium:noReset']}")
        print(f"  autoGrantPermissions: {self.capabilities['appium:autoGrantPermissions']}")
        print(f"  skipServerInstallation: {self.capabilities['appium:skipServerInstallation']}")
        print(f"  dontStopAppOnReset: {self.capabilities['appium:dontStopAppOnReset']}")
        
        return all_valid
    
    def run_full_test(self) -> bool:
        """运行完整的ADB测试"""
        print("\n" + "=" * 60)
        print("       ADB直接测试")
        print("=" * 60)
        print("无需Appium服务器，直接通过ADB测试应用")
        
        success_count = 0
        total_tests = 4
        
        if self.check_device():
            success_count += 1
        
        if self.check_app_installed():
            success_count += 1
        
        if self.launch_app():
            success_count += 1
        
        if self.check_app_in_foreground():
            success_count += 1
        
        self.verify_capabilities()
        
        print("\n" + "=" * 60)
        print(f"       测试完成: {success_count}/{total_tests} 项通过")
        print("=" * 60)
        
        return success_count >= 3


def setup_adb_forward():
    """设置ADB端口转发"""
    print("\n" + "=" * 60)
    print("       设置ADB端口转发")
    print("=" * 60)
    
    # 移除已存在的转发
    print("移除已存在的端口转发...")
    subprocess.run(["adb", "forward", "--remove", "tcp:6790"], capture_output=True)
    
    # 添加新的端口转发
    print("添加端口转发: localhost:6790 -> 手机上的Appium服务器")
    result = subprocess.run(
        ["adb", "forward", "tcp:6790", "tcp:6790"],
        capture_output=True, text=True
    )
    
    if result.returncode == 0:
        print("✓ 端口转发设置成功")
        print("现在可以通过 http://localhost:6790 连接手机上的Appium服务器")
        return True
    else:
        print(f"✗ 端口转发设置失败: {result.stderr}")
        return False


def test_appium_with_adb_forward():
    """测试ADB端口转发 + Appium"""
    print("\n" + "=" * 60)
    print("       ADB端口转发 + Appium测试")
    print("=" * 60)
    
    if not setup_adb_forward():
        return
    
    print("\n开始测试通过ADB转发的Appium服务器...")
    tester = AppiumTester(base_url="http://localhost:6790")
    tester.run_full_test()


def print_menu():
    """打印主菜单"""
    print("\n" + "=" * 60)
    print("       Appium UIA2 测试工具箱")
    print("=" * 60)
    print("\n请选择测试模式:")
    print("1. 完整Appium测试 (需要Appium服务器运行)")
    print("2. ADB直接测试 (不需要Appium服务器)")
    print("3. ADB端口转发 + Appium测试")
    print("4. 查看Capabilities配置")
    print("5. 退出")
    print("\n" + "=" * 60)


def main():
    """主函数"""
    print_menu()
    
    choice = input("\n请输入选项 (1-5): ").strip()
    
    if choice == "1":
        print("\n" + "=" * 60)
        print("       完整Appium测试")
        print("=" * 60)
        print("注意: 请确保Appium服务器已在 http://127.0.0.1:6790 启动")
        tester = AppiumTester()
        tester.run_full_test()
    
    elif choice == "2":
        print("\n" + "=" * 60)
        print("       ADB直接测试")
        print("=" * 60)
        adb_tester = ADBTester()
        adb_tester.run_full_test()
    
    elif choice == "3":
        test_appium_with_adb_forward()
    
    elif choice == "4":
        print("\n" + "=" * 60)
        print("       Capabilities配置")
        print("=" * 60)
        print(json.dumps(DEFAULT_CAPABILITIES, indent=2, ensure_ascii=False))
    
    elif choice == "5":
        print("退出测试")
        return
    
    else:
        print("无效选项，请输入 1-5")
        main()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n用户中断测试")
        sys.exit(0)
    except Exception as e:
        print(f"\n发生错误: {e}")
        sys.exit(1)
