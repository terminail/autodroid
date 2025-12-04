"""
Device management API endpoints for Autodroid system.
Handles device registration, APK management, and device information retrieval.
"""

from fastapi import APIRouter, HTTPException
from typing import List, Dict, Any

from core.device.device_manager import DeviceManager

# Initialize router
router = APIRouter(prefix="/api/devices", tags=["devices"])

# Initialize device manager
device_manager = DeviceManager()

@router.get("/", response_model=List[Dict[str, Any]])
async def get_devices():
    """Get all registered devices"""
    return [device.__dict__ for device in device_manager.devices.values()]

@router.get("/{udid}", response_model=Dict[str, Any])
async def get_device(udid: str):
    """Get specific device information"""
    device = device_manager.devices.get(udid)
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")
    return device.__dict__

@router.post("/register")
async def register_device(device_info: Dict[str, Any]):
    """Register a device from app report"""
    try:
        device = device_manager.register_device(device_info)
        return {
            "message": f"Device registered successfully",
            "device": device.__dict__
        }
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

# APK Management Endpoints
@router.get("/{udid}/apks")
async def get_device_apks(udid: str):
    """Get all APKs for a device"""
    try:
        apks = device_manager.get_apks(udid)
        return {
            "message": f"Found {len(apks)} APKs for device {udid}",
            "apks": [apk.__dict__ for apk in apks]
        }
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

@router.get("/{udid}/apks/{apkid}")
async def get_device_apk(udid: str, apkid: str):
    """Get a specific APK for a device"""
    try:
        apk = device_manager.get_apk(udid, apkid)
        if not apk:
            raise HTTPException(status_code=404, detail=f"APK with ID {apkid} not found for device {udid}")
        return {
            "message": f"Found APK {apkid} for device {udid}",
            "apk": apk.__dict__
        }
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

@router.post("/{udid}/apks")
async def add_device_apk(udid: str, apk_info: Any):
    """Add one or multiple APKs to a device"""
    try:
        # Check if apk_info is a single APK or a list of APKs
        if isinstance(apk_info, list):
            # Handle bulk APK registration
            added_apks = []
            errors = []
            
            for apk_item in apk_info:
                try:
                    apk = device_manager.add_apk(udid, apk_item)
                    added_apks.append(apk.__dict__)
                except ValueError as e:
                    errors.append({
                        "apk_info": apk_item,
                        "error": str(e)
                    })
            
            return {
                "message": f"Added {len(added_apks)} APKs to device {udid}",
                "added_apks": added_apks,
                "errors": errors,
                "total_processed": len(apk_info)
            }
        else:
            # Handle single APK registration
            apk = device_manager.add_apk(udid, apk_info)
            return {
                "message": f"APK added successfully to device {udid}",
                "apk": apk.__dict__
            }
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.post("/{udid}/apks/bulk")
async def add_device_apks_bulk(udid: str, apk_list: List[Dict[str, Any]]):
    """Add multiple APKs to a device in bulk"""
    try:
        added_apks = []
        errors = []
        
        for apk_info in apk_list:
            try:
                apk = device_manager.add_apk(udid, apk_info)
                added_apks.append(apk.__dict__)
            except ValueError as e:
                errors.append({
                    "apk_info": apk_info,
                    "error": str(e)
                })
        
        return {
            "message": f"Added {len(added_apks)} APKs to device {udid}",
            "added_apks": added_apks,
            "errors": errors,
            "total_processed": len(apk_list)
        }
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/{udid}/apks/{apkid}")
async def update_device_apk(udid: str, apkid: str, apk_info: Dict[str, Any]):
    """Update an APK on a device"""
    try:
        apk = device_manager.update_apk(udid, apkid, apk_info)
        return {
            "message": f"APK updated successfully for device {udid}",
            "apk": apk.__dict__
        }
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

@router.delete("/{udid}/apks/{apkid}")
async def delete_device_apk(udid: str, apkid: str):
    """Delete an APK from a device"""
    try:
        success = device_manager.delete_apk(udid, apkid)
        if success:
            return {
                "message": f"APK deleted successfully from device {udid}"
            }
        else:
            raise HTTPException(status_code=404, detail=f"APK with ID {apkid} not found for device {udid}")
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

@router.get("/{udid}/apks/{apkid}/workflows")
async def get_apk_workflows(udid: str, apkid: str):
    """Get workflows associated with a specific APK on a device"""
    try:
        # Check if device and APK exist
        apk = device_manager.get_apk(udid, apkid)
        if not apk:
            raise HTTPException(status_code=404, detail=f"APK with ID {apkid} not found for device {udid}")
        
        # TODO: Implement workflow association with APK
        # For now, return an empty list
        return {
            "message": f"Found 0 workflows for APK {apkid} on device {udid}",
            "workflows": []
        }
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))