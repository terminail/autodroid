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
    id: str
    udid: str
    name: str
    model: str
    platform: str
    version: str
    connected: bool = False
    last_connected: Optional[datetime] = None
    total_operations: int = 0
    total_screenshots: int = 0


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