"""
Device management API endpoints for Autodroid Analyzer system.
Handles device operations and information retrieval.
"""

from fastapi import APIRouter, HTTPException
from typing import List

from .models import DeviceInfo

# Initialize router
router = APIRouter(prefix="/api/devices", tags=["devices"])


@router.get("/", response_model=List[DeviceInfo])
async def get_devices():
    """获取设备列表"""
    try:
        # 导入数据库管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.database import get_database_manager
        
        db_manager = get_database_manager()
        
        # 获取设备列表
        devices = db_manager.get_devices()
        
        # 转换为Pydantic模型
        api_devices = []
        for device in devices:
            api_devices.append(DeviceInfo(
                id=device.id,
                udid=device.udid,
                name=device.name,
                model=device.model,
                platform=device.platform,
                version=device.version,
                connected=device.connected,
                last_connected=device.last_connected,
                total_operations=device.total_operations,
                total_screenshots=device.total_screenshots
            ))
        
        return api_devices
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取设备列表失败: {str(e)}")


@router.get("/{device_id}", response_model=DeviceInfo)
async def get_device(device_id: str):
    """获取特定设备信息"""
    try:
        # 导入数据库管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.database import get_database_manager
        
        db_manager = get_database_manager()
        
        # 获取特定设备
        device = db_manager.get_device(device_id)
        if not device:
            raise HTTPException(status_code=404, detail="设备未找到")
        
        return DeviceInfo(
            id=device.id,
            udid=device.udid,
            name=device.name,
            model=device.model,
            platform=device.platform,
            version=device.version,
            connected=device.connected,
            last_connected=device.last_connected,
            total_operations=device.total_operations,
            total_screenshots=device.total_screenshots
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取设备信息失败: {str(e)}")


@router.get("/{device_id}/apks")
async def get_device_apks(device_id: str):
    """获取设备上的APK列表"""
    try:
        # 导入APK管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.apk.service import ApkManager
        
        apk_manager = ApkManager()
        
        # 获取设备上的APK列表
        apks = apk_manager.get_apks_for_device(device_id)
        
        return {
            "device_id": device_id,
            "apks": apks,
            "count": len(apks)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取设备APK列表失败: {str(e)}")


@router.get("/{device_id}/statistics")
async def get_device_statistics(device_id: str):
    """获取设备统计信息"""
    try:
        # 导入数据库管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.database import get_database_manager
        
        db_manager = get_database_manager()
        
        # 获取设备操作统计
        operations_count = db_manager.get_device_operation_count(device_id)
        
        # 获取设备截屏统计
        screenshots_count = db_manager.get_device_screenshot_count(device_id)
        
        # 获取设备APK统计
        apks_count = db_manager.get_device_apk_count(device_id)
        
        return {
            "device_id": device_id,
            "operations_count": operations_count,
            "screenshots_count": screenshots_count,
            "apks_count": apks_count
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取设备统计信息失败: {str(e)}")


@router.get("/{device_id}/operations/summary")
async def get_device_operations_summary(device_id: str):
    """获取设备操作摘要"""
    try:
        # 导入数据库管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.database import get_database_manager
        
        db_manager = get_database_manager()
        
        # 获取设备操作类型统计
        operation_types = db_manager.get_device_operation_type_statistics(device_id)
        
        # 获取设备时间分布统计
        time_distribution = db_manager.get_device_operation_time_distribution(device_id)
        
        return {
            "device_id": device_id,
            "operation_types": operation_types,
            "time_distribution": time_distribution
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取设备操作摘要失败: {str(e)}")