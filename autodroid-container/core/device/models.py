"""
Device-related data models for Autodroid system
"""

from typing import Dict, List, Optional, Any
from dataclasses import dataclass, field


@dataclass
class ApkInfo:
    """APK information model"""
    apkid: str
    package_name: str
    app_name: str
    version: str
    version_code: int
    installed_time: int
    is_system: bool
    icon_path: str = ""


@dataclass
class DeviceInfo:
    """Device information model"""
    udid: str
    device_name: str
    android_version: str
    battery_level: int
    is_online: bool
    connection_type: str  # 'usb' or 'network'
    user_id: Optional[str] = None  # Associated user ID
    apks: Dict[str, ApkInfo] = field(default_factory=dict)


@dataclass 
class DeviceRegistration:
    """Device registration request model"""
    udid: str
    device_name: str
    android_version: str
    battery_level: int
    connection_type: str
    user_id: Optional[str] = None


@dataclass
class ApkRegistration:
    """APK registration request model"""
    apkid: str
    package_name: str
    app_name: str
    version: str
    version_code: int
    is_system: bool
    icon_path: str = ""