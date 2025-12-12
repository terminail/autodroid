"""
截屏模块的数据模型定义
包含Pydantic数据模型
数据库模型已统一移至core/database/models.py
"""

from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

# Pydantic数据模型
class ScreenshotInfo(BaseModel):
    """截屏信息响应模型"""
    id: str  # 截屏唯一标识
    apk_id: str  # 关联的APK ID
    timestamp: float  # 截屏时间戳
    file_path: str  # 文件路径
    page_title: Optional[str] = None  # 页面标题
    analysis_status: str = 'pending'  # 分析状态
    created_at: datetime  # 创建时间

class PageElementInfo(BaseModel):
    """页面元素信息模型"""
    id: str  # 元素唯一标识
    screenshot_id: str  # 关联的截屏ID
    element_type: str  # 元素类型
    text_content: Optional[str] = None  # 文本内容
    bounds: str  # 边界坐标（JSON格式存储）
    importance: int = 3  # 重要性评分(1-5)
    custom_tags: Optional[str] = None  # 自定义标签（JSON格式存储）
    created_at: datetime  # 创建时间

class ScreenshotAnalysisResultInfo(BaseModel):
    """截屏分析结果信息模型"""
    id: str  # 分析结果ID
    screenshot_id: str  # 关联的截屏ID
    analysis_type: str  # 分析类型
    result: str  # 分析结果（JSON格式存储）
    confidence: float = 0.0  # 置信度
    analysis_time: datetime  # 分析时间

class PageStructureInfo(BaseModel):
    """页面结构信息模型"""
    id: str  # 结构ID
    screenshot_id: str  # 关联的截屏ID
    structure_type: str  # 结构类型
    elements_count: int = 0  # 元素数量
    layout_info: str  # 布局信息（JSON格式存储）
    created_at: datetime  # 创建时间