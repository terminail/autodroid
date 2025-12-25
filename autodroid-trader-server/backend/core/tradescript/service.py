from typing import Optional, Dict, Any, List
import logging

from .database import TradeScriptDatabase
from .models import (
    TradeScriptStatus,
    TradeScriptCreateRequest,
    TradeScriptUpdateRequest,
    TradeScriptResponse,
    TradeScriptListResponse,
    TradeScriptCreateResponse,
    TradeScriptUpdateResponse
)

logger = logging.getLogger(__name__)


class TradeScriptService:
    """交易脚本服务类"""
    
    def __init__(self):
        """初始化交易脚本服务"""
        self.tradescript_db = TradeScriptDatabase()
    
    def create_tradescript(self, request: TradeScriptCreateRequest) -> TradeScriptCreateResponse:
        """创建交易脚本"""
        try:
            tradescript_id = self.tradescript_db.create_tradescript(
                apk_package=request.apk_package,
                name=request.name,
                description=request.description,
                metadata=request.metadata,
                script_path=request.script_path,
                status=request.status.value if request.status else TradeScriptStatus.NEW
            )
            
            if not tradescript_id:
                return TradeScriptCreateResponse(
                    message="创建交易脚本失败，请检查APK是否存在",
                    tradescript=None
                )
            
            tradescript_dict = self.tradescript_db.get_tradescript_by_id(tradescript_id)
            if not tradescript_dict:
                return TradeScriptCreateResponse(
                    message="创建交易脚本失败",
                    tradescript=None
                )
            
            return TradeScriptCreateResponse(
                message="交易脚本创建成功",
                tradescript=TradeScriptResponse(**tradescript_dict)
            )
            
        except Exception as e:
            logger.error(f"创建交易脚本失败: {e}")
            return TradeScriptCreateResponse(
                message=f"创建交易脚本失败: {str(e)}",
                tradescript=None
            )
    
    def get_tradescript_by_id(self, tradescript_id: str) -> Optional[TradeScriptResponse]:
        """根据ID获取交易脚本"""
        try:
            tradescript_dict = self.tradescript_db.get_tradescript_by_id(tradescript_id)
            if not tradescript_dict:
                return None
            
            return TradeScriptResponse(**tradescript_dict)
            
        except Exception as e:
            logger.error(f"获取交易脚本失败: {e}")
            return None
    
    def get_all_tradescripts(self) -> TradeScriptListResponse:
        """获取所有交易脚本"""
        try:
            tradescripts = self.tradescript_db.get_all_tradescripts()
            return TradeScriptListResponse(
                tradescripts=[TradeScriptResponse(**ts) for ts in tradescripts],
                total=len(tradescripts)
            )
        except Exception as e:
            logger.error(f"获取交易脚本列表失败: {e}")
            return TradeScriptListResponse(tradescripts=[], total=0)
    
    def get_tradescripts_by_apk(self, apk_package: str) -> TradeScriptListResponse:
        """根据APK包名获取交易脚本"""
        try:
            tradescripts = self.tradescript_db.get_tradescripts_by_apk(apk_package)
            return TradeScriptListResponse(
                tradescripts=[TradeScriptResponse(**ts) for ts in tradescripts],
                total=len(tradescripts)
            )
        except Exception as e:
            logger.error(f"获取APK交易脚本失败: {e}")
            return TradeScriptListResponse(tradescripts=[], total=0)
    
    def get_tradescripts_by_status(self, status: TradeScriptStatus) -> TradeScriptListResponse:
        """根据状态获取交易脚本"""
        try:
            tradescripts = self.tradescript_db.get_tradescripts_by_status(status.value)
            return TradeScriptListResponse(
                tradescripts=[TradeScriptResponse(**ts) for ts in tradescripts],
                total=len(tradescripts)
            )
        except Exception as e:
            logger.error(f"获取交易脚本失败: {e}")
            return TradeScriptListResponse(tradescripts=[], total=0)
    
    def update_tradescript(
        self,
        tradescript_id: str,
        request: TradeScriptUpdateRequest
    ) -> TradeScriptUpdateResponse:
        """更新交易脚本"""
        try:
            success = self.tradescript_db.update_tradescript(
                tradescript_id=tradescript_id,
                name=request.name,
                description=request.description,
                metadata=request.metadata,
                script_path=request.script_path,
                status=request.status.value if request.status else None
            )
            
            if not success:
                return TradeScriptUpdateResponse(
                    message="交易脚本不存在或更新失败",
                    tradescript=None
                )
            
            tradescript = self.get_tradescript_by_id(tradescript_id)
            return TradeScriptUpdateResponse(
                message="交易脚本更新成功",
                tradescript=tradescript
            )
            
        except Exception as e:
            logger.error(f"更新交易脚本失败: {e}")
            return TradeScriptUpdateResponse(
                message=f"更新交易脚本失败: {str(e)}",
                tradescript=None
            )
    
    def delete_tradescript(self, tradescript_id: str) -> bool:
        """删除交易脚本"""
        try:
            return self.tradescript_db.delete_tradescript(tradescript_id)
        except Exception as e:
            logger.error(f"删除交易脚本失败: {e}")
            return False
    
    def get_tradescript_count(self) -> int:
        """获取交易脚本总数"""
        try:
            return self.tradescript_db.get_tradescript_count()
        except Exception as e:
            logger.error(f"获取交易脚本总数失败: {e}")
            return 0
