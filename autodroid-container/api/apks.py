"""
APK management API endpoints for Autodroid system.
Handles APK operations independent of device dependencies.
"""

from fastapi import APIRouter, HTTPException
from typing import List, Dict, Any
import os
import yaml

from core.device.models import ApkInfo, ApkRegistration
from core.apk.apk_database import ApkDatabase

# Initialize router
router = APIRouter(prefix="/api/apks", tags=["apks"])

# Initialize APK database
apk_db = ApkDatabase()

@router.get("/", response_model=List[Dict[str, Any]])
async def get_all_apks():
    """Get all registered APKs (across all devices)"""
    return apk_db.get_all_apks()

@router.get("/{apkid}", response_model=Dict[str, Any])
async def get_apk(apkid: str):
    """Get specific APK information"""
    apk = apk_db.get_apk(apkid)
    if not apk:
        raise HTTPException(status_code=404, detail="APK not found")
    return apk

@router.post("/register")
async def register_apk(apk_registration: ApkRegistration):
    """Register a new APK in the database"""
    try:
        # Create APK in database
        success = apk_db.create_apk(
            apkid=apk_registration.apkid,
            package_name=apk_registration.package_name,
            app_name=apk_registration.app_name,
            version=apk_registration.version,
            version_code=apk_registration.version_code,
            installed_time=int(os.path.getctime(__file__)),  # Use current time
            is_system=apk_registration.is_system,
            icon_path=apk_registration.icon_path
        )
        
        if not success:
            raise HTTPException(status_code=400, detail="Failed to register APK")
        
        # Get the created APK
        apk = apk_db.get_apk(apk_registration.apkid)
        
        return {
            "message": "APK registered successfully",
            "apk": apk
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/{apkid}")
async def update_apk(apkid: str, apk_info: Dict[str, Any]):
    """Update APK information in the database"""
    # Check if APK exists
    existing_apk = apk_db.get_apk(apkid)
    if not existing_apk:
        raise HTTPException(status_code=404, detail="APK not found")
    
    try:
        # Update APK in database
        success = apk_db.update_apk(apkid, apk_info)
        
        if not success:
            raise HTTPException(status_code=400, detail="Failed to update APK")
        
        # Get the updated APK
        updated_apk = apk_db.get_apk(apkid)
        
        return {
            "message": "APK updated successfully",
            "apk": updated_apk
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.delete("/{apkid}")
async def delete_apk(apkid: str):
    """Delete APK from the database"""
    # Check if APK exists
    existing_apk = apk_db.get_apk(apkid)
    if not existing_apk:
        raise HTTPException(status_code=404, detail="APK not found")
    
    # Delete from database
    success = apk_db.delete_apk(apkid)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to delete APK")
    
    return {
        "message": "APK deleted successfully"
    }

@router.get("/{apkid}/workflows")
async def get_apk_workflows(apkid: str):
    """Get workflows associated with a specific APK"""
    # Check if APK exists
    apk = apk_db.get_apk(apkid)
    if not apk:
        raise HTTPException(status_code=404, detail="APK not found")
    
    # TODO: Implement workflow association with APK in database
    # For now, return an empty list
    return {
        "message": f"Found 0 workflows for APK {apkid}",
        "workflows": []
    }

@router.get("/{apkid}/devices")
async def get_apk_devices(apkid: str):
    """Get devices that have this APK installed"""
    # Check if APK exists
    apk = apk_db.get_apk(apkid)
    if not apk:
        raise HTTPException(status_code=404, detail="APK not found")
    
    # Get devices from database
    devices = apk_db.get_devices_for_apk(apkid)
    
    return {
        "message": f"Found {len(devices)} devices with APK {apkid}",
        "devices": devices
    }

@router.get("/search/{query}")
async def search_apks(query: str):
    """Search APKs by package name or app name"""
    # Search in database
    matching_apks = apk_db.search_apks(query)
    
    return {
        "message": f"Found {len(matching_apks)} APKs matching '{query}'",
        "apks": matching_apks
    }

@router.post("/{apkid}/devices/{device_udid}")
async def associate_apk_with_device(apkid: str, device_udid: str):
    """Associate an APK with a device"""
    # Check if APK exists
    apk = apk_db.get_apk(apkid)
    if not apk:
        raise HTTPException(status_code=404, detail="APK not found")
    
    # Associate APK with device
    success = apk_db.associate_apk_with_device(apkid, device_udid)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to associate APK with device")
    
    return {
        "message": f"APK {apkid} associated with device {device_udid} successfully"
    }

@router.delete("/{apkid}/devices/{device_udid}")
async def dissociate_apk_from_device(apkid: str, device_udid: str):
    """Dissociate an APK from a device"""
    # Check if APK exists
    apk = apk_db.get_apk(apkid)
    if not apk:
        raise HTTPException(status_code=404, detail="APK not found")
    
    # Dissociate APK from device
    success = apk_db.dissociate_apk_from_device(apkid, device_udid)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to dissociate APK from device")
    
    return {
        "message": f"APK {apkid} dissociated from device {device_udid} successfully"
    }

@router.get("/devices/{device_udid}")
async def get_apks_for_device(device_udid: str):
    """Get all APKs installed on a specific device"""
    # Get APKs for device from database
    apks = apk_db.get_apks_for_device(device_udid)
    
    return {
        "message": f"Found {len(apks)} APKs on device {device_udid}",
        "apks": apks
    }