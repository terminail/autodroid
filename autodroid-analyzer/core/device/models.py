"""
设备模块的数据模型定义
包含Pydantic数据模型
数据库模型已统一移至core/database/models.py
"""

from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

# Pydantic数据模型
class DeviceInfo(BaseModel):
    """设备信息响应模型"""
    id: str  # 设备ID
    device_name: str  # 设备名称
    android_version: str  # Android版本
    is_connected: bool = False  # 是否连接
    connection_type: str = 'USB'  # 连接类型
    battery_level: int = 0  # 电池电量
    created_at: datetime  # 创建时间

class DeviceCreateRequest(BaseModel):
    """设备创建请求模型"""
    id: str  # 设备ID
    device_name: str  # 设备名称
    android_version: str  # Android版本
    connection_type: str = 'USB'  # 连接类型

class DeviceConnectionLogInfo(BaseModel):
    """设备连接日志信息模型"""
    id: str  # 日志ID
    device_id: str  # 设备ID
    connection_type: str  # 连接类型
    status: str  # 连接状态
    timestamp: datetime  # 连接时间
    duration: Optional[int] = None  # 连接时长（秒）
    error_message: Optional[str] = None  # 错误信息

class DeviceAppInfo(BaseModel):
    """设备应用信息模型"""
    id: str  # 记录ID
    device_id: str  # 设备ID
    package_name: str  # 应用包名
    app_name: str  # 应用名称
    version_name: Optional[str] = None  # 版本名称
    version_code: Optional[int] = None  # 版本代码
    install_time: Optional[datetime] = None  # 安装时间
    is_system_app: bool = False  # 是否系统应用