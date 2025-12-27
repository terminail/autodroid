from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from datetime import datetime
from enum import Enum


class TradePlanStatus(str, Enum):
    """交易计划状态枚举"""
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"
    EXECUTING = "EXECUTING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"


class Ohlcv(BaseModel):
    """OHLCV 数据类"""
    open: Optional[float] = None
    high: Optional[float] = None
    low: Optional[float] = None
    close: Optional[float] = None
    volume: Optional[float] = None


class TradePlanResponse(BaseModel):
    """交易计划响应模型"""
    id: str
    script_id: Optional[str] = None
    user_id: Optional[str] = None
    name: str
    description: Optional[str] = None
    exchange: Optional[str] = None
    symbol: Optional[str] = None
    symbol_name: Optional[str] = None
    ohlcv: Optional[Ohlcv] = None
    change_percent: Optional[float] = None
    data: Optional[Dict[str, Any]] = None
    status: TradePlanStatus
    executable: bool = True
    created_at: datetime
    started_at: Optional[datetime] = None
    ended_at: Optional[datetime] = None
    execution_result: Optional[str] = None
    execution_message: Optional[str] = None


class TradePlanListResponse(BaseModel):
    """交易计划列表响应模型"""
    tradeplans: List[TradePlanResponse]
    total: int


class TradeScriptResponse(BaseModel):
    """交易脚本响应模型"""
    id: str
    apk_package: str
    apk_flow: str
    name: str
    description: Optional[str] = None
    metadata: Dict[str, Any] = {}
    script_path: str
    created_at: datetime
    updated_at: datetime


class TradeScriptListResponse(BaseModel):
    """交易脚本列表响应模型"""
    tradescripts: List[TradeScriptResponse]
    total: int