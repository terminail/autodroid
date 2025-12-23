from typing import Optional, Dict, Any, List
from datetime import datetime
import json
import uuid
from peewee import DoesNotExist

from ..database.base import BaseDatabase
from ..database.models import TradePlan
from .models import TradePlanStatus


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
        ohlcv: Optional[Dict[str, Any]] = None,
        change_percent: Optional[float] = None,
        data: Optional[Dict[str, Any]] = None,
        status: str = TradePlanStatus.PENDING
    ) -> Optional[str]:
        """创建交易计划"""
        try:
            tradeplan_id = self._generate_tradeplan_id()
            
            TradePlan.create(
                id=tradeplan_id,
                script_id=script_id,
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
            
            return tradeplan_id
            
        except Exception as e:
            return None
    
    def get_tradeplan_by_id(self, tradeplan_id: str) -> Optional[Dict[str, Any]]:
        """根据ID获取交易计划"""
        try:
            tradeplan = TradePlan.get(TradePlan.id == tradeplan_id)
            return self._tradeplan_to_dict(tradeplan)
        except DoesNotExist:
            return None
        except Exception:
            return None
    
    def get_all_tradeplans(self) -> List[Dict[str, Any]]:
        """获取所有交易计划"""
        try:
            tradeplans = TradePlan.select()
            return [self._tradeplan_to_dict(tp) for tp in tradeplans]
        except Exception:
            return []
    
    def get_tradeplans_by_status(self, status: str) -> List[Dict[str, Any]]:
        """根据状态获取交易计划"""
        try:
            tradeplans = TradePlan.select().where(TradePlan.status == status)
            return [self._tradeplan_to_dict(tp) for tp in tradeplans]
        except Exception:
            return []
    
    def get_pending_tradeplans(self) -> List[Dict[str, Any]]:
        """获取待批准的交易计划"""
        return self.get_tradeplans_by_status(TradePlanStatus.PENDING)
    
    def get_approved_tradeplans(self) -> List[Dict[str, Any]]:
        """获取已批准的交易计划"""
        return self.get_tradeplans_by_status(TradePlanStatus.APPROVED)
    
    def update_tradeplan(
        self,
        tradeplan_id: str,
        name: Optional[str] = None,
        description: Optional[str] = None,
        exchange: Optional[str] = None,
        symbol: Optional[str] = None,
        symbol_name: Optional[str] = None,
        ohlcv: Optional[Dict[str, Any]] = None,
        change_percent: Optional[float] = None,
        data: Optional[Dict[str, Any]] = None,
        status: Optional[str] = None
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
            
            tradeplan.save()
            return True
            
        except DoesNotExist:
            return False
        except Exception:
            return False
    
    def update_tradeplan_status(self, tradeplan_id: str, status: str) -> bool:
        """更新交易计划状态"""
        return self.update_tradeplan(tradeplan_id, status=status)
    
    def batch_update_tradeplan_status(self, tradeplan_ids: List[str], status: str) -> int:
        """批量更新交易计划状态"""
        try:
            updated_count = TradePlan.update(status=status).where(
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
    
    def _tradeplan_to_dict(self, tradeplan: TradePlan) -> Dict[str, Any]:
        """将WorkPlan对象转换为字典"""
        return {
            "id": tradeplan.id,
            "script_id": tradeplan.script_id,
            "user_id": tradeplan.user_id,
            "name": tradeplan.name,
            "description": tradeplan.description,
            "exchange": tradeplan.exchange,
            "symbol": tradeplan.symbol,
            "symbol_name": tradeplan.symbol_name,
            "ohlcv": json.loads(tradeplan.ohlcv) if tradeplan.ohlcv else None,
            "change_percent": float(tradeplan.change_percent) if tradeplan.change_percent else None,
            "data": json.loads(tradeplan.data) if tradeplan.data else {},
            "status": tradeplan.status,
            "created_at": tradeplan.created_at,
            "started_at": tradeplan.started_at,
            "ended_at": tradeplan.ended_at,
            "execution_result": tradeplan.execution_result,
            "execution_message": tradeplan.execution_message
        }
    
    def create_demo_tradeplans(self) -> int:
        """创建演示用的交易计划数据"""
        try:
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
            for demo in demo_tradeplans:
                tradeplan_id = self.create_tradeplan(
                    name=demo["name"],
                    description=demo["description"],
                    data=demo["data"],
                    status=demo["status"]
                )
                if tradeplan_id:
                    created_count += 1
            
            return created_count
            
        except Exception:
            return 0
