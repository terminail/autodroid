"""
Device management API endpoints for Autodroid system.
Handles device registration, APK management, and device information retrieval.
"""

from fastapi import APIRouter, HTTPException
from typing import List, Dict, Any
from datetime import datetime

from core.device.service import DeviceManager
from core.device.models import DeviceInfoResponse, DeviceCreateRequest, DeviceListResponse, DeviceAssignmentRequest, DeviceStatusUpdateRequest, DeviceCreateResponse, DeviceDeleteResponse, DeviceAssignmentResponse, DeviceCheckResponse
from core.apk.models import ApkInfo, ApkCreateRequest

# Initialize router
router = APIRouter(prefix="/api/devices", tags=["devices"])

# Initialize device manager
device_manager = DeviceManager()

@router.get("/", response_model=DeviceListResponse)
async def get_devices():
    """Get all registered devices"""
    devices = device_manager.get_all_devices()
    total_count = device_manager.get_device_count()
    online_count = device_manager.get_online_device_count()
    
    return DeviceListResponse(
        devices=devices,
        total_count=total_count,
        online_count=online_count
    )

@router.get("/{serialno}", response_model=DeviceInfoResponse)
async def get_device(serialno: str):
    """Get specific device information"""
    # Check if device exists
    if not device_manager.device_exists(serialno):
        raise HTTPException(status_code=404, detail="Device not found")
    
    # Get device directly from database
    device = device_manager.get_device_by_serialno(serialno)
    
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")
    
    return device

@router.post("/", response_model=DeviceCreateResponse)
async def register_device(device_create_request: DeviceCreateRequest):
    """Register a device from app report"""
    try:
        device = device_manager.register_device(device_create_request)
        
        return DeviceCreateResponse(
            success=True,
            message=f"Device registered successfully",
            device_id=device.serialno,
            serialno=device.serialno,
            udid=device.udid,
            registered_at=device.registered_at,
            device=DeviceInfoResponse.from_orm(device) if hasattr(device, '__dict__') else None
        )
    except Exception as e:
        return DeviceCreateResponse(
            success=False,
            message=str(e),
            device_id=None,
            udid=None,
            registered_at=None,
            device=None
        )

@router.delete("/{serialno}")
async def delete_device(serialno: str):
    """Delete a device"""
    # Check if device exists
    if not device_manager.device_exists(serialno):
        raise HTTPException(status_code=404, detail="Device not found")
    
    # Delete device
    success = device_manager.delete_device(serialno)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to delete device")
    
    return {
        "message": f"Device {serialno} deleted successfully"
    }

@router.post("/{serialno}/check", response_model=DeviceCheckResponse)
async def check_device(serialno: str):
    """检查设备调试设置、安装app等情况"""
    try:
        # 调用设备管理器检查设备状态
        device_info = device_manager.check_device(serialno)
        
        # 获取设备详细信息
        device_detail = device_manager.get_device_by_serialno(serialno)
        device_detail_response = None
        if device_detail:
            device_detail_response = DeviceInfoResponse.from_orm(device_detail)
        
        return DeviceCheckResponse(
            success=device_info["success"],
            message=device_info["message"],
            serialno=device_info["serialno"],
            udid=device_info.get("udid"),
            usb_debug_enabled=device_info.get("usb_debug_enabled", False),
            wifi_debug_enabled=device_info.get("wifi_debug_enabled", False),
            installed_apps=device_info.get("installed_apps", []),
            check_time=datetime.now(),
            device_info=device_detail_response
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/{serialno}/apks")
async def get_device_apks(serialno: str):
    """Get all APKs for a device"""
    # Check if device exists
    if not device_manager.device_exists(serialno):
        raise HTTPException(status_code=404, detail="Device not found")
    
    try:
        apks = device_manager.get_apks(serialno)
        return {
            "message": f"Found {len(apks)} APKs for device {serialno}",
            "apks": apks
        }
    except Exception as e:
        import logging
        logging.error(f"Error getting APKs for device {serialno}: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/{serialno}/apks/{package_name}", response_model=ApkInfo)
async def get_device_apk(serialno: str, package_name: str):
    """Get a specific APK for a device"""
    # Check if device exists
    if not device_manager.device_exists(serialno):
        raise HTTPException(status_code=404, detail="Device not found")
    
    try:
        apk = device_manager.get_apk(serialno, package_name)
        if not apk:
            raise HTTPException(status_code=404, detail=f"APK with package name {package_name} not found for device {serialno}")
        
        return apk
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.get("/users/{user_id}", response_model=List[DeviceInfoResponse])
async def get_devices_by_user(user_id: str):
    """Get devices assigned to a specific user"""
    try:
        devices = device_manager.get_devices_by_user(user_id)
        return devices
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))

@router.post("/{serialno}/assign")
async def assign_device_to_user(serialno: str, assignment_request: DeviceAssignmentRequest):
    """Assign a device to a user"""
    # Check if device exists
    if not device_manager.device_exists(serialno):
        raise HTTPException(status_code=404, detail="Device not found")
    
    try:
        success = device_manager.assign_device_to_user(serialno, assignment_request.user_id)
        if not success:
            raise HTTPException(status_code=400, detail="Failed to assign device to user")
        
        return {
            "message": f"Device {serialno} assigned to user {assignment_request.user_id} successfully"
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.post("/{serialno}/unassign")
async def unassign_device_from_user(serialno: str):
    """Unassign a device from a user"""
    # Check if device exists
    if not device_manager.device_exists(serialno):
        raise HTTPException(status_code=404, detail="Device not found")
    
    try:
        success = device_manager.unassign_device_from_user(serialno)
        if not success:
            raise HTTPException(status_code=400, detail="Failed to unassign device from user")
        
        return {
            "message": f"Device {serialno} unassigned from user successfully"
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
