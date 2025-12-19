"""
设备模块的数据模型定义
包含Pydantic请求/响应模型
"""

from typing import List, Optional, Dict, Any
from datetime import datetime
from pydantic import BaseModel, Field


class DeviceInfo(BaseModel):
    """设备信息响应模型"""
    udid: str = Field(..., description="设备UDID")
    device_name: str = Field(..., description="设备名称")
    android_version: str = Field(..., description="Android版本")
    battery_level: int = Field(..., description="电池电量")
    is_online: bool = Field(..., description="是否在线")
    connection_type: str = Field(..., description="连接类型")
    user_id: Optional[str] = Field(None, description="用户ID")


class DeviceCreateRequest(BaseModel):
    """设备创建请求模型"""
    udid: str = Field(..., description="设备UDID")
    device_name: str = Field(..., description="设备名称")
    android_version: str = Field(..., description="Android版本")
    battery_level: int = Field(..., description="电池电量")
    is_online: bool = Field(..., description="是否在线")
    connection_type: str = Field(..., description="连接类型")


class DeviceUpdateRequest(BaseModel):
    """设备更新请求模型"""
    device_name: Optional[str] = Field(None, description="设备名称")
    android_version: Optional[str] = Field(None, description="Android版本")
    battery_level: Optional[int] = Field(None, description="电池电量")
    is_online: Optional[bool] = Field(None, description="是否在线")
    connection_type: Optional[str] = Field(None, description="连接类型")


class DeviceListResponse(BaseModel):
    """设备列表响应模型"""
    devices: List[DeviceInfo] = Field(..., description="设备列表")
    total_count: int = Field(..., description="总数")
    online_count: int = Field(..., description="在线设备数量")


class DeviceSearchRequest(BaseModel):
    """设备搜索请求模型"""
    device_name: Optional[str] = Field(None, description="设备名称")
    android_version: Optional[str] = Field(None, description="Android版本")
    is_online: Optional[bool] = Field(None, description="是否在线")
    connection_type: Optional[str] = Field(None, description="连接类型")
    user_id: Optional[str] = Field(None, description="用户ID")
    limit: int = Field(100, description="返回数量限制")
    offset: int = Field(0, description="偏移量")


class DeviceAssignmentRequest(BaseModel):
    """设备分配请求模型"""
    udid: str = Field(..., description="设备UDID")
    user_id: str = Field(..., description="用户ID")


class DeviceStatusUpdateRequest(BaseModel):
    """设备状态更新请求模型"""
    is_online: bool = Field(..., description="是否在线")
    battery_level: int = Field(..., description="电池电量")


# 从apk模块导入ApkInfo模型，避免循环导入
from ..apk.models import ApkInfo


class DeviceApkListResponse(BaseModel):
    """设备APK列表响应模型"""
    apks: List[ApkInfo] = Field(..., description="APK列表")
    total_count: int = Field(..., description="总数")