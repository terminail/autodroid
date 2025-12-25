"""
交易计划执行 Daemon
使用异步任务队列处理交易计划的执行，支持后台运行和状态监控
"""

import asyncio
import logging
import uuid
from datetime import datetime
from typing import Optional, Dict, Any, List
from enum import Enum
from dataclasses import dataclass, field
from asyncio import Queue

from .database import TradePlanDatabase
from .models import TradePlanStatus

logger = logging.getLogger(__name__)


class TaskType(str, Enum):
    """任务类型"""
    START_SINGLE = "start_single"
    START_ALL_APPROVED = "start_all_approved"
    STOP_SINGLE = "stop_single"
    STOP_ALL = "stop_all"


@dataclass
class TradePlanTask:
    """交易计划任务"""
    task_id: str
    task_type: TaskType
    tradeplan_id: Optional[str] = None
    reason: Optional[str] = None
    created_at: datetime = field(default_factory=datetime.now)
    status: str = "pending"
    message: str = ""
    result: Optional[Dict[str, Any]] = None


class TradePlanDaemon:
    """
    交易计划执行 Daemon
    负责异步处理交易计划的启动和停止任务
    """
    
    _instance: Optional['TradePlanDaemon'] = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance
    
    def __init__(self):
        if self._initialized:
            return
        self._initialized = True
        
        self.task_queue: Queue = Queue()
        self.db = TradePlanDatabase()
        self.running = False
        self.worker_task: Optional[asyncio.Task] = None
        self.task_history: Dict[str, TradePlanTask] = {}
        self.max_history_size = 100
        
        logger.info("交易计划 Daemon 已初始化")
    
    async def start(self):
        """启动 Daemon 工作循环"""
        if self.running:
            logger.warning("Daemon 已在运行中")
            return
        
        self.running = True
        self.worker_task = asyncio.create_task(self._worker_loop())
        logger.info("交易计划 Daemon 已启动")
    
    async def stop(self):
        """停止 Daemon"""
        if not self.running:
            return
        
        self.running = False
        if self.worker_task:
            self.worker_task.cancel()
            try:
                await self.worker_task
            except asyncio.CancelledError:
                pass
        logger.info("交易计划 Daemon 已停止")
    
    async def _worker_loop(self):
        """工作循环：处理任务队列"""
        logger.info("Daemon worker loop started")
        while self.running:
            try:
                task = await asyncio.wait_for(
                    self.task_queue.get(),
                    timeout=1.0
                )
                logger.info(f"Got task from queue: {task.task_id} - {task.task_type}")
                await self._process_task(task)
                self.task_queue.task_done()
            except asyncio.TimeoutError:
                continue
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"处理任务时出错: {e}")
    
    async def _process_task(self, task: TradePlanTask):
        """处理单个任务"""
        task.status = "running"
        task.message = "任务正在执行"
        
        try:
            if task.task_type == TaskType.START_SINGLE:
                result = await self._execute_single_plan(task.tradeplan_id)
            elif task.task_type == TaskType.START_ALL_APPROVED:
                result = await self._execute_all_approved()
            elif task.task_type == TaskType.STOP_SINGLE:
                result = await self._stop_single_plan(task.tradeplan_id, task.reason)
            elif task.task_type == TaskType.STOP_ALL:
                result = await self._stop_all_executing()
            else:
                result = {"success": False, "message": "未知任务类型"}
            
            task.result = result
            task.status = "completed"
            task.message = result.get("message", "任务完成")
            
        except Exception as e:
            task.status = "failed"
            task.message = f"任务执行失败: {str(e)}"
            task.result = {"success": False, "message": str(e)}
            logger.error(f"任务 {task.task_id} 执行失败: {e}")
        
        self._add_to_history(task)
    
    async def _execute_single_plan(self, tradeplan_id: str) -> Dict[str, Any]:
        """执行单个交易计划"""
        tradeplan = self.db.get_tradeplan_by_id(tradeplan_id)
        if not tradeplan:
            return {"success": False, "message": "交易计划不存在"}
        
        if tradeplan["status"] != TradePlanStatus.APPROVED.value:
            return {"success": False, "message": "只有已批准的交易计划才能执行"}
        
        self.db.update_tradeplan_status(tradeplan_id, TradePlanStatus.EXECUTING)
        self.db.update_tradeplan_execution_time(tradeplan_id, started_at=datetime.now())
        
        try:
            await self._run_tradeplan_execution(tradeplan_id, tradeplan)
            return {"success": True, "message": f"交易计划执行完成: {tradeplan['name']}"}
        except Exception as e:
            self.db.update_tradeplan_status(tradeplan_id, TradePlanStatus.FAILED)
            self.db.update_tradeplan_execution_time(tradeplan_id, ended_at=datetime.now())
            self.db.update_tradeplan_execution_result(
                tradeplan_id,
                execution_result="FAILED",
                execution_message=str(e)
            )
            return {"success": False, "message": f"执行失败: {str(e)}"}
    
    async def _execute_all_approved(self) -> Dict[str, Any]:
        """执行所有已批准的交易计划"""
        approved_plans = self.db.get_approved_tradeplans()
        
        if not approved_plans:
            return {"success": True, "message": "没有已批准的交易计划", "started": 0}
        
        started_count = 0
        for plan in approved_plans:
            try:
                tradeplan_id = plan["id"]
                self.db.update_tradeplan_status(tradeplan_id, TradePlanStatus.EXECUTING)
                self.db.update_tradeplan_execution_time(tradeplan_id, started_at=datetime.now())
                
                await self._run_tradeplan_execution(tradeplan_id, plan)
                started_count += 1
            except Exception as e:
                logger.error(f"执行交易计划 {plan['id']} 失败: {e}")
        
        return {
            "success": True,
            "message": f"已启动 {started_count} 个交易计划",
            "started": started_count
        }
    
    async def _run_tradeplan_execution(self, tradeplan_id: str, tradeplan: Dict[str, Any]):
        """运行交易计划执行逻辑"""
        from datetime import datetime as dt
        
        start_time = dt.now()
        tradeplan_name = tradeplan.get('name', tradeplan_id)
        
        logger.info(f"开始执行 {tradeplan_name}")
        
        if tradeplan.get("data"):
            logger.info(f"交易计划配置: {tradeplan['data']}")
        
        self.db.update_tradeplan_execution_result(
            tradeplan_id,
            execution_result="RUNNING",
            execution_message=f"正在执行 {tradeplan_name}..."
        )
        
        for i in range(10):
            await asyncio.sleep(1)
            self.db.update_tradeplan_execution_result(
                tradeplan_id,
                execution_result="RUNNING",
                execution_message=f"执行中... {i+1}/10 秒"
            )
        
        end_time = dt.now()
        duration = end_time - start_time
        duration_minutes = duration.total_seconds() / 60
        
        self.db.update_tradeplan_status(tradeplan_id, TradePlanStatus.COMPLETED)
        self.db.update_tradeplan_execution_time(tradeplan_id, ended_at=end_time)
        self.db.update_tradeplan_execution_result(
            tradeplan_id,
            execution_result="SUCCESS",
            execution_message="交易计划执行成功"
        )
        
        logger.info(f"结束执行 {tradeplan_name}，花费时间 {duration_minutes:.1f} 分钟")
    
    async def _stop_single_plan(self, tradeplan_id: str, reason: Optional[str]) -> Dict[str, Any]:
        """停止单个交易计划"""
        tradeplan = self.db.get_tradeplan_by_id(tradeplan_id)
        if not tradeplan:
            return {"success": False, "message": "交易计划不存在"}
        
        if tradeplan["status"] != TradePlanStatus.EXECUTING.value:
            return {"success": False, "message": "只有正在执行的交易计划才能停止"}
        
        self.db.update_tradeplan_status(tradeplan_id, TradePlanStatus.FAILED)
        self.db.update_tradeplan_execution_time(tradeplan_id, ended_at=datetime.now())
        self.db.update_tradeplan_execution_result(
            tradeplan_id,
            execution_result="STOPPED",
            execution_message=reason or "用户手动停止"
        )
        
        return {"success": True, "message": f"交易计划已停止: {tradeplan['name']}"}
    
    async def _stop_all_executing(self) -> Dict[str, Any]:
        """停止所有正在执行的交易计划"""
        all_plans = self.db.get_all_tradeplans()
        executing = [p for p in all_plans if p["status"] == TradePlanStatus.EXECUTING.value]
        
        if not executing:
            return {"success": True, "message": "没有正在执行的交易计划", "stopped": 0}
        
        stopped_count = 0
        for plan in executing:
            tradeplan_id = plan["id"]
            self.db.update_tradeplan_status(tradeplan_id, TradePlanStatus.FAILED)
            self.db.update_tradeplan_execution_time(tradeplan_id, ended_at=datetime.now())
            self.db.update_tradeplan_execution_result(
                tradeplan_id,
                execution_result="STOPPED",
                execution_message="批量停止"
            )
            stopped_count += 1
        
        return {
            "success": True,
            "message": f"已停止 {stopped_count} 个交易计划",
            "stopped": stopped_count
        }
    
    def _add_to_history(self, task: TradePlanTask):
        """添加任务到历史记录"""
        self.task_history[task.task_id] = task
        if len(self.task_history) > self.max_history_size:
            oldest_key = next(iter(self.task_history))
            del self.task_history[oldest_key]
    
    def submit_task(self, task: TradePlanTask) -> str:
        """提交任务到队列"""
        self.task_queue.put_nowait(task)
        return task.task_id
    
    def get_task_status(self, task_id: str) -> Optional[Dict[str, Any]]:
        """获取任务状态"""
        task = self.task_history.get(task_id)
        if not task:
            return None
        
        return {
            "task_id": task.task_id,
            "task_type": task.task_type,
            "status": task.status,
            "message": task.message,
            "created_at": task.created_at.isoformat(),
            "result": task.result
        }
    
    def get_all_tasks(self) -> List[Dict[str, Any]]:
        """获取所有任务状态"""
        return [
            {
                "task_id": task.task_id,
                "task_type": task.task_type,
                "tradeplan_id": task.tradeplan_id,
                "status": task.status,
                "message": task.message,
                "created_at": task.created_at.isoformat()
            }
            for task in self.task_history.values()
        ]


def get_daemon() -> TradePlanDaemon:
    """获取全局 Daemon 单例实例"""
    return TradePlanDaemon._instance or TradePlanDaemon()


def create_task(
    task_type: TaskType,
    tradeplan_id: Optional[str] = None,
    reason: Optional[str] = None
) -> TradePlanTask:
    """创建新任务"""
    return TradePlanTask(
        task_id=f"task_{uuid.uuid4().hex[:12]}",
        task_type=task_type,
        tradeplan_id=tradeplan_id,
        reason=reason
    )
