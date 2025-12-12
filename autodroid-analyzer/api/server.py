"""
Server management API endpoints for Autodroid Analyzer system.
Handles server information, health checks, and configuration.
"""

from fastapi import APIRouter
from typing import Dict, Any
import socket
import platform
import time

from .models import ServerInfo, HealthCheck

# Initialize router
router = APIRouter(prefix="/api/server", tags=["server"])


@router.get("", response_model=ServerInfo)
async def get_server_info():
    """获取服务器信息"""
    try:
        # 获取主机名和IP地址
        hostname = socket.gethostname()
        ip_address = socket.gethostbyname(hostname)
        
        # 获取平台信息
        system_platform = platform.system()
        
        # 定义服务列表
        services = {
            "api": "running",
            "database": "running",
            "analysis": "available"
        }
        
        # 定义能力列表
        capabilities = {
            "apk_analysis": True,
            "screenshot_analysis": True,
            "user_operation_tracking": True,
            "device_management": True
        }
        
        return ServerInfo(
            name="Autodroid Analyzer Server",
            hostname=hostname,
            ipAddress=ip_address,
            platform=system_platform,
            apiEndpoint="/api",
            services=services,
            capabilities=capabilities
        )
    except Exception as e:
        # 如果获取信息失败，返回默认值
        return ServerInfo(
            name="Autodroid Analyzer Server",
            hostname="unknown",
            ipAddress="unknown",
            platform="unknown",
            apiEndpoint="/api",
            services={"api": "running"},
            capabilities={}
        )


@router.get("/health", response_model=HealthCheck)
async def health_check():
    """健康检查"""
    try:
        # 检查数据库连接
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.database import get_database_manager
        
        db_manager = get_database_manager()
        db_status = "healthy"
        
        try:
            # 尝试执行一个简单的数据库查询
            devices_count = db_manager.get_device_count()
            db_status = "healthy"
        except Exception:
            db_status = "unhealthy"
        
        # 检查APK管理器
        try:
            from core.apk.service import ApkManager
            apk_manager = ApkManager()
            apks_count = len(apk_manager.get_all_apks())
            apk_status = "healthy"
        except Exception:
            apk_status = "unhealthy"
        
        services = {
            "database": db_status,
            "apk_manager": apk_status,
            "api": "healthy"
        }
        
        # 检查整体状态
        overall_status = "healthy" if all(status == "healthy" for status in services.values()) else "degraded"
        
        return HealthCheck(
            status=overall_status,
            timestamp=time.time(),
            services=services
        )
    except Exception as e:
        return HealthCheck(
            status="unhealthy",
            timestamp=time.time(),
            services={"api": "unhealthy", "error": str(e)}
        )


@router.get("/config")
async def get_server_config():
    """获取服务器配置"""
    try:
        # 导入配置管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from config import ConfigManager
        
        config_manager = ConfigManager()
        config = config_manager.get_config()
        
        # 返回配置信息（过滤敏感信息）
        safe_config = {
            "server": config.get("server", {}),
            "database": {
                "type": config.get("database", {}).get("type"),
                "host": config.get("database", {}).get("host"),
                "port": config.get("database", {}).get("port"),
                "name": config.get("database", {}).get("name")
            },
            "analysis": config.get("analysis", {}),
            "logging": config.get("logging", {})
        }
        
        return safe_config
    except Exception as e:
        return {"error": f"获取配置失败: {str(e)}"}


@router.get("/stats")
async def get_server_stats():
    """获取服务器统计信息"""
    try:
        # 导入数据库管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.database import get_database_manager
        from core.apk.service import ApkManager
        
        db_manager = get_database_manager()
        apk_manager = ApkManager()
        
        # 获取各种统计信息
        devices_count = db_manager.get_device_count()
        apks_count = len(apk_manager.get_all_apks())
        operations_count = db_manager.get_total_operation_count()
        screenshots_count = db_manager.get_total_screenshot_count()
        
        return {
            "devices": devices_count,
            "apks": apks_count,
            "operations": operations_count,
            "screenshots": screenshots_count,
            "timestamp": time.time()
        }
    except Exception as e:
        return {"error": f"获取统计信息失败: {str(e)}"}