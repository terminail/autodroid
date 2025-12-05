"""
APK management API endpoints for Autodroid system.
Handles APK operations independent of device dependencies.
"""

from fastapi import APIRouter, HTTPException
from typing import List, Optional

from core.apk.service import ApkManager
from core.apk.models import ApkInfo, ApkCreateRequest, ApkUpdateRequest, ApkListResponse, ApkRegisterRequest, ApkSearchRequest

# Initialize router
router = APIRouter(prefix="/api/apks", tags=["apks"])

# Initialize APK manager
apk_manager = ApkManager()

@router.get("/", response_model=ApkListResponse)
async def get_all_apks():
    """Get all registered APKs (across all devices)"""
    apks = apk_manager.get_all_apks()
    total_count = apk_manager.get_apk_count()
    
    return ApkListResponse(
        apks=apks,
        total_count=total_count
    )

@router.get("/{package_name}", response_model=Optional[ApkInfo])
async def get_apk(package_name: str):
    """Get specific APK information"""
    apk = apk_manager.get_apk(package_name)
    if not apk:
        raise HTTPException(status_code=404, detail="APK not found")
    return apk

@router.post("/")
async def register_apk(apk_create_request: ApkCreateRequest):
    """Register a new APK in the database"""
    try:
        # 创建APK注册请求
        apk_register_request = ApkRegisterRequest(
            device_udid="",  # 空设备UDID，表示仅注册APK不关联设备
            apk_info=apk_create_request
        )
        
        # 注册APK
        apk_info = apk_manager.register_apk_to_device(apk_register_request)
        
        return {
            "message": "APK registered successfully",
            "apk": apk_info
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/{package_name}")
async def update_apk(package_name: str, apk_update_request: ApkUpdateRequest):
    """Update APK information in the database"""
    # Check if APK exists
    existing_apk = apk_manager.get_apk(package_name)
    if not existing_apk:
        raise HTTPException(status_code=404, detail="APK not found")
    
    try:
        # Update APK in database
        updated_apk = apk_manager.update_apk(package_name, apk_update_request)
        
        if not updated_apk:
            raise HTTPException(status_code=400, detail="Failed to update APK")
        
        return {
            "message": "APK updated successfully",
            "apk": updated_apk
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.delete("/{package_name}")
async def delete_apk(package_name: str):
    """Delete APK from the database"""
    # Check if APK exists
    existing_apk = apk_manager.get_apk(package_name)
    if not existing_apk:
        raise HTTPException(status_code=404, detail="APK not found")
    
    # Delete from database
    success = apk_manager.delete_apk(package_name)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to delete APK")
    
    return {
        "message": "APK deleted successfully"
    }

@router.get("/{package_name}/workflows")
async def get_apk_workflows(package_name: str):
    """Get workflows associated with a specific APK"""
    # Check if APK exists
    apk = apk_manager.get_apk(package_name)
    if not apk:
        raise HTTPException(status_code=404, detail="APK not found")
    
    # TODO: Implement workflow association with APK in database
    # For now, return an empty list
    return {
        "message": f"Found 0 workflows for APK {package_name}",
        "workflows": []
    }

@router.get("/{package_name}/devices")
async def get_apk_devices(package_name: str):
    """Get devices that have this APK installed"""
    # Check if APK exists
    apk = apk_manager.get_apk(package_name)
    if not apk:
        raise HTTPException(status_code=404, detail="APK not found")
    
    # Get devices from database
    devices = apk_manager.get_devices_for_apk(package_name)
    
    return {
        "message": f"Found {len(devices)} devices with APK {package_name}",
        "devices": devices
    }

@router.post("/search")
async def search_apks(apk_search_request: ApkSearchRequest):
    """Search APKs by package name or app name"""
    # Search in database
    search_result = apk_manager.search_apks(apk_search_request)
    
    return {
        "message": f"Found {len(search_result.apks)} APKs matching search criteria",
        "result": search_result
    }

@router.post("/{package_name}/devices/{device_udid}")
async def associate_apk_with_device(package_name: str, device_udid: str):
    """Associate an APK with a device"""
    # Check if APK exists
    apk = apk_manager.get_apk(package_name)
    if not apk:
        raise HTTPException(status_code=404, detail="APK not found")
    
    try:
        # 创建APK注册请求
        apk_register_request = ApkRegisterRequest(
            device_udid=device_udid,
            apk_info=ApkCreateRequest(
                package_name=apk.package_name,
                app_name=apk.app_name,
                version=apk.version,
                version_code=apk.version_code,
                is_system=apk.is_system,
                icon_path=apk.icon_path
            )
        )
        
        # 注册APK到设备
        apk_info = apk_manager.register_apk_to_device(apk_register_request)
        
        return {
            "message": f"APK {package_name} associated with device {device_udid} successfully",
            "apk": apk_info
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.delete("/{package_name}/devices/{device_udid}")
async def dissociate_apk_from_device(package_name: str, device_udid: str):
    """Dissociate an APK from a device"""
    # Check if APK exists
    apk = apk_manager.get_apk(package_name)
    if not apk:
        raise HTTPException(status_code=404, detail="APK not found")
    
    # Dissociate APK from device
    success = apk_manager.dissociate_apk_from_device(package_name, device_udid)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to dissociate APK from device")
    
    return {
        "message": f"APK {package_name} dissociated from device {device_udid} successfully"
    }

@router.get("/devices/{device_udid}")
async def get_apks_for_device(device_udid: str):
    """Get all APKs installed on a specific device"""
    # Get APKs for device from database
    apks = apk_manager.get_apks_for_device(device_udid)
    
    return {
        "message": f"Found {len(apks)} APKs on device {device_udid}",
        "apks": apks
    }