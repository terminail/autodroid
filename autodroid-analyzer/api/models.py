"""
Pydantic models for Autodroid Analyzer API

This module contains all the data models used in the Autodroid Analyzer API.
Separating models from the main application logic improves code organization and maintainability.
"""

from pydantic import BaseModel
from typing import Dict, List, Any, Optional
from datetime import datetime


class ScreenshotInfo(BaseModel):
    """Model for screenshot information"""
    id: str
    apk_id: str
    timestamp: datetime
    file_path: str
    page_title: Optional[str] = None
    analysis_status: str = "pending"
    created_at: datetime


class PageElementInfo(BaseModel):
    """Model for page element information"""
    id: str
    screenshot_id: str
    element_type: str
    bounds: Dict[str, int]
    text: Optional[str] = None
    resource_id: Optional[str] = None
    class_name: Optional[str] = None
    package_name: Optional[str] = None
    content_desc: Optional[str] = None
    clickable: bool = False
    long_clickable: bool = False
    scrollable: bool = False
    enabled: bool = True
    focused: bool = False
    selected: bool = False
    created_at: datetime


class UserOperationInfo(BaseModel):
    """Model for user operation information"""
    id: str
    apk_id: str
    timestamp: datetime
    action_type: str
    target_element: Optional[str] = None
    input_text: Optional[str] = None
    coordinates: Optional[Dict[str, int]] = None
    screenshot_id: Optional[str] = None
    created_at: datetime


class ApkInfo(BaseModel):
    """Model for APK information"""
    id: str
    app_name: str
    version_name: Optional[str] = None
    version_code: Optional[int] = None
    install_time: Optional[datetime] = None
    last_analyzed: Optional[datetime] = None
    total_operations: int = 0
    total_screenshots: int = 0
    is_packed: bool = False
    packer_type: Optional[str] = None
    packer_confidence: float = 0.0
    packer_indicators: Optional[str] = None
    packer_analysis_time: Optional[datetime] = None


class DeviceInfo(BaseModel):
    """设备信息模型"""
    id: str  # 设备ID
    device_name: str  # 设备名称
    android_version: str  # Android版本
    api_level: int = 0  # API级别
    is_connected: bool = False  # 是否连接
    connection_type: str = 'USB'  # 连接类型
    battery_level: int = 0  # 电池电量
    battery_status: str = 'Unknown'  # 电池状态
    is_charging: bool = False  # 是否充电
    device_model: str = ''  # 设备型号
    created_at: Optional[datetime] = None  # 创建时间
    last_updated: Optional[datetime] = None  # 最后更新时间
    last_connected: Optional[datetime] = None  # 最后连接时间
    wifi_debug_guide: Optional[Dict[str, Any]] = None  # WiFi调试指导信息
    
    
class WifiDebugGuide(BaseModel):
    """WiFi调试指导信息模型"""
    android_version: str  # Android版本
    connection_type: str  # 当前连接类型
    supported: bool  # 是否支持WiFi调试
    steps: List[Dict[str, str]]  # 操作步骤列表，每个步骤包含标题和详细说明
    commands: Optional[List[str]] = None  # 需要执行的ADB命令（如适用）
    requirements: Optional[List[str]] = None  # 前置条件列表
    estimated_time: str = ""  # 预计完成时间


class DeviceApksResponse(BaseModel):
    """设备APK列表响应模型"""
    device_id: str
    apks: List[Dict[str, Any]]
    count: int


class DeviceStatistics(BaseModel):
    """设备统计信息模型"""
    device_id: str
    operations_count: int
    screenshots_count: int
    apks_count: int


class DeviceOperationsSummary(BaseModel):
    """设备操作摘要模型"""
    device_id: str
    operation_types: Dict[str, Any]
    time_distribution: Dict[str, Any]


class AnalysisResult(BaseModel):
    """Model for analysis result"""
    apk_id: str
    analysis_type: str
    status: str
    result: Dict[str, Any]
    timestamp: datetime
    duration: float


class AnalysisRequest(BaseModel):
    """Model for analysis request"""
    apk_id: str
    analysis_type: str
    parameters: Dict[str, Any] = {}


class HealthCheck(BaseModel):
    """Model for health check response"""
    status: str
    timestamp: float
    services: Dict[str, str]


class ServerInfo(BaseModel):
    """Model for server information response"""
    name: str
    hostname: str
    ipAddress: str
    platform: str
    apiEndpoint: str
    services: Dict[str, str]
    capabilities: Dict[str, bool]