"""
APK模块的数据模型定义
包含Pydantic数据模型
数据库模型已统一移至core/database/models.py
"""

from typing import List, Optional, Dict, Any
from datetime import datetime
from pydantic import BaseModel, Field, ConfigDict


class ApkInfo(BaseModel):
    """APK信息响应模型"""
    id: str = Field(..., description="APK唯一标识，用应用包名package_name表示")
    app_name: str = Field(..., description="应用名称")
    version_name: Optional[str] = Field(None, description="版本名称")
    version_code: Optional[int] = Field(None, description="版本代码")
    install_time: Optional[datetime] = Field(None, description="安装时间")
    last_analyzed: Optional[datetime] = Field(None, description="最后分析时间")
    total_operations: int = Field(0, description="总操作数")
    total_screenshots: int = Field(0, description="总截屏数")
    is_packed: bool = Field(False, description="是否被加固")
    packer_type: Optional[str] = Field(None, description="加固类型")
    packer_confidence: float = Field(0.0, description="加固检测置信度")
    packer_indicators: Optional[str] = Field(None, description="加固特征指标")
    packer_analysis_time: Optional[datetime] = Field(None, description="加固分析时间")
    
    model_config = ConfigDict()


class ApkCreateRequest(BaseModel):
    """APK创建请求模型"""
    id: str = Field(..., description="APK唯一标识，用应用包名package_name表示")
    app_name: str = Field(..., description="应用名称")
    version_name: Optional[str] = Field(None, description="版本名称")
    version_code: Optional[int] = Field(None, description="版本代码")
    install_time: Optional[datetime] = Field(None, description="安装时间")


class ApkUpdateRequest(BaseModel):
    """APK更新请求模型"""
    app_name: Optional[str] = Field(None, description="应用名称")
    version_name: Optional[str] = Field(None, description="版本名称")
    version_code: Optional[int] = Field(None, description="版本代码")
    install_time: Optional[datetime] = Field(None, description="安装时间")
    last_analyzed: Optional[datetime] = Field(None, description="最后分析时间")
    total_operations: Optional[int] = Field(None, description="总操作数")
    total_screenshots: Optional[int] = Field(None, description="总截屏数")
    is_packed: Optional[bool] = Field(None, description="是否被加固")
    packer_type: Optional[str] = Field(None, description="加固类型")
    packer_confidence: Optional[float] = Field(None, description="加固检测置信度")
    packer_indicators: Optional[str] = Field(None, description="加固特征指标")
    packer_analysis_time: Optional[datetime] = Field(None, description="加固分析时间")


class ApkListResponse(BaseModel):
    """APK列表响应模型"""
    apks: List[ApkInfo] = Field(..., description="APK列表")
    total_count: int = Field(..., description="总数")


class DeviceApkInfo(BaseModel):
    """设备APK关联信息模型"""
    id: str = Field(..., description="APK唯一标识，用应用包名package_name表示")
    device_udid: str = Field(..., description="设备UDID")
    install_time: Optional[datetime] = Field(None, description="安装时间")
    
    model_config = ConfigDict()


class ApkRegisterRequest(BaseModel):
    """APK注册到设备请求模型"""
    device_udid: str = Field(..., description="设备UDID")
    apk_info: ApkCreateRequest = Field(..., description="APK信息")


class ApkSearchRequest(BaseModel):
    """APK搜索请求模型"""
    id: Optional[str] = Field(None, description="APK唯一标识，用应用包名package_name表示")
    app_name: Optional[str] = Field(None, description="应用名称")
    version_name: Optional[str] = Field(None, description="版本名称")
    is_packed: Optional[bool] = Field(None, description="是否被加固")
    packer_type: Optional[str] = Field(None, description="加固类型")
    limit: int = Field(100, description="返回数量限制")
    offset: int = Field(0, description="偏移量")


class PackerDetectionRequest(BaseModel):
    """加固检测请求模型"""
    apk_path: Optional[str] = Field(None, description="APK文件路径")
    package_name: Optional[str] = Field(None, description="应用包名")
    device_id: Optional[str] = Field(None, description="设备ID")


class PackerDetectionResult(BaseModel):
    """加固检测结果模型"""
    is_packed: bool = Field(..., description="是否被加固")
    packer_type: Optional[str] = Field(None, description="加固类型")
    confidence: float = Field(..., description="置信度")
    indicators: List[str] = Field(..., description="检测指标")
    detailed_analysis: Dict[str, Any] = Field(..., description="详细分析结果")
    error: Optional[str] = Field(None, description="错误信息")