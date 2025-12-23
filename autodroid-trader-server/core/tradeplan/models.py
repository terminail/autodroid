from pydantic import BaseModel, Field
from typing import Optional, Dict, Any, List
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


class TradePlanCreateRequest(BaseModel):
    """创建交易计划请求模型"""
    script_id: Optional[str] = None
    user_id: Optional[str] = None
    name: str
    description: Optional[str] = None
    exchange: Optional[str] = None
    symbol: Optional[str] = None
    symbol_name: Optional[str] = None
    ohlcv: Optional[Dict[str, Any]] = None
    change_percent: Optional[float] = None
    data: Optional[Dict[str, Any]] = None
    status: Optional[TradePlanStatus] = TradePlanStatus.PENDING


class TradePlanUpdateRequest(BaseModel):
    """更新交易计划请求模型"""
    name: Optional[str] = None
    description: Optional[str] = None
    exchange: Optional[str] = None
    symbol: Optional[str] = None
    symbol_name: Optional[str] = None
    ohlcv: Optional[Dict[str, Any]] = None
    change_percent: Optional[float] = None
    data: Optional[Dict[str, Any]] = None
    status: Optional[TradePlanStatus] = None


class TradePlanStatusUpdateRequest(BaseModel):
    """交易计划状态更新请求模型"""
    status: TradePlanStatus = Field(..., description=f"新状态：{', '.join([s.value for s in TradePlanStatus])}")


class TradePlanExecuteRequest(BaseModel):
    """执行交易计划请求模型（已废弃，使用TradePlanStartExecuteRequest）"""
    device_udid: Optional[str] = None


class TradePlanStartExecuteRequest(BaseModel):
    """开始执行交易计划请求模型"""
    device_udid: Optional[str] = None


class TradePlanStopRequest(BaseModel):
    """停止交易计划执行请求模型（已废弃，使用TradePlanStopExecuteRequest）"""
    reason: Optional[str] = None


class TradePlanStopExecuteRequest(BaseModel):
    """停止执行交易计划请求模型"""
    reason: Optional[str] = None


class TradePlanResponse(BaseModel):
    """交易计划响应模型"""
    id: str
    script_id: Optional[str]
    user_id: Optional[str]
    name: str
    description: Optional[str]
    exchange: Optional[str] = None
    symbol: Optional[str] = None
    symbol_name: Optional[str] = None
    ohlcv: Optional[Dict[str, Any]] = None
    change_percent: Optional[float] = None
    data: Optional[Dict[str, Any]]
    status: TradePlanStatus
    created_at: datetime
    started_at: Optional[datetime]
    ended_at: Optional[datetime]
    execution_result: Optional[str] = None
    execution_message: Optional[str] = None


class TradePlanListResponse(BaseModel):
    """交易计划列表响应模型"""
    tradeplans: List[TradePlanResponse]
    total: int


class TradePlanCreateResponse(BaseModel):
    """创建交易计划响应模型"""
    message: str
    tradeplan: Optional[TradePlanResponse] = None


class TradePlanUpdateResponse(BaseModel):
    """更新交易计划响应模型"""
    message: str
    tradeplan: Optional[TradePlanResponse] = None


class TradePlanStatusUpdateResponse(BaseModel):
    """更新交易计划状态响应模型"""
    message: str
    tradeplan: Optional[TradePlanResponse] = None


class TradePlanExecuteResponse(BaseModel):
    """交易计划执行响应模型（已废弃，使用TradePlanStartExecuteResponse）"""
    message: str
    tradeplan_id: str
    status: TradePlanStatus
    execution_result: Optional[str] = None
    execution_message: Optional[str] = None


class TradePlanStartExecuteResponse(BaseModel):
    """开始执行交易计划响应模型"""
    message: str
    tradeplan_id: str
    status: TradePlanStatus
    execution_result: Optional[str] = None
    execution_message: Optional[str] = None


class TradePlanStopExecuteResponse(BaseModel):
    """停止执行交易计划响应模型"""
    message: str
    tradeplan_id: str
    status: TradePlanStatus
    execution_result: Optional[str] = None
    execution_message: Optional[str] = None
