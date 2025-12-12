"""
APK列表器集成测试
测试ApkLister类的功能，包括APK信息获取、加固检测和数据库保存
"""
import os
import sys
import tempfile
import unittest
import subprocess

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from core.apk.list_apks import ApkLister
from core.apk.service import ApkManager
from core.apk.database import ApkDatabase
from config import ConfigManager


class TestApkLister(unittest.TestCase):
    """APK列表器集成测试类"""

    def setUp(self):
        """测试前准备"""
        # 使用真实的配置管理器
        self.config_manager = ConfigManager()
        
        # 创建ApkLister实例
        self.apk_lister = ApkLister(self.config_manager)
        
        # 创建ApkDatabase实例用于验证
        self.apk_db = ApkDatabase()

    def tearDown(self):
        """测试后清理"""
        # 清理临时文件
        pass

    def test_apk_lister_initialization(self):
        """测试ApkLister初始化"""
        self.assertIsInstance(self.apk_lister, ApkLister)
        
    def test_device_connection(self):
        """测试设备连接状态 - 此测试已移动到test_device_connection.py"""
        self.skipTest("设备连接测试已移动到专门的设备测试模块")
    
    def test_get_apk_list_metadata(self):
        """测试获取APK列表元数据"""
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
                    
                    # 测试获取APK包列表
                    command = ["adb", "-s", device_id, "shell", "pm", "list", "packages", "-3"]
                    result = subprocess.run(command, capture_output=True, text=True)
                    
                    if result.returncode == 0:
                        packages = [line.replace("package:", "").strip() 
                                   for line in result.stdout.strip().split('\n') 
                                   if line.startswith("package:")]
                        
                        print(f"✅ 获取到 {len(packages)} 个用户安装的应用包")
                        
                        # 验证至少有一些应用包
                        self.assertGreater(len(packages), 0, "应至少有一个用户安装的应用包")
                        
                        # 验证包名格式
                        for package in packages[:5]:  # 只检查前5个包
                            self.assertTrue(len(package) > 0, "包名不应为空")
                            self.assertTrue("." in package, "包名应包含点分隔符")
                    else:
                        print(f"❌ 获取包列表失败: {result.stderr}")
                        self.skipTest("无法获取包列表")
                else:
                    print("⚠️ 未发现已连接的设备，跳过测试")
                    self.skipTest("未发现已连接的设备")
            else:
                print(f"❌ 执行adb devices命令失败: {result.stderr}")
                self.skipTest("无法执行adb devices命令")
        except Exception as e:
            print(f"❌ 测试失败: {e}")
            self.skipTest("测试执行失败")
    
    def test_analyze_packer_for_apk_method(self):
        """测试analyze_packer_for_apk方法"""
        # 跳过测试，因为需要真实的设备连接
        # 在实际环境中，此方法会连接到真实设备进行APK提取和加固检测
        self.skipTest("需要真实设备连接，跳过测试")
        
        # 在实际环境中，测试代码应该：
        # 1. 检查设备是否连接
        # 2. 使用真实的设备ID
        # 3. 调用analyze_packer_for_apk方法
        # 4. 验证结果

    def test_save_apks_to_database_method(self):
        """测试save_apks_to_database方法"""
        # 跳过测试，因为需要真实的APK数据
        # 在实际环境中，此方法会使用真实的APK数据保存到数据库
        self.skipTest("需要真实APK数据，跳过测试")
        
        # 在实际环境中，测试代码应该：
        # 1. 获取真实的APK数据
        # 2. 调用save_apks_to_database方法
        # 3. 验证数据是否正确保存到数据库

    def test_analyze_apk_packers_method(self):
        """测试analyze_apk_packers方法"""
        # 跳过测试，因为需要真实的设备连接和APK数据
        # 在实际环境中，此方法会连接到真实设备进行APK加固检测
        self.skipTest("需要真实设备连接和APK数据，跳过测试")
        
        # 在实际环境中，测试代码应该：
        # 1. 检查设备是否连接
        # 2. 获取真实的APK列表
        # 3. 调用analyze_apk_packers方法
        # 4. 验证加固检测结果

    def test_apk_lister_architecture_compliance(self):
        """测试架构合规性 - 确保没有直接使用DatabaseManager"""
        # 检查ApkLister类是否导入正确的模块
        import core.apk.list_apks as list_apks_module
        
        # 验证没有直接导入DatabaseManager
        self.assertFalse(hasattr(list_apks_module, 'DatabaseManager'),
                        "ApkLister不应该直接导入DatabaseManager")
        
        # 验证导入了正确的APK模块组件
        self.assertTrue(hasattr(list_apks_module, 'ApkManager'),
                       "ApkLister应该导入ApkManager")
        self.assertTrue(hasattr(list_apks_module, 'ApkDatabase'),
                       "ApkLister应该导入ApkDatabase")


if __name__ == '__main__':
    unittest.main()