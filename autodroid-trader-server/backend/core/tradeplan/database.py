from typing import Optional, List
from datetime import datetime
import json
import uuid
import logging
from peewee import DoesNotExist

from ..database.base import BaseDatabase
from ..database.models import TradePlan
from .models import TradePlanStatus

logger = logging.getLogger(__name__)


class TradePlanDatabase(BaseDatabase):
    """交易计划数据库管理类（使用peewee ORM）"""
    
    def __init__(self):
        """初始化交易计划数据库"""
        super().__init__()
    
    def _generate_tradeplan_id(self) -> str:
        """生成交易计划ID"""
        return f"tp_{uuid.uuid4().hex[:16]}"
    
    def create_tradeplan(
        self,
        name: str,
        script_id: Optional[str] = None,
        user_id: Optional[str] = None,
        description: Optional[str] = None,
        exchange: Optional[str] = None,
        symbol: Optional[str] = None,
        symbol_name: Optional[str] = None,
        ohlcv: Optional[dict] = None,
        change_percent: Optional[float] = None,
        data: Optional[dict] = None,
        status: str = TradePlanStatus.PENDING
    ) -> Optional[str]:
        """创建交易计划"""
        try:
            from ..database.models import TradeScript
            tradeplan_id = self._generate_tradeplan_id()
            
            script_obj = None
            if script_id:
                try:
                    script_obj = TradeScript.get(TradeScript.id == script_id)
                    logger.debug(f"[DEBUG] 找到关联的 TradeScript: {script_id}")
                except DoesNotExist:
                    logger.debug(f"[DEBUG] 未找到 TradeScript: {script_id}")
            
            logger.debug(f"[DEBUG] 创建 TradePlan，script: {script_obj}, name: {name}")
            
            TradePlan.create(
                id=tradeplan_id,
                script=script_obj,
                user_id=user_id,
                name=name,
                description=description,
                exchange=exchange,
                symbol=symbol,
                symbol_name=symbol_name,
                ohlcv=json.dumps(ohlcv) if ohlcv else None,
                change_percent=change_percent,
                data=json.dumps(data) if data else None,
                status=status
            )
            
            logger.debug(f"[DEBUG] TradePlan 创建成功: {tradeplan_id}")
            return tradeplan_id
            
        except Exception as e:
            logger.error(f"[DEBUG] 创建 TradePlan 失败: {e}")
            import traceback
            logger.error(traceback.format_exc())
            return None
    
    def get_tradeplan_by_id(self, tradeplan_id: str) -> Optional[TradePlan]:
        """根据ID获取交易计划"""
        try:
            tradeplan = TradePlan.get(TradePlan.id == tradeplan_id)
            return tradeplan
        except DoesNotExist:
            return None
        except Exception:
            return None
    
    def get_all_tradeplans(self) -> List[TradePlan]:
        """获取所有交易计划"""
        try:
            tradeplans = TradePlan.select()
            return list(tradeplans)
        except Exception:
            return []
    
    def get_tradeplans_by_status(self, status: str) -> List[TradePlan]:
        """根据状态获取交易计划，按创建时间排序"""
        try:
            tradeplans = (TradePlan
                .select()
                .where(TradePlan.status == status)
                .order_by(TradePlan.created_at.asc()))
            return list(tradeplans)
        except Exception:
            return []
    
    def get_pending_tradeplans(self) -> List[TradePlan]:
        """获取待批准的交易计划"""
        return self.get_tradeplans_by_status(TradePlanStatus.PENDING)
    
    def get_approved_tradeplans(self) -> List[TradePlan]:
        """获取已批准的交易计划"""
        return self.get_tradeplans_by_status(TradePlanStatus.APPROVED)
    
    def get_executable_tradeplans(self) -> List[TradePlan]:
        """获取可执行的交易计划（executable=True），按创建时间排序"""
        try:
            tradeplans = (TradePlan
                .select()
                .where(TradePlan.executable == True)
                .order_by(TradePlan.created_at.asc()))
            return list(tradeplans)
        except Exception:
            return []
    
    def get_next_executable_tradeplan(self) -> Optional[TradePlan]:
        """获取按创建时间排序的第一个可执行的交易计划"""
        try:
            tradeplans = self.get_executable_tradeplans()
            return tradeplans[0] if tradeplans else None
        except Exception:
            return None
    
    def update_tradeplan(
        self,
        tradeplan_id: str,
        name: Optional[str] = None,
        description: Optional[str] = None,
        exchange: Optional[str] = None,
        symbol: Optional[str] = None,
        symbol_name: Optional[str] = None,
        ohlcv: Optional[dict] = None,
        change_percent: Optional[float] = None,
        data: Optional[dict] = None,
        status: Optional[str] = None,
        executable: Optional[bool] = None,
        execution_result: Optional[str] = None,
        execution_message: Optional[str] = None
    ) -> bool:
        """更新交易计划"""
        try:
            tradeplan = TradePlan.get(TradePlan.id == tradeplan_id)
            
            if name is not None:
                tradeplan.name = name
            if description is not None:
                tradeplan.description = description
            if exchange is not None:
                tradeplan.exchange = exchange
            if symbol is not None:
                tradeplan.symbol = symbol
            if symbol_name is not None:
                tradeplan.symbol_name = symbol_name
            if ohlcv is not None:
                tradeplan.ohlcv = json.dumps(ohlcv)
            if change_percent is not None:
                tradeplan.change_percent = change_percent
            if data is not None:
                tradeplan.data = json.dumps(data)
            if status is not None:
                tradeplan.status = status
            if executable is not None:
                tradeplan.executable = executable
            if execution_result is not None:
                tradeplan.execution_result = execution_result
            if execution_message is not None:
                tradeplan.execution_message = execution_message
            
            tradeplan.save()
            return True
            
        except DoesNotExist:
            return False
        except Exception:
            return False
    
    def update_tradeplan_status(
        self,
        tradeplan_id: str,
        status: str,
        executable: Optional[bool] = None,
        execution_result: Optional[str] = None,
        execution_message: Optional[str] = None
    ) -> bool:
        """更新交易计划状态"""
        return self.update_tradeplan(
            tradeplan_id,
            status=status,
            executable=executable,
            execution_result=execution_result,
            execution_message=execution_message
        )
    
    def batch_update_tradeplan_status(self, tradeplan_ids: List[str], status: str) -> int:
        """批量更新交易计划状态"""
        try:
            updated_count = TradePlan.update(status=status).where(
                TradePlan.id.in_(tradeplan_ids)
            ).execute()
            return updated_count
        except Exception:
            return 0
    
    def batch_update_tradeplan_executable(self, tradeplan_ids: List[str], executable: bool) -> int:
        """批量更新交易计划可执行状态"""
        try:
            updated_count = TradePlan.update(executable=executable).where(
                TradePlan.id.in_(tradeplan_ids)
            ).execute()
            return updated_count
        except Exception:
            return 0
    
    def update_tradeplan_execution_time(
        self,
        tradeplan_id: str,
        started_at: Optional[datetime] = None,
        ended_at: Optional[datetime] = None
    ) -> bool:
        """更新交易计划执行时间"""
        try:
            tradeplan = TradePlan.get(TradePlan.id == tradeplan_id)
            
            if started_at is not None:
                tradeplan.started_at = started_at
            if ended_at is not None:
                tradeplan.ended_at = ended_at
            
            tradeplan.save()
            return True
            
        except DoesNotExist:
            return False
        except Exception:
            return False
    
    def update_tradeplan_execution_result(
        self,
        tradeplan_id: str,
        execution_result: Optional[str] = None,
        execution_message: Optional[str] = None
    ) -> bool:
        """更新交易计划执行结果"""
        try:
            tradeplan = TradePlan.get(TradePlan.id == tradeplan_id)
            
            if execution_result is not None:
                tradeplan.execution_result = execution_result
            if execution_message is not None:
                tradeplan.execution_message = execution_message
            
            tradeplan.save()
            return True
            
        except DoesNotExist:
            return False
        except Exception:
            return False
    
    def delete_tradeplan(self, tradeplan_id: str) -> bool:
        """删除交易计划"""
        try:
            tradeplan = TradePlan.get(TradePlan.id == tradeplan_id)
            tradeplan.delete_instance()
            return True
        except DoesNotExist:
            return False
        except Exception:
            return False
    
    def create_or_update_demo_tradeplans(self) -> dict:
        """创建或更新演示用的交易计划数据"""
        try:
            from ..database.models import TradeScript, Apk
            import json
            print("[DEBUG] 开始创建演示交易计划...")
            
            existing_scripts = list(TradeScript.select().limit(1))
            print(f"[DEBUG] 现有 TradeScript 数量: {len(existing_scripts)}")
            
            script_id = None
            
            if not existing_scripts:
                print("[DEBUG] 没有现有脚本，检查 Apk...")
                existing_apks = list(Apk.select().limit(1))
                print(f"[DEBUG] 现有 Apk 数量: {len(existing_apks)}")
                
                apk = None
                if not existing_apks:
                    print("[DEBUG] 创建新的 Apk...")
                    apk_id = f"apk_{uuid.uuid4().hex[:16]}"
                    Apk.create(
                        id=apk_id,
                        package_name="com.autodroid.trader.demo",
                        app_name="AutoDroid Trader Demo",
                        name="AutoDroid Trader Demo",
                        description="AutoDroid 交易演示应用",
                        version="1.0.0",
                        version_code=1
                    )
                    apk = Apk.get(Apk.id == apk_id)
                    print(f"[DEBUG] Apk 创建成功: {apk_id}")
                else:
                    apk = existing_apks[0]
                    print(f"[DEBUG] 使用现有 Apk: {apk.id}")
                
                if apk:
                    script_id = f"ts_{uuid.uuid4().hex[:16]}"
                    print(f"[DEBUG] 创建新的 TradeScript: {script_id}")
                    TradeScript.create(
                        id=script_id,
                        apk=apk,
                        name="默认交易脚本",
                        description="用于演示的默认交易脚本",
                        metadata=json.dumps({"strategy_type": "demo"}),
                        script_path="demo_strategy.py",
                        status="OK"
                    )
                    print("[DEBUG] TradeScript 创建成功")
            else:
                script_id = existing_scripts[0].id
                print(f"[DEBUG] 使用现有脚本: {script_id}")
            
            if not script_id:
                print("[DEBUG] 错误: script_id 为空")
                return {"created": 0, "updated": 0}
            
            print(f"[DEBUG] 开始创建演示交易计划，script_id: {script_id}")
            
            demo_tradeplans = [
                {
                    "name": "网格交易策略 - 腾讯控股",
                    "description": "使用网格交易策略在腾讯控股股票上进行交易",
                    "data": {
                        "symbol": "00700.HK",
                        "grid_size": 0.5,
                        "grid_count": 10,
                        "amount_per_grid": 10000
                    },
                    "status": TradePlanStatus.PENDING
                },
                {
                    "name": "定投策略 - 纳斯达克100",
                    "description": "定期定额投资纳斯达克100指数基金",
                    "data": {
                        "symbol": "QQQ",
                        "amount": 5000,
                        "frequency": "monthly"
                    },
                    "status": TradePlanStatus.PENDING
                },
                {
                    "name": "均线突破策略 - 苹果公司",
                    "description": "当价格突破均线时执行买入操作",
                    "data": {
                        "symbol": "AAPL",
                        "ma_period": 20,
                        "amount": 20000
                    },
                    "status": TradePlanStatus.APPROVED
                },
                {
                    "name": "RSI反转策略 - 特斯拉",
                    "description": "基于RSI指标的反转交易策略",
                    "data": {
                        "symbol": "TSLA",
                        "rsi_oversold": 30,
                        "rsi_overbought": 70,
                        "amount": 15000
                    },
                    "status": TradePlanStatus.APPROVED
                }
            ]
            
            created_count = 0
            updated_count = 0
            
            for demo in demo_tradeplans:
                print(f"[DEBUG] 检查交易计划: {demo['name']}")
                existing = self.get_tradeplan_by_name(demo["name"])
                print(f"[DEBUG] 找到现有计划: {existing}")
                
                if existing:
                    print(f"[DEBUG] 更新现有计划: {existing.id}")
                    self.update_tradeplan(
                        tradeplan_id=str(existing.id),
                        description=demo["description"],
                        data=demo["data"],
                        status=demo["status"]
                    )
                    updated_count += 1
                else:
                    print(f"[DEBUG] 创建新计划，使用 script_id: {script_id}")
                    tradeplan_id = self.create_tradeplan(
                        script_id=script_id,
                        name=demo["name"],
                        description=demo["description"],
                        data=demo["data"],
                        status=demo["status"]
                    )
                    print(f"[DEBUG] 创建结果: {tradeplan_id}")
                    if tradeplan_id:
                        created_count += 1
            
            print(f"[DEBUG] 完成 - 创建: {created_count}, 更新: {updated_count}")
            
            return {"created": created_count, "updated": updated_count}
            
        except Exception as e:
            logger.error(f"创建演示交易计划失败: {e}")
            return {"created": 0, "updated": 0}
    
    def get_tradeplan_by_name(self, name: str) -> Optional[TradePlan]:
        """根据名称获取交易计划"""
        try:
            tradeplan = TradePlan.get(TradePlan.name == name)
            return tradeplan
        except Exception:
            return None