"""
Analysis API endpoints for Autodroid Analyzer system.
Handles analysis operations, results, and analysis management.
"""

from fastapi import APIRouter, HTTPException, Query
from typing import List, Optional

from .models import AnalysisResult, AnalysisRequest, ScreenshotInfo, PageElementInfo, UserOperationInfo

# Initialize router
router = APIRouter(prefix="/api/analysis", tags=["analysis"])


@router.get("/apks/{apk_id}/operations", response_model=List[UserOperationInfo])
async def get_operations_by_apk(
    apk_id: str, 
    limit: int = Query(100, description="返回数量限制"),
    sort: str = Query("desc", description="排序方式: asc或desc")
):
    """获取特定APK的操作记录（按时间倒序排列）"""
    try:
        # 导入数据库管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.database import get_database_manager
        
        db_manager = get_database_manager()
        
        # 直接使用APK ID获取操作记录
        operations = db_manager.get_user_operations(apk_id, limit)
        
        if not operations:
            return []
        
        # 转换为Pydantic模型
        api_operations = []
        for op in operations:
            api_operations.append(UserOperationInfo(
                id=op.id,
                apk_id=op.apk_id,
                timestamp=op.timestamp,
                action_type=op.action_type,
                target_element=op.target_element,
                input_text=op.input_text,
                coordinates=op.coordinates,
                screenshot_id=op.screenshot_id,
                created_at=op.created_at
            ))
        
        # 按时间排序
        if sort.lower() == "desc":
            api_operations = sorted(api_operations, key=lambda x: x.timestamp, reverse=True)
        else:
            api_operations = sorted(api_operations, key=lambda x: x.timestamp)
        
        # 限制返回数量
        return api_operations[:limit]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取操作记录失败: {str(e)}")


@router.get("/apks/{apk_id}/screenshots", response_model=List[ScreenshotInfo])
async def get_screenshots_by_apk(apk_id: str, limit: int = 50):
    """获取特定APK的截屏列表"""
    try:
        # 导入数据库管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.database import get_database_manager
        
        db_manager = get_database_manager()
        
        # 使用新的APK相关方法获取截屏
        screenshots = db_manager.get_screenshots_by_apk(apk_id, limit)
        
        # 转换为Pydantic模型
        api_screenshots = []
        for screenshot in screenshots:
            api_screenshots.append(ScreenshotInfo(
                id=screenshot.id,
                apk_id=screenshot.apk_id,
                timestamp=screenshot.timestamp,
                file_path=screenshot.file_path,
                page_title=screenshot.page_title,
                analysis_status=screenshot.analysis_status,
                created_at=screenshot.created_at
            ))
        
        return api_screenshots
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取APK截屏列表失败: {str(e)}")


@router.get("/operations", response_model=List[UserOperationInfo])
async def get_operations(
    action_type: Optional[str] = Query(None, description="操作类型"),
    limit: int = Query(100, description="返回数量限制")
):
    """获取用户操作记录"""
    try:
        # 导入数据库管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.database import get_database_manager
        
        db_manager = get_database_manager()
        
        operations = db_manager.get_user_operations(
            action_type=action_type, 
            limit=limit
        )
        
        # 转换为Pydantic模型
        api_operations = []
        for op in operations:
            api_operations.append(UserOperationInfo(
                id=op.id,
                apk_id=op.apk_id,
                timestamp=op.timestamp,
                action_type=op.action_type,
                target_element=op.target_element,
                input_text=op.input_text,
                coordinates=op.coordinates,
                screenshot_id=op.screenshot_id,
                created_at=op.created_at
            ))
        
        return api_operations
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取操作记录失败: {str(e)}")


@router.get("/screenshots", response_model=List[ScreenshotInfo])
async def get_screenshots(limit: int = 50):
    """获取截屏列表"""
    try:
        # 导入数据库管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.database import get_database_manager
        
        db_manager = get_database_manager()
        
        screenshots = db_manager.get_screenshots(limit)
        
        # 转换为Pydantic模型
        api_screenshots = []
        for screenshot in screenshots:
            api_screenshots.append(ScreenshotInfo(
                id=screenshot.id,
                apk_id=screenshot.apk_id,
                timestamp=screenshot.timestamp,
                file_path=screenshot.file_path,
                page_title=screenshot.page_title,
                analysis_status=screenshot.analysis_status,
                created_at=screenshot.created_at
            ))
        
        return api_screenshots
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取截屏列表失败: {str(e)}")


@router.get("/screenshots/{screenshot_id}/elements", response_model=List[PageElementInfo])
async def get_elements_by_screenshot(screenshot_id: str):
    """获取特定截屏的元素列表"""
    try:
        # 导入数据库管理器
        import sys
        import os
        sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
        from core.database import get_database_manager
        
        db_manager = get_database_manager()
        
        elements = db_manager.get_page_elements_by_screenshot(screenshot_id)
        
        # 转换为Pydantic模型
        api_elements = []
        for element in elements:
            api_elements.append(PageElementInfo(
                id=element.id,
                screenshot_id=element.screenshot_id,
                element_type=element.element_type,
                bounds=element.bounds,
                text=element.text,
                resource_id=element.resource_id,
                class_name=element.class_name,
                package_name=element.package_name,
                content_desc=element.content_desc,
                clickable=element.clickable,
                long_clickable=element.long_clickable,
                scrollable=element.scrollable,
                enabled=element.enabled,
                focused=element.focused,
                selected=element.selected,
                created_at=element.created_at
            ))
        
        return api_elements
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取截屏元素失败: {str(e)}")


@router.post("/apks/{apk_id}/analyze", response_model=AnalysisResult)
async def analyze_apk(apk_id: str, analysis_request: AnalysisRequest):
    """对特定APK进行分析"""
    try:
        # 这里可以添加具体的分析逻辑
        # 暂时返回一个模拟结果
        return AnalysisResult(
            apk_id=apk_id,
            analysis_type=analysis_request.analysis_type,
            status="completed",
            result={"message": "Analysis completed successfully"},
            timestamp=datetime.now(),
            duration=0.5
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"分析APK失败: {str(e)}")


@router.get("/apks/{apk_id}/results", response_model=List[AnalysisResult])
async def get_analysis_results(apk_id: str, limit: int = 10):
    """获取特定APK的分析结果"""
    try:
        # 这里可以添加从数据库获取分析结果的逻辑
        # 暂时返回一个空列表
        return []
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取分析结果失败: {str(e)}")