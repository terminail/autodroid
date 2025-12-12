"""
APK management API endpoints for Autodroid Analyzer system.
Handles APK operations and information retrieval.
"""

from fastapi import APIRouter, HTTPException
from typing import List

from .models import ApkInfo

# Initialize router
router = APIRouter(prefix="/api/apks", tags=["apks"])


@router.get("/", response_model=List[ApkInfo])
async def get_apks():
    """获取APK列表"""
    try:
        # 导入APK管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.apk.service import ApkManager
        
        apk_manager = ApkManager()
        
        # 使用ApkManager获取所有APK列表
        apks = apk_manager.get_all_apks()
        
        # 转换为API格式
        api_apks = []
        for apk in apks:
            api_apks.append(ApkInfo(
                id=apk.id,  # 使用id字段而不是package_name
                app_name=apk.app_name,
                version_name=apk.version_name,
                version_code=apk.version_code,
                install_time=apk.install_time,
                last_analyzed=apk.last_analyzed,
                total_operations=apk.total_operations,
                total_screenshots=apk.total_screenshots,
                is_packed=apk.is_packed,
                packer_type=apk.packer_type,
                packer_confidence=apk.packer_confidence,
                packer_indicators=apk.packer_indicators,
                packer_analysis_time=apk.packer_analysis_time
            ))
        return api_apks
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取APK列表失败: {str(e)}")


@router.get("/{apk_id}", response_model=ApkInfo)
async def get_apk(apk_id: str):
    """获取特定APK信息"""
    try:
        # 导入APK管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.apk.service import ApkManager
        
        apk_manager = ApkManager()
        
        # 使用ApkManager获取特定APK信息
        apk = apk_manager.get_apk(apk_id)
        if not apk:
            raise HTTPException(status_code=404, detail="APK未找到")
        
        return ApkInfo(
            id=apk.id,  # 使用id字段而不是package_name
            app_name=apk.app_name,
            version_name=apk.version_name,
            version_code=apk.version_code,
            install_time=apk.install_time,
            last_analyzed=apk.last_analyzed,
            total_operations=apk.total_operations,
            total_screenshots=apk.total_screenshots,
            is_packed=apk.is_packed,
            packer_type=apk.packer_type,
            packer_confidence=apk.packer_confidence,
            packer_indicators=apk.packer_indicators,
            packer_analysis_time=apk.packer_analysis_time
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取APK信息失败: {str(e)}")


@router.get("/{apk_id}/statistics")
async def get_apk_statistics(apk_id: str):
    """获取APK统计信息"""
    try:
        # 导入数据库管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.database import get_database_manager
        
        db_manager = get_database_manager()
        
        # 获取操作记录数量
        operations_count = db_manager.get_user_operation_count(apk_id)
        
        # 获取截屏数量
        screenshots_count = db_manager.get_screenshot_count(apk_id)
        
        # 获取最近操作时间
        recent_operation = db_manager.get_recent_user_operation(apk_id)
        recent_screenshot = db_manager.get_recent_screenshot(apk_id)
        
        return {
            "apk_id": apk_id,
            "operations_count": operations_count,
            "screenshots_count": screenshots_count,
            "recent_operation_time": recent_operation.timestamp if recent_operation else None,
            "recent_screenshot_time": recent_screenshot.timestamp if recent_screenshot else None
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取APK统计信息失败: {str(e)}")


@router.get("/{apk_id}/operations/summary")
async def get_apk_operations_summary(apk_id: str):
    """获取APK操作摘要"""
    try:
        # 导入数据库管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.database import get_database_manager
        
        db_manager = get_database_manager()
        
        # 获取操作类型统计
        operation_types = db_manager.get_operation_type_statistics(apk_id)
        
        # 获取时间分布统计
        time_distribution = db_manager.get_operation_time_distribution(apk_id)
        
        return {
            "apk_id": apk_id,
            "operation_types": operation_types,
            "time_distribution": time_distribution
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取APK操作摘要失败: {str(e)}")