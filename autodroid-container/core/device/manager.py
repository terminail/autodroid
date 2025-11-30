import subprocess
import json
import time
from typing import Dict, List, Optional
from dataclasses import dataclass

from core.zenoh.manager import ZenohManager

@dataclass
class DeviceInfo:
    udid: str
    device_name: str
    android_version: str
    battery_level: int
    is_online: bool
    connection_type: str  # 'usb' or 'network'
    zenoh_connected: bool = False

class DeviceManager:
    def __init__(self):
        self.devices: Dict[str, DeviceInfo] = {}
        self.max_concurrent_tasks = 5
        self.zenoh_manager = ZenohManager()
        self.zenoh_initialized = False
    
    def get_connected_devices(self) -> List[str]:
        """Get list of connected device UDIDs using ADB"""
        try:
            result = subprocess.run(
                ['adb', 'devices'],
                capture_output=True,
                text=True,
                check=True
            )
            lines = result.stdout.strip().split('\n')[1:]  # Skip header
            return [line.split('\t')[0] for line in lines if line.strip()]
        except subprocess.CalledProcessError:
            return []
    
    def get_device_info(self, udid: str) -> Optional[DeviceInfo]:
        """Get detailed information about a specific device"""
        try:
            # Get device name
            device_name = subprocess.run(
                ['adb', '-s', udid, 'shell', 'getprop', 'ro.product.model'],
                capture_output=True,
                text=True,
                check=True
            ).stdout.strip()
            
            # Get Android version
            android_version = subprocess.run(
                ['adb', '-s', udid, 'shell', 'getprop', 'ro.build.version.release'],
                capture_output=True,
                text=True,
                check=True
            ).stdout.strip()
            
            # Get battery level
            battery_output = subprocess.run(
                ['adb', '-s', udid, 'shell', 'dumpsys', 'battery'],
                capture_output=True,
                text=True,
                check=True
            ).stdout
            battery_level = int([line for line in battery_output.split('\n') if 'level' in line][0].split(':')[1].strip())
            
            # Determine connection type
            connection_type = 'usb' if ':' not in udid else 'network'
            
            return DeviceInfo(
                udid=udid,
                device_name=device_name,
                android_version=android_version,
                battery_level=battery_level,
                is_online=True,
                connection_type=connection_type
            )
        except Exception:
            return None
    
    def scan_devices(self) -> List[DeviceInfo]:
        """Scan all connected devices and update device list"""
        connected_udids = self.get_connected_devices()
        device_list = []
        
        for udid in connected_udids:
            info = self.get_device_info(udid)
            if info:
                self.devices[udid] = info
                device_list.append(info)
        
        # Remove devices that are no longer connected
        for udid in list(self.devices.keys()):
            if udid not in connected_udids:
                del self.devices[udid]
        
        return device_list
    
    def is_device_available(self, udid: str) -> bool:
        """Check if a device is available for automation"""
        device = self.devices.get(udid)
        return device is not None and device.is_online and device.battery_level > 20
    
    def get_available_devices(self) -> List[DeviceInfo]:
        """Get list of devices available for automation"""
        return [device for device in self.devices.values() if self.is_device_available(device.udid)]
    
    async def initialize_zenoh(self) -> bool:
        """Initialize Zenoh manager for device discovery"""
        if not self.zenoh_initialized:
            self.zenoh_initialized = await self.zenoh_manager.initialize()
        return self.zenoh_initialized
    
    async def discover_devices_zenoh(self, timeout: int = 5) -> List[str]:
        """Discover devices using Zenoh"""
        if not await self.initialize_zenoh():
            return []
        
        return await self.zenoh_manager.discover_devices(timeout)
    
    async def update_devices_from_zenoh(self):
        """Update device list with information from Zenoh"""
        if not await self.initialize_zenoh():
            return
        
        zenoh_devices = self.zenoh_manager.get_devices()
        
        for device_id, device_info in zenoh_devices.items():
            # Check if device already exists
            if device_id in self.devices:
                # Update existing device with Zenoh information
                existing_device = self.devices[device_id]
                self.devices[device_id] = DeviceInfo(
                    udid=device_id,
                    device_name=device_info.get('data', {}).get('device_name', existing_device.device_name),
                    android_version=device_info.get('data', {}).get('android_version', existing_device.android_version),
                    battery_level=existing_device.battery_level,
                    is_online=True,
                    connection_type='zenoh',
                    zenoh_connected=True
                )
            else:
                # Add new device from Zenoh
                self.devices[device_id] = DeviceInfo(
                    udid=device_id,
                    device_name=device_info.get('data', {}).get('device_name', device_id),
                    android_version=device_info.get('data', {}).get('android_version', 'Unknown'),
                    battery_level=50,  # Default value, will be updated later
                    is_online=True,
                    connection_type='zenoh',
                    zenoh_connected=True
                )
    
    async def scan_devices(self) -> List[DeviceInfo]:
        """Scan all connected devices using both ADB and Zenoh"""
        # Scan using ADB first
        connected_udids = self.get_connected_devices()
        
        # Update devices from ADB
        for udid in connected_udids:
            info = self.get_device_info(udid)
            if info:
                # Check if device was already discovered via Zenoh
                if udid in self.devices:
                    # Keep Zenoh connection status
                    info.zenoh_connected = self.devices[udid].zenoh_connected
                self.devices[udid] = info
        
        # Update devices from Zenoh
        await self.update_devices_from_zenoh()
        
        # Remove devices that are no longer connected
        for udid in list(self.devices.keys()):
            if udid not in connected_udids and not self.devices[udid].zenoh_connected:
                del self.devices[udid]
        
        return list(self.devices.values())
