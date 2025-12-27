from typing import Optional, Dict, Any, List
from datetime import datetime
import asyncio
import json
import logging

from .database import TradePlanDatabase
from ..database.models import TradePlan as TradePlanModel
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
    
    def _to_tradeplan_response(self, tradeplan: TradePlanModel) -> TradePlanResponse:
        """将 Peewee TradePlan 模型转换为 TradePlanResponse Pydantic 模型"""
        return TradePlanResponse(
            id=tradeplan.id,
            script_id=tradeplan.script.id if tradeplan.script else None,
            user_id=tradeplan.user_id,
            name=tradeplan.name,
            description=tradeplan.description,
            exchange=tradeplan.exchange,
            symbol=tradeplan.symbol,
            symbol_name=tradeplan.symbol_name,
            ohlcv=json.loads(tradeplan.ohlcv) if tradeplan.ohlcv else None,
            change_percent=tradeplan.change_percent,
            data=json.loads(tradeplan.data) if tradeplan.data else None,
            status=TradePlanStatus(tradeplan.status),
            executable=tradeplan.executable,
            created_at=tradeplan.created_at,
            started_at=tradeplan.started_at,
            ended_at=tradeplan.ended_at,
            execution_result=tradeplan.execution_result,
            execution_message=tradeplan.execution_message
        )
    
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
            
            tradeplan = self.tradeplan_db.get_tradeplan_by_id(tradeplan_id)
            if not tradeplan:
                return TradePlanCreateResponse(
                    message="创建交易计划失败",
                    tradeplan=None
                )
            
            return TradePlanCreateResponse(
                message="交易计划创建成功",
                trade_plan_response=self._to_tradeplan_response(tradeplan)
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
            tradeplan = self.tradeplan_db.get_tradeplan_by_id(tradeplan_id)
            if not tradeplan:
                return None
            
            return self._to_tradeplan_response(tradeplan)
            
        except Exception as e:
            logger.error(f"获取交易计划失败: {e}")
            return None
    
    def get_all_tradeplans(self) -> TradePlanListResponse:
        """获取所有交易计划"""
        try:
            tradeplans = self.tradeplan_db.get_all_tradeplans()
            return TradePlanListResponse(
                tradeplans=[self._to_tradeplan_response(tp) for tp in tradeplans],
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
                tradeplans=[self._to_tradeplan_response(tp) for tp in tradeplans],
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
                tradeplans=[self._to_tradeplan_response(tp) for tp in tradeplans],
                total=len(tradeplans)
            )
        except Exception as e:
            logger.error(f"获取已批准交易计划失败: {e}")
            return TradePlanListResponse(tradeplans=[], total=0)
    
    def get_executing_tradeplans(self) -> TradePlanListResponse:
        """获取正在执行的交易计划"""
        try:
            tradeplans = self.tradeplan_db.get_tradeplans_by_status(TradePlanStatus.EXECUTING)
            return TradePlanListResponse(
                tradeplans=[self._to_tradeplan_response(tp) for tp in tradeplans],
                total=len(tradeplans)
            )
        except Exception as e:
            logger.error(f"获取正在执行的交易计划失败: {e}")
            return TradePlanListResponse(tradeplans=[], total=0)
    
    def get_next_executable_tradeplan(self) -> Optional[TradePlanResponse]:
        """获取按创建时间排序的第一个可执行的交易计划"""
        try:
            tradeplan = self.tradeplan_db.get_next_executable_tradeplan()
            if not tradeplan:
                return None
            return self._to_tradeplan_response(tradeplan)
        except Exception as e:
            logger.error(f"获取下一个可执行交易计划失败: {e}")
            return None
    
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
                status=request.status,
                executable=request.executable,
                execution_result=request.execution_result,
                execution_message=request.execution_message
            )
            
            if not success:
                return TradePlanStatusUpdateResponse(
                    success=False,
                    message="交易计划不存在或状态更新失败",
                    trade_plan_response=None
                )
            
            tradeplan = self.get_tradeplan_by_id(tradeplan_id)
            return TradePlanStatusUpdateResponse(
                success=True,
                message="交易计划状态更新成功",
                trade_plan_response=tradeplan
            )
            
        except Exception as e:
            logger.error(f"更新交易计划状态失败: {e}")
            return TradePlanStatusUpdateResponse(
                success=False,
                message=f"更新交易计划状态失败: {str(e)}",
                trade_plan_response=None
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
            
            if tradeplan.status != TradePlanStatus.APPROVED.value:
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
                message=f"交易计划开始执行: {tradeplan.name}",
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
            
            if tradeplan.status != TradePlanStatus.EXECUTING.value:
                return TradePlanStopExecuteResponse(
                    message="只有正在执行中的交易计划才能停止",
                    tradeplan_id=tradeplan_id,
                    status=tradeplan.status
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
                message=f"交易计划已停止: {tradeplan.name}",
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
    
    def create_or_update_demo_tradeplans(self) -> Dict[str, Any]:
        """创建或更新演示用的交易计划数据"""
        try:
            print("[SERVICE DEBUG] 开始创建演示交易计划...")
            result = self.tradeplan_db.create_or_update_demo_tradeplans()
            print(f"[SERVICE DEBUG] 数据库层返回结果: {result}")
            return {
                "message": f"成功创建 {result['created']} 个演示交易计划，更新 {result['updated']} 个",
                "created_count": result['created'],
                "updated_count": result['updated']
            }
        except Exception as e:
            logger.error(f"创建/更新演示交易计划失败: {e}")
            return {
                "message": f"创建/更新演示交易计划失败: {str(e)}",
                "created_count": 0,
                "updated_count": 0
            }
    
    def start_approved_tradeplans(self) -> Dict[str, Any]:
        """开始执行所有已批准的交易计划"""
        try:
            approved_response = self.get_approved_tradeplans()
            approved_plans = approved_response.tradeplans if approved_response.tradeplans else []
            
            if not approved_plans:
                return {
                    "message": "没有已批准的交易计划",
                    "started_count": 0,
                    "failed_count": 0
                }
            
            started_count = 0
            failed_count = 0
            
            for plan in approved_plans:
                tradeplan_id = plan.id
                request = TradePlanStartExecuteRequest()
                result = self.execute_tradeplan(tradeplan_id, request)
                if result.status == TradePlanStatus.EXECUTING:
                    started_count += 1
                else:
                    failed_count += 1
            
            return {
                "message": f"已启动 {started_count} 个交易计划，失败 {failed_count} 个",
                "started_count": started_count,
                "failed_count": failed_count
            }
            
        except Exception as e:
            logger.error(f"批量启动交易计划失败: {e}")
            return {
                "message": f"批量启动交易计划失败: {str(e)}",
                "started_count": 0,
                "failed_count": 0
            }
    
    def stop_approved_tradeplans(self) -> Dict[str, Any]:
        """停止所有正在执行的交易计划"""
        try:
            all_tradeplans = self.get_all_tradeplans()
            executing_plans = [p for p in all_tradeplans.tradeplans if p.status == TradePlanStatus.EXECUTING.value]
            
            if not executing_plans:
                return {
                    "message": "没有正在执行的交易计划",
                    "stopped_count": 0,
                    "failed_count": 0
                }
            
            stopped_count = 0
            failed_count = 0
            
            for plan in executing_plans:
                tradeplan_id = plan.id
                request = TradePlanStopExecuteRequest(reason="用户手动批量停止")
                result = self.stop_tradeplan(tradeplan_id, request)
                if result.status != TradePlanStatus.EXECUTING.value:
                    stopped_count += 1
                else:
                    failed_count += 1
            
            return {
                "message": f"已停止 {stopped_count} 个交易计划，失败 {failed_count} 个",
                "stopped_count": stopped_count,
                "failed_count": failed_count
            }
            
        except Exception as e:
            logger.error(f"批量停止交易计划失败: {e}")
            return {
                "message": f"批量停止交易计划失败: {str(e)}",
                "stopped_count": 0,
                "failed_count": 0
            }
