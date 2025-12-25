from pydantic import BaseModel, Field
from typing import Optional, Dict, Any, List
from datetime import datetime
from enum import Enum


class TradeScriptStatus(str, Enum):
    """交易脚本状态枚举"""
    NEW = "NEW"
    INPROGRESS = "INPROGRESS"
    FAILED = "FAILED"
    OK = "OK"


class TradeScriptCreateRequest(BaseModel):
    """创建交易脚本请求模型"""
    apk_package: str
    name: str
    description: Optional[str] = None
    metadata: Optional[Dict[str, Any]] = None
    script_path: str
    status: Optional[TradeScriptStatus] = TradeScriptStatus.NEW


class TradeScriptUpdateRequest(BaseModel):
    """更新交易脚本请求模型"""
    name: Optional[str] = None
    description: Optional[str] = None
    metadata: Optional[Dict[str, Any]] = None
    script_path: Optional[str] = None
    status: Optional[TradeScriptStatus] = None


class TradeScriptResponse(BaseModel):
    """交易脚本响应模型"""
    id: str
    apk_package: str
    name: str
    description: Optional[str]
    metadata: Dict[str, Any]
    script_path: str
    status: TradeScriptStatus
    created_at: datetime


class TradeScriptListResponse(BaseModel):
    """交易脚本列表响应模型"""
    tradescripts: List[TradeScriptResponse]
    total: int


class TradeScriptCreateResponse(BaseModel):
    """创建交易脚本响应模型"""
    message: str
    tradescript: Optional[TradeScriptResponse] = None


class TradeScriptUpdateResponse(BaseModel):
    """更新交易脚本响应模型"""
    message: str
    tradescript: Optional[TradeScriptResponse] = None
