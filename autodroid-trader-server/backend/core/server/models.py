"""
服务器模块的数据模型定义
包含Pydantic请求/响应模型
"""

from typing import Dict, Any, Optional
from datetime import datetime
from pydantic import BaseModel, Field


class ServerInfoResponse(BaseModel):
    """服务器信息响应模型"""
    ip: str = Field(..., description="服务器IP地址")
    port: int = Field(..., description="服务器端口")
    name: str = Field(..., description="服务器名称")
    platform: str = Field(..., description="服务器平台")
    services: Dict[str, str] = Field(default_factory=dict, description="服务器提供的服务")
    capabilities: Dict[str, bool] = Field(default_factory=dict, description="服务器能力")


class ServerCreateRequest(BaseModel):
    """服务器创建请求模型"""
    ip: str = Field(..., description="服务器IP地址")
    port: int = Field(..., description="服务器端口")
    name: Optional[str] = Field(None, description="服务器名称")
    platform: Optional[str] = Field(None, description="服务器平台")
    services: Optional[Dict[str, str]] = Field(default_factory=dict, description="服务器提供的服务")
    capabilities: Optional[Dict[str, bool]] = Field(default_factory=dict, description="服务器能力")


class ServerUpdateRequest(BaseModel):
    """服务器更新请求模型"""
    name: Optional[str] = Field(None, description="服务器名称")
    platform: Optional[str] = Field(None, description="服务器平台")
    services: Optional[Dict[str, str]] = Field(None, description="服务器提供的服务")
    capabilities: Optional[Dict[str, bool]] = Field(None, description="服务器能力")



class ServerCreateResponse(BaseModel):
    """服务器创建响应模型"""
    success: bool = Field(..., description="操作是否成功")
    message: str = Field(..., description="响应消息")
    server: Optional[ServerInfoResponse] = Field(None, description="服务器信息")


class ServerUpdateResponse(BaseModel):
    """服务器更新响应模型"""
    success: bool = Field(..., description="操作是否成功")
    message: str = Field(..., description="响应消息")
    server: Optional[ServerInfoResponse] = Field(None, description="服务器信息")


class ServerDeleteResponse(BaseModel):
    """服务器删除响应模型"""
    success: bool = Field(..., description="操作是否成功")
    message: str = Field(..., description="响应消息")


class QRCodeResponse(BaseModel):
    """二维码生成响应模型"""
    success: bool = Field(..., description="操作是否成功")
    message: str = Field(..., description="响应消息")
    qr_code_data: Optional[str] = Field(None, description="二维码数据")
    qr_code_image: Optional[str] = Field(None, description="二维码图像（Base64编码）")
    expires_at: Optional[datetime] = Field(None, description="二维码过期时间")