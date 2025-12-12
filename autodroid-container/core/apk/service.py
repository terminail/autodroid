"""
APK模块的业务逻辑服务
提供APK管理和设备关联功能
"""

import time
from typing import List, Optional, Dict, Any
from peewee import DoesNotExist

from .database import ApkDatabase
from .models import ApkInfo, ApkCreateRequest, ApkUpdateRequest, ApkListResponse, DeviceApkInfo, ApkRegisterRequest, ApkSearchRequest


class ApkManager:
    def __init__(self):
        """初始化APK管理器，使用统一的数据库接口"""
        self.db = ApkDatabase()
    
    def register_apk_to_device(self, request: ApkRegisterRequest) -> ApkInfo:
        """将APK注册到设备"""
        apk = self.db.register_apk_to_device(request.device_udid, request.apk_info.dict())
        
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
    
    def get_apk(self, package_name: str) -> Optional[ApkInfo]:
        """获取APK信息"""
        apk = self.db.get_apk(package_name)
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
    
    def get_all_apks(self) -> List[ApkInfo]:
        """获取所有APK列表"""
        apks = self.db.get_all_apks()
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
    
    def search_apks(self, request: ApkSearchRequest) -> ApkListResponse:
        """搜索APK"""
        search_params = {
            'package_name': request.package_name,
            'app_name': request.app_name,
            'version': request.version,
            'is_system': request.is_system,
            'limit': request.limit,
            'offset': request.offset
        }
        
        # 移除None值
        search_params = {k: v for k, v in search_params.items() if v is not None}
        
        apks = self.db.search_apks(**search_params)
        total_count = self.db.get_apk_count()
        
        apk_info_list = [
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
        
        return ApkListResponse(
            apks=apk_info_list,
            total_count=total_count
        )
    
    def update_apk(self, package_name: str, request: ApkUpdateRequest) -> Optional[ApkInfo]:
        """更新APK信息"""
        update_data = request.dict(exclude_unset=True)
        apk = self.db.update_apk(package_name, update_data)
        
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
    
    def delete_apk(self, package_name: str) -> bool:
        """删除APK"""
        return self.db.delete_apk(package_name)
    
    def dissociate_apk_from_device(self, package_name: str, device_udid: str) -> bool:
        """将APK与设备解关联"""
        return self.db.dissociate_apk_from_device(package_name, device_udid)
    
    def get_devices_for_apk(self, package_name: str) -> List[str]:
        """获取安装该APK的设备列表"""
        return self.db.get_devices_for_apk(package_name)
    
    def get_apks_for_device(self, device_udid: str) -> List[ApkInfo]:
        """获取设备的所有APK"""
        apks = self.db.get_apks_for_device(device_udid)
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
    
    def get_apk_count(self) -> int:
        """获取APK总数"""
        return self.db.get_apk_count()
    
    def get_device_apk_count(self, device_udid: str) -> int:
        """获取设备的APK数量"""
        return self.db.get_device_apk_count(device_udid)


if __name__ == "__main__":
    """Main entry point for APK manager service"""
    import logging
    import time
    
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)
    
    apk_manager = ApkManager()
    logger.info("APK Manager Service Started")
    
    # Keep the service running
    try:
        while True:
            time.sleep(60)
    except KeyboardInterrupt:
        logger.info("APK Manager Service Stopped")