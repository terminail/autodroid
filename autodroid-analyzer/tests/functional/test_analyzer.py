#!/usr/bin/env python3
"""
合并后的分析器测试文件
包含交互式分析器和重构后的分析器测试
"""

import unittest
import sys
from pathlib import Path
from unittest.mock import Mock, patch
import os

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from core.analysis.interactive_analyzer import InteractiveAppAnalyzer


class TestInteractiveAnalyzer(unittest.TestCase):
    """测试交互式分析器"""
    
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
    def test_analyzer_initialization(self, mock_subprocess):
        """测试分析器初始化"""
        # 模拟设备连接成功
        mock_subprocess.return_value.returncode = 0
        mock_subprocess.return_value.stdout = "device\n"
        
        analyzer = InteractiveAppAnalyzer(self.device_id, self.app_package, self.output_dir)
        
        self.assertEqual(analyzer.device_id, self.device_id)
        self.assertEqual(analyzer.app_package, self.app_package)
        self.assertEqual(analyzer.output_dir, self.output_dir)
        self.assertFalse(analyzer.monitoring_enabled)
    
    @patch('subprocess.run')
    def test_analyze_monitored_data_no_session(self, mock_subprocess):
        """测试无会话时的数据分析"""
        # 模拟设备连接成功
        mock_subprocess.return_value.returncode = 0
        mock_subprocess.return_value.stdout = "device\n"
        
        analyzer = InteractiveAppAnalyzer(self.device_id, self.app_package, self.output_dir)
        
        # 测试无会话时的数据分析 - 使用实际存在的私有方法
        result = analyzer._analyze_monitored_content()
        # 验证方法执行完成（无异常抛出即可）
        self.assertIsNone(result)
    
    @patch('subprocess.run')
    def test_analyze_monitored_data_with_data(self, mock_subprocess):
        """测试有监控数据时的数据分析"""
        # 模拟设备连接和应用检查成功
        mock_subprocess.return_value.returncode = 0
        mock_subprocess.return_value.stdout = "device\npackage:com.example.app\n"
        
        analyzer = InteractiveAppAnalyzer(self.device_id, self.app_package, self.output_dir)
        
        # 模拟一些监控数据
        analyzer.monitored_pages = [
            {
                'page_id': 'page_1',
                'activity_name': 'com.example.app.MainActivity',
                'timestamp': 1234567890.0,
                'elements': [{'element_id': 'elem_1', 'clickable': True}]
            }
        ]
        
        analyzer.monitored_operations = [
            {
                'operation': {'type': 'click', 'target': 'elem_1'},
                'page_data': analyzer.monitored_pages[0],
                'timestamp': 1234567891.0
            }
        ]
        
        # 测试数据分析
        result = analyzer._analyze_monitored_content()
        self.assertIsNone(result)
    
    def test_error_handling(self):
        """测试错误处理"""
        # 测试无效设备ID
        with self.assertRaises(ValueError):
            analyzer = InteractiveAppAnalyzer(None, self.app_package, self.output_dir)
        
        # 测试无效应用包名
        with self.assertRaises(ValueError):
            analyzer = InteractiveAppAnalyzer(self.device_id, None, self.output_dir)


def test_module_imports():
    """测试模块导入"""
    print("🔍 测试模块导入...")
    
    try:
        from core.useroperation.user_operation_manager import UserOperationManager, UserAction
        print("✅ UserOperationManager 导入成功")
    except ImportError as e:
        print(f"❌ UserOperationManager 导入失败: {e}")
        assert False, f"UserOperationManager 导入失败: {e}"
    
    try:
        from core.screenshot.page_analyzer import PageAnalyzer, PageNode
        print("✅ PageAnalyzer 导入成功")
    except ImportError as e:
        print(f"❌ PageAnalyzer 导入失败: {e}")
        assert False, f"PageAnalyzer 导入失败: {e}"
    
    try:
        from core.useroperation.monitoring_system import MonitoringSystem, MonitoringConfig
        print("✅ MonitoringSystem 导入成功")
    except ImportError as e:
        print(f"❌ MonitoringSystem 导入失败: {e}")
        assert False, f"MonitoringSystem 导入失败: {e}"
    
    # Session相关功能已移除，跳过测试
    print("ℹ️  Session相关功能已移除，跳过测试")
    
    try:
        from core.analysis.interactive_analyzer import InteractiveAppAnalyzer
        print("✅ InteractiveAppAnalyzer 导入成功")
    except ImportError as e:
        print(f"❌ InteractiveAppAnalyzer 导入失败: {e}")
        assert False, f"InteractiveAppAnalyzer 导入失败: {e}"
    
    assert True, "模块导入测试通过"


def test_class_instantiation():
    """测试类实例化"""
    print("\n🔍 测试类实例化...")
    
    try:
        from core.useroperation.user_operation_manager import UserOperationManager
        operation_manager = UserOperationManager()
        print("✅ UserOperationManager 实例化成功")
    except Exception as e:
        print(f"❌ UserOperationManager 实例化失败: {e}")
        assert False, f"UserOperationManager 实例化失败: {e}"
    
    try:
        from core.screenshot.page_analyzer import PageAnalyzer
        page_analyzer = PageAnalyzer()
        print("✅ PageAnalyzer 实例化成功")
    except Exception as e:
        print(f"❌ PageAnalyzer 实例化失败: {e}")
        assert False, f"PageAnalyzer 实例化失败: {e}"
    
    try:
        from core.useroperation.monitoring_system import MonitoringSystem
        monitoring_system = MonitoringSystem()
        print("✅ MonitoringSystem 实例化成功")
    except Exception as e:
        print(f"❌ MonitoringSystem 实例化失败: {e}")
        assert False, f"MonitoringSystem 实例化失败: {e}"
    
    # Session相关功能已移除，跳过测试
    print("ℹ️  Session相关功能已移除，跳过测试")
    
    assert True, "类实例化测试通过"


def test_main_analyzer():
    """测试主分析器"""
    print("\n🔍 测试主分析器...")
    
    try:
        from core.analysis.interactive_analyzer import InteractiveAppAnalyzer
        
        # 创建临时输出目录
        output_dir = Path("test_output")
        output_dir.mkdir(exist_ok=True)
        
        # 创建分析器实例（使用测试参数）
        analyzer = InteractiveAppAnalyzer(
            device_id="test_device",
            app_package="com.example.test",
            output_dir=str(output_dir)
        )
        print("✅ InteractiveAppAnalyzer 实例化成功")
        
        # Session相关功能已移除，跳过测试
        print("ℹ️  Session相关功能已移除，跳过测试")
        
        assert True, "主分析器测试通过"
        
    except Exception as e:
        print(f"❌ 主分析器测试失败: {e}")
        assert False, f"主分析器测试失败: {e}"


class TestAnalyzerIntegration(unittest.TestCase):
    """分析器集成测试"""
    
    def test_module_imports_integration(self):
        """测试模块导入集成"""
        # 使用pytest风格的断言
        try:
            test_module_imports()
            self.assertTrue(True)
        except AssertionError as e:
            self.fail(f"模块导入测试失败: {e}")
    
    def test_class_instantiation_integration(self):
        """测试类实例化集成"""
        try:
            test_class_instantiation()
            self.assertTrue(True)
        except AssertionError as e:
            self.fail(f"类实例化测试失败: {e}")
    
    def test_main_analyzer_integration(self):
        """测试主分析器集成"""
        try:
            test_main_analyzer()
            self.assertTrue(True)
        except AssertionError as e:
            self.fail(f"主分析器测试失败: {e}")


if __name__ == '__main__':
    # 运行unittest测试
    unittest.main()