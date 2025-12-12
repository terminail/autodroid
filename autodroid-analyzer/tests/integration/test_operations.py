"""测试用户操作模块功能"""

import unittest
import tempfile
import os
import sys
from pathlib import Path
from datetime import datetime

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from core.useroperation.user_operation_manager import UserOperationManager
from core.apk.database import Apk
from core.database.models import UserOperation


class TestUserOperationModule(unittest.TestCase):
    """测试用户操作模块"""
    
    def setUp(self):
        """测试设置"""
        self.temp_dir = tempfile.mkdtemp()
        self.db_path = Path(self.temp_dir) / "test.db"
        
        # 设置环境变量以使用测试数据库
        os.environ['AUTODROID_DB_PATH'] = str(self.db_path)
        
        # 初始化用户操作管理器
        self.operation_manager = UserOperationManager()
        
    def tearDown(self):
        """测试清理"""
        import shutil
        if self.temp_dir and Path(self.temp_dir).exists():
            shutil.rmtree(self.temp_dir)
        
        # 清理环境变量
        if 'AUTODROID_DB_PATH' in os.environ:
            del os.environ['AUTODROID_DB_PATH']
    
    def test_user_operation_manager_initialization(self):
        """测试用户操作管理器初始化"""
        # 验证操作管理器已正确初始化
        self.assertIsNotNone(self.operation_manager)
        self.assertEqual(len(self.operation_manager.user_actions), 0)
        self.assertIsNone(self.operation_manager.session_id)
    
    def test_user_operation_creation(self):
        """测试用户操作创建"""
        # 测试操作管理器可以创建操作记录
        operation_data = {
            'type': 'click',
            'element': {"id": "button_login", "text": "登录"},
            'coordinates': {"x": 100, "y": 200}
        }
        
        # 记录操作
        self.operation_manager.record_operation(operation_data, "page_1")
        
        # 验证操作已记录
        self.assertEqual(len(self.operation_manager.user_actions), 1)
        action = self.operation_manager.user_actions[0]
        self.assertEqual(action.action_type, 'click')
        self.assertEqual(action.target_element['text'], '登录')
        self.assertEqual(action.result_page, 'page_1')
    
    def test_user_operation_recording(self):
        """测试用户操作记录"""
        # 记录多个用户操作
        operations = [
            {
                'type': 'click',
                'element': {"id": "button_start", "text": "开始"},
                'coordinates': {"x": 100, "y": 200}
            },
            {
                'type': 'input',
                'element': {"id": "input_name", "text": "姓名"},
                'coordinates': {"x": 150, "y": 300},
                'input_text': '张三'
            },
            {
                'type': 'click',
                'element': {"id": "button_submit", "text": "提交"},
                'coordinates': {"x": 200, "y": 400}
            }
        ]
        
        # 记录所有操作
        for i, op_data in enumerate(operations):
            self.operation_manager.record_operation(op_data, f"page_{i}")
        
        # 验证操作已记录
        self.assertEqual(len(self.operation_manager.user_actions), 3)
        
        # 验证操作顺序和内容
        action1 = self.operation_manager.user_actions[0]
        self.assertEqual(action1.action_type, 'click')
        self.assertEqual(action1.target_element['text'], '开始')
        self.assertEqual(action1.result_page, 'page_0')
        
        action2 = self.operation_manager.user_actions[1]
        self.assertEqual(action2.action_type, 'input')
        self.assertEqual(action2.input_text, '张三')
        self.assertEqual(action2.result_page, 'page_1')
        
        action3 = self.operation_manager.user_actions[2]
        self.assertEqual(action3.action_type, 'click')
        self.assertEqual(action3.target_element['text'], '提交')
        self.assertEqual(action3.result_page, 'page_2')
    
    def test_user_operation_pattern_detection(self):
        """测试用户操作序列获取"""
        # 设置会话ID
        self.operation_manager.set_session_id("test_session_123")
        
        # 记录多个操作
        operations = [
            {
                'type': 'click',
                'element': {"id": "button_login", "text": "登录"},
                'coordinates': {"x": 100, "y": 200}
            },
            {
                'type': 'input',
                'element': {"id": "input_username", "text": "用户名"},
                'coordinates': {"x": 150, "y": 300},
                'input_text': 'user123'
            },
            {
                'type': 'click',
                'element': {"id": "button_submit", "text": "提交"},
                'coordinates': {"x": 200, "y": 400}
            }
        ]
        
        # 记录所有操作
        for i, op_data in enumerate(operations):
            self.operation_manager.record_operation(op_data, f"page_{i}")
        
        # 获取操作序列
        sequence = self.operation_manager.get_operation_sequence()
        self.assertIsNotNone(sequence)
        self.assertEqual(len(sequence), 3)
        
        # 验证操作序列内容
        self.assertEqual(sequence[0].action_type, 'click')
        self.assertEqual(sequence[1].action_type, 'input')
        self.assertEqual(sequence[2].action_type, 'click')
        
        # 验证会话ID
        self.assertEqual(self.operation_manager.session_id, "test_session_123")
    
    def test_user_operation_sequence_analysis(self):
        """测试用户操作序列分析"""
        # 设置会话ID
        self.operation_manager.set_session_id("analysis_session_456")
        
        # 记录操作序列
        operations = [
            {
                'type': 'click',
                'element': {"id": "button_start", "text": "开始"},
                'coordinates': {"x": 100, "y": 200}
            },
            {
                'type': 'input',
                'element': {"id": "input_name", "text": "姓名"},
                'coordinates': {"x": 150, "y": 300},
                'input_text': '张三'
            },
            {
                'type': 'click',
                'element': {"id": "button_submit", "text": "提交"},
                'coordinates': {"x": 200, "y": 400}
            }
        ]
        
        # 记录所有操作
        for i, op_data in enumerate(operations):
            self.operation_manager.record_operation(op_data, f"page_{i}")
        
        # 获取操作序列
        sequence = self.operation_manager.get_operation_sequence()
        self.assertEqual(len(sequence), 3)
        
        # 手动分析操作序列
        total_operations = len(sequence)
        operation_types = {}
        
        for action in sequence:
            op_type = action.action_type
            operation_types[op_type] = operation_types.get(op_type, 0) + 1
        
        # 验证分析结果
        self.assertEqual(total_operations, 3)
        self.assertEqual(operation_types.get('click', 0), 2)
        self.assertEqual(operation_types.get('input', 0), 1)
        
        # 验证会话ID一致性
        self.assertEqual(self.operation_manager.session_id, "analysis_session_456")
    
    def test_user_operation_module_structure(self):
        """测试用户操作模块结构"""
        # 验证模块文件存在
        self.assertTrue(os.path.exists('core/useroperation'))
        self.assertTrue(os.path.exists('core/useroperation/user_operation_manager.py'))
        
        # 验证导入路径
        try:
            from core.useroperation.user_operation_manager import UserOperationManager
            from core.database.models import UserOperation
            self.assertTrue(True)  # 导入成功
        except ImportError as e:
            self.fail(f"导入失败: {e}")


if __name__ == '__main__':
    unittest.main()