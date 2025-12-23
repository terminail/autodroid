from typing import Optional, Dict, Any, List
from datetime import datetime
import asyncio
import logging

from .database import TradePlanDatabase
from .models import (
    TradePlanStatus,
    TradePlanCreateRequest,
    TradePlanUpdateRequest,
    TradePlanStatusUpdateRequest,
    TradePlanStartExecuteRequest,
    TradePlanStopExecuteRequest,
    TradePlanResponse,
    TradePlanListResponse,
    TradePlanCreateResponse,
    TradePlanUpdateResponse,
    TradePlanStatusUpdateResponse,
    TradePlanStartExecuteResponse,
    TradePlanStopExecuteResponse
)

logger = logging.getLogger(__name__)


class TradePlanService:
    """交易计划服务类"""
    
    def __init__(self):
        """初始化交易计划服务"""
        self.tradeplan_db = TradePlanDatabase()
    
    def create_tradeplan(self, request: TradePlanCreateRequest) -> TradePlanCreateResponse:
        """创建交易计划"""
        try:
            tradeplan_id = self.tradeplan_db.create_tradeplan(
                name=request.name,
                script_id=request.script_id,
                user_id=request.user_id,
                description=request.description,
                exchange=request.exchange,
                symbol=request.symbol,
                symbol_name=request.symbol_name,
                ohlcv=request.ohlcv,
                change_percent=request.change_percent,
                data=request.data,
                status=request.status or TradePlanStatus.PENDING
            )
            
            if not tradeplan_id:
                return TradePlanCreateResponse(
                    message="创建交易计划失败",
                    tradeplan=None
                )
            
            tradeplan_dict = self.tradeplan_db.get_tradeplan_by_id(tradeplan_id)
            if not tradeplan_dict:
                return TradePlanCreateResponse(
                    message="创建交易计划失败",
                    tradeplan=None
                )
            
            return TradePlanCreateResponse(
                message="交易计划创建成功",
                tradeplan=TradePlanResponse(**tradeplan_dict)
            )
            
        except Exception as e:
            logger.error(f"创建交易计划失败: {e}")
            return TradePlanCreateResponse(
                message=f"创建交易计划失败: {str(e)}",
                tradeplan=None
            )
    
    def get_tradeplan_by_id(self, tradeplan_id: str) -> Optional[TradePlanResponse]:
        """根据ID获取交易计划"""
        try:
            tradeplan_dict = self.tradeplan_db.get_tradeplan_by_id(tradeplan_id)
            if not tradeplan_dict:
                return None
            
            return TradePlanResponse(**tradeplan_dict)
            
        except Exception as e:
            logger.error(f"获取交易计划失败: {e}")
            return None
    
    def get_all_tradeplans(self) -> TradePlanListResponse:
        """获取所有交易计划"""
        try:
            tradeplans = self.tradeplan_db.get_all_tradeplans()
            return TradePlanListResponse(
                tradeplans=[TradePlanResponse(**tp) for tp in tradeplans],
                total=len(tradeplans)
            )
        except Exception as e:
            logger.error(f"获取交易计划列表失败: {e}")
            return TradePlanListResponse(tradeplans=[], total=0)
    
    def get_pending_tradeplans(self) -> TradePlanListResponse:
        """获取待批准的交易计划"""
        try:
            tradeplans = self.tradeplan_db.get_pending_tradeplans()
            return TradePlanListResponse(
                tradeplans=[TradePlanResponse(**tp) for tp in tradeplans],
                total=len(tradeplans)
            )
        except Exception as e:
            logger.error(f"获取待批准交易计划失败: {e}")
            return TradePlanListResponse(tradeplans=[], total=0)
    
    def get_approved_tradeplans(self) -> TradePlanListResponse:
        """获取已批准的交易计划"""
        try:
            tradeplans = self.tradeplan_db.get_approved_tradeplans()
            return TradePlanListResponse(
                tradeplans=[TradePlanResponse(**tp) for tp in tradeplans],
                total=len(tradeplans)
            )
        except Exception as e:
            logger.error(f"获取已批准交易计划失败: {e}")
            return TradePlanListResponse(tradeplans=[], total=0)
    
    def update_tradeplan(
        self,
        tradeplan_id: str,
        request: TradePlanUpdateRequest
    ) -> TradePlanUpdateResponse:
        """更新交易计划"""
        try:
            success = self.tradeplan_db.update_tradeplan(
                tradeplan_id=tradeplan_id,
                name=request.name,
                description=request.description,
                exchange=request.exchange,
                symbol=request.symbol,
                symbol_name=request.symbol_name,
                ohlcv=request.ohlcv,
                change_percent=request.change_percent,
                data=request.data,
                status=request.status
            )
            
            if not success:
                return TradePlanUpdateResponse(
                    message="交易计划不存在或更新失败",
                    tradeplan=None
                )
            
            tradeplan = self.get_tradeplan_by_id(tradeplan_id)
            return TradePlanUpdateResponse(
                message="交易计划更新成功",
                tradeplan=tradeplan
            )
            
        except Exception as e:
            logger.error(f"更新交易计划失败: {e}")
            return TradePlanUpdateResponse(
                message=f"更新交易计划失败: {str(e)}",
                tradeplan=None
            )
    
    def update_tradeplan_status(
        self,
        tradeplan_id: str,
        request: TradePlanStatusUpdateRequest
    ) -> TradePlanStatusUpdateResponse:
        """更新交易计划状态"""
        try:
            success = self.tradeplan_db.update_tradeplan_status(
                tradeplan_id=tradeplan_id,
                status=request.status
            )
            
            if not success:
                return TradePlanStatusUpdateResponse(
                    message="交易计划不存在或状态更新失败",
                    tradeplan=None
                )
            
            tradeplan = self.get_tradeplan_by_id(tradeplan_id)
            return TradePlanStatusUpdateResponse(
                message="交易计划状态更新成功",
                tradeplan=tradeplan
            )
            
        except Exception as e:
            logger.error(f"更新交易计划状态失败: {e}")
            return TradePlanStatusUpdateResponse(
                message=f"更新交易计划状态失败: {str(e)}",
                tradeplan=None
            )
    
    def execute_tradeplan(
        self,
        tradeplan_id: str,
        request: TradePlanStartExecuteRequest
    ) -> TradePlanStartExecuteResponse:
        """执行交易计划（异步执行，支持实时状态更新）"""
        try:
            tradeplan = self.tradeplan_db.get_tradeplan_by_id(tradeplan_id)
            if not tradeplan:
                return TradePlanStartExecuteResponse(
                    message="交易计划不存在",
                    tradeplan_id=tradeplan_id,
                    status=TradePlanStatus.FAILED
                )
            
            if tradeplan["status"] != TradePlanStatus.APPROVED.value:
                return TradePlanStartExecuteResponse(
                    message="只有已批准的交易计划才能执行",
                    tradeplan_id=tradeplan_id,
                    status=TradePlanStatus.FAILED
                )
            
            # 更新状态为执行中
            self.tradeplan_db.update_tradeplan_status(tradeplan_id, TradePlanStatus.EXECUTING)
            self.tradeplan_db.update_tradeplan_execution_time(
                tradeplan_id,
                started_at=datetime.now()
            )
            
            # 启动异步执行任务
            asyncio.create_task(self._execute_tradeplan_async(tradeplan_id, tradeplan))
            
            return TradePlanStartExecuteResponse(
                message=f"交易计划开始执行: {tradeplan['name']}",
                tradeplan_id=tradeplan_id,
                status=TradePlanStatus.EXECUTING
            )
            
        except Exception as e:
            logger.error(f"启动交易计划执行失败: {e}")
            self.tradeplan_db.update_tradeplan_status(tradeplan_id, TradePlanStatus.FAILED)
            return TradePlanStartExecuteResponse(
                message=f"启动执行失败: {str(e)}",
                tradeplan_id=tradeplan_id,
                status=TradePlanStatus.FAILED
            )
    
    async def _execute_tradeplan_async(self, tradeplan_id: str, tradeplan: Dict[str, Any]):
        """异步执行交易计划（实际执行逻辑）"""
        try:
            logger.info(f"开始执行交易计划: {tradeplan_id}")
            
            # 模拟执行过程（实际项目中这里应该调用真正的执行引擎）
            # 可以在这里更新执行进度和状态
            for i in range(1, 6):
                await asyncio.sleep(1)
                logger.info(f"交易计划 {tradeplan_id} 执行进度: {i * 20}%")
            
            # 执行完成
            self.tradeplan_db.update_tradeplan_status(tradeplan_id, TradePlanStatus.COMPLETED)
            self.tradeplan_db.update_tradeplan_execution_time(
                tradeplan_id,
                ended_at=datetime.now()
            )
            self.tradeplan_db.update_tradeplan_execution_result(
                tradeplan_id,
                execution_result="SUCCESS",
                execution_message="交易计划执行成功"
            )
            
            logger.info(f"交易计划执行完成: {tradeplan_id}")
            
        except Exception as e:
            logger.error(f"执行交易计划失败: {e}")
            self.tradeplan_db.update_tradeplan_status(tradeplan_id, TradePlanStatus.FAILED)
            self.tradeplan_db.update_tradeplan_execution_time(
                tradeplan_id,
                ended_at=datetime.now()
            )
            self.tradeplan_db.update_tradeplan_execution_result(
                tradeplan_id,
                execution_result="FAILED",
                execution_message=f"执行失败: {str(e)}"
            )
    
    def stop_tradeplan(
        self,
        tradeplan_id: str,
        request: TradePlanStopExecuteRequest
    ) -> TradePlanStopExecuteResponse:
        """停止正在执行的交易计划"""
        try:
            tradeplan = self.tradeplan_db.get_tradeplan_by_id(tradeplan_id)
            if not tradeplan:
                return TradePlanStopExecuteResponse(
                    message="交易计划不存在",
                    tradeplan_id=tradeplan_id,
                    status=TradePlanStatus.FAILED
                )
            
            if tradeplan["status"] != TradePlanStatus.EXECUTING.value:
                return TradePlanStopExecuteResponse(
                    message="只有正在执行中的交易计划才能停止",
                    tradeplan_id=tradeplan_id,
                    status=tradeplan["status"]
                )
            
            # 更新状态为已停止
            self.tradeplan_db.update_tradeplan_status(tradeplan_id, TradePlanStatus.FAILED)
            self.tradeplan_db.update_tradeplan_execution_time(
                tradeplan_id,
                ended_at=datetime.now()
            )
            self.tradeplan_db.update_tradeplan_execution_result(
                tradeplan_id,
                execution_result="STOPPED",
                execution_message=request.reason or "用户手动停止"
            )
            
            return TradePlanStopExecuteResponse(
                message=f"交易计划已停止: {tradeplan['name']}",
                tradeplan_id=tradeplan_id,
                status=TradePlanStatus.FAILED,
                execution_result="STOPPED",
                execution_message=request.reason or "用户手动停止"
            )
            
        except Exception as e:
            logger.error(f"停止交易计划失败: {e}")
            return TradePlanStopExecuteResponse(
                message=f"停止失败: {str(e)}",
                tradeplan_id=tradeplan_id,
                status=TradePlanStatus.FAILED
            )
    
    def delete_tradeplan(self, tradeplan_id: str) -> bool:
        """删除交易计划"""
        try:
            return self.tradeplan_db.delete_tradeplan(tradeplan_id)
        except Exception as e:
            logger.error(f"删除交易计划失败: {e}")
            return False
    
    def create_demo_tradeplans(self) -> Dict[str, Any]:
        """创建演示用的交易计划数据"""
        try:
            created_count = self.tradeplan_db.create_demo_tradeplans()
            return {
                "message": f"成功创建 {created_count} 个演示交易计划",
                "created_count": created_count
            }
        except Exception as e:
            logger.error(f"创建演示交易计划失败: {e}")
            return {
                "message": f"创建演示交易计划失败: {str(e)}",
                "created_count": 0
            }
