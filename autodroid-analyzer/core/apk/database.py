"""
APK数据库管理类（使用peewee ORM）
"""

import time
from typing import List, Optional, Dict, Any
from peewee import DoesNotExist

from core.database.base import BaseDatabase
from core.database.models import Apk, Device, AnalysisResult


class ApkDatabase(BaseDatabase):
    """APK数据库管理类"""
    
    def __init__(self):
        """初始化APK数据库"""
        super().__init__()
    
    def register_apk(self, apk_data: Dict[str, Any]) -> Optional[Apk]:
        """注册APK信息"""
        try:
            with Apk._meta.database.atomic():
                # 使用id字段作为主键，兼容package_name字段
                package_name = apk_data.get('id') or apk_data.get('package_name')
                if not package_name:
                    raise ValueError("必须提供id或package_name字段")
                
                apk, created = Apk.get_or_create(
                    id=package_name,  # 使用id字段作为主键
                    defaults={
                        'app_name': apk_data['app_name'],
                        'version_name': apk_data.get('version_name') or apk_data.get('version'),  # 兼容新旧字段名
                        'version_code': apk_data.get('version_code'),
                        'install_time': apk_data.get('install_time') or apk_data.get('installed_time'),  # 兼容新旧字段名
                        'is_packed': apk_data.get('is_packed', False),
                        'packer_type': apk_data.get('packer_type'),
                        'packer_confidence': apk_data.get('packer_confidence', 0.0)
                    }
                )
                
                if not created:
                    # 更新现有记录
                    update_fields = {}
                    for field in ['app_name', 'version_name', 'version_code', 'install_time', 
                                 'is_packed', 'packer_type', 'packer_confidence']:
                        if field in apk_data and apk_data[field] is not None:
                            update_fields[field] = apk_data[field]
                    
                    if update_fields:
                        Apk.update(**update_fields).where(
                            Apk.id == package_name
                        ).execute()
                        apk = Apk.get(Apk.id == package_name)
                
                return apk
                
        except Exception as e:
            print(f"注册APK失败: {str(e)}")
            return None
    
    def get_apk(self, package_name: str) -> Optional[Apk]:
        """获取特定APK信息"""
        try:
            return Apk.get(Apk.id == package_name)  # 使用id字段
        except DoesNotExist:
            return None
    
    def get_all_apks(self) -> List[Apk]:
        """获取所有APK信息"""
        try:
            return list(Apk.select().order_by(Apk.app_name))
        except Exception as e:
            print(f"获取APK列表失败: {str(e)}")
            return []
    
    def update_apk(self, package_name: str, update_data: Dict[str, Any]) -> bool:
        """更新APK信息"""
        try:
            # 过滤有效字段
            valid_fields = {"app_name", "version_name", "version_code", "install_time", 
                           "is_packed", "packer_type", "packer_confidence"}
            update_fields = {k: v for k, v in update_data.items() if k in valid_fields and v is not None}
            
            if not update_fields:
                return False
            
            # 执行更新
            query = Apk.update(**update_fields).where(Apk.id == package_name)
            return query.execute() > 0
            
        except Exception as e:
            print(f"更新APK失败: {str(e)}")
            return False
    
    def delete_apk(self, package_name: str) -> bool:
        """删除APK记录"""
        try:
            deleted_count = Apk.delete().where(Apk.id == package_name).execute()
            return deleted_count > 0
        except Exception as e:
            print(f"删除APK失败: {str(e)}")
            return False
    
    def search_apks(self, **kwargs) -> List[Apk]:
        """搜索APK"""
        try:
            query = Apk.select()
            
            # 构建查询条件
            if kwargs.get('id') or kwargs.get('package_name'):
                package_name = kwargs.get('id') or kwargs.get('package_name')
                query = query.where(Apk.id.contains(package_name))  # 使用id字段
            
            if kwargs.get('app_name'):
                query = query.where(Apk.app_name.contains(kwargs['app_name']))
            
            if kwargs.get('version_name') or kwargs.get('version'):
                version = kwargs.get('version_name') or kwargs.get('version')
                query = query.where(Apk.version_name.contains(version))  # 使用version_name字段
            
            if kwargs.get('is_packed') is not None:
                query = query.where(Apk.is_packed == kwargs['is_packed'])
            
            if kwargs.get('packer_type'):
                query = query.where(Apk.packer_type.contains(kwargs['packer_type']))
            
            # 应用分页
            limit = kwargs.get('limit', 100)
            offset = kwargs.get('offset', 0)
            query = query.limit(limit).offset(offset)
            
            return list(query.order_by(Apk.app_name))
            
        except Exception as e:
            print(f"搜索APK失败: {str(e)}")
            return []
    
    def get_apk_count(self) -> int:
        """获取APK总数"""
        try:
            return Apk.select().count()
        except Exception as e:
            print(f"获取APK数量失败: {str(e)}")
            return 0
    
    def save_packer_detection_result(self, package_name: str, detection_result: Dict[str, Any]) -> bool:
        """保存加固检测结果到数据库"""
        try:
            update_data = {
                'is_packed': detection_result.get('is_packed', False),
                'packer_type': detection_result.get('packer_type'),
                'packer_confidence': detection_result.get('confidence', 0.0)
            }
            
            return self.update_apk(package_name, update_data)
            
        except Exception as e:
            print(f"保存加固检测结果失败: {str(e)}")
            return False
    
    def get_packed_apks(self) -> List[Apk]:
        """获取所有被加固的APK"""
        try:
            return list(Apk.select().where(Apk.is_packed == True).order_by(Apk.app_name))
        except Exception as e:
            print(f"获取加固APK失败: {str(e)}")
            return []
    
    def get_apks_by_packer_type(self, packer_type: str) -> List[Apk]:
        """按加固类型获取APK"""
        try:
            return list(Apk.select().where(Apk.packer_type == packer_type).order_by(Apk.app_name))
        except Exception as e:
            print(f"按加固类型获取APK失败: {str(e)}")
            return []