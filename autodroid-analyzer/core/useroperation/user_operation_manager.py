"""
用户操作管理模块
负责管理用户操作记录、分析和存储
"""

import time
from typing import List, Dict, Any, Optional
from dataclasses import dataclass


@dataclass
class UserAction:
    """用户操作数据类"""
    timestamp: float
    action_type: str
    target_element: Optional[Dict[str, Any]]
    input_text: Optional[str]
    coordinates: Optional[Dict[str, int]]
    result_page: Optional[str]


class UserOperationManager:
    """用户操作管理器"""
    
    def __init__(self, db_manager=None):
        self.db_manager = db_manager
        self.user_actions: List[UserAction] = []
        self.session_id: Optional[str] = None
    
    def record_operation(self, operation: Dict[str, Any], current_page_id: str) -> None:
        """记录用户操作"""
        try:
            # 创建用户操作记录
            action = UserAction(
                timestamp=time.time(),
                action_type=operation['type'],
                target_element=operation.get('element'),
                input_text=operation.get('input_text'),
                coordinates=operation.get('coordinates'),
                result_page=current_page_id
            )
            
            # 添加到操作列表
            self.user_actions.append(action)
            
            # 保存到数据库（如果数据库管理器可用）
            if self.db_manager:
                operation_data = {
                    'timestamp': action.timestamp,
                    'action_type': action.action_type,
                    'target_element': action.target_element,
                    'input_text': action.input_text,
                    'coordinates': action.coordinates,
                    'result_page': action.result_page
                }
                
                if self.session_id:
                    self.db_manager.save_user_operation(self.session_id, operation_data)
            
            print(f"📝 记录用户操作: {operation['type']}")
            
        except Exception as e:
            print(f"❌ 记录用户操作失败: {e}")
    
    def set_session_id(self, session_id: str) -> None:
        """设置会话ID"""
        self.session_id = session_id
    
    def get_operation_sequence(self) -> List[UserAction]:
        """获取用户操作序列"""
        return self.user_actions.copy()
    
    def clear_operations(self) -> None:
        """清空操作记录"""
        self.user_actions.clear()
    
    def analyze_operation_sequence(self) -> Dict[str, Any]:
        """分析用户操作序列"""
        if not self.user_actions:
            return {"total_operations": 0, "operation_types": {}, "analysis": "无用户操作记录"}
        
        # 统计操作类型
        operation_types = {}
        for action in self.user_actions:
            op_type = action.action_type
            operation_types[op_type] = operation_types.get(op_type, 0) + 1
        
        # 分析操作模式
        total_operations = len(self.user_actions)
        unique_pages = len(set(action.result_page for action in self.user_actions if action.result_page))
        
        analysis_result = {
            "total_operations": total_operations,
            "unique_pages": unique_pages,
            "operation_types": operation_types,
            "operation_sequence": [
                {
                    "timestamp": action.timestamp,
                    "type": action.action_type,
                    "target": action.target_element.get('text', '') if action.target_element else None,
                    "page": action.result_page
                }
                for action in self.user_actions
            ]
        }
        
        return analysis_result