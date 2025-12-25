from typing import List, Optional
from fastapi import APIRouter, HTTPException, Path as PathParam, Query

from core.tradescript import (
    TradeScriptService,
    TradeScriptStatus,
    TradeScriptCreateRequest,
    TradeScriptUpdateRequest,
    TradeScriptResponse,
    TradeScriptListResponse,
    TradeScriptCreateResponse,
    TradeScriptUpdateResponse
)

router = APIRouter(prefix="/api/tradescripts", tags=["tradescripts"])

tradescript_service = TradeScriptService()


@router.get("", response_model=TradeScriptListResponse, summary="获取所有交易脚本", tags=["tradescripts"])
async def get_all_tradescripts(
    status: Optional[TradeScriptStatus] = Query(None, description="按状态筛选")
):
    """
    获取所有交易脚本列表
    
    可以通过status参数筛选特定状态的脚本
    """
    if status:
        return tradescript_service.get_tradescripts_by_status(status)
    else:
        return tradescript_service.get_all_tradescripts()


@router.get("/{tradescript_id}", response_model=TradeScriptResponse, summary="获取交易脚本详情", tags=["tradescripts"])
async def get_tradescript(
    tradescript_id: str = PathParam(..., description="交易脚本ID")
):
    """
    根据ID获取交易脚本的详细信息
    """
    tradescript = tradescript_service.get_tradescript_by_id(tradescript_id)
    
    if not tradescript:
        raise HTTPException(status_code=404, detail=f"交易脚本 '{tradescript_id}' 不存在")
    
    return tradescript


@router.get("/apk/{apk_package}", response_model=TradeScriptListResponse, summary="获取APK的交易脚本", tags=["tradescripts"])
async def get_tradescripts_by_apk(
    apk_package: str = PathParam(..., description="APK包名")
):
    """
    获取指定APK的所有交易脚本
    """
    return tradescript_service.get_tradescripts_by_apk(apk_package)


@router.post("", response_model=TradeScriptCreateResponse, summary="创建交易脚本", tags=["tradescripts"])
async def create_tradescript(request: TradeScriptCreateRequest):
    """
    创建新的交易脚本
    
    交易脚本定义了交易策略的元数据和执行脚本路径
    """
    return tradescript_service.create_tradescript(request)


@router.put("/{tradescript_id}", response_model=TradeScriptUpdateResponse, summary="更新交易脚本", tags=["tradescripts"])
async def update_tradescript(
    tradescript_id: str = PathParam(..., description="交易脚本ID"),
    request: TradeScriptUpdateRequest = None
):
    """
    更新交易脚本信息
    
    可以更新名称、描述、元数据、脚本路径和状态
    """
    if not tradescript_service.get_tradescript_by_id(tradescript_id):
        raise HTTPException(status_code=404, detail=f"交易脚本 '{tradescript_id}' 不存在")
    
    return tradescript_service.update_tradescript(tradescript_id, request)


@router.delete("/{tradescript_id}", summary="删除交易脚本", tags=["tradescripts"])
async def delete_tradescript(
    tradescript_id: str = PathParam(..., description="交易脚本ID")
):
    """
    删除指定的交易脚本
    
    注意：删除交易脚本会级联删除关联的交易计划
    """
    if not tradescript_service.get_tradescript_by_id(tradescript_id):
        raise HTTPException(status_code=404, detail=f"交易脚本 '{tradescript_id}' 不存在")
    
    success = tradescript_service.delete_tradescript(tradescript_id)
    
    if not success:
        raise HTTPException(status_code=400, detail="删除交易脚本失败")
    
    return {"message": f"交易脚本 '{tradescript_id}' 删除成功"}
