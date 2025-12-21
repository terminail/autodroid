import time
from typing import List, Optional, Dict, Any
from peewee import DoesNotExist

from .database import DeviceDatabase
from .models import DeviceInfoResponse
from ..apk.models import ApkInfo

class DeviceManager:
    def __init__(self):
        """初始化设备管理器，使用统一的数据库接口"""
        self.max_concurrent_tasks = 5
        self.db = DeviceDatabase()
    
    def is_device_available(self, udid: str) -> bool:
        """检查设备是否可用于自动化"""
        return self.db.is_device_available(udid)
    
    def get_available_devices(self) -> List[DeviceInfoResponse]:
        """获取可用于自动化的设备列表"""
        devices = self.db.get_available_devices()
        return [
            DeviceInfoResponse(
                udid=device.udid,
                name=device.device_name,
                android_version=device.android_version,
                ip=device.ip_address if hasattr(device, 'ip_address') else None,
                registered_at=getattr(device, 'registered_at', None),
                status="online" if device.is_online else "offline"
            ) for device in devices
        ]
    
    def register_device(self, device_info: Dict[str, Any]) -> DeviceInfoResponse:
        """从应用报告注册设备"""
        device = self.db.register_device(device_info)
        
        # 返回DeviceInfoResponse对象
        return DeviceInfoResponse(
            udid=device.udid,
            name=device_info.get('name') or device.device_name,
            model=device_info.get('model'),
            manufacturer=device_info.get('manufacturer'),
            android_version=device_info.get('android_version') or device.android_version,
            api_level=device_info.get('api_level'),
            platform=device_info.get('platform'),
            brand=device_info.get('brand'),
            device=device_info.get('device'),
            product=device_info.get('product'),
            ip=device_info.get('ip'),
            screen_width=device_info.get('screen_width'),
            screen_height=device_info.get('screen_height'),
            registered_at=getattr(device, 'registered_at', None),
            status="online" if device.is_online else "offline"
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
    
    def get_devices_by_user(self, user_id: str) -> List[DeviceInfoResponse]:
        """根据用户ID获取设备列表"""
        devices = self.db.get_devices_by_user(user_id)
        return [
            DeviceInfoResponse(
                udid=device.udid,
                name=device.device_name,
                android_version=device.android_version,
                ip=device.ip_address if hasattr(device, 'ip_address') else None,
                registered_at=getattr(device, 'registered_at', None),
                status="online" if device.is_online else "offline"
            ) for device in devices
        ]
    
    def assign_device_to_user(self, udid: str, user_id: str) -> bool:
        """将设备分配给用户"""
        return self.db.assign_device_to_user(udid, user_id)
    
    def unassign_device_from_user(self, udid: str) -> bool:
        """取消设备与用户的关联"""
        return self.db.unassign_device_from_user(udid)
    
    def get_all_devices(self) -> List[DeviceInfoResponse]:
        """获取所有设备列表"""
        devices = self.db.get_all_devices()
        return [
            DeviceInfoResponse(
                udid=device.udid,
                name=device.device_name,
                android_version=device.android_version,
                ip=device.ip_address if hasattr(device, 'ip_address') else None,
                registered_at=getattr(device, 'registered_at', None),
                updated_at=getattr(device, 'updated_at', None),
                status="online" if device.is_online else "offline"
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
    
    def check_device_debug_permissions(self, udid: str) -> Dict[str, Any]:
        """检查设备调试权限状态"""
        import logging
        logger = logging.getLogger(__name__)
        
        logger.info(f"检查设备调试权限: {udid}")
        
        try:
            # 导入ADB设备类
            import sys
            import os
            sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', 'workscripts'))
            from adb_device import ADBDevice
            
            # 创建ADB设备实例
            adb_device = ADBDevice(udid)
            
            # 检查设备连接状态
            if not adb_device.is_connected():
                logger.warning(f"设备 {udid} 未连接或无法访问")
                return {
                    "success": False,
                    "message": f"设备 {udid} 未连接或无法访问",
                    "udid": udid,
                    "usb_debug_enabled": False,
                    "wifi_debug_enabled": False
                }
            
            # 检查USB调试状态
            usb_debug_enabled = adb_device.is_usb_debug_enabled()
            logger.info(f"设备 {udid} USB调试状态: {usb_debug_enabled}")
            
            # 检查WiFi调试状态
            wifi_debug_enabled = adb_device.is_wifi_debug_enabled()
            logger.info(f"设备 {udid} WiFi调试状态: {wifi_debug_enabled}")
            
            # 更新数据库中的调试权限状态
            self.db.update_device_debug_status(
                udid, 
                usb_debug_enabled, 
                wifi_debug_enabled,
                "SUCCESS",
                "调试权限检查完成"
            )
            
            logger.info(f"设备 {udid} 调试权限检查完成")
            
            return {
                "success": True,
                "message": "调试权限检查完成",
                "udid": udid,
                "usb_debug_enabled": usb_debug_enabled,
                "wifi_debug_enabled": wifi_debug_enabled
            }
            
        except Exception as e:
            logger.error(f"检查设备 {udid} 调试权限时出错: {str(e)}")
            # 更新数据库中的调试权限检查失败状态
            self.db.update_device_debug_check_failed(udid, str(e))
            
            return {
                "success": False,
                "message": f"检查调试权限时出错: {str(e)}",
                "udid": udid,
                "usb_debug_enabled": False,
                "wifi_debug_enabled": False
            }

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
