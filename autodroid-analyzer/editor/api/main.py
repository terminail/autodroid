"""
编辑管理模块FastAPI后端服务
提供用户操作记录、截屏管理、页面元素编辑的API接口
"""

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
from datetime import datetime
import json
import os
from pathlib import Path

# 导入数据库管理器
import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..'))
from core.database import get_database_manager

# 导入共享数据模型
from core.database.models import UserOperation, Screenshot, PageElement, Device, Apk

# 导入APK列表器和Pydantic模型
from core.apk.list_apks import ApkLister
from core.apk.service import ApkManager
from core.apk.models import ApkInfo
from core.useroperation.models import UserOperationInfo
from core.device.models import DeviceInfo
from config import ConfigManager

# 直接导入screenshot模块的models文件，避免触发cv2依赖
import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', 'core', 'screenshot'))
from models import ScreenshotInfo, PageElementInfo

app = FastAPI(title="AutoDroid Editor Management API", version="1.0.0")

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境应限制为具体域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 数据库服务
db_manager = get_database_manager()

# APK管理器
apk_manager = ApkManager()

# 数据模型 - 已移至共享模块 database.models

# API路由
@app.get("/")
async def root():
    """API根路径"""
    return {"message": "AutoDroid Editor Management API", "version": "1.0.0"}

@app.get("/apks")
async def get_apks() -> List[ApkInfo]:
    """获取APK列表"""
    try:
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

@app.get("/apks/{apk_id}")
async def get_apk(apk_id: str) -> ApkInfo:
    """获取特定APK信息"""
    try:
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

@app.get("/apks/{apk_id}/operations")
async def get_operations_by_apk(
    apk_id: str, 
    limit: int = Query(100, description="返回数量限制"),
    sort: str = Query("desc", description="排序方式: asc或desc")
) -> List[UserOperationInfo]:
    """获取特定APK的操作记录（按时间倒序排列）"""
    try:
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

@app.get("/apks/{apk_id}/screenshots")
async def get_screenshots_by_apk(apk_id: str, limit: int = 50) -> List[ScreenshotInfo]:
    """获取特定APK的截屏列表"""
    try:
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



@app.get("/operations")
async def get_operations(
    action_type: Optional[str] = Query(None, description="操作类型"),
    limit: int = Query(100, description="返回数量限制")
) -> List[UserOperationInfo]:
    """获取用户操作记录"""
    try:
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

@app.delete("/operations/{operation_id}")
async def delete_operation(operation_id: str):
    """删除用户操作记录"""
    try:
        success = db_manager.delete_user_operation(operation_id)
        if not success:
            raise HTTPException(status_code=404, detail="操作记录不存在")
        return {"message": "操作记录删除成功"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"删除操作记录失败: {str(e)}")

@app.put("/operations/{operation_id}/screenshot")
async def associate_operation_with_screenshot(
    operation_id: str, 
    screenshot_id: str
):
    """关联操作记录到截屏"""
    try:
        success = db_manager.associate_operation_with_screenshot(operation_id, screenshot_id)
        if not success:
            raise HTTPException(status_code=404, detail="操作记录或截屏不存在")
        return {"message": "关联成功"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"关联操作到截屏失败: {str(e)}")

@app.get("/screenshots")
async def get_screenshots(
    page_id: Optional[str] = Query(None, description="页面ID"),
    limit: int = Query(50, description="返回数量限制")
) -> List[ScreenshotInfo]:
    """获取截屏列表"""
    try:
        screenshots = db_manager.get_screenshots(
            page_id=page_id, 
            limit=limit
        )
        
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

@app.get("/screenshots/{screenshot_id}/elements")
async def get_screenshot_elements(screenshot_id: str) -> List[PageElementInfo]:
    """获取截屏的页面元素"""
    try:
        elements = db_manager.get_page_elements(screenshot_id=screenshot_id)
        
        # 转换为Pydantic模型
        api_elements = []
        for element in elements:
            api_elements.append(PageElementInfo(
                id=element.id,
                screenshot_id=element.screenshot_id,
                element_type=element.element_type,
                text_content=element.text_content,
                bounds=element.bounds,
                importance=element.importance,
                custom_tags=element.custom_tags,
                created_at=element.created_at
            ))
        
        return api_elements
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取页面元素失败: {str(e)}")

@app.post("/screenshots/{screenshot_id}/elements")
async def create_element(screenshot_id: str, element: Dict[str, Any]):
    """创建页面元素"""
    try:
        element_id = db_manager.create_page_element(screenshot_id, element)
        return {"element_id": element_id, "message": "元素创建成功"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"创建元素失败: {str(e)}")

@app.put("/elements/{element_id}")
async def update_element(element_id: str, updates: Dict[str, Any]):
    """更新页面元素"""
    try:
        success = db_manager.update_page_element(element_id, updates)
        if not success:
            raise HTTPException(status_code=404, detail="元素不存在")
        return {"message": "元素更新成功"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"更新元素失败: {str(e)}")

@app.delete("/elements/{element_id}")
async def delete_element(element_id: str):
    """删除页面元素"""
    try:
        success = db_manager.delete_page_element(element_id)
        if not success:
            raise HTTPException(status_code=404, detail="元素不存在")
        return {"message": "元素删除成功"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"删除元素失败: {str(e)}")



@app.post("/generate-script/{apk_id}")
async def generate_automation_script(apk_id: str, script_type: str = "python_adb") -> Dict[str, Any]:
    """为特定APK生成自动化脚本"""
    try:
        # 获取APK数据
        operations = db_manager.get_user_operations(apk_id)
        
        # 获取APK的截屏
        screenshots = db_manager.get_screenshots_by_apk(apk_id, limit=50)
        
        # 获取所有截屏的页面元素
        all_elements = []
        for screenshot in screenshots:
            screenshot_id = screenshot.get("screenshot_id")
            elements = db_manager.get_page_elements_by_screenshot(screenshot_id)
            all_elements.extend(elements)
        
        # 生成脚本
        script_content = generate_script_content(operations, screenshots, all_elements, script_type)
        
        # 保存脚本文件
        script_path = save_script_file(apk_id, script_content, script_type)
        
        return {
            "script_path": script_path,
            "script_type": script_type,
            "operations_count": len(operations),
            "screenshots_count": len(screenshots),
            "elements_count": len(all_elements)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"生成脚本失败: {str(e)}")

@app.post("/apks/{apk_id}/generate-script")
async def generate_apk_automation_script(apk_id: str, config: Dict[str, Any]) -> Dict[str, Any]:
    """为特定APK生成自动化脚本"""
    try:
        script_type = config.get("script_type", "python_adb")
        
        # 获取APK数据
        operations = db_manager.get_user_operations(apk_id)
        
        if not operations:
            raise HTTPException(status_code=404, detail="未找到该APK的操作记录")
        
        # 获取APK的截屏
        screenshots = db_manager.get_screenshots_by_apk(apk_id, limit=50)
        
        # 获取所有截屏的页面元素
        all_elements = []
        for screenshot in screenshots:
            screenshot_id = screenshot.get("screenshot_id")
            elements = db_manager.get_page_elements_by_screenshot(screenshot_id)
            all_elements.extend(elements)
        
        # 生成脚本
        script_content = generate_script_content(operations, screenshots, all_elements, script_type)
        
        # 保存脚本文件
        script_path = save_script_file(apk_id, script_content, script_type)
        
        return {
            "script_path": script_path,
            "script_type": script_type,
            "apk_id": apk_id,
            "operations_count": len(operations),
            "screenshots_count": len(screenshots),
            "elements_count": len(all_elements)
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"生成APK自动化脚本失败: {str(e)}")

@app.get("/apks/{apk_id}/scripts")
async def get_apk_scripts(apk_id: str) -> List[Dict[str, Any]]:
    """获取特定APK的脚本列表"""
    try:
        # 这里实现获取脚本列表的逻辑
        # 暂时返回空列表
        return []
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取APK脚本列表失败: {str(e)}")

@app.post("/operations/{operation_id}/associate")
async def associate_operation_with_screenshot_api(operation_id: str, data: Dict[str, Any]):
    """关联操作记录到截屏"""
    try:
        screenshot_id = data.get("screenshot_id")
        if not screenshot_id:
            raise HTTPException(status_code=400, detail="缺少screenshot_id参数")
        
        success = db_manager.associate_operation_with_screenshot(operation_id, screenshot_id)
        if not success:
            raise HTTPException(status_code=404, detail="操作记录或截屏不存在")
        return {"message": "关联成功"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"关联操作到截屏失败: {str(e)}")

def generate_script_content(operations, screenshots, elements, script_type):
    """生成脚本内容"""
    # 这里实现脚本生成逻辑
    # 根据script_type生成不同类型的脚本
    if script_type == "python_adb":
        return generate_python_adb_script(operations, screenshots, elements)
    elif script_type == "appium":
        return generate_appium_script(operations, screenshots, elements)
    else:
        return generate_adb_shell_script(operations, screenshots, elements)

def save_script_file(apk_id, script_content, script_type):
    """保存脚本文件"""
    scripts_dir = Path("analysis_output") / "scripts"
    scripts_dir.mkdir(exist_ok=True)
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"{apk_id}_{script_type}_{timestamp}.py"
    file_path = scripts_dir / filename
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(script_content)
    
    return str(file_path)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)