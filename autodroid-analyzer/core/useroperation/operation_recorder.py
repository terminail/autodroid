"""
操作记录器模块
负责记录用户的操作历史，包括点击、输入、滑动等操作
"""

import time
from typing import List, Dict, Any, Optional
from dataclasses import dataclass, asdict
from datetime import datetime


@dataclass
class UserOperation:
    """用户操作记录"""
    id: str
    timestamp: datetime
    operation_type: str  # click, input, swipe, etc.
    element_info: Dict[str, Any]
    screenshot_path: Optional[str] = None
    description: Optional[str] = None


class OperationRecorder:
    """操作记录器"""
    
    def __init__(self):
        self.operations: List[UserOperation] = []
    
    def record_operation(self, operation_type: str, element_info: Dict[str, Any], 
                        screenshot_path: Optional[str] = None, description: Optional[str] = None) -> str:
        """记录用户操作"""
        operation_id = f"op_{int(time.time() * 1000)}"
        operation = UserOperation(
            id=operation_id,
            timestamp=datetime.now(),
            operation_type=operation_type,
            element_info=element_info,
            screenshot_path=screenshot_path,
            description=description
        )
        
        self.operations.append(operation)
        return operation_id
    
    def get_operations(self, limit: Optional[int] = None) -> List[Dict[str, Any]]:
        """获取操作记录，按时间倒序排列"""
        # 按时间倒序排序
        sorted_ops = sorted(self.operations, key=lambda x: x.timestamp, reverse=True)
        
        if limit:
            sorted_ops = sorted_ops[:limit]
        
        return [asdict(op) for op in sorted_ops]
    
    def clear_operations(self):
        """清空操作记录"""
        self.operations.clear()
    
    def get_operation_by_id(self, operation_id: str) -> Optional[Dict[str, Any]]:
        """根据ID获取操作记录"""
        for op in self.operations:
            if op.id == operation_id:
                return asdict(op)
        return None