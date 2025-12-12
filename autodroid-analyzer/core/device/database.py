"""
设备模块数据库服务类
"""

from datetime import datetime, timedelta
from typing import List, Optional, Dict, Any
from peewee import DoesNotExist

from core.database.base import BaseDatabase
from core.database.models import Device


class DeviceDatabase(BaseDatabase):
    """设备模块数据库服务类"""
    
    def __init__(self):
        """初始化设备数据库服务"""
        super().__init__()
    
    def register_device(self, device_data: Dict[str, Any]) -> Optional[Device]:
        """注册或更新设备信息"""
        try:
            with Device._meta.database.atomic():
                device, created = Device.get_or_create(
                    device_id=device_data['device_id'],
                    defaults={
                        'device_name': device_data.get('device_name', ''),
                        'device_model': device_data.get('device_model', ''),
                        'android_version': device_data.get('android_version', ''),
                        'api_level': device_data.get('api_level', 0),
                        'screen_width': device_data.get('screen_width', 0),
                        'screen_height': device_data.get('screen_height', 0),
                        'density': device_data.get('density', 0),
                        'is_connected': device_data.get('is_connected', False),
                        'last_connected': device_data.get('last_connected')
                    }
                )
                
                if not created:
                    # 更新现有记录
                    update_fields = {}
                    for field in ['device_name', 'device_model', 'android_version', 'api_level',
                                 'screen_width', 'screen_height', 'density', 'is_connected', 'last_connected']:
                        if field in device_data and device_data[field] is not None:
                            update_fields[field] = device_data[field]
                    
                    if update_fields:
                        Device.update(**update_fields).where(
                            Device.device_id == device_data['device_id']
                        ).execute()
                        device = Device.get(Device.device_id == device_data['device_id'])
                
                return device
                
        except Exception as e:
            print(f"注册设备失败: {str(e)}")
            return None
    
    def get_device(self, device_id: str) -> Optional[Device]:
        """根据设备ID获取设备信息"""
        try:
            return Device.get(Device.device_id == device_id)
        except Device.DoesNotExist:
            return None

    def get_all_devices(self) -> List[Device]:
        """获取所有设备信息"""
        try:
            return list(Device.select().order_by(Device.device_name))
        except Exception as e:
            print(f"获取设备列表失败: {str(e)}")
            return []
    
    def update_device(self, device_id: str, update_data: Dict[str, Any]) -> bool:
        """更新设备信息"""
        try:
            valid_fields = {"device_name", "device_model", "android_version", "api_level",
                          "screen_width", "screen_height", "density", "is_connected", "last_connected"}
            update_fields = {k: v for k, v in update_data.items() if k in valid_fields and v is not None}
            
            if not update_fields:
                return False
            
            query = Device.update(**update_fields).where(Device.device_id == device_id)
            return query.execute() > 0
            
        except Exception as e:
            print(f"更新设备失败: {str(e)}")
            return False

    def delete_device(self, device_id: str) -> bool:
        """删除设备记录"""
        try:
            deleted_count = Device.delete().where(Device.device_id == device_id).execute()
            return deleted_count > 0
        except Exception as e:
            print(f"删除设备失败: {str(e)}")
            return False

    def get_connected_devices(self) -> List[Device]:
        """获取已连接的设备"""
        try:
            return list(Device.select().where(Device.is_connected == True).order_by(Device.device_name))
        except Exception as e:
            print(f"获取已连接设备失败: {str(e)}")
            return []
    
    def set_device_connection_status(self, device_id: str, is_connected: bool) -> bool:
        """设置设备连接状态"""
        try:
            from datetime import datetime
            update_data = {
                'is_connected': is_connected,
                'last_connected': datetime.now() if is_connected else None
            }
            return self.update_device(device_id, update_data)
        except Exception as e:
            print(f"设置设备连接状态失败: {str(e)}")
            return False
    
    def search_devices(self, **kwargs) -> List[Device]:
        """搜索设备"""
        try:
            query = Device.select()
            
            if 'device_name' in kwargs and kwargs['device_name']:
                query = query.where(Device.device_name.contains(kwargs['device_name']))
            
            if 'device_model' in kwargs and kwargs['device_model']:
                query = query.where(Device.device_model.contains(kwargs['device_model']))
            
            if 'android_version' in kwargs and kwargs['android_version']:
                query = query.where(Device.android_version.contains(kwargs['android_version']))
            
            if 'is_connected' in kwargs:
                query = query.where(Device.is_connected == kwargs['is_connected'])
            
            if 'min_api_level' in kwargs:
                query = query.where(Device.api_level >= kwargs['min_api_level'])
            
            if 'max_api_level' in kwargs:
                query = query.where(Device.api_level <= kwargs['max_api_level'])
            
            return list(query.order_by(Device.device_name))
        except Exception as e:
            print(f"搜索设备失败: {str(e)}")
            return []

    def get_device_count(self) -> int:
        """获取设备总数"""
        return Device.select().count()

    def get_recently_connected_devices(self, days: int = 7) -> List[Device]:
        """获取最近连接的设备"""
        try:
            cutoff_date = datetime.now() - timedelta(days=days)
            return list(Device
                .select()
                .where(Device.last_connected >= cutoff_date)
                .order_by(Device.last_connected.desc()))
        except Exception as e:
            print(f"获取最近连接设备失败: {str(e)}")
            return []