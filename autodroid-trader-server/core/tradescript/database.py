from typing import Optional, Dict, Any, List
from datetime import datetime
import json
import uuid
from peewee import DoesNotExist

from ..database.base import BaseDatabase
from ..database.models import TradeScript, Apk
from .models import TradeScriptStatus


class TradeScriptDatabase(BaseDatabase):
    """交易脚本数据库管理类（使用peewee ORM）"""
    
    def __init__(self):
        """初始化交易脚本数据库"""
        super().__init__()
    
    def _generate_tradescript_id(self) -> str:
        """生成交易脚本ID"""
        return f"ts_{uuid.uuid4().hex[:16]}"
    
    def create_tradescript(
        self,
        apk_package: str,
        name: str,
        description: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None,
        script_path: str = "",
        status: str = TradeScriptStatus.NEW
    ) -> Optional[str]:
        """创建交易脚本"""
        try:
            tradescript_id = self._generate_tradescript_id()
            
            apk = Apk.get(Apk.package_name == apk_package)
            
            TradeScript.create(
                id=tradescript_id,
                apk=apk,
                name=name,
                description=description,
                metadata=json.dumps(metadata) if metadata else "{}",
                script_path=script_path,
                status=status
            )
            
            return tradescript_id
            
        except DoesNotExist:
            return None
        except Exception as e:
            return None
    
    def get_tradescript_by_id(self, tradescript_id: str) -> Optional[Dict[str, Any]]:
        """根据ID获取交易脚本"""
        try:
            tradescript = TradeScript.get(TradeScript.id == tradescript_id)
            return self._tradescript_to_dict(tradescript)
        except DoesNotExist:
            return None
        except Exception:
            return None
    
    def get_all_tradescripts(self) -> List[Dict[str, Any]]:
        """获取所有交易脚本"""
        try:
            tradescripts = TradeScript.select()
            return [self._tradescript_to_dict(ts) for ts in tradescripts]
        except Exception:
            return []
    
    def get_tradescripts_by_apk(self, apk_package: str) -> List[Dict[str, Any]]:
        """根据APK获取交易脚本"""
        try:
            apk = Apk.get(Apk.package_name == apk_package)
            tradescripts = TradeScript.select().where(TradeScript.apk == apk)
            return [self._tradescript_to_dict(ts) for ts in tradescripts]
        except DoesNotExist:
            return []
        except Exception:
            return []
    
    def get_tradescripts_by_status(self, status: str) -> List[Dict[str, Any]]:
        """根据状态获取交易脚本"""
        try:
            tradescripts = TradeScript.select().where(TradeScript.status == status)
            return [self._tradescript_to_dict(ts) for ts in tradescripts]
        except Exception:
            return []
    
    def update_tradescript(
        self,
        tradescript_id: str,
        name: Optional[str] = None,
        description: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None,
        script_path: Optional[str] = None,
        status: Optional[str] = None
    ) -> bool:
        """更新交易脚本"""
        try:
            tradescript = TradeScript.get(TradeScript.id == tradescript_id)
            
            if name is not None:
                tradescript.name = name
            if description is not None:
                tradescript.description = description
            if metadata is not None:
                tradescript.metadata = json.dumps(metadata)
            if script_path is not None:
                tradescript.script_path = script_path
            if status is not None:
                tradescript.status = status
            
            tradescript.save()
            return True
            
        except DoesNotExist:
            return False
        except Exception:
            return False
    
    def update_tradescript_status(self, tradescript_id: str, status: str) -> bool:
        """更新交易脚本状态"""
        return self.update_tradescript(tradescript_id, status=status)
    
    def delete_tradescript(self, tradescript_id: str) -> bool:
        """删除交易脚本"""
        try:
            tradescript = TradeScript.get(TradeScript.id == tradescript_id)
            tradescript.delete_instance()
            return True
        except DoesNotExist:
            return False
        except Exception:
            return False
    
    def _tradescript_to_dict(self, tradescript: TradeScript) -> Dict[str, Any]:
        """将TradeScript对象转换为字典"""
        return {
            "id": tradescript.id,
            "apk_package": tradescript.apk.package_name if tradescript.apk else None,
            "name": tradescript.name,
            "description": tradescript.description,
            "metadata": json.loads(tradescript.metadata) if tradescript.metadata else {},
            "script_path": tradescript.script_path,
            "status": tradescript.status,
            "created_at": tradescript.created_at
        }
