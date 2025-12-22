"""
设备模块的数据模型定义
包含Pydantic请求/响应模型
"""

from typing import List, Optional, Dict, Any
from datetime import datetime
from pydantic import BaseModel, Field


class DeviceInfoResponse(BaseModel):
    """设备信息响应模型"""
    serialno: str = Field(..., description="设备序列号")
    udid: Optional[str] = Field(None, description="设备UDID")
    name: Optional[str] = Field(None, description="设备名称")
    model: Optional[str] = Field(None, description="设备型号")
    manufacturer: Optional[str] = Field(None, description="设备制造商")
    android_version: Optional[str] = Field(None, description="Android版本")
    api_level: Optional[int] = Field(None, description="API级别")
    platform: Optional[str] = Field(None, description="平台")
    brand: Optional[str] = Field(None, description="品牌")
    device: Optional[str] = Field(None, description="设备")
    product: Optional[str] = Field(None, description="产品")
    ip: Optional[str] = Field(None, description="IP地址")
    screen_width: Optional[int] = Field(None, description="屏幕宽度")
    screen_height: Optional[int] = Field(None, description="屏幕高度")
    registered_at: Optional[datetime] = Field(None, description="注册时间")
    updated_at: Optional[datetime] = Field(None, description="更新时间")
    status: Optional[str] = Field(None, description="设备状态")
    usb_debug_enabled: Optional[bool] = Field(False, description="USB调试是否开启")
    wifi_debug_enabled: Optional[bool] = Field(False, description="WiFi调试是否开启")
    check_status: Optional[str] = Field("UNKNOWN", description="设备检查状态")
    check_message: Optional[str] = Field(None, description="设备检查消息")
    check_time: Optional[datetime] = Field(None, description="设备检查时间")
    apps: Optional[List[Dict[str, Any]]] = Field(None, description="已安装的支持应用列表")
    
    class Config:
        from_attributes = True


class DeviceCreateRequest(BaseModel):
    """设备创建请求模型"""
    serialno: str = Field(..., description="设备序列号")
    udid: Optional[str] = Field(None, description="设备UDID")
    name: str = Field(..., description="设备名称")
    model: Optional[str] = Field(None, description="设备型号")
    manufacturer: Optional[str] = Field(None, description="设备制造商")
    android_version: Optional[str] = Field(None, description="Android版本")
    api_level: Optional[int] = Field(None, description="API级别")
    platform: Optional[str] = Field(None, description="平台")
    brand: Optional[str] = Field(None, description="品牌")
    device: Optional[str] = Field(None, description="设备")
    product: Optional[str] = Field(None, description="产品")
    ip: Optional[str] = Field(None, description="IP地址")
    screen_width: Optional[int] = Field(None, description="屏幕宽度")
    screen_height: Optional[int] = Field(None, description="屏幕高度")


class DeviceUpdateRequest(BaseModel):
    """设备更新请求模型"""
    name: Optional[str] = Field(None, description="设备名称")
    model: Optional[str] = Field(None, description="设备型号")
    manufacturer: Optional[str] = Field(None, description="设备制造商")
    android_version: Optional[str] = Field(None, description="Android版本")
    api_level: Optional[int] = Field(None, description="API级别")
    platform: Optional[str] = Field(None, description="平台")
    brand: Optional[str] = Field(None, description="品牌")
    device: Optional[str] = Field(None, description="设备")
    product: Optional[str] = Field(None, description="产品")
    ip: Optional[str] = Field(None, description="IP地址")
    screen_width: Optional[int] = Field(None, description="屏幕宽度")
    screen_height: Optional[int] = Field(None, description="屏幕高度")
    # 保留原有字段以兼容其他可能的请求
    name: Optional[str] = Field(None, description="设备名称")
    battery_level: Optional[int] = Field(None, description="电池电量")
    is_online: Optional[bool] = Field(None, description="是否在线")
    connection_type: Optional[str] = Field(None, description="连接类型")


class DeviceListResponse(BaseModel):
    """设备列表响应模型"""
    devices: List[DeviceInfoResponse] = Field(..., description="设备列表")
    total_count: int = Field(..., description="总数")
    online_count: int = Field(..., description="在线设备数量")


class DeviceSearchRequest(BaseModel):
    """设备搜索请求模型"""
    name: Optional[str] = Field(None, description="设备名称")
    android_version: Optional[str] = Field(None, description="Android版本")
    is_online: Optional[bool] = Field(None, description="是否在线")
    connection_type: Optional[str] = Field(None, description="连接类型")
    user_id: Optional[str] = Field(None, description="用户ID")
    limit: int = Field(100, description="返回数量限制")
    offset: int = Field(0, description="偏移量")


class DeviceAssignmentRequest(BaseModel):
    """设备分配请求模型"""
    serialno: str = Field(..., description="设备序列号")
    user_id: str = Field(..., description="用户ID")


class DeviceStatusUpdateRequest(BaseModel):
    """设备状态更新请求模型"""
    is_online: bool = Field(..., description="是否在线")
    battery_level: int = Field(..., description="电池电量")


class DeviceCreateResponse(BaseModel):
    """设备创建响应模型"""
    success: bool = Field(..., description="操作是否成功")
    message: str = Field(..., description="响应消息")
    device_id: Optional[str] = Field(None, description="设备ID")
    serialno: Optional[str] = Field(None, description="设备序列号")
    udid: Optional[str] = Field(None, description="设备UDID")
    registered_at: Optional[datetime] = Field(None, description="注册时间")
    device: Optional[DeviceInfoResponse] = Field(None, description="设备信息")


class DeviceUpdateResponse(BaseModel):
    """设备更新响应模型"""
    success: bool = Field(..., description="操作是否成功")
    message: str = Field(..., description="响应消息")
    device: Optional[DeviceInfoResponse] = Field(None, description="设备信息")


class DeviceDeleteResponse(BaseModel):
    """设备删除响应模型"""
    success: bool = Field(..., description="操作是否成功")
    message: str = Field(..., description="响应消息")


class DeviceAssignmentResponse(BaseModel):
    """设备分配响应模型"""
    success: bool = Field(..., description="操作是否成功")
    message: str = Field(..., description="响应消息")


class DeviceStatusUpdateResponse(BaseModel):
    """设备状态更新响应模型"""
    success: bool = Field(..., description="操作是否成功")
    message: str = Field(..., description="响应消息")


# 从apk模块导入ApkInfo模型，避免循环导入
from ..apk.models import ApkInfo


class DeviceApkListResponse(BaseModel):
    """设备APK列表响应模型"""
    apks: List[ApkInfo] = Field(..., description="APK列表")
    total_count: int = Field(..., description="总数")


class DeviceCheckResponse(BaseModel):
    """设备检查响应模型"""
    success: bool = Field(..., description="操作是否成功")
    message: str = Field(..., description="响应消息")
    serialno: str = Field(..., description="设备序列号")
    udid: Optional[str] = Field(None, description="设备UDID")
    usb_debug_enabled: bool = Field(False, description="USB调试是否开启")
    wifi_debug_enabled: bool = Field(False, description="WiFi调试是否开启")
    installed_apps: List[Dict[str, Any]] = Field(default_factory=list, description="已安装的支持应用列表")
    check_time: Optional[datetime] = Field(None, description="检查时间")