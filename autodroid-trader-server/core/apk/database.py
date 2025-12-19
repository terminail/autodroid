import time
from typing import List, Optional, Dict, Any
from peewee import DoesNotExist

from ..database.base import BaseDatabase
from ..database.models import Apk, Device, DeviceApk


class ApkDatabase(BaseDatabase):
    """APK数据库管理类（使用peewee ORM）"""
    
    def __init__(self):
        """初始化APK数据库"""
        super().__init__()
    
    def register_apk_to_device(self, apk_data: Dict[str, Any], device_udid: str) -> Optional[Apk]:
        """将APK注册到设备（多对多关系）"""
        try:
            with Apk._meta.database.atomic():
                # 1. 检查APK是否已存在，不存在则插入
                apk, created = Apk.get_or_create(
                    package_name=apk_data['package_name'],
                    defaults={
                        'app_name': apk_data['app_name'],
                        'version': apk_data.get('version'),
                        'version_code': apk_data.get('version_code'),
                        'installed_time': apk_data.get('installed_time'),
                        'is_system': apk_data.get('is_system', False),
                        'icon_path': apk_data.get('icon_path')
                    }
                )
                
                # 2. 创建设备-APK关联关系
                DeviceApk.get_or_create(
                    device_udid=device_udid,
                    package_name=apk.package_name,
                    defaults={'installed_time': apk_data.get('installed_time')}
                )
                
                return apk
                
        except Exception:
            return None
    
    def get_apk(self, package_name: str) -> Optional[Apk]:
        """获取特定APK信息"""
        try:
            return Apk.get(Apk.package_name == package_name)
        except DoesNotExist:
            return None
    
    def get_all_apks(self) -> List[Apk]:
        """获取所有APK信息"""
        try:
            return list(Apk.select().order_by(Apk.app_name))
        except Exception:
            return []
    
    def get_device_apks(self, device_udid: str) -> List[Apk]:
        """获取特定设备的所有APK信息"""
        try:
            return list(
                Apk.select()
                .join(DeviceApk)
                .where(DeviceApk.device_udid == device_udid)
                .order_by(Apk.app_name)
            )
        except Exception:
            return []
    
    def update_apk(self, package_name: str, update_data: Dict[str, Any]) -> bool:
        """更新APK信息"""
        try:
            # 过滤有效字段
            valid_fields = {"app_name", "version", "version_code", "installed_time", "is_system", "icon_path"}
            update_fields = {k: v for k, v in update_data.items() if k in valid_fields and v is not None}
            
            if not update_fields:
                return False
            
            # 执行更新
            query = Apk.update(**update_fields, updated_at=time.time()).where(Apk.package_name == package_name)
            return query.execute() > 0
            
        except Exception:
            return False
    
    def delete_apk(self, package_name: str) -> bool:
        """删除APK记录"""
        try:
            with Apk._meta.database.atomic():
                # 先删除关联的设备记录
                DeviceApk.delete().where(DeviceApk.package_name == package_name).execute()
                
                # 再删除APK记录
                deleted_count = Apk.delete().where(Apk.package_name == package_name).execute()
                
                return deleted_count > 0
                
        except Exception:
            return False
    
    def search_apks(self, query: str) -> List[Apk]:
        """搜索APK"""
        try:
            search_pattern = f"%{query}%"
            return list(
                Apk.select()
                .where((Apk.package_name ** search_pattern) | (Apk.app_name ** search_pattern))
                .order_by(Apk.app_name)
            )
        except Exception:
            return []
    
    def associate_apk_with_device(self, package_name: str, device_udid: str) -> bool:
        """将APK与设备关联"""
        try:
            DeviceApk.get_or_create(
                device_udid=device_udid,
                package_name=package_name
            )
            return True
        except Exception:
            return False
    
    def dissociate_apk_from_device(self, package_name: str, device_udid: str) -> bool:
        """取消APK与设备的关联"""
        try:
            deleted_count = DeviceApk.delete().where(
                (DeviceApk.package_name == package_name) & 
                (DeviceApk.device_udid == device_udid)
            ).execute()
            return deleted_count > 0
        except Exception:
            return False
    
    def get_devices_for_apk(self, package_name: str) -> List[str]:
        """获取安装了特定APK的设备列表"""
        try:
            device_apks = DeviceApk.select(DeviceApk.device_udid).where(
                DeviceApk.package_name == package_name
            )
            return [da.device_udid for da in device_apks]
        except Exception:
            return []
    
    def get_apks_for_device(self, device_udid: str) -> List[Apk]:
        """获取设备上安装的所有APK"""
        try:
            return list(
                Apk.select()
                .join(DeviceApk)
                .where(DeviceApk.device_udid == device_udid)
                .order_by(Apk.app_name)
            )
        except Exception:
            return []
    
    def get_apk_count(self) -> int:
        """获取APK总数"""
        try:
            return Apk.select().count()
        except Exception:
            return 0
    
    def get_device_apk_count(self, device_udid: str) -> int:
        """获取设备的APK数量"""
        try:
            return (
                DeviceApk.select()
                .where(DeviceApk.device_udid == device_udid)
                .count()
            )
        except Exception:
            return 0