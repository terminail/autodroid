"""
分析模块的数据模型定义
包含Pydantic数据模型
数据库模型已统一移至core/database/models.py
"""

from pydantic import BaseModel
from typing import Optional
from datetime import datetime

# Pydantic数据模型
class AnalysisResultInfo(BaseModel):
    """分析结果信息模型"""
    id: str  # 分析结果ID
    apk_id: str  # 关联的APK ID
    analysis_type: str  # 分析类型
    result: str  # 分析结果（JSON格式存储）
    confidence: float = 0.0  # 置信度
    analysis_time: datetime  # 分析时间

class AnalysisTaskInfo(BaseModel):
    """分析任务信息模型"""
    id: str  # 任务ID
    apk_id: str  # 关联的APK ID
    task_type: str  # 任务类型
    status: str = 'pending'  # 任务状态: pending, running, completed, failed
    parameters: Optional[str] = None  # 任务参数（JSON格式）
    result: Optional[str] = None  # 任务结果（JSON格式）
    created_time: datetime  # 创建时间
    started_time: Optional[datetime] = None  # 开始时间
    completed_time: Optional[datetime] = None  # 完成时间
    error_message: Optional[str] = None  # 错误信息

class AnalysisPatternInfo(BaseModel):
    """分析模式信息模型"""
    id: str  # 模式ID
    name: str  # 模式名称
    description: Optional[str] = None  # 模式描述
    pattern_type: str  # 模式类型
    pattern_data: str  # 模式数据（JSON格式）
    confidence_threshold: float = 0.7  # 置信度阈值
    is_active: bool = True  # 是否激活
    created_time: datetime  # 创建时间

class AnalysisReportInfo(BaseModel):
    """分析报告信息模型"""
    id: str  # 报告ID
    apk_id: str  # 关联的APK ID
    report_type: str  # 报告类型
    title: str  # 报告标题
    content: str  # 报告内容
    summary: Optional[str] = None  # 报告摘要
    generated_time: datetime  # 生成时间