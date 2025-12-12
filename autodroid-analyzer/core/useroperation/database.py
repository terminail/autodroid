"""
用户操作模块数据库服务类
"""

import json
from typing import List, Optional, Dict, Any
from peewee import DoesNotExist

from core.database.base import BaseDatabase
from core.database.models import UserOperation, Apk, Screenshot


class UserOperationDatabase(BaseDatabase):
    """用户操作模块数据库服务类"""
    
    def __init__(self):
        """初始化用户操作数据库服务"""
        super().__init__()
    
    def save_operation(self, operation_data: Dict[str, Any]) -> Optional[UserOperation]:
        """保存用户操作记录"""
        try:
            with UserOperation._meta.database.atomic():
                operation = UserOperation.create(
                    apk=operation_data['apk_id'],
                    timestamp=operation_data['timestamp'],
                    action_type=operation_data['action_type'],
                    target_element=json.dumps(operation_data.get('target_element', {}), ensure_ascii=False),
                    input_text=operation_data.get('input_text'),
                    coordinates=json.dumps(operation_data.get('coordinates', {}), ensure_ascii=False),
                    screenshot=operation_data.get('screenshot_id')
                )
                
                # 更新APK的操作计数
                apk = Apk.get(Apk.id == operation_data['apk_id'])
                Apk.update(total_operations=apk.total_operations + 1).where(
                    Apk.id == operation_data['apk_id']
                ).execute()
                
                return operation
                
        except Exception as e:
            print(f"保存用户操作失败: {str(e)}")
            return None
    
    def get_operation(self, operation_id: int) -> Optional[UserOperation]:
        """获取特定用户操作记录"""
        try:
            return UserOperation.get(UserOperation.id == operation_id)
        except DoesNotExist:
            return None
    
    def get_operations_by_apk(self, apk_id: str, limit: int = 100) -> List[UserOperation]:
        """获取特定APK的用户操作记录"""
        try:
            return list(UserOperation
                       .select()
                       .where(UserOperation.apk == apk_id)
                       .order_by(UserOperation.timestamp.desc())
                       .limit(limit))
        except Exception as e:
            print(f"获取用户操作记录失败: {str(e)}")
            return []
    
    def get_recent_operations(self, limit: int = 50) -> List[UserOperation]:
        """获取最近的操作记录"""
        try:
            return list(UserOperation
                       .select()
                       .order_by(UserOperation.timestamp.desc())
                       .limit(limit))
        except Exception as e:
            print(f"获取最近操作记录失败: {str(e)}")
            return []
    
    def update_operation(self, operation_id: int, update_data: Dict[str, Any]) -> bool:
        """更新用户操作记录"""
        try:
            valid_fields = {"action_type", "target_element", "input_text", "coordinates", "screenshot"}
            update_fields = {}
            
            for field in valid_fields:
                if field in update_data and update_data[field] is not None:
                    if field in ['target_element', 'coordinates']:
                        update_fields[field] = json.dumps(update_data[field], ensure_ascii=False)
                    else:
                        update_fields[field] = update_data[field]
            
            if not update_fields:
                return False
            
            query = UserOperation.update(**update_fields).where(UserOperation.id == operation_id)
            return query.execute() > 0
            
        except Exception as e:
            print(f"更新用户操作失败: {str(e)}")
            return False
    
    def delete_operation(self, operation_id: int) -> bool:
        """删除用户操作记录"""
        try:
            # 先获取操作信息以更新APK计数
            operation = self.get_operation(operation_id)
            if operation:
                apk_id = operation.apk.id
                
                # 删除操作记录
                deleted_count = UserOperation.delete().where(UserOperation.id == operation_id).execute()
                
                if deleted_count > 0:
                    # 更新APK的操作计数
                    apk = Apk.get(Apk.id == apk_id)
                    new_count = max(0, apk.total_operations - 1)
                    Apk.update(total_operations=new_count).where(Apk.id == apk_id).execute()
                    return True
            
            return False
        except Exception as e:
            print(f"删除用户操作失败: {str(e)}")
            return False
    
    def associate_operation_with_screenshot(self, operation_id: int, screenshot_id: str) -> bool:
        """关联操作记录到截屏"""
        try:
            update_data = {'screenshot': screenshot_id}
            return self.update_operation(operation_id, update_data)
        except Exception as e:
            print(f"关联操作到截屏失败: {str(e)}")
            return False
    
    def get_operations_by_screenshot(self, screenshot_id: str) -> List[UserOperation]:
        """获取特定截屏关联的操作记录"""
        try:
            return list(UserOperation
                       .select()
                       .where(UserOperation.screenshot == screenshot_id)
                       .order_by(UserOperation.timestamp))
        except Exception as e:
            print(f"获取截屏关联操作失败: {str(e)}")
            return []
    
    def get_operations_by_action_type(self, apk_id: str, action_type: str) -> List[UserOperation]:
        """按操作类型获取记录"""
        try:
            return list(UserOperation
                       .select()
                       .where((UserOperation.apk == apk_id) & 
                              (UserOperation.action_type == action_type))
                       .order_by(UserOperation.timestamp.desc()))
        except Exception as e:
            print(f"按操作类型获取记录失败: {str(e)}")
            return []
    
    def get_operation_statistics(self, apk_id: str) -> Dict[str, Any]:
        """获取操作统计信息"""
        try:
            total_operations = UserOperation.select().where(UserOperation.apk == apk_id).count()
            
            # 按操作类型统计
            type_counts = {}
            for operation in UserOperation.select(UserOperation.action_type).where(UserOperation.apk == apk_id):
                action_type = operation.action_type
                type_counts[action_type] = type_counts.get(action_type, 0) + 1
            
            # 获取操作时间范围
            first_operation = (UserOperation
                             .select()
                             .where(UserOperation.apk == apk_id)
                             .order_by(UserOperation.timestamp.asc())
                             .first())
            
            last_operation = (UserOperation
                            .select()
                            .where(UserOperation.apk == apk_id)
                            .order_by(UserOperation.timestamp.desc())
                            .first())
            
            time_range = {
                'first_operation': first_operation.timestamp if first_operation else None,
                'last_operation': last_operation.timestamp if last_operation else None
            }
            
            return {
                'total_operations': total_operations,
                'action_type_counts': type_counts,
                'time_range': time_range
            }
        except Exception as e:
            print(f"获取操作统计失败: {str(e)}")
            return {'total_operations': 0, 'action_type_counts': {}, 'time_range': {}}
    
    def search_operations(self, **kwargs) -> List[UserOperation]:
        """搜索操作记录"""
        try:
            query = UserOperation.select()
            
            if kwargs.get('apk_id'):
                query = query.where(UserOperation.apk == kwargs['apk_id'])
            
            if kwargs.get('action_type'):
                query = query.where(UserOperation.action_type == kwargs['action_type'])
            
            if kwargs.get('has_screenshot') is not None:
                if kwargs['has_screenshot']:
                    query = query.where(UserOperation.screenshot.is_null(False))
                else:
                    query = query.where(UserOperation.screenshot.is_null())
            
            if kwargs.get('has_input_text') is not None:
                if kwargs['has_input_text']:
                    query = query.where(UserOperation.input_text.is_null(False))
                else:
                    query = query.where(UserOperation.input_text.is_null())
            
            # 时间范围查询
            if kwargs.get('start_timestamp'):
                query = query.where(UserOperation.timestamp >= kwargs['start_timestamp'])
            
            if kwargs.get('end_timestamp'):
                query = query.where(UserOperation.timestamp <= kwargs['end_timestamp'])
            
            # 应用分页
            limit = kwargs.get('limit', 100)
            offset = kwargs.get('offset', 0)
            query = query.limit(limit).offset(offset)
            
            return list(query.order_by(UserOperation.timestamp.desc()))
            
        except Exception as e:
            print(f"搜索操作记录失败: {str(e)}")
            return []
    
    def get_operation_count_by_apk(self, apk_id: str) -> int:
        """获取特定APK的操作数量"""
        try:
            return UserOperation.select().where(UserOperation.apk == apk_id).count()
        except Exception as e:
            print(f"获取操作数量失败: {str(e)}")
            return 0
    
    def get_operation_count(self) -> int:
        """获取操作总数"""
        try:
            return UserOperation.select().count()
        except Exception as e:
            print(f"获取操作总数失败: {str(e)}")
            return 0
    
    def get_operations_without_screenshot(self, apk_id: str) -> List[UserOperation]:
        """获取未关联截屏的操作记录"""
        try:
            return list(UserOperation
                       .select()
                       .where((UserOperation.apk == apk_id) & 
                              (UserOperation.screenshot.is_null()))
                       .order_by(UserOperation.timestamp.desc()))
        except Exception as e:
            print(f"获取未关联截屏操作失败: {str(e)}")
            return []