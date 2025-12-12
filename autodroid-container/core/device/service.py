import time
from typing import List, Optional, Dict, Any
from peewee import DoesNotExist

from .database import DeviceDatabase
from . import DeviceInfo
from ..apk.models import ApkInfo

class DeviceManager:
    def __init__(self):
        """初始化设备管理器，使用统一的数据库接口"""
        self.max_concurrent_tasks = 5
        self.db = DeviceDatabase()
    
    def is_device_available(self, udid: str) -> bool:
        """检查设备是否可用于自动化"""
        return self.db.is_device_available(udid)
    
    def get_available_devices(self) -> List[DeviceInfo]:
        """获取可用于自动化的设备列表"""
        devices = self.db.get_available_devices()
        return [
            DeviceInfo(
                udid=device.udid,
                device_name=device.device_name,
                android_version=device.android_version,
                battery_level=device.battery_level,
                is_online=device.is_online,
                connection_type=device.connection_type,
                user_id=device.user.id if device.user else None
            ) for device in devices
        ]
    
    def register_device(self, device_info: Dict[str, Any]) -> DeviceInfo:
        """从应用报告注册设备"""
        device = self.db.register_device(device_info)
        
        # 返回DeviceInfo对象
        return DeviceInfo(
            udid=device.udid,
            device_name=device.device_name,
            android_version=device.android_version,
            battery_level=device.battery_level,
            is_online=device.is_online,
            connection_type=device.connection_type,
            user_id=device.user.id if device.user else None
        )
    
    def add_apk(self, udid: str, apk_info: Dict[str, Any]) -> ApkInfo:
        """将APK添加到设备"""
        apk = self.db.add_apk_to_device(udid, apk_info)
        
        # 返回ApkInfo对象
        return ApkInfo(
            package_name=apk.package_name,
            app_name=apk.app_name,
            version=apk.version,
            version_code=apk.version_code,
            installed_time=apk.installed_time,
            is_system=apk.is_system,
            icon_path=apk.icon_path
        )
    
    def get_apks(self, udid: str) -> List[ApkInfo]:
        """获取设备的所有APK"""
        apks = self.db.get_device_apks(udid)
        return [
            ApkInfo(
                package_name=apk.package_name,
                app_name=apk.app_name,
                version=apk.version,
                version_code=apk.version_code,
                installed_time=apk.installed_time,
                is_system=apk.is_system,
                icon_path=apk.icon_path
            ) for apk in apks
        ]
    
    def get_apk(self, udid: str, package_name: str) -> Optional[ApkInfo]:
        """获取设备的特定APK"""
        apk = self.db.get_device_apk(udid, package_name)
        if apk:
            return ApkInfo(
                package_name=apk.package_name,
                app_name=apk.app_name,
                version=apk.version,
                version_code=apk.version_code,
                installed_time=apk.installed_time,
                is_system=apk.is_system,
                icon_path=apk.icon_path
            )
        return None
    
    def update_apk(self, udid: str, package_name: str, apk_info: Dict[str, Any]) -> ApkInfo:
        """更新设备的APK信息"""
        apk = self.db.update_device_apk(udid, package_name, apk_info)
        
        return ApkInfo(
            package_name=apk.package_name,
            app_name=apk.app_name,
            version=apk.version,
            version_code=apk.version_code,
            installed_time=apk.installed_time,
            is_system=apk.is_system,
            icon_path=apk.icon_path
        )
    
    def delete_apk(self, udid: str, package_name: str) -> bool:
        """从设备删除APK"""
        return self.db.remove_apk_from_device(udid, package_name)
    
    def get_devices_by_user(self, user_id: str) -> List[DeviceInfo]:
        """根据用户ID获取设备列表"""
        devices = self.db.get_devices_by_user(user_id)
        return [
            DeviceInfo(
                udid=device.udid,
                device_name=device.device_name,
                android_version=device.android_version,
                battery_level=device.battery_level,
                is_online=device.is_online,
                connection_type=device.connection_type,
                user_id=device.user.id if device.user else None
            ) for device in devices
        ]
    
    def assign_device_to_user(self, udid: str, user_id: str) -> bool:
        """将设备分配给用户"""
        return self.db.assign_device_to_user(udid, user_id)
    
    def unassign_device_from_user(self, udid: str) -> bool:
        """取消设备与用户的关联"""
        return self.db.unassign_device_from_user(udid)
    
    def get_all_devices(self) -> List[DeviceInfo]:
        """获取所有设备列表"""
        devices = self.db.get_all_devices()
        return [
            DeviceInfo(
                udid=device.udid,
                device_name=device.device_name,
                android_version=device.android_version,
                battery_level=device.battery_level,
                is_online=device.is_online,
                connection_type=device.connection_type,
                user_id=device.user.id if device.user else None
            ) for device in devices
        ]
    
    def delete_device(self, udid: str) -> bool:
        """删除设备"""
        return self.db.delete_device(udid)
    
    def update_device_status(self, udid: str, is_online: bool, battery_level: int) -> bool:
        """更新设备状态"""
        return self.db.update_device_status(udid, is_online, battery_level)
    
    def get_device_count(self) -> int:
        """获取设备总数"""
        return self.db.get_device_count()
    
    def get_online_device_count(self) -> int:
        """获取在线设备数量"""
        return self.db.get_online_device_count()

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
