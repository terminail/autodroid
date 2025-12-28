"""
交易计划执行 Daemon
使用定时轮询从 Trader Server 获取可执行的交易计划并执行
"""

import asyncio
import logging
import requests
import hashlib
from datetime import datetime
from typing import Optional, Dict, Any
from pathlib import Path

from core.tradescript.models import TradePlanResponse
from core.tradescript.service import TradeScriptExecutionData
from core.config import get_apks_path

logger = logging.getLogger(__name__)


class TradePlanDaemon:
    """
    交易计划执行 Daemon
    负责从 Server 轮询获取可执行的交易计划并执行
    """
    
    _instance: Optional['TradePlanDaemon'] = None
    
    def __new__(cls, trader_server_api_endpoint: str = "http://localhost:8000/api"):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
            cls._instance._trader_server_api_endpoint = trader_server_api_endpoint
        return cls._instance
    
    def __init__(self, trader_server_api_endpoint: str = "http://localhost:8000/api"):
        if self._initialized:
            return
        self._initialized = True
        self._trader_server_api_endpoint = trader_server_api_endpoint
        
        self.running = False
        self.executor_task: Optional[asyncio.Task] = None
        self.poll_interval = 5
        self._request_timeout = 10
        
        logger.info("交易计划 Daemon 已初始化")
    
    async def start(self, poll_interval: int = 5):
        """启动 Daemon"""
        if self.running:
            logger.warning("Daemon 已在运行中")
            return
        
        self.running = True
        self.poll_interval = poll_interval
        # 只启动执行循环，移除轮询循环（避免重复获取）
        self.executor_task = asyncio.create_task(self._execution_loop())
        logger.info(f"交易计划 Daemon 已启动，轮询间隔: {poll_interval}秒")
    
    async def stop(self):
        """停止 Daemon"""
        if not self.running:
            return
        
        self.running = False
        if self.executor_task:
            self.executor_task.cancel()
            try:
                await self.executor_task
            except asyncio.CancelledError:
                pass
        logger.info("交易计划 Daemon 已停止")
    
    async def _execution_loop(self):
        """执行循环：从 Server 获取并执行交易计划"""
        print("[DEBUG] 交易计划轮询循环已启动", flush=True)
        logger.info("交易计划轮询循环已启动")
        poll_count = 0
        while self.running:
            poll_count += 1
            dots = "." * (poll_count % 10)
            print(f"\r轮询中: {poll_count} {dots}", end="", flush=True)
            try:
                tradeplan = await self._fetch_next_executable()
                if tradeplan:
                    print(f"\n✓ 找到可执行交易计划: {tradeplan.name} ({tradeplan.id})", flush=True)
                    await self._execute_tradeplan(tradeplan)
            except Exception as e:
                logger.error(f"执行交易计划时出错: {e}")
            await asyncio.sleep(self.poll_interval)
    
    async def _fetch_next_executable(self) -> Optional[TradePlanResponse]:
        """从 Server 获取下一个可执行的交易计划"""
        try:
            response = requests.get(
                f"{self._trader_server_api_endpoint}/tradeplans/execute/nextable",
                timeout=self._request_timeout
            )
            if response.status_code != 200:
                logger.warning(f"获取可执行计划失败: {response.status_code}")
                return None
            data = response.json()
            tradeplans = data.get("tradeplans", [])
            if tradeplans:
                return TradePlanResponse(**tradeplans[0])
            return None
        except requests.RequestException as e:
            logger.error(f"连接 Server 失败: {e}")
            return None
    
    async def _execute_tradeplan(self, tradeplan: TradePlanResponse):
        """执行单个交易计划"""
        tradeplan_id = tradeplan.id
        tradeplan_name = tradeplan.name
        
        logger.info(f"开始执行交易计划: {tradeplan_name} ({tradeplan_id})")
        
        try:
            await self._update_execution_status(
                tradeplan_id,
                execution_result="RUNNING",
                execution_message=f"正在执行 {tradeplan_name}..."
            )
            
            execution_result = await self._run_tradeplan_execution(tradeplan)
            
            if execution_result.get("success"):
                await self._mark_completed(tradeplan, tradeplan)
                logger.info(f"交易计划执行完成: {tradeplan_name}")
            else:
                await self._mark_failed(tradeplan_id, execution_result.get("message") or "未知错误")
                logger.error(f"交易计划执行失败: {tradeplan_name} - {execution_result.get('message')}")
                
        except Exception as e:
            await self._mark_failed(tradeplan_id, str(e))
            logger.error(f"执行交易计划异常: {tradeplan_name} - {e}")
    
    async def _run_tradeplan_execution(self, tradeplan: TradePlanResponse) -> Dict[str, Any]:
        """运行交易计划执行逻辑"""
        start_time = datetime.now()
        
        script_id = tradeplan.script_id
        apk_flow = self._get_apk_flow_from_script_id(script_id)
        logger.info(f"执行 {tradeplan.name}")
        logger.info(f"APK Flow: {apk_flow}")
        
        if tradeplan.data:
            logger.info(f"交易计划配置: {tradeplan.data}")
        
        data = tradeplan.data or {}
        script_id = tradeplan.script_id
        
        if script_id:
            result = await self._execute_script(script_id, data)
        else:
            await asyncio.sleep(5)
            result = {"success": True, "message": "模拟执行完成"}
        
        return result
    
    async def _execute_script(self, script_id: str, data: TradeScriptExecutionData) -> Dict[str, Any]:
        """执行交易脚本"""
        try:
            from .tradescript.service import TradeScriptService
            service = TradeScriptService()
            result = await service.execute_script(script_id, data)
            return {"success": True, "result": result}
        except Exception as e:
            logger.error(f"执行脚本失败: {e}")
            return {"success": False, "message": str(e)}
    
    def _get_apk_flow_from_script_id(self, script_id: str) -> str:
        """根据 script_id 查找对应的 apk_flow"""
        if not script_id:
            return "Unknown"
        
        try:
            apks_path = get_apks_path()
            if not apks_path.exists():
                return "Unknown"
            
            for apk_package_dir in apks_path.iterdir():
                if not apk_package_dir.is_dir():
                    continue
                
                for flow_dir in apk_package_dir.iterdir():
                    if not flow_dir.is_dir():
                        continue
                    
                    id_string = f"{apk_package_dir.name}_{flow_dir.name}"
                    id_hash = hashlib.md5(id_string.encode()).hexdigest()
                    
                    if id_hash == script_id:
                        return str(flow_dir.relative_to(apks_path))
            
            return "Unknown"
        except Exception as e:
            logger.error(f"查找 apk_flow 失败: {e}")
            return "Unknown"
    
    async def _update_execution_status(self, tradeplan_id: str, execution_result: str, execution_message: str):
        """更新执行状态"""
        try:
            response = requests.patch(
                f"{self._trader_server_api_endpoint}/tradeplans/{tradeplan_id}/status",
                json={
                    "status": "EXECUTING",
                    "execution_result": execution_result,
                    "execution_message": execution_message
                },
                timeout=self._request_timeout
            )
            if response.status_code != 200:
                logger.warning(f"更新执行状态失败: {response.status_code}")
        except Exception as e:
            logger.error(f"更新执行状态出错: {e}")
    
    async def _mark_completed(self, tradeplan: TradePlanResponse):
        """标记交易计划为已完成"""
        try:
            response = requests.patch(
                f"{self._trader_server_api_endpoint}/tradeplans/{tradeplan.id}/status",
                json={
                    "status": "COMPLETED",
                    "execution_result": "SUCCESS",
                    "execution_message": f"交易计划 {tradeplan.name} 执行成功",
                    "executable": False
                },
                timeout=self._request_timeout
            )
            if response.status_code != 200:
                logger.warning(f"标记完成失败: {response.status_code}")
        except Exception as e:
            logger.error(f"标记完成出错: {e}")
    
    async def _mark_failed(self, tradeplan_id: str, message: str):
        """标记执行失败"""
        try:
            response = requests.patch(
                f"{self._trader_server_api_endpoint}/tradeplans/{tradeplan_id}/status",
                json={
                    "status": "FAILED",
                    "execution_result": "FAILED",
                    "execution_message": message,
                    "executable": False
                },
                timeout=self._request_timeout
            )
            if response.status_code != 200:
                logger.warning(f"标记失败失败: {response.status_code}")
        except Exception as e:
            logger.error(f"标记失败出错: {e}")
    
    async def stop_executing_plan(self, tradeplan_id: str, reason: Optional[str] = None):
        """停止正在执行的交易计划"""
        try:
            response = requests.post(
                f"{self._trader_server_api_endpoint}/tradeplans/{tradeplan_id}/stop",
                json={"reason": reason},
                timeout=self._request_timeout
            )
            if response.status_code == 200:
                result = response.json()
                logger.info(f"已停止交易计划: {tradeplan_id}")
                return result
            else:
                logger.error(f"停止交易计划失败: {response.status_code}")
                return {"success": False, "message": "停止失败"}
        except Exception as e:
            logger.error(f"停止交易计划出错: {e}")
            return {"success": False, "message": str(e)}
    
    async def stop_all_executing(self):
        """停止所有正在执行的交易计划"""
        try:
            response = requests.post(
                f"{self._trader_server_api_endpoint}/tradeplans/stop-approved",
                timeout=self._request_timeout
            )
            if response.status_code == 200:
                result = response.json()
                logger.info("已提交停止所有执行中的计划")
                return result
            else:
                return {"success": False, "message": "停止失败"}
        except Exception as e:
            logger.error(f"停止所有计划出错: {e}")
            return {"success": False, "message": str(e)}


def get_daemon(server_url: str = "http://localhost:8000") -> TradePlanDaemon:
    """获取全局 Daemon 单例实例"""
    return TradePlanDaemon._instance or TradePlanDaemon(server_url)


async def run_daemon(trader_server_api_endpoint: str = "http://localhost:8000/api", poll_interval: int = 5):
    """运行 Daemon（用于直接启动）"""
    daemon = get_daemon(trader_server_api_endpoint)
    await daemon.start(poll_interval)
    
    try:
        while True:
            await asyncio.sleep(1)
    except KeyboardInterrupt:
        await daemon.stop()


if __name__ == "__main__":
    import sys
    from .config import get_server_config
    
    config = get_server_config()
    trader_server_api_endpoint = config.get('trader_server_api_endpoint', 'http://localhost:8008/api')
    poll_interval = config.get('poll_interval', 5)
    
    if len(sys.argv) > 1:
        trader_server_api_endpoint = sys.argv[1]
    if len(sys.argv) > 2:
        poll_interval = int(sys.argv[2])
    
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    asyncio.run(run_daemon(trader_server_api_endpoint, poll_interval))
