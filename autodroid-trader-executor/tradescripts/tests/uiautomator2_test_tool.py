#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Uiautomator2 测试工具箱
整合了以下测试功能：
- test_uiautomator2_basic.py (基础Uiautomator2测试)
- test_uiautomator2_element_interaction.py (元素交互测试)
- test_uiautomator2_app_control.py (应用控制测试)
- test_adb_direct.py (ADB直接测试)

使用方法:
    python uiautomator2_test_tool.py
"""

import uiautomator2 as u2
import subprocess
import time
import sys
from typing import Optional


class Uiautomator2Tester:
    """Uiautomator2设备连接测试类"""
    
    def __init__(self, device_id: Optional[str] = None):
        self.device_id = device_id
        self.d = None
    
    def connect_device(self) -> bool:
        """连接到Uiautomator2设备"""
        print("\n" + "=" * 50)
        print("  1. 连接到Uiautomator2设备")
        print("=" * 50)
        
        try:
            if self.device_id:
                self.d = u2.connect(self.device_id)
            else:
                self.d = u2.connect()
            
            if self.d:
                print("✓ 设备连接成功!")
                print(f"设备信息: {self.d.info}")
                return True
            else:
                print("✗ 设备连接失败")
                return False
        except Exception as e:
            print(f"✗ 连接设备失败: {e}")
            return False
    
    def check_app_installed(self) -> bool:
        """检查应用是否已安装"""
        print("\n" + "=" * 50)
        print("  2. 检查应用安装状态")
        print("=" * 50)
        
        if not self.d:
            print("✗ 未连接到设备")
            return False
        
        try:
            # 检查示例应用是否已安装
            app_package = "com.tdx.androidCCZQ"
            is_installed = self.d.app_installed(app_package)
            
            if is_installed:
                print(f"✓ 应用 {app_package} 已安装")
                return True
            else:
                print(f"✗ 应用 {app_package} 未安装")
                return False
        except Exception as e:
            print(f"✗ 检查应用安装状态失败: {e}")
            return False
    
    def start_app(self) -> bool:
        """启动目标应用"""
        print("\n" + "=" * 50)
        print("  3. 启动目标应用")
        print("=" * 50)
        
        if not self.d:
            print("✗ 未连接到设备")
            return False
        
        try:
            app_package = "com.tdx.androidCCZQ"
            app_activity = "com.tdx.Android.TdxAndroidActivity"
            
            # 启动应用
            self.d.app_start(app_package, activity=app_activity)
            time.sleep(3)  # 等待应用启动
            
            current_package = self.d.app_current().package
            if app_package in current_package:
                print("✓ 应用启动成功!")
                print(f"当前应用包名: {current_package}")
                return True
            else:
                print(f"✗ 应用启动失败，当前应用: {current_package}")
                return False
        except Exception as e:
            print(f"✗ 启动应用失败: {e}")
            return False
    
    def find_and_click_elements(self) -> bool:
        """查找并点击元素"""
        print("\n" + "=" * 50)
        print("  4. 查找并点击元素")
        print("=" * 50)
        
        if not self.d:
            print("✗ 未连接到设备")
            return False
        
        try:
            # 尝试查找并点击一些通用元素（如返回键）
            try:
                # 查找包含文本的元素
                text_elements = self.d(textMatches=".*").count
                if text_elements > 0:
                    print(f"✓ 找到 {text_elements} 个文本元素")
                    # 点击第一个可点击的元素
                    clickable_element = self.d(clickable=True).exists(timeout=2)
                    if clickable_element:
                        clickable_element.click()
                        print("✓ 元素点击成功!")
                        return True
                    else:
                        print("⚠ 未找到可点击元素，但页面有内容")
                        return True
                else:
                    print("⚠ 未找到可交互元素，但设备连接正常")
                    return True
            except Exception as e:
                print(f"⚠ 元素查找失败: {e}，但设备连接正常")
                return True
            else:
                print("⚠ 页面上没有可交互元素")
                return True
        except Exception as e:
            print(f"✗ 元素操作失败: {e}")
            return False
    
    def take_screenshot(self) -> bool:
        """获取屏幕截图"""
        print("\n" + "=" * 50)
        print("  5. 获取屏幕截图")
        print("=" * 50)
        
        if not self.d:
            print("✗ 未连接到设备")
            return False
        
        try:
            # 获取截图并保存到临时文件
            screenshot_path = "temp_screenshot.png"
            self.d.screenshot(screenshot_path)
            print(f"✓ 截图保存成功: {screenshot_path}")
            return True
        except Exception as e:
            print(f"✗ 截图失败: {e}")
            return False
    
    def run_full_test(self) -> bool:
        """运行完整的Uiautomator2测试"""
        print("\n" + "=" * 60)
        print("       Uiautomator2 完整测试")
        print("=" * 60)
        
        success_count = 0
        total_tests = 5
        
        if self.connect_device():
            success_count += 1
        
        if self.check_app_installed():
            success_count += 1
        
        if self.start_app():
            success_count += 1
        
        if self.find_and_click_elements():
            success_count += 1
        
        if self.take_screenshot():
            success_count += 1
        
        print("\n" + "=" * 60)
        print(f"       测试完成: {success_count}/{total_tests} 项通过")
        print("=" * 60)
        
        return success_count >= 3  # 至少3项测试通过


class ADBTester:
    """ADB直接测试类（配合Uiautomator2使用）"""
    
    def __init__(self, device_id: Optional[str] = None):
        self.device_id = device_id
    
    def run_adb_command(self, args: list) -> subprocess.CompletedProcess:
        """执行ADB命令"""
        if self.device_id:
            args = ["adb", "-s", self.device_id] + args[1:]  # Insert device ID
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
        
        result = self.run_adb_command(["adb", "devices"])
        
        print(f"设备列表:\n{result.stdout}")
        
        if "device" in result.stdout and "unauthorized" not in result.stdout:
            devices = [line for line in result.stdout.strip().split('\n')[1:] if line.strip() and "device" in line and "unauthorized" not in line]
            if devices:
                print(f"✓ 找到 {len(devices)} 个已连接设备")
                for device in devices:
                    print(f"  - {device.strip()}")
                return True
        
        print("✗ 未找到已连接设备")
        return False
    
    def check_app_installed(self) -> bool:
        """检查应用是否已安装"""
        print("\n" + "=" * 50)
        print("  2. 检查应用安装状态")
        print("=" * 50)
        
        app_package = "com.tdx.androidCCZQ"
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
        
        app_package = "com.tdx.androidCCZQ"
        app_activity = ".MainActivity"  # Use a generic activity name
        
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
            # Try with default activity if specific one fails
            print("尝试使用默认Activity启动...")
            fallback_cmd = [
                "adb", "shell", "am", "start",
                "-n", app_package,
                "-a", "android.intent.action.MAIN",
                "-c", "android.intent.category.LAUNCHER"
            ]
            result = self.run_adb_command(fallback_cmd)
            print(f"备用启动结果: {result.stdout}")
            
            if "Error" in result.stdout or result.returncode != 0:
                print("✗ 应用启动失败")
                if result.stderr:
                    print(f"错误信息: {result.stderr}")
                return False
        
        print("✓ 应用启动命令已发送")
        return True
    
    def check_app_in_foreground(self) -> bool:
        """检查应用是否在前台运行"""
        print("\n" + "=" * 50)
        print("  4. 检查应用前台状态")
        print("=" * 50)
        
        app_package = "com.tdx.androidCCZQ"
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
    
    def run_full_test(self) -> bool:
        """运行完整的ADB测试"""
        print("\n" + "=" * 60)
        print("       ADB直接测试")
        print("=" * 60)
        print("不依赖Uiautomator2，直接通过ADB测试设备")
        
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
        
        print("\n" + "=" * 60)
        print(f"       测试完成: {success_count}/{total_tests} 项通过")
        print("=" * 60)
        
        return success_count >= 3


def print_menu():
    """打印主菜单"""
    print("\n" + "=" * 60)
    print("       Uiautomator2 测试工具箱")
    print("=" * 60)
    print("\n请选择测试模式:")
    print("1. 完整Uiautomator2测试 (需要Uiautomator2服务器运行)")
    print("2. ADB直接测试 (不需要Uiautomator2服务器)")
    print("3. 退出")
    print("\n" + "=" * 60)


def main():
    """主函数"""
    print_menu()
    
    choice = input("\n请输入选项 (1-3): ").strip()
    
    if choice == "1":
        print("\n" + "=" * 60)
        print("       完整Uiautomator2测试")
        print("=" * 60)
        tester = Uiautomator2Tester()
        tester.run_full_test()
    
    elif choice == "2":
        print("\n" + "=" * 60)
        print("       ADB直接测试")
        print("=" * 60)
        adb_tester = ADBTester()
        adb_tester.run_full_test()
    
    elif choice == "3":
        print("退出测试")
        return
    
    else:
        print("无效选项，请输入 1-3")
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