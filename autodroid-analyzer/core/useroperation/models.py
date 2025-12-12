"""
用户操作模块的数据模型定义
包含Pydantic数据模型
数据库模型已统一移至core/database/models.py
"""

from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

# Pydantic数据模型
class UserOperationInfo(BaseModel):
    """用户操作信息模型"""
    id: int  # 操作ID
    apk_id: str  # 关联的APK ID
    timestamp: float  # 操作时间戳
    action_type: str  # 操作类型
    target_element: Optional[str] = None  # 目标元素（JSON格式存储）
    input_text: Optional[str] = None  # 输入文本
    coordinates: Optional[str] = None  # 坐标信息（JSON格式存储）
    screenshot_id: Optional[str] = None  # 关联的截屏ID
    created_at: datetime  # 创建时间

class OperationSequenceInfo(BaseModel):
    """操作序列信息模型"""
    id: str  # 序列ID
    apk_id: str  # 关联的APK ID
    sequence_name: str  # 序列名称
    description: Optional[str] = None  # 序列描述
    operations: str  # 操作列表（JSON格式存储）
    total_steps: int = 0  # 总步骤数
    average_duration: float = 0.0  # 平均执行时长
    created_at: datetime  # 创建时间

class OperationPatternInfo(BaseModel):
    """操作模式信息模型"""
    id: str  # 模式ID
    apk_id: str  # 关联的APK ID
    pattern_type: str  # 模式类型
    pattern_data: str  # 模式数据（JSON格式存储）
    confidence: float = 0.0  # 置信度
    frequency: int = 0  # 出现频率
    created_at: datetime  # 创建时间

class UserBehaviorInfo(BaseModel):
    """用户行为信息模型"""
    id: str  # 行为ID
    apk_id: str  # 关联的APK ID
    behavior_type: str  # 行为类型
    behavior_data: str  # 行为数据（JSON格式存储）
    session_id: Optional[str] = None  # 会话ID
    start_time: datetime  # 开始时间
    end_time: Optional[datetime] = None  # 结束时间
    duration: float = 0.0  # 持续时间

class OperationStatisticsInfo(BaseModel):
    """操作统计信息模型"""
    id: str  # 统计ID
    apk_id: str  # 关联的APK ID
    statistic_type: str  # 统计类型
    statistic_data: str  # 统计数据（JSON格式存储）
    period_start: datetime  # 统计周期开始
    period_end: datetime  # 统计周期结束
    created_at: datetime  # 创建时间