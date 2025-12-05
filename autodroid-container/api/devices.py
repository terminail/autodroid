"""
Device management API endpoints for Autodroid system.
Handles device registration, APK management, and device information retrieval.
"""

from fastapi import APIRouter, HTTPException
from typing import List, Dict, Any

from core.device.service import DeviceManager
from core.device.models import DeviceInfo, DeviceCreateRequest, DeviceUpdateRequest, DeviceListResponse, DeviceAssignmentRequest, DeviceStatusUpdateRequest
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

@router.get("/{udid}", response_model=DeviceInfo)
async def get_device(udid: str):
    """Get specific device information"""
    # Check if device is available
    if not device_manager.is_device_available(udid):
        raise HTTPException(status_code=404, detail="Device not found")
    
    # Get device from database
    devices = device_manager.get_all_devices()
    device = next((d for d in devices if d.udid == udid), None)
    
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")
    
    return device

@router.post("/")
async def register_device(device_create_request: DeviceCreateRequest):
    """Register a device from app report"""
    try:
        device_info = device_create_request.dict()
        device = device_manager.register_device(device_info)
        
        return {
            "message": f"Device registered successfully",
            "device": device
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/{udid}")
async def update_device(udid: str, device_update_request: DeviceUpdateRequest):
    """Update device information"""
    # Check if device exists
    if not device_manager.is_device_available(udid):
        raise HTTPException(status_code=404, detail="Device not found")
    
    try:
        # Update device status
        update_data = device_update_request.dict(exclude_unset=True)
        
        # Handle status updates separately
        if 'is_online' in update_data or 'battery_level' in update_data:
            is_online = update_data.get('is_online', False)
            battery_level = update_data.get('battery_level', 0)
            device_manager.update_device_status(udid, is_online, battery_level)
        
        # TODO: Implement full device update functionality
        # For now, just return the current device info
        devices = device_manager.get_all_devices()
        device = next((d for d in devices if d.udid == udid), None)
        
        return {
            "message": f"Device {udid} updated successfully",
            "device": device
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.delete("/{udid}")
async def delete_device(udid: str):
    """Delete a device"""
    # Check if device exists
    if not device_manager.is_device_available(udid):
        raise HTTPException(status_code=404, detail="Device not found")
    
    # Delete device
    success = device_manager.delete_device(udid)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to delete device")
    
    return {
        "message": f"Device {udid} deleted successfully"
    }

# APK Management Endpoints
@router.get("/{udid}/apks")
async def get_device_apks(udid: str):
    """Get all APKs for a device"""
    # Check if device exists
    if not device_manager.is_device_available(udid):
        raise HTTPException(status_code=404, detail="Device not found")
    
    try:
        apks = device_manager.get_apks(udid)
        return {
            "message": f"Found {len(apks)} APKs for device {udid}",
            "apks": apks
        }
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))

@router.get("/{udid}/apks/{package_name}", response_model=ApkInfo)
async def get_device_apk(udid: str, package_name: str):
    """Get a specific APK for a device"""
    # Check if device exists
    if not device_manager.is_device_available(udid):
        raise HTTPException(status_code=404, detail="Device not found")
    
    try:
        apk = device_manager.get_apk(udid, package_name)
        if not apk:
            raise HTTPException(status_code=404, detail=f"APK with package name {package_name} not found for device {udid}")
        
        return apk
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))

@router.post("/{udid}/apks")
async def add_device_apk(udid: str, apk_info: ApkCreateRequest):
    """Add an APK to a device"""
    # Check if device exists
    if not device_manager.is_device_available(udid):
        raise HTTPException(status_code=404, detail="Device not found")
    
    try:
        apk = device_manager.add_apk(udid, apk_info.dict())
        
        return {
            "message": f"APK added to device {udid} successfully",
            "apk": apk
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/{udid}/apks/{package_name}")
async def update_device_apk(udid: str, package_name: str, apk_info: Dict[str, Any]):
    """Update an APK for a device"""
    # Check if device exists
    if not device_manager.is_device_available(udid):
        raise HTTPException(status_code=404, detail="Device not found")
    
    try:
        apk = device_manager.update_apk(udid, package_name, apk_info)
        
        return {
            "message": f"Updated APK {package_name} for device {udid}",
            "apk": apk
        }
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))

@router.delete("/{udid}/apks/{package_name}")
async def delete_device_apk(udid: str, package_name: str):
    """Delete an APK from a device"""
    # Check if device exists
    if not device_manager.is_device_available(udid):
        raise HTTPException(status_code=404, detail="Device not found")
    
    try:
        success = device_manager.delete_apk(udid, package_name)
        if not success:
            raise HTTPException(status_code=404, detail=f"APK with package name {package_name} not found for device {udid}")
        
        return {
            "message": f"Deleted APK {package_name} from device {udid}"
        }
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))

@router.get("/{udid}/apks/{package_name}/workflows")
async def get_apk_workflows(udid: str, package_name: str):
    """Get workflows associated with a specific APK on a device"""
    # Check if device exists
    if not device_manager.is_device_available(udid):
        raise HTTPException(status_code=404, detail="Device not found")
    
    try:
        # Check if APK exists
        apk = device_manager.get_apk(udid, package_name)
        if not apk:
            raise HTTPException(status_code=404, detail=f"APK with package name {package_name} not found for device {udid}")
        
        # TODO: Implement workflow association with APK
        # For now, return an empty list
        return {
            "message": f"Found 0 workflows for APK {package_name} on device {udid}",
            "workflows": []
        }
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))

@router.get("/users/{user_id}", response_model=List[DeviceInfo])
async def get_devices_by_user(user_id: str):
    """Get devices assigned to a specific user"""
    try:
        devices = device_manager.get_devices_by_user(user_id)
        return devices
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))

@router.post("/{udid}/assign")
async def assign_device_to_user(udid: str, assignment_request: DeviceAssignmentRequest):
    """Assign a device to a user"""
    # Check if device exists
    if not device_manager.is_device_available(udid):
        raise HTTPException(status_code=404, detail="Device not found")
    
    try:
        success = device_manager.assign_device_to_user(udid, assignment_request.user_id)
        
        if not success:
            raise HTTPException(status_code=400, detail="Failed to assign device to user")
        
        return {
            "message": f"Device {udid} assigned to user {assignment_request.user_id} successfully"
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.post("/{udid}/unassign")
async def unassign_device_from_user(udid: str):
    """Unassign a device from a user"""
    # Check if device exists
    if not device_manager.is_device_available(udid):
        raise HTTPException(status_code=404, detail="Device not found")
    
    try:
        success = device_manager.unassign_device_from_user(udid)
        
        if not success:
            raise HTTPException(status_code=400, detail="Failed to unassign device from user")
        
        return {
            "message": f"Device {udid} unassigned from user successfully"
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/{udid}/status")
async def update_device_status(udid: str, status_request: DeviceStatusUpdateRequest):
    """Update device status (online/offline and battery level)"""
    # Check if device exists
    if not device_manager.is_device_available(udid):
        raise HTTPException(status_code=404, detail="Device not found")
    
    try:
        success = device_manager.update_device_status(udid, status_request.is_online, status_request.battery_level)
        
        if not success:
            raise HTTPException(status_code=400, detail="Failed to update device status")
        
        return {
            "message": f"Device {udid} status updated successfully"
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))