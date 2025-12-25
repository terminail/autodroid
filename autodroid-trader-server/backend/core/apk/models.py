"""
APK模块的数据模型定义
包含Pydantic请求/响应模型
"""

from typing import List, Optional, Dict, Any
from datetime import datetime
from pydantic import BaseModel, Field


class ApkInfo(BaseModel):
    """APK信息响应模型"""
    package_name: str = Field(..., description="包名")
    app_name: str = Field(..., description="应用名称")
    version: str = Field(..., description="版本名称")
    version_code: int = Field(..., description="版本代码")
    installed_time: Optional[datetime] = Field(None, description="安装时间")
    is_system: bool = Field(False, description="是否为系统应用")
    icon_path: Optional[str] = Field(None, description="图标路径")

    class Config:
        json_encoders = {
            datetime: lambda v: v.isoformat() if v else None
        }


class ApkCreateRequest(BaseModel):
    """APK创建请求模型"""
    package_name: str = Field(..., description="包名")
    app_name: str = Field(..., description="应用名称")
    version: str = Field(..., description="版本名称")
    version_code: int = Field(..., description="版本代码")
    is_system: bool = Field(False, description="是否为系统应用")
    icon_path: Optional[str] = Field(None, description="图标路径")


class ApkUpdateRequest(BaseModel):
    """APK更新请求模型"""
    app_name: Optional[str] = Field(None, description="应用名称")
    version: Optional[str] = Field(None, description="版本名称")
    version_code: Optional[int] = Field(None, description="版本代码")
    is_system: Optional[bool] = Field(None, description="是否为系统应用")
    icon_path: Optional[str] = Field(None, description="图标路径")


class ApkListResponse(BaseModel):
    """APK列表响应模型"""
    apks: List[ApkInfo] = Field(..., description="APK列表")
    total_count: int = Field(..., description="总数")


class DeviceApkInfo(BaseModel):
    """设备APK关联信息模型"""
    package_name: str = Field(..., description="包名")
    serialno: str = Field(..., description="设备序列号")
    installed_time: Optional[datetime] = Field(None, description="安装时间")

    class Config:
        json_encoders = {
            datetime: lambda v: v.isoformat() if v else None
        }


class ApkRegisterRequest(BaseModel):
    """APK注册到设备请求模型"""
    serialno: str = Field(..., description="设备序列号")
    apk_info: ApkCreateRequest = Field(..., description="APK信息")


class ApkSearchRequest(BaseModel):
    """APK搜索请求模型"""
    package_name: Optional[str] = Field(None, description="包名")
    app_name: Optional[str] = Field(None, description="应用名称")
    version: Optional[str] = Field(None, description="版本名称")
    is_system: Optional[bool] = Field(None, description="是否为系统应用")
    limit: int = Field(100, description="返回数量限制")
    offset: int = Field(0, description="偏移量")