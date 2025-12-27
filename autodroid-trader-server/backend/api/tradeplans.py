"""
Trade Plan API endpoints for Autodroid system.
Handles trade plan CRUD operations, status management, and execution.
"""

import logging
from fastapi import APIRouter, HTTPException
from typing import List, Optional
from pydantic import BaseModel

from core.tradeplan.models import (
    TradePlanCreateRequest,
    TradePlanCreateResponse,
    TradePlanUpdateRequest,
    TradePlanUpdateResponse,
    TradePlanStatusUpdateRequest,
    TradePlanStatusUpdateResponse,
    TradePlanResponse,
    TradePlanListResponse,
)
from core.tradeplan.service import TradePlanService
from core.tradeplan.database import TradePlanDatabase

logger = logging.getLogger(__name__)

# Initialize router
router = APIRouter(prefix="/api/tradeplans", tags=["tradeplans"])

# Initialize trade plan service
tradeplan_service = TradePlanService()
tradeplan_db = TradePlanDatabase()


class AsyncTaskResponse(BaseModel):
    """异步任务响应"""
    success: bool = True
    message: str
    task_id: Optional[str] = None


class TaskStatusResponse(BaseModel):
    """任务状态响应"""
    task_id: str
    task_type: str
    status: str
    message: str
    created_at: str
    result: Optional[dict] = None


@router.get("", response_model=TradePlanListResponse)
async def get_all_tradeplans():
    """获取所有交易计划"""
    try:
        return tradeplan_service.get_all_tradeplans()
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/pending", response_model=TradePlanListResponse)
async def get_pending_tradeplans():
    """获取待批准的交易计划"""
    try:
        return tradeplan_service.get_pending_tradeplans()
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/approved", response_model=TradePlanListResponse)
async def get_approved_tradeplans():
    """获取已批准的交易计划"""
    try:
        return tradeplan_service.get_approved_tradeplans()
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/execute/nextable", response_model=TradePlanListResponse)
async def get_next_executable_tradeplan():
    """获取按创建时间排序的第一个可执行的交易计划"""
    try:
        tradeplan = tradeplan_service.get_next_executable_tradeplan()
        if not tradeplan:
            return TradePlanListResponse(tradeplans=[], total=0)
        return TradePlanListResponse(tradeplans=[tradeplan], total=1)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/execute/start")
async def execute_start_tradeplans():
    """开始执行所有已批准的交易计划（将可执行状态设为True）"""
    try:
        approved_result = tradeplan_service.get_approved_tradeplans()
        tradeplan_ids = [tp.id for tp in approved_result.tradeplans]
        
        if tradeplan_ids:
            tradeplan_db.batch_update_tradeplan_executable(tradeplan_ids, True)
            return AsyncTaskResponse(
                success=True,
                message=f"已将 {len(tradeplan_ids)} 个已批准的交易计划标记为可执行",
                task_id=None
            )
        else:
            return AsyncTaskResponse(
                success=True,
                message="没有找到已批准的交易计划",
                task_id=None
            )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/execute/stop")
async def execute_stop_tradeplans():
    """停止执行所有交易计划（将所有交易计划的可执行状态设为False）"""
    try:
        all_tradeplans = tradeplan_service.get_all_tradeplans()
        tradeplan_ids = [tp.id for tp in all_tradeplans.tradeplans]
        
        if tradeplan_ids:
            tradeplan_db.batch_update_tradeplan_executable(tradeplan_ids, False)
            return AsyncTaskResponse(
                success=True,
                message=f"已将 {len(tradeplan_ids)} 个交易计划标记为不可执行",
                task_id=None
            )
        else:
            return AsyncTaskResponse(
                success=True,
                message="没有找到交易计划",
                task_id=None
            )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/{tradeplan_id}", response_model=TradePlanResponse)
async def get_tradeplan(tradeplan_id: str):
    """根据ID获取交易计划"""
    try:
        tradeplan = tradeplan_service.get_tradeplan_by_id(tradeplan_id)
        if not tradeplan:
            raise HTTPException(status_code=404, detail="交易计划不存在")
        return tradeplan
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("", response_model=TradePlanCreateResponse)
async def create_tradeplan(request: TradePlanCreateRequest):
    """创建新的交易计划"""
    try:
        result = tradeplan_service.create_tradeplan(request)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.put("/{tradeplan_id}", response_model=TradePlanUpdateResponse)
async def update_tradeplan(tradeplan_id: str, request: TradePlanUpdateRequest):
    """更新交易计划"""
    try:
        result = tradeplan_service.update_tradeplan(tradeplan_id, request)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.patch("/{tradeplan_id}/status", response_model=TradePlanStatusUpdateResponse)
async def update_tradeplan_status(tradeplan_id: str, request: TradePlanStatusUpdateRequest):
    """更新交易计划状态（单个）"""
    try:
        result = tradeplan_service.update_tradeplan_status(tradeplan_id, request)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))



@router.delete("/{tradeplan_id}")
async def delete_tradeplan(tradeplan_id: str):
    """删除交易计划"""
    try:
        success = tradeplan_service.delete_tradeplan(tradeplan_id)
        if not success:
            raise HTTPException(status_code=404, detail="交易计划不存在或删除失败")
        return {"message": "交易计划删除成功"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/demo/create")
async def create_or_update_demo_tradeplans():
    """创建或更新演示用的交易计划数据"""
    try:
        print("[API DEBUG] 收到 /demo/create 请求")
        result = tradeplan_service.create_or_update_demo_tradeplans()
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
