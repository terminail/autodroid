"""测试重构后的数据库模块和模型"""

import unittest
import tempfile
import os
import sys
from pathlib import Path
from datetime import datetime

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from core.database import get_database_manager
from core.apk.database import Apk
from core.device.database import Device
from core.useroperation.database import UserOperation
from core.screenshot.database import Screenshot


class TestDatabaseModule(unittest.TestCase):
    """测试数据库模块"""
    
    def setUp(self):
        """测试设置"""
        self.temp_dir = tempfile.mkdtemp()
        self.db_path = Path(self.temp_dir) / "test.db"
        
        # 设置环境变量以使用测试数据库
        os.environ['AUTODROID_DB_PATH'] = str(self.db_path)
        
        # 获取数据库管理器
        self.db_manager = get_database_manager(str(self.db_path))
        
    def tearDown(self):
        """测试清理"""
        import shutil
        if self.temp_dir and Path(self.temp_dir).exists():
            shutil.rmtree(self.temp_dir)
        
        # 清理环境变量
        if 'AUTODROID_DB_PATH' in os.environ:
            del os.environ['AUTODROID_DB_PATH']
    
    def test_database_manager_initialization(self):
        """测试数据库管理器初始化"""
        # 验证数据库管理器已正确初始化
        self.assertIsNotNone(self.db_manager)
        self.assertIsNotNone(self.db_manager.get_connection())
    
    def test_apk_model_operations(self):
        """测试APK模型操作"""
        # 清理可能存在的测试数据
        Apk.delete().where(Apk.id == 'com.example.testapp').execute()
        
        # 创建APK记录
        apk_data = {
            'id': 'com.example.testapp',
            'app_name': '测试应用',
            'version_name': '1.0.0',
            'version_code': 1,
            'install_time': datetime(2024, 1, 1, 12, 0, 0),
            'last_analyzed': datetime.now()
        }
        
        # 测试创建
        apk = Apk.create(**apk_data)
        self.assertIsNotNone(apk)
        self.assertEqual(apk.id, 'com.example.testapp')
        self.assertEqual(apk.app_name, '测试应用')
        
        # 测试查询
        retrieved_apk = Apk.get(Apk.id == 'com.example.testapp')
        self.assertEqual(retrieved_apk.id, 'com.example.testapp')
        
        # 测试更新
        Apk.update(app_name='更新后的应用').where(Apk.id == 'com.example.testapp').execute()
        updated_apk = Apk.get(Apk.id == 'com.example.testapp')
        self.assertEqual(updated_apk.app_name, '更新后的应用')
        
        # 测试删除
        deleted_count = Apk.delete().where(Apk.id == 'com.example.testapp').execute()
        self.assertEqual(deleted_count, 1)
    
    def test_device_model_operations(self):
        """测试设备模型操作"""
        # 创建设备记录
        device_data = {
            'id': 'emulator-5554',
            'device_name': '测试设备',
            'android_version': '13',
            'is_connected': True,
            'created_at': datetime.now()
        }
        
        # 测试创建
        device = Device.create(**device_data)
        self.assertIsNotNone(device)
        self.assertEqual(device.id, 'emulator-5554')
        self.assertEqual(device.device_name, '测试设备')
        
        # 测试查询
        retrieved_device = Device.get(Device.id == 'emulator-5554')
        self.assertEqual(retrieved_device.id, 'emulator-5554')
        
        # 测试更新
        Device.update(device_name='更新后的设备').where(Device.id == 'emulator-5554').execute()
        updated_device = Device.get(Device.id == 'emulator-5554')
        self.assertEqual(updated_device.device_name, '更新后的设备')
        
        # 测试删除
        deleted_count = Device.delete().where(Device.id == 'emulator-5554').execute()
        self.assertEqual(deleted_count, 1)
    
    def test_user_operation_model_operations(self):
        """测试用户操作模型操作"""
        # 清理可能存在的测试数据
        Apk.delete().where(Apk.id == 'com.example.operationapp').execute()
        
        # 先创建APK记录
        apk = Apk.create(
            id='com.example.operationapp',
            app_name='操作测试应用',
            version_name='1.0.0',
            version_code=1,
            install_time=datetime(2024, 1, 1, 12, 0, 0),
            last_analyzed=datetime.now()
        )
        
        # 创建用户操作记录
        operation_data = {
            'apk': apk,
            'action_type': 'click',
            'target_element': '{"id": "button_login", "text": "登录"}',
            'coordinates': '{"x": 100, "y": 200}',
            'timestamp': datetime.now()
        }
        
        # 测试创建
        operation = UserOperation.create(**operation_data)
        self.assertIsNotNone(operation)
        self.assertIsNotNone(operation.id)  # ID应该由数据库自动生成
        self.assertEqual(operation.apk.id, 'com.example.operationapp')
        
        # 测试查询
        operations = UserOperation.select().where(UserOperation.apk == apk)
        self.assertEqual(len(list(operations)), 1)
        
        # 测试更新
        UserOperation.update(action_type='input').where(UserOperation.apk == apk).execute()
        updated = UserOperation.get(UserOperation.apk == apk)
        self.assertEqual(updated.action_type, 'input')
        
        # 测试删除
        deleted_count = UserOperation.delete().where(UserOperation.apk == apk).execute()
        self.assertEqual(deleted_count, 1)
        
        # 清理APK记录
        Apk.delete().where(Apk.id == 'com.example.operationapp').execute()
    
    def test_screenshot_model_operations(self):
        """测试截屏模型操作"""
        # 清理可能存在的测试数据
        Apk.delete().where(Apk.id == 'com.example.screenshotapp').execute()
        Screenshot.delete().where(Screenshot.id == 'screenshot_001').execute()
        
        # 先创建APK记录
        apk = Apk.create(
            id='com.example.screenshotapp',
            app_name='截屏测试应用',
            version_name='1.0.0',
            version_code=1,
            install_time=datetime(2024, 1, 1, 12, 0, 0),
            last_analyzed=datetime.now()
        )
        
        # 创建截屏记录
        screenshot_data = {
            'id': 'screenshot_001',
            'apk': apk,
            'file_path': '/path/to/screenshot1.png',
            'width': 1080,
            'height': 1920,
            'file_size': 102400,
            'timestamp': datetime.now()
        }
        
        # 测试创建
        screenshot = Screenshot.create(**screenshot_data)
        self.assertIsNotNone(screenshot)
        self.assertEqual(screenshot.id, 'screenshot_001')
        self.assertEqual(screenshot.apk.id, 'com.example.screenshotapp')
        
        # 测试查询
        retrieved = Screenshot.get(Screenshot.id == 'screenshot_001')
        self.assertEqual(retrieved.id, 'screenshot_001')
        
        # 测试更新
        Screenshot.update(file_path='/new/path/screenshot.png').where(Screenshot.id == 'screenshot_001').execute()
        updated = Screenshot.get(Screenshot.id == 'screenshot_001')
        self.assertEqual(updated.file_path, '/new/path/screenshot.png')
        
        # 测试删除
        deleted_count = Screenshot.delete().where(Screenshot.id == 'screenshot_001').execute()
        self.assertEqual(deleted_count, 1)
    
    def test_database_module_structure(self):
        """测试数据库模块结构"""
        # 验证核心数据库文件存在
        self.assertTrue(os.path.exists('core/database/__init__.py'))
        self.assertTrue(os.path.exists('core/database/base.py'))
        self.assertTrue(os.path.exists('core/database/models.py'))
        
        # 验证模块化数据库文件存在
        self.assertTrue(os.path.exists('core/apk/database.py'))
        self.assertTrue(os.path.exists('core/analysis/database.py'))
        self.assertTrue(os.path.exists('core/device/database.py'))
        self.assertTrue(os.path.exists('core/screenshot/database.py'))
        self.assertTrue(os.path.exists('core/useroperation/database.py'))
        
        # 验证冗余文件已删除
        self.assertFalse(os.path.exists('core/database/database_manager.py'))
        self.assertFalse(os.path.exists('core/database/database_service.py'))


if __name__ == '__main__':
    unittest.main()