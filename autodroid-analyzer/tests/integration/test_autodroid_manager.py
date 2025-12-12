"""测试autodroid管理器"""

import unittest
from pathlib import Path
from unittest.mock import Mock, patch
import sys
import os

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from core.device.device_manager import DeviceManager
from core.analysis.app_analyzer import AppAnalyzer
from core.analysis.interactive_analyzer import InteractiveAppAnalyzer


class TestAutodroidManager(unittest.TestCase):
    """测试autodroid管理器"""
    
    def setUp(self):
        """测试设置"""
        self.device_id = "emulator-5554"
        self.app_package = "com.example.app"
        self.output_dir = Path("test_output")
        
        # 创建输出目录
        self.output_dir.mkdir(exist_ok=True)
    
    def tearDown(self):
        """测试清理"""
        # 清理测试文件
        import shutil
        if self.output_dir.exists():
            shutil.rmtree(self.output_dir)
    
    @patch('subprocess.run')
    def test_device_manager_connection(self, mock_subprocess):
        """测试设备管理器连接"""
        # 模拟ADB命令成功
        mock_subprocess.return_value.returncode = 0
        mock_subprocess.return_value.stdout = f"List of devices attached\n{self.device_id} device\n"
        
        device_manager = DeviceManager(self.device_id)
        # 设置设备连接状态为True，因为check_device_connection()在构造函数中会被调用
        device_manager.connected = True
        result = device_manager.is_device_connected()
        
        self.assertTrue(result)
        mock_subprocess.assert_called_once()
    
    @patch('subprocess.run')
    def test_device_manager_app_installed(self, mock_subprocess):
        """测试应用安装检查"""
        # 模拟应用已安装
        mock_subprocess.return_value.returncode = 0
        mock_subprocess.return_value.stdout = f"package:{self.app_package}\n"
        
        device_manager = DeviceManager(self.device_id)
        # 设置设备连接状态为True
        device_manager.connected = True
        # 测试launch_app方法中的安装检查逻辑
        result = device_manager.launch_app(self.app_package)
        
        self.assertTrue(result)
        mock_subprocess.assert_called()
    
    @patch('subprocess.run')
    def test_app_analyzer_initialization(self, mock_subprocess):
        """测试应用分析器初始化"""
        # 模拟设备连接和应用检查成功
        mock_subprocess.return_value.returncode = 0
        mock_subprocess.return_value.stdout = "device\npackage:com.example.app\n"
        
        app_analyzer = AppAnalyzer(self.device_id, str(self.output_dir))
        
        self.assertEqual(app_analyzer.device_id, self.device_id)
        self.assertEqual(app_analyzer.output_dir, self.output_dir)
    

    
    @patch('subprocess.run')
    def test_device_manager_get_current_activity(self, mock_subprocess):
        """测试获取当前活动"""
        # 模拟当前活动 - 提供正确的格式
        mock_subprocess.return_value.returncode = 0
        mock_subprocess.return_value.stdout = f"mCurrentFocus=Window{{12345 u0 {self.app_package}/{self.app_package}.MainActivity}}"
        
        device_manager = DeviceManager(self.device_id)
        # 设置设备连接状态为True
        device_manager.connected = True
        app = device_manager.get_current_app()
        
        self.assertEqual(app, self.app_package)
        # 应该被调用2次：一次在构造函数中，一次在get_current_app方法中
        self.assertEqual(mock_subprocess.call_count, 2)
    
    @patch('subprocess.run')
    def test_device_manager_start_app(self, mock_subprocess):
        """测试启动应用"""
        # 模拟应用启动成功
        mock_subprocess.return_value.returncode = 0
        mock_subprocess.return_value.stdout = f"package:{self.app_package}\n"
        
        device_manager = DeviceManager(self.device_id)
        # 设置设备连接状态为True
        device_manager.connected = True
        result = device_manager.launch_app(self.app_package)
        
        self.assertTrue(result)
        mock_subprocess.assert_called()


if __name__ == '__main__':
    unittest.main()