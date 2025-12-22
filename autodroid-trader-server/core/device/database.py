import time
from datetime import datetime
from typing import List, Optional, Dict, Any
from peewee import DoesNotExist

from ..database.base import BaseDatabase
from ..database.models import Device, Apk, DeviceApk, User


class DeviceDatabase(BaseDatabase):
    """设备数据库管理类（使用peewee ORM）"""
    
    def __init__(self):
        """初始化设备数据库"""
        super().__init__()
    
    def is_device_available(self, serialno: str) -> bool:
        """检查设备是否可用于自动化"""
        try:
            device = Device.get(Device.serialno == serialno)
            return device.is_online and device.battery_level > 20
        except DoesNotExist:
            return False
    
    def get_device(self, serialno: str) -> Optional[Device]:
        """根据序列号获取设备"""
        try:
            return Device.get(Device.serialno == serialno)
        except DoesNotExist:
            return None
    
    def get_all_devices(self) -> List[Device]:
        """获取所有设备"""
        return list(Device.select())
    
    def get_available_devices(self) -> List[Device]:
        """获取可用于自动化的设备列表"""
        return list(Device.select().where(
            (Device.is_online == True) & (Device.battery_level > 20)
        ))
    
    def register_device(self, device_info: Dict[str, Any]) -> Device:
        """从应用报告注册设备"""
        serialno = device_info.get('serialno')
        if not serialno:
            raise ValueError("设备序列号是必需的")
        
        # 获取或创建用户（如果提供了user_id）
        user_id = device_info.get('user_id')
        user = None
        if user_id:
            try:
                user = User.get(User.id == user_id)
            except DoesNotExist:
                # 如果用户不存在，保持为None
                pass
        
        # 创建或更新设备记录
        device, created = Device.get_or_create(
            serialno=serialno,
            defaults={
                'udid': device_info.get('udid'),
                'name': device_info.get('name', 'Unknown Device'),
                'model': device_info.get('model'),
                'manufacturer': device_info.get('manufacturer'),
                'android_version': device_info.get('android_version'),
                'api_level': device_info.get('api_level'),
                'platform': device_info.get('platform', 'Android'),
                'brand': device_info.get('brand'),
                'device': device_info.get('device'),
                'product': device_info.get('product'),
                'ip': device_info.get('ip'),
                'screen_width': device_info.get('screen_width'),
                'screen_height': device_info.get('screen_height'),
                'battery_level': device_info.get('battery_level', 50),
                'is_online': True,
                'connection_type': device_info.get('connection_type', 'network'),
                'user': user
            }
        )
        
        # 如果不是新创建的，更新设备信息
        if not created:
            device.name = device_info.get('name', device.name)
            device.model = device_info.get('model')
            device.manufacturer = device_info.get('manufacturer')
            device.android_version = device_info.get('android_version')
            device.api_level = device_info.get('api_level')
            device.platform = device_info.get('platform', device.platform)
            device.brand = device_info.get('brand')
            device.device = device_info.get('device')
            device.product = device_info.get('product')
            device.ip = device_info.get('ip')
            device.screen_width = device_info.get('screen_width')
            device.screen_height = device_info.get('screen_height')
            device.battery_level = device_info.get('battery_level', device.battery_level)
            device.is_online = True  # 注册时设置为在线
            device.connection_type = device_info.get('connection_type', device.connection_type)
            device.user = user
            device.updated_at = time.time()
            device.save()
        
        return device
    
    def update_device_status(self, serialno: str, is_online: bool, battery_level: int) -> bool:
        """更新设备状态"""
        try:
            device = Device.get(Device.serialno == serialno)
            device.is_online = is_online
            device.battery_level = battery_level
            device.last_seen = datetime.datetime.now()
            device.save()
            return True
        except DoesNotExist:
            return False
    
    def delete_device(self, serialno: str) -> bool:
        """删除设备"""
        try:
            device = Device.get(Device.serialno == serialno)
            # 删除关联的APK记录
            DeviceApk.delete().where(DeviceApk.device == device).execute()
            # 删除设备
            device.delete_instance()
            return True
        except DoesNotExist:
            return False
    
    def add_apk_to_device(self, serialno: str, apk_info: Dict[str, Any]) -> Apk:
        """将APK添加到设备"""
        try:
            device = Device.get(Device.serialno == serialno)
        except DoesNotExist:
            raise ValueError(f"未找到序列号为 {serialno} 的设备")
        
        package_name = apk_info.get('package_name')
        if not package_name:
            raise ValueError("包名是必需的")
        
        # 创建或更新APK记录
        apk, apk_created = Apk.get_or_create(
            package_name=package_name,
            defaults={
                'app_name': apk_info.get('app_name', ''),
                'version': apk_info.get('version', '1.0'),
                'version_code': apk_info.get('version_code', 1),
                'installed_time': apk_info.get('installed_time', int(time.time())),
                'is_system': apk_info.get('is_system', False),
                'icon_path': apk_info.get('icon_path', '')
            }
        )
        
        # 如果不是新创建的，更新APK信息
        if not apk_created:
            apk.app_name = apk_info.get('app_name', apk.app_name)
            apk.version = apk_info.get('version', apk.version)
            apk.version_code = apk_info.get('version_code', apk.version_code)
            apk.installed_time = apk_info.get('installed_time', apk.installed_time)
            apk.is_system = apk_info.get('is_system', apk.is_system)
            apk.icon_path = apk_info.get('icon_path', apk.icon_path)
            apk.updated_at = time.time()
            apk.save()
        
        # 创建设备-APK关联关系
        DeviceApk.get_or_create(
            device=device,
            apk=apk,
            defaults={'installed_time': apk_info.get('installed_time', int(time.time()))}
        )
        
        return apk
    
    def get_device_apks(self, serialno: str) -> List[Apk]:
        """获取设备的所有APK"""
        try:
            device = Device.get(Device.serialno == serialno)
        except DoesNotExist:
            raise ValueError(f"未找到序列号为 {serialno} 的设备")
        
        # 查询设备关联的所有APK
        return list(Apk
                    .select()
                    .join(DeviceApk)
                    .where(DeviceApk.device == device)
                    .order_by(Apk.app_name))
    
    def get_device_apk(self, serialno: str, package_name: str) -> Optional[Apk]:
        """获取设备的特定APK"""
        try:
            device = Device.get(Device.serialno == serialno)
            return (Apk
                    .select()
                    .join(DeviceApk)
                    .where((DeviceApk.device == device) & (Apk.package_name == package_name))
                    .get())
        except DoesNotExist:
            return None
    
    def update_device_apk(self, serialno: str, package_name: str, apk_info: Dict[str, Any]) -> Apk:
        """更新设备的APK信息"""
        try:
            device = Device.get(Device.serialno == serialno)
            apk = Apk.get(Apk.package_name == package_name)
            
            # 检查设备是否关联该APK
            device_apk = DeviceApk.get_or_none(
                (DeviceApk.device == device) & (DeviceApk.apk == apk)
            )
            if not device_apk:
                raise ValueError(f"设备 {serialno} 未安装APK {package_name}")
            
            # 更新APK信息
            if 'app_name' in apk_info:
                apk.app_name = apk_info['app_name']
            if 'version' in apk_info:
                apk.version = apk_info['version']
            if 'version_code' in apk_info:
                apk.version_code = apk_info['version_code']
            if 'installed_time' in apk_info:
                apk.installed_time = apk_info['installed_time']
            if 'is_system' in apk_info:
                apk.is_system = apk_info['is_system']
            if 'icon_path' in apk_info:
                apk.icon_path = apk_info['icon_path']
            
            apk.updated_at = time.time()
            apk.save()
            
            return apk
        except DoesNotExist:
            raise ValueError(f"未找到设备 {serialno} 或APK {package_name}")
    
    def remove_apk_from_device(self, serialno: str, package_name: str) -> bool:
        """从设备移除APK"""
        try:
            device = Device.get(Device.serialno == serialno)
            apk = Apk.get(Apk.package_name == package_name)
            
            # 删除关联关系
            deleted_count = DeviceApk.delete().where(
                (DeviceApk.device == device) & (DeviceApk.apk == apk)
            ).execute()
            
            return deleted_count > 0
        except DoesNotExist:
            return False
    
    def search_devices(self, query: str) -> List[Device]:
        """搜索设备"""
        search_pattern = f"%{query}%"
        return list(Device.select().where(
            (Device.serialno.like(search_pattern)) | 
            (Device.name.like(search_pattern))
        ))
    
    def get_devices_by_user(self, user_id: str) -> List[Device]:
        """获取用户的所有设备"""
        try:
            user = User.get(User.id == user_id)
            return list(Device.select().where(Device.user == user))
        except DoesNotExist:
            return []
    
    def assign_device_to_user(self, serialno: str, user_id: str) -> bool:
        """将设备分配给用户"""
        try:
            device = Device.get(Device.serialno == serialno)
            user = User.get(User.id == user_id)
            device.user = user
            device.updated_at = time.time()
            device.save()
            return True
        except DoesNotExist:
            return False
    
    def unassign_device_from_user(self, serialno: str) -> bool:
        """取消设备与用户的关联"""
        try:
            device = Device.get(Device.serialno == serialno)
            device.user = None
            device.updated_at = time.time()
            device.save()
            return True
        except DoesNotExist:
            return False
    
    def get_device_count(self) -> int:
        """获取设备总数"""
        return Device.select().count()
    
    def get_online_device_count(self) -> int:
        """获取在线设备数量"""
        return Device.select().where(Device.is_online == True).count()
    
    def update_device_debug_status(self, serialno: str, usb_debug_enabled: bool, wifi_debug_enabled: bool, 
                                  check_status: str = "SUCCESS", check_message: str = None) -> bool:
        """更新设备调试权限状态"""
        try:
            device = Device.get(Device.serialno == serialno)
            device.usb_debug_enabled = usb_debug_enabled
            device.wifi_debug_enabled = wifi_debug_enabled
            device.check_status = check_status
            device.check_message = check_message
            device.check_time = time.time()
            device.updated_at = time.time()
            device.save()
            return True
        except DoesNotExist:
            return False
    
    def update_device_check_failed(self, serialno: str, error_message: str) -> bool:
        """更新设备检查失败状态"""
        try:
            device = Device.get(Device.serialno == serialno)
            device.check_status = "FAILED"
            device.check_message = error_message
            device.check_time = time.time()
            device.updated_at = time.time()
            device.save()
            return True
        except DoesNotExist:
            return False
    
    def update_device_apps(self, serialno: str, installed_apps: list) -> bool:
        """更新设备已安装的应用列表"""
        try:
            device = Device.get(Device.serialno == serialno)
            # 删除现有的应用记录
            DeviceApk.delete().where(DeviceApk.device == device).execute()
            
            # 添加新的应用记录
            for app in installed_apps:
                package_name = app.get('package_name', '')
                if not package_name:
                    continue
                    
                # 获取或创建APK记录
                try:
                    apk = Apk.get(Apk.package_name == package_name)
                except DoesNotExist:
                    # 如果APK不存在，创建一个新的记录
                    apk = Apk.create(
                        id=package_name,  # 使用包名作为ID
                        package_name=package_name,
                        app_name=app.get('app_name', ''),
                        name=app.get('app_name', ''),
                        version=app.get('version', ''),
                        version_code=app.get('version_code', 0),
                        installed_time=app.get('installed_time', 0),
                        is_system=app.get('is_system', False),
                        icon_path=app.get('icon_path', '')
                    )
                
                # 创建设备-APK关联
                DeviceApk.create(
                    device=device,
                    apk=apk,
                    installed_time=datetime.now() if app.get('installed_time') is None else app.get('installed_time')
                )
            
            return True
        except DoesNotExist:
            return False