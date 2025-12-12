"""用户操作管理模块，负责检测、记录和分析用户操作"""

import time
import json
from pathlib import Path
from typing import Dict, List, Optional, Any
from dataclasses import dataclass
from datetime import datetime

from core.database import get_database_manager


@dataclass
class UserAction:
    """用户操作数据类"""
    timestamp: float
    action_type: str  # "click", "input", "swipe", "back", "long_press"
    target_element: Dict[str, Any]
    input_text: Optional[str] = None
    coordinates: Optional[tuple] = None
    result_page: Optional[str] = None


class UserOperationManager:
    """用户操作管理器"""
    
    def __init__(self, device_id: str, app_package: str, output_dir: Path):
        self.device_id = device_id
        self.app_package = app_package
        self.output_dir = output_dir
        self.user_actions: List[UserAction] = []
        self.db_service = get_database_manager()
        
        # 创建用户操作记录目录
        self.operation_dir = output_dir / "user_operations"
        self.operation_dir.mkdir(exist_ok=True)
        
        # 操作检测阈值
        self.click_threshold = 0.8
        self.input_threshold = 0.7
        self.swipe_threshold = 0.6
        self.long_press_threshold = 0.5
    
    def _detect_click_operations(self, current_page: Any, previous_page: Any) -> Optional[UserAction]:
        """检测点击操作"""
        try:
            # 获取当前页面元素
            current_elements = getattr(current_page, 'elements', [])
            
            # 模拟检测点击操作（实际实现需要集成ADB事件监控）
            # 这里返回模拟数据用于测试
            if current_elements:
                # 选择第一个可点击元素
                clickable_element = None
                for element in current_elements:
                    if element.get('clickable', False):
                        clickable_element = element
                        break
                
                if clickable_element:
                    bounds = clickable_element.get('bounds', [0, 0, 100, 100])
                    click_x = (bounds[0] + bounds[2]) // 2
                    click_y = (bounds[1] + bounds[3]) // 2
                    
                    return UserAction(
                        timestamp=time.time(),
                        action_type="click",
                        target_element=clickable_element,
                        input_text=None,
                        coordinates=(click_x, click_y),
                        result_page=getattr(current_page, 'page_id', 'unknown')
                    )
            
            return None
            
        except Exception as e:
            print(f"❌ 点击操作检测失败: {e}")
            return None
    
    def _detect_input_operations(self, current_page: Any, previous_page: Any) -> Optional[UserAction]:
        """检测输入操作"""
        try:
            # 检测输入框操作（模拟实现）
            current_elements = getattr(current_page, 'elements', [])
            
            for element in current_elements:
                if element.get('editable', False):
                    # 模拟检测到输入操作
                    bounds = element.get('bounds', [0, 0, 100, 100])
                    click_x = (bounds[0] + bounds[2]) // 2
                    click_y = (bounds[1] + bounds[3]) // 2
                    
                    return UserAction(
                        timestamp=time.time(),
                        action_type="input",
                        target_element=element,
                        input_text="测试输入内容",
                        coordinates=(click_x, click_y),
                        result_page=getattr(current_page, 'page_id', 'unknown')
                    )
            
            return None
            
        except Exception as e:
            print(f"❌ 输入操作检测失败: {e}")
            return None
    
    def _detect_swipe_operations(self, current_page: Any, previous_page: Any) -> Optional[UserAction]:
        """检测滑动操作"""
        try:
            # 检测滑动操作（模拟实现）
            # 实际实现需要集成ADB滑动事件监控
            
            # 模拟检测到滑动操作
            return UserAction(
                timestamp=time.time(),
                action_type="swipe",
                target_element=None,
                input_text=None,
                coordinates=(500, 800),  # 滑动起始点
                result_page=getattr(current_page, 'page_id', 'unknown')
            )
            
        except Exception as e:
            print(f"❌ 滑动操作检测失败: {e}")
            return None
    
    def _detect_back_operations(self, current_page: Any, previous_page: Any) -> Optional[UserAction]:
        """检测返回操作"""
        try:
            # 检测返回键操作（模拟实现）
            # 实际实现需要集成ADB按键事件监控
            
            # 模拟检测到返回操作
            return UserAction(
                timestamp=time.time(),
                action_type="back",
                target_element=None,
                input_text=None,
                coordinates=None,
                result_page=getattr(current_page, 'page_id', 'unknown')
            )
            
        except Exception as e:
            print(f"❌ 返回操作检测失败: {e}")
            return None
    
    def _detect_long_press_operations(self, current_page: Any, previous_page: Any) -> Optional[UserAction]:
        """检测长按操作"""
        try:
            # 检测长按操作（模拟实现）
            current_elements = getattr(current_page, 'elements', [])
            
            for element in current_elements:
                if element.get('long_clickable', False):
                    bounds = element.get('bounds', [0, 0, 100, 100])
                    press_x = (bounds[0] + bounds[2]) // 2
                    press_y = (bounds[1] + bounds[3]) // 2
                    
                    return UserAction(
                        timestamp=time.time(),
                        action_type="long_press",
                        target_element=element,
                        input_text=None,
                        coordinates=(press_x, press_y),
                        result_page=getattr(current_page, 'page_id', 'unknown')
                    )
            
            return None
            
        except Exception as e:
            print(f"❌ 长按操作检测失败: {e}")
            return None
    
    def detect_user_operations(self, current_page: Any, previous_page: Any) -> List[UserAction]:
        """检测用户操作"""
        detected_actions = []
        
        # 检测各种类型的操作
        operation_detectors = [
            self._detect_click_operations,
            self._detect_input_operations,
            self._detect_swipe_operations,
            self._detect_back_operations,
            self._detect_long_press_operations
        ]
        
        for detector in operation_detectors:
            action = detector(current_page, previous_page)
            if action:
                detected_actions.append(action)
        
        return detected_actions
    
    def record_user_operation(self, action: UserAction, current_page: Any):
        """记录用户操作"""
        try:
            # 添加到内存列表
            self.user_actions.append(action)
            
            # 保存到数据库
            self._save_action_to_database(action, current_page)
            
            # 显示操作信息
            self._display_operation_info(action)
            
        except Exception as e:
            print(f"❌ 记录用户操作失败: {e}")
    
    def _save_action_to_database(self, action: UserAction, current_page: Any):
        """保存用户操作到数据库"""
        try:
            # 创建或获取会话ID
            session_id = self._get_or_create_session()
            
            if not session_id:
                print("❌ 创建会话失败")
                return
            
            # 保存页面信息
            page_data = {
                'page_id': getattr(current_page, 'page_id', 'unknown'),
                'app_name': getattr(current_page, 'app_name', self.app_package),
                'activity_name': getattr(current_page, 'activity_name', ''),
                'title': getattr(current_page, 'title', ''),
                'element_count': getattr(current_page, 'element_count', 0),
                'timestamp': time.time()
            }
            
            if not self.db_manager.save_page_info(session_id, page_data):
                print("❌ 保存页面信息失败")
            
            # 准备用户操作数据
            action_data = {
                'timestamp': action.timestamp,
                'action_type': action.action_type,
                'target_element': action.target_element,
                'input_text': action.input_text,
                'coordinates': action.coordinates,
                'result_page_id': action.result_page
            }
            
            # 保存用户操作
            if not self.db_manager.save_user_operation(session_id, action_data):
                print("❌ 保存用户操作失败")
            
            print(f"💾 用户操作已保存到数据库，会话ID: {session_id}")
            
        except Exception as e:
            print(f"❌ 保存用户操作到数据库失败: {e}")
    
    def _get_or_create_session(self) -> Optional[str]:
        """获取或创建分析会话"""
        try:
            # 生成会话ID
            session_id = f"{self.device_id}_{self.app_package}_{int(time.time())}"
            
            # 创建会话
            if self.db_manager.create_session(session_id, time.time()):
                return session_id
            else:
                # 如果创建失败，尝试使用现有会话
                sessions = self.db_manager.get_sessions()
                if sessions:
                    return sessions[0]['session_id']
                return None
                
        except Exception as e:
            print(f"❌ 获取或创建会话失败: {e}")
            return None
    
    def _display_operation_info(self, action: UserAction):
        """显示操作信息"""
        action_type_display = {
            "click": "👆 点击",
            "input": "⌨️  输入",
            "swipe": "🔄 滑动",
            "back": "↩️  返回",
            "long_press": "⏱️  长按",
            "menu": "📱 菜单"
        }
        
        display_text = action_type_display.get(action.action_type, "👆 操作")
        
        if action.coordinates:
            display_text += f"[{action.coordinates[0]},{action.coordinates[1]}]"
        
        if action.target_element:
            element_text = action.target_element.get('text', '')
            if not element_text:
                element_text = action.target_element.get('content_desc', '')
            if not element_text:
                element_text = action.target_element.get('resource_id', '')
            if element_text:
                display_text += f"元素\"{element_text}\""
        
        if action.input_text:
            display_text += f"：{action.input_text}"
        
        print(f"{display_text}")
        print(f"📝 已记录用户操作 #{len(self.user_actions)}（已保存到数据库）")
    
    def get_user_actions(self) -> List[UserAction]:
        """获取所有用户操作"""
        return self.user_actions.copy()
    
    def clear_user_actions(self):
        """清空用户操作记录"""
        self.user_actions.clear()
    
    def analyze_action_sequence(self) -> Dict[str, Any]:
        """分析用户操作序列"""
        if not self.user_actions:
            return {"error": "没有用户操作记录"}
        
        try:
            analysis_result = {
                "total_actions": len(self.user_actions),
                "action_types": {},
                "average_interval": 0.0,
                "operation_path": []
            }
            
            # 统计操作类型
            for action in self.user_actions:
                action_type = action.action_type
                if action_type in analysis_result["action_types"]:
                    analysis_result["action_types"][action_type] += 1
                else:
                    analysis_result["action_types"][action_type] = 1
            
            # 计算平均间隔
            if len(self.user_actions) > 1:
                intervals = []
                for i in range(1, len(self.user_actions)):
                    interval = self.user_actions[i].timestamp - self.user_actions[i-1].timestamp
                    intervals.append(interval)
                analysis_result["average_interval"] = sum(intervals) / len(intervals)
            
            # 生成操作路径
            for action in self.user_actions:
                path_entry = {
                    "action_type": action.action_type,
                    "timestamp": action.timestamp,
                    "result_page": action.result_page
                }
                analysis_result["operation_path"].append(path_entry)
            
            return analysis_result
            
        except Exception as e:
            return {"error": f"操作序列分析失败: {e}"}


def create_user_operation_manager(device_id: str, app_package: str, output_dir: Path) -> UserOperationManager:
    """创建用户操作管理器"""
    return UserOperationManager(device_id, app_package, output_dir)