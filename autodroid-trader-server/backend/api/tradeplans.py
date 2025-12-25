"""
Trade Plan API endpoints for Autodroid system.
Handles trade plan CRUD operations, status management, and execution.
"""

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
    TradePlanStartExecuteRequest,
    TradePlanStartExecuteResponse,
    TradePlanStopExecuteRequest,
    TradePlanStopExecuteResponse,
    TradePlanResponse,
    TradePlanListResponse,
)
from core.tradeplan.service import TradePlanService
from core.tradeplan.daemon import (
    get_daemon,
    TaskType,
    create_task
)

# Initialize router
router = APIRouter(prefix="/api/tradeplans", tags=["tradeplans"])

# Initialize trade plan service
tradeplan_service = TradePlanService()


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


@router.post("/{tradeplan_id}/start", response_model=TradePlanStartExecuteResponse)
async def start_tradeplan(tradeplan_id: str, request: TradePlanStartExecuteRequest = TradePlanStartExecuteRequest()):
    """启动单个交易计划（异步执行，支持实时状态更新）"""
    try:
        result = tradeplan_service.execute_tradeplan(tradeplan_id, request)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/{tradeplan_id}/stop", response_model=TradePlanStopExecuteResponse)
async def stop_tradeplan(tradeplan_id: str, request: TradePlanStopExecuteRequest = TradePlanStopExecuteRequest()):
    """停止正在执行的交易计划"""
    try:
        result = tradeplan_service.stop_tradeplan(tradeplan_id, request)
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
        result = tradeplan_service.create_or_update_demo_tradeplans()
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/start-approved")
async def start_approved_tradeplans():
    """开始执行所有已批准的交易计划"""
    try:
        daemon = get_daemon()
        task = create_task(TaskType.START_ALL_APPROVED)
        task_id = daemon.submit_task(task)
        return AsyncTaskResponse(
            message="已提交执行所有已批准交易计划的任务",
            task_id=task_id
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/stop-approved")
async def stop_approved_tradeplans():
    """停止所有正在执行的交易计划"""
    try:
        daemon = get_daemon()
        task = create_task(TaskType.STOP_ALL)
        task_id = daemon.submit_task(task)
        return AsyncTaskResponse(
            message="已提交停止所有正在执行的交易计划的任务",
            task_id=task_id
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
