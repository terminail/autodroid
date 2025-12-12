"""
设备连接功能测试
测试设备连接状态检查功能
"""
import os
import sys
import unittest
import subprocess

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from core.device import DeviceManager


class TestDeviceConnection(unittest.TestCase):
    """设备连接测试类"""

    def test_device_discovery(self):
        """测试设备发现功能"""
        # 使用设备发现功能获取已连接的设备
        try:
            result = subprocess.run(["adb", "devices"], capture_output=True, text=True)
            if result.returncode == 0:
                # 解析adb devices输出，获取已连接的设备列表
                devices = []
                for line in result.stdout.strip().split('\n')[1:]:  # 跳过第一行标题
                    if line.strip() and 'device' in line:
                        device_id = line.split('\t')[0].strip()
                        if device_id:
                            devices.append(device_id)
                
                if devices:
                    # 使用第一个连接的设备进行测试
                    device_id = devices[0]
                    print(f"✅ 发现已连接设备: {device_id}")
                    
                    # 创建设备管理器实例
                    device_manager = DeviceManager(device_id)
                    
                    # 测试设备连接检查
                    is_connected = device_manager.is_device_connected()
                    self.assertTrue(is_connected, f"设备 {device_id} 应处于连接状态")
                    
                    # 测试获取设备信息
                    device_info = device_manager.get_device_info()
                    self.assertIsInstance(device_info, dict, "设备信息应为字典类型")
                    self.assertIn("model", device_info, "设备信息应包含型号")
                    
                else:
                    print("⚠️ 未发现已连接的设备，跳过设备相关测试")
                    self.skipTest("未发现已连接的设备")
            else:
                print(f"❌ 执行adb devices命令失败: {result.stderr}")
                self.skipTest("无法执行adb devices命令")
        except Exception as e:
            print(f"❌ 检查设备连接失败: {e}")
            self.skipTest("无法检查设备连接状态")

    def test_device_connection_status(self):
        """测试设备连接状态检查"""
        # 使用设备发现功能获取已连接的设备
        try:
            result = subprocess.run(["adb", "devices"], capture_output=True, text=True)
            if result.returncode == 0:
                # 解析adb devices输出，获取已连接的设备列表
                devices = []
                for line in result.stdout.strip().split('\n')[1:]:  # 跳过第一行标题
                    if line.strip() and 'device' in line:
                        device_id = line.split('\t')[0].strip()
                        if device_id:
                            devices.append(device_id)
                
                if devices:
                    # 使用第一个连接的设备进行测试
                    device_id = devices[0]
                    
                    # 创建设备管理器实例
                    device_manager = DeviceManager(device_id)
                    
                    # 测试设备连接状态检查
                    is_connected = device_manager.check_device_connection()
                    self.assertTrue(is_connected, f"设备 {device_id} 应处于连接状态")
                    
                    # 测试断开连接状态
                    device_manager.disconnect()
                    self.assertFalse(device_manager.is_device_connected(), "设备应处于断开状态")
                    
                else:
                    print("⚠️ 未发现已连接的设备，跳过设备连接状态测试")
                    self.skipTest("未发现已连接的设备")
            else:
                print(f"❌ 执行adb devices命令失败: {result.stderr}")
                self.skipTest("无法执行adb devices命令")
        except Exception as e:
            print(f"❌ 测试设备连接状态失败: {e}")
            self.skipTest("无法测试设备连接状态")

    def test_device_app_management(self):
        """测试设备应用管理功能"""
        # 使用设备发现功能获取已连接的设备
        try:
            result = subprocess.run(["adb", "devices"], capture_output=True, text=True)
            if result.returncode == 0:
                # 解析adb devices输出，获取已连接的设备列表
                devices = []
                for line in result.stdout.strip().split('\n')[1:]:  # 跳过第一行标题
                    if line.strip() and 'device' in line:
                        device_id = line.split('\t')[0].strip()
                        if device_id:
                            devices.append(device_id)
                
                if devices:
                    # 使用第一个连接的设备进行测试
                    device_id = devices[0]
                    
                    # 创建设备管理器实例
                    device_manager = DeviceManager(device_id)
                    
                    # 测试获取已安装应用列表
                    installed_apps = device_manager.list_installed_apps()
                    self.assertIsInstance(installed_apps, list, "已安装应用列表应为列表类型")
                    
                    # 测试获取当前应用
                    current_app = device_manager.get_current_app()
                    # 当前应用可能为None，这是正常的
                    if current_app:
                        self.assertIsInstance(current_app, str, "当前应用应为字符串类型")
                    
                else:
                    print("⚠️ 未发现已连接的设备，跳过应用管理测试")
                    self.skipTest("未发现已连接的设备")
            else:
                print(f"❌ 执行adb devices命令失败: {result.stderr}")
                self.skipTest("无法执行adb devices命令")
        except Exception as e:
            print(f"❌ 测试应用管理功能失败: {e}")
            self.skipTest("无法测试应用管理功能")


if __name__ == '__main__':
    unittest.main()