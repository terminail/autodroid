"""
设备模块数据库服务类
"""

from datetime import datetime, timedelta
from typing import List, Optional, Dict, Any
from peewee import DoesNotExist

from core.database.base import BaseDatabase
from core.database.models import Device
from core.device.models import DeviceInfo


class DeviceDatabase(BaseDatabase):
    """设备模块数据库服务类"""
    
    def __init__(self):
        """初始化设备数据库服务"""
        super().__init__()
    
    def _device_to_info(self, device: Device) -> DeviceInfo:
        """将Device模型转换为DeviceInfo"""
        return DeviceInfo(
            id=str(device.id),
            device_name=device.device_name or '',
            device_model=device.device_model or '',
            android_version=device.android_version or '',
            api_level=device.api_level or 0,
            is_connected=device.is_connected or False,
            connection_type=device.connection_type or 'USB',
            battery_level=device.battery_level or 0,
            battery_status=device.battery_status or 'Unknown',
            is_charging=device.is_charging or False,
            created_at=device.created_at,
            last_updated=device.last_updated,
            last_connected=device.last_connected
        )
    
    def register_device(self, device_data: Dict[str, Any]) -> Optional[DeviceInfo]:
        """注册或更新设备信息 - 返回DeviceInfo对象"""
        try:
            with Device._meta.database.atomic():
                device, created = Device.get_or_create(
                    id=device_data['device_id'],
                    defaults={
                        'device_name': device_data.get('device_name', ''),
                        'device_model': device_data.get('device_model', ''),
                        'android_version': device_data.get('android_version', ''),
                        'api_level': device_data.get('api_level', 0),
                        'is_connected': device_data.get('is_connected', False),
                        'connection_type': device_data.get('connection_type', 'USB'),
                        'battery_level': device_data.get('battery_level', 0),
                        'battery_status': device_data.get('battery_status', 'Unknown'),
                        'is_charging': device_data.get('is_charging', False),
                        'last_connected': device_data.get('last_connected', datetime.now()),
                        'last_updated': datetime.now()
                    }
                )
                
                if not created:
                    # 更新现有记录
                    update_fields = {}
                    valid_fields = {"device_name", "device_model", "android_version", "api_level",
                                  "is_connected", "connection_type", "battery_level", 
                                  "battery_status", "is_charging", "last_connected", "last_updated"}
                    
                    for field in valid_fields:
                        if field in device_data and device_data[field] is not None:
                            update_fields[field] = device_data[field]
                    
                    # 确保更新last_updated字段
                    if 'last_updated' not in update_fields:
                        update_fields['last_updated'] = datetime.now()
                    
                    if update_fields:
                        Device.update(**update_fields).where(
                            Device.id == device_data['device_id']
                        ).execute()
                        device = Device.get(Device.id == device_data['device_id'])
                
                return self._device_to_info(device)
                
        except Exception as e:
            print(f"注册设备失败: {str(e)}")
            return None
    
    def create_device(self, device_data: Dict[str, Any]) -> Optional[DeviceInfo]:
        """创建设备记录 - 返回DeviceInfo对象"""
        try:
            device = Device.create(
                id=device_data['device_id'],
                device_name=device_data.get('device_name', ''),
                device_model=device_data.get('device_model', ''),
                android_version=device_data.get('android_version', ''),
                api_level=device_data.get('api_level', 0),
                is_connected=device_data.get('is_connected', False),
                connection_type=device_data.get('connection_type', 'USB'),
                battery_level=device_data.get('battery_level', 0),
                battery_status=device_data.get('battery_status', 'Unknown'),
                is_charging=device_data.get('is_charging', False),
                created_at=device_data.get('created_at', datetime.now()),
                last_updated=datetime.now(),
                last_connected=device_data.get('last_connected', datetime.now())
            )
            return self._device_to_info(device)
        except Exception as e:
            print(f"创建设备失败: {str(e)}")
            return None
    
    def get_device(self, device_id: str) -> Optional[DeviceInfo]:
        """根据设备ID获取设备信息 - 返回DeviceInfo对象"""
        try:
            # 尝试按字符串device_id查找
            device = Device.get(Device.id == device_id)
            return self._device_to_info(device)
        except Device.DoesNotExist:
            try:
                # 尝试按数字id查找
                device = Device.get(Device.id == str(device_id))
                return self._device_to_info(device)
            except (Device.DoesNotExist, ValueError):
                return None

    def get_all_devices(self, order_by: str = "device_name", order_desc: bool = False) -> List[DeviceInfo]:
        """获取所有设备信息 - 返回DeviceInfo对象列表
        
        Args:
            order_by: 排序字段 (device_name, created_at)
            order_desc: 是否降序排列
        """
        try:
            # 构建排序字段 - 注意：last_connected字段不存在，使用created_at代替
            order_field = None
            if order_by == "device_name":
                order_field = Device.device_name
            elif order_by == "last_connected":
                # last_connected字段不存在，使用created_at代替
                order_field = Device.created_at
            elif order_by == "created_at":
                order_field = Device.created_at
            else:
                order_field = Device.device_name
            
            # 应用排序方向
            if order_desc:
                order_field = order_field.desc()
            
            devices = list(Device.select().order_by(order_field))
            return [self._device_to_info(device) for device in devices]
        except Exception as e:
            print(f"获取设备列表失败: {str(e)}")
            return []
    
    def update_device(self, device_id: str, update_data: Dict[str, Any]) -> bool:
        """更新设备信息"""
        try:
            valid_fields = {"device_name", "device_model", "android_version", "api_level", 
                          "is_connected", "connection_type", "battery_level", "battery_status", 
                          "is_charging", "last_connected", "last_updated"}
            update_fields = {k: v for k, v in update_data.items() if k in valid_fields and v is not None}
            
            if not update_fields:
                return False
            
            # 确保更新last_updated字段
            if 'last_updated' not in update_fields:
                update_fields['last_updated'] = datetime.now()
            
            query = Device.update(**update_fields).where(Device.id == device_id)
            return query.execute() > 0
            
        except Exception as e:
            print(f"更新设备失败: {str(e)}")
            return False

    def delete_device(self, device_id: str) -> bool:
        """删除设备记录"""
        try:
            deleted_count = Device.delete().where(Device.id == device_id).execute()
            return deleted_count > 0
        except Exception as e:
            print(f"删除设备失败: {str(e)}")
            return False

    def get_connected_devices(self) -> List[DeviceInfo]:
        """获取已连接的设备 - 返回DeviceInfo对象列表"""
        try:
            devices = list(Device.select().where(Device.is_connected == True).order_by(Device.device_name))
            return [self._device_to_info(device) for device in devices]
        except Exception as e:
            print(f"获取已连接设备失败: {str(e)}")
            return []
    
    def set_device_connection_status(self, device_id: str, is_connected: bool) -> bool:
        """设置设备连接状态"""
        try:
            from datetime import datetime
            update_data = {
                'is_connected': is_connected
            }
            return self.update_device(device_id, update_data)
        except Exception as e:
            print(f"设置设备连接状态失败: {str(e)}")
            return False
    
    def search_devices(self, **kwargs) -> List[DeviceInfo]:
        """搜索设备 - 返回DeviceInfo对象列表"""
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
            
            devices = list(query.order_by(Device.device_name))
            return [self._device_to_info(device) for device in devices]
        except Exception as e:
            print(f"搜索设备失败: {str(e)}")
            return []

    def get_device_count(self) -> int:
        """获取设备总数"""
        return Device.select().count()

    def get_recently_connected_devices(self, days: int = 7) -> List[DeviceInfo]:
        """获取最近连接的设备 - 返回DeviceInfo对象列表"""
        try:
            cutoff_date = datetime.now() - timedelta(days=days)
            devices = list(Device
                .select()
                .where(Device.created_at >= cutoff_date)
                .order_by(Device.created_at.desc()))
            return [self._device_to_info(device) for device in devices]
        except Exception as e:
            print(f"获取最近连接设备失败: {str(e)}")
            return []