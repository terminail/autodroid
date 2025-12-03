import subprocess
import json
import time
from typing import Dict, List, Optional, Any

from .models import ApkInfo, DeviceInfo

class DeviceManager:
    def __init__(self):
        self.devices: Dict[str, DeviceInfo] = {}
        self.max_concurrent_tasks = 5
    
    def is_device_available(self, udid: str) -> bool:
        """Check if a device is available for automation"""
        device = self.devices.get(udid)
        return device is not None and device.is_online and device.battery_level > 20
    
    def get_available_devices(self) -> List[DeviceInfo]:
        """Get list of devices available for automation"""
        return [device for device in self.devices.values() if self.is_device_available(device.udid)]
    
    def register_device(self, device_info: Dict[str, Any]) -> DeviceInfo:
        """Register a device from app report"""
        udid = device_info.get('udid')
        if not udid:
            raise ValueError("Device UDID is required")
        
        # Create DeviceInfo object from the provided data
        device = DeviceInfo(
            udid=udid,
            device_name=device_info.get('device_name', 'Unknown Device'),
            android_version=device_info.get('android_version', 'Unknown'),
            battery_level=device_info.get('battery_level', 50),
            is_online=True,
            connection_type=device_info.get('connection_type', 'network'),
            user_id=device_info.get('user_id')  # 关联的用户ID
        )
        
        # Add or update the device in the device list
        self.devices[udid] = device
        return device
    
    def add_apk(self, udid: str, apk_info: Dict[str, Any]) -> ApkInfo:
        """Add an APK to a device"""
        device = self.devices.get(udid)
        if not device:
            raise ValueError(f"Device with UDID {udid} not found")
        
        apkid = apk_info.get('apkid')
        if not apkid:
            raise ValueError("APK ID is required")
        
        # Create ApkInfo object
        apk = ApkInfo(
            apkid=apkid,
            package_name=apk_info.get('package_name', ''),
            app_name=apk_info.get('app_name', ''),
            version=apk_info.get('version', '1.0'),
            version_code=apk_info.get('version_code', 1),
            installed_time=apk_info.get('installed_time', int(time.time())),
            is_system=apk_info.get('is_system', False),
            icon_path=apk_info.get('icon_path', '')
        )
        
        # Add APK to device
        device.apks[apkid] = apk
        return apk
    
    def get_apks(self, udid: str) -> List[ApkInfo]:
        """Get all APKs for a device"""
        device = self.devices.get(udid)
        if not device:
            raise ValueError(f"Device with UDID {udid} not found")
        
        return list(device.apks.values())
    
    def get_apk(self, udid: str, apkid: str) -> Optional[ApkInfo]:
        """Get a specific APK for a device"""
        device = self.devices.get(udid)
        if not device:
            raise ValueError(f"Device with UDID {udid} not found")
        
        return device.apks.get(apkid)
    
    def update_apk(self, udid: str, apkid: str, apk_info: Dict[str, Any]) -> ApkInfo:
        """Update an APK for a device"""
        device = self.devices.get(udid)
        if not device:
            raise ValueError(f"Device with UDID {udid} not found")
        
        existing_apk = device.apks.get(apkid)
        if not existing_apk:
            raise ValueError(f"APK with ID {apkid} not found for device {udid}")
        
        # Update APK info
        updated_apk = existing_apk.__dict__.copy()
        updated_apk.update(apk_info)
        
        # Create new ApkInfo object
        apk = ApkInfo(
            apkid=updated_apk['apkid'],
            package_name=updated_apk['package_name'],
            app_name=updated_apk['app_name'],
            version=updated_apk['version'],
            version_code=updated_apk['version_code'],
            installed_time=updated_apk['installed_time'],
            is_system=updated_apk['is_system'],
            icon_path=updated_apk['icon_path']
        )
        
        # Update APK in device
        device.apks[apkid] = apk
        return apk
    
    def delete_apk(self, udid: str, apkid: str) -> bool:
        """Delete an APK from a device"""
        device = self.devices.get(udid)
        if not device:
            raise ValueError(f"Device with UDID {udid} not found")
        
        if apkid in device.apks:
            del device.apks[apkid]
            return True
        return False
    
    def get_devices_by_user(self, user_id: str) -> List[DeviceInfo]:
        """根据用户ID获取设备列表"""
        return [device for device in self.devices.values() if device.user_id == user_id]
    
    def assign_device_to_user(self, udid: str, user_id: str) -> bool:
        """将设备分配给用户"""
        device = self.devices.get(udid)
        if not device:
            return False
        
        device.user_id = user_id
        return True
    
    def unassign_device_from_user(self, udid: str) -> bool:
        """取消设备与用户的关联"""
        device = self.devices.get(udid)
        if not device:
            return False
        
        device.user_id = None
        return True

if __name__ == "__main__":
    """Main entry point for device manager service"""
    import logging
    import time
    
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)
    
    device_manager = DeviceManager()
    logger.info("Device Manager Service Started")
    
    # Keep the service running
    try:
        while True:
            time.sleep(60)
    except KeyboardInterrupt:
        logger.info("Device Manager Service Stopped")
