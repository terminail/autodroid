import time
import subprocess
from typing import List, Optional, Dict, Any
from peewee import DoesNotExist

from .database import DeviceDatabase
from .models import DeviceInfoResponse
from ..apk.models import ApkInfo
from workscripts.adb_device import ADBDevice

class DeviceManager:
    def __init__(self):
        """初始化设备管理器，使用统一的数据库接口"""
        self.max_concurrent_tasks = 5
        self.db = DeviceDatabase()
    
    def is_device_available(self, serialno: str) -> bool:
        """检查设备是否可用于自动化"""
        return self.db.is_device_available(serialno)
    
    def device_exists(self, serialno: str) -> bool:
        """检查设备是否存在"""
        return self.db.get_device(serialno) is not None
    
    def get_available_devices(self) -> List[DeviceInfoResponse]:
        """获取可用于自动化的设备列表"""
        devices = self.db.get_available_devices()
        return [
            DeviceInfoResponse(
                serialno=device.serialno,
                udid=device.udid,
                name=device.name,
                android_version=device.android_version,
                ip=device.ip_address if hasattr(device, 'ip_address') else None,
                registered_at=getattr(device, 'registered_at', None),
                status="online" if device.is_online else "offline"
            ) for device in devices
        ]
    
    def register_device(self, device_info: Dict[str, Any]) -> DeviceInfoResponse:
        """从应用报告注册设备"""
        serialno = device_info.get('serialno')
        
        # 使用ADB获取设备详细信息
        try:
            adb_device = ADBDevice(serialno)
            adb_device_info = adb_device.get_device_info()
            
            # 将ADB获取的信息合并到device_info中
            # 只在客户端未提供或值为空时才使用ADB获取的信息
            for key, value in adb_device_info.items():
                if key not in device_info or device_info.get(key) is None or device_info.get(key) == "":
                    device_info[key] = value
        except Exception as e:
            # 如果ADB连接失败，继续使用客户端提供的信息
            print(f"Warning: Failed to get device info via ADB for {serialno}: {e}")
        
        device = self.db.register_device(device_info)
        
        # 返回DeviceInfoResponse对象
        return DeviceInfoResponse(
            serialno=device.serialno,
            udid=device.udid,
            name=device_info.get('name') or device.name,
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
    
    def add_apk(self, serialno: str, apk_info: Dict[str, Any]) -> ApkInfo:
        """添加APK到设备"""
        apk = self.db.add_apk_to_device(serialno, apk_info)
        return ApkInfo(
            package_name=apk.package_name,
            app_name=apk.app_name,
            version=apk.version,
            version_code=apk.version_code,
            installed_time=apk.installed_time,
            is_system=apk.is_system,
            icon_path=apk.icon_path
        )
    
    def get_apks(self, serialno: str) -> List[ApkInfo]:
        """获取设备的所有APK"""
        apks = self.db.get_device_apks(serialno)
        return [
            ApkInfo(
                package_name=apk.package_name,
                app_name=apk.app_name,
                version=apk.version,
                version_code=apk.version_code,
                installed_time=apk.installed_time,
                is_system=apk.is_system,
                icon_path=apk.icon_path
            )
            for apk in apks
        ]
    
    def get_apk(self, serialno: str, package_name: str) -> Optional[ApkInfo]:
        """获取设备的特定APK"""
        apk = self.db.get_device_apk(serialno, package_name)
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
    
    def update_apk(self, serialno: str, package_name: str, apk_info: Dict[str, Any]) -> ApkInfo:
        """更新设备的APK信息"""
        apk = self.db.update_device_apk(serialno, package_name, apk_info)
        return ApkInfo(
            package_name=apk.package_name,
            app_name=apk.app_name,
            version=apk.version,
            version_code=apk.version_code,
            installed_time=apk.installed_time,
            is_system=apk.is_system,
            icon_path=apk.icon_path
        )
    
    def delete_apk(self, serialno: str, package_name: str) -> bool:
        """从设备删除APK"""
        return self.db.remove_apk_from_device(serialno, package_name)
    
    def get_devices_by_user(self, user_id: str) -> List[DeviceInfoResponse]:
        """根据用户ID获取设备列表"""
        devices = self.db.get_devices_by_user(user_id)
        result = []
        
        for device in devices:
            # 获取设备的应用列表
            try:
                device_apks = self.db.get_device_apks(device.serialno)
                apps = [
                    {
                        "package_name": apk.package_name,
                        "app_name": apk.app_name,
                        "version": apk.version,
                        "version_code": apk.version_code,
                        "installed_time": apk.installed_time,
                        "is_system": apk.is_system,
                        "icon_path": apk.icon_path
                    }
                    for apk in device_apks
                ]
            except Exception as e:
                import logging
                logging.error(f"获取设备 {device.serialno} 的应用列表失败: {str(e)}")
                apps = []
            
            device_info = DeviceInfoResponse(
                serialno=device.serialno,
                udid=device.udid,
                name=device.name,
                android_version=device.android_version,
                ip=device.ip_address if hasattr(device, 'ip_address') else None,
                registered_at=getattr(device, 'registered_at', None),
                status="online" if device.is_online else "offline",
                apps=apps
            )
            result.append(device_info)
        
        return result
    
    def assign_device_to_user(self, serialno: str, user_id: str) -> bool:
        """将设备分配给用户"""
        return self.db.assign_device_to_user(serialno, user_id)
    
    def unassign_device_from_user(self, serialno: str) -> bool:
        """取消分配设备给用户"""
        return self.db.unassign_device_from_user(serialno)
    
    def get_all_devices(self) -> List[DeviceInfoResponse]:
        """获取所有设备列表"""
        devices = self.db.get_all_devices()
        result = []
        
        for device in devices:
            # 获取设备的应用列表
            try:
                device_apks = self.db.get_device_apks(device.serialno)
                apps = [
                    {
                        "package_name": apk.package_name,
                        "app_name": apk.app_name,
                        "version": apk.version,
                        "version_code": apk.version_code,
                        "installed_time": apk.installed_time,
                        "is_system": apk.is_system,
                        "icon_path": apk.icon_path
                    }
                    for apk in device_apks
                ]
            except Exception as e:
                import logging
                logging.error(f"获取设备 {device.serialno} 的应用列表失败: {str(e)}")
                apps = []
            
            device_info = DeviceInfoResponse(
                serialno=device.serialno,
                udid=device.udid,
                name=device.name,
                android_version=device.android_version,
                ip=device.ip_address if hasattr(device, 'ip_address') else None,
                registered_at=getattr(device, 'registered_at', None),
                updated_at=getattr(device, 'updated_at', None),
                status="online" if device.is_online else "offline",
                apps=apps
            )
            result.append(device_info)
        
        return result
    
    def _parse_device_apps(self, apps_json: str) -> List[Dict[str, Any]]:
        """解析设备已安装应用的JSON字符串"""
        import json
        try:
            if not apps_json:
                return []
            return json.loads(apps_json)
        except (json.JSONDecodeError, TypeError):
            return []
    
    def delete_device(self, serialno: str) -> bool:
        """删除设备"""
        return self.db.delete_device(serialno)
    
    def update_device_status(self, serialno: str, is_online: bool, battery_level: int) -> bool:
        """更新设备状态"""
        return self.db.update_device_status(serialno, is_online, battery_level)
    
    def get_device_count(self) -> int:
        """获取设备总数"""
        return self.db.get_device_count()
    
    def get_online_device_count(self) -> int:
        """获取在线设备数量"""
        return self.db.get_online_device_count()
    
    def check_device(self, serialno: str) -> Dict[str, Any]:
        """检查设备调试设置、安装app等情况"""
        import logging
        import yaml
        logger = logging.getLogger(__name__)
        
        logger.info(f"检查设备状态: {serialno}")
        
        try:
            # 导入ADB设备类
            import sys
            import os
            sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', 'workscripts'))
            from adb_device import ADBDevice
            from core.apk.models import ApkInfo
            
            # 创建ADB设备实例
            adb_device = ADBDevice(serialno)
            
            # 检查设备连接状态
            if not adb_device.is_connected():
                logger.warning(f"设备 {serialno} 未连接或无法访问")
                return {
                    "success": False,
                    "message": f"设备 {serialno} 未连接或无法访问",
                    "serialno": serialno,
                    "udid": serialno,
                    "usb_debug_enabled": False,
                    "wifi_debug_enabled": False,
                    "installed_apps": []
                }
            
            # 检查USB调试状态
            usb_debug_enabled = adb_device.is_usb_debug_enabled()
            logger.info(f"设备 {serialno} USB调试状态: {usb_debug_enabled}")
            
            # 检查WiFi调试状态
            wifi_debug_enabled = adb_device.is_wifi_debug_enabled()
            logger.info(f"设备 {serialno} WiFi调试状态: {wifi_debug_enabled}")
            
            # 检查支持的应用安装状态
            installed_apps = []
            try:
                # 读取配置文件获取支持的应用列表
                config_path = os.path.join(os.path.dirname(__file__), '..', '..', 'config.yaml')
                with open(config_path, 'r', encoding='utf-8') as f:
                    config = yaml.safe_load(f)
                
                supported_apps = config.get('supported_apps', [])
                logger.info(f"支持的应用列表: {supported_apps}")
                
                # 检查每个支持的应用是否已安装
                for app in supported_apps:
                    app_package = app.get('app_package', '')
                    app_name = app.get('name', '')
                    
                    if app_package and adb_device.is_app_installed(app_package):
                        # 获取应用的详细信息
                        app_info = self._get_app_info(adb_device, app_package, app_name)
                        # 转换ApkInfo对象为字典
                        installed_apps.append({
                            'package_name': app_info.package_name,
                            'app_name': app_info.app_name,
                            'version': app_info.version,
                            'version_code': app_info.version_code,
                            'installed_time': app_info.installed_time,
                            'is_system': app_info.is_system,
                            'icon_path': app_info.icon_path
                        })
                        logger.info(f"应用 {app_name} ({app_package}) 已安装")
                    else:
                        logger.info(f"应用 {app_name} ({app_package}) 未安装")
                
                # 更新数据库中的应用安装状态
                self.db.update_device_apps(serialno, installed_apps)
                
            except Exception as e:
                logger.error(f"检查应用安装状态时出错: {str(e)}")
            
            # 更新数据库中的调试权限状态
            self.db.update_device_debug_status(
                serialno, 
                usb_debug_enabled, 
                wifi_debug_enabled,
                "SUCCESS",
                "设备检查完成"
            )
            
            logger.info(f"设备 {serialno} 检查完成")
            
            return {
                "success": True,
                "message": "设备检查完成",
                "serialno": serialno,
                "udid": serialno,
                "usb_debug_enabled": usb_debug_enabled,
                "wifi_debug_enabled": wifi_debug_enabled,
                "installed_apps": installed_apps
            }
            
        except Exception as e:
            logger.error(f"检查设备 {serialno} 时出错: {str(e)}")
            # 更新数据库中的检查失败状态
            self.db.update_device_check_failed(serialno, str(e))
            
            return {
                "success": False,
                "message": f"检查设备时出错: {str(e)}",
                "serialno": serialno,
                "udid": serialno,
                "usb_debug_enabled": False,
                "wifi_debug_enabled": False,
                "installed_apps": []
            }
    
    def _get_app_main_activity(self, adb_device, package_name: str) -> str:
        """获取应用的主Activity"""
        import logging
        logger = logging.getLogger(__name__)
        
        try:
            # 方法1: 使用dumpsys package获取应用信息
            result = subprocess.run(
                adb_device._get_adb_prefix() + ["shell", "dumpsys", "package", package_name],
                capture_output=True, text=True, timeout=10
            )
            
            if result.returncode == 0:
                # 查找主Activity
                for line in result.stdout.split('\n'):
                    if 'android.intent.action.MAIN:' in line:
                        # 查找下一行的Activity信息
                        lines = result.stdout.split('\n')
                        for i, l in enumerate(lines):
                            if 'android.intent.action.MAIN:' in l:
                                # 查找下一个包含Activity的行
                                for j in range(i+1, min(i+5, len(lines))):
                                    if lines[j].strip() and not lines[j].startswith(' '):
                                        break
                                    if 'Activity' in lines[j] and package_name in lines[j]:
                                        # 提取Activity名称
                                        activity = lines[j].strip().split()[-1]
                                        # 如果是完整包名，简化为相对路径
                                        if activity.startswith(package_name):
                                            activity = activity.replace(package_name, '')
                                            if activity.startswith('.'):
                                                activity = activity[1:]
                                            else:
                                                activity = '.' + activity
                                        logger.debug(f"方法1找到Activity: {activity}")
                                        return activity
                
                # 方法2: 尝试从dumpsys中查找Activity Resolver Table
                for line in result.stdout.split('\n'):
                    if package_name in line and 'Activity' in line and 'filter' not in line:
                        parts = line.strip().split()
                        for part in parts:
                            if package_name in part and '/' in part:
                                activity = part.split('/')[-1]
                                if activity.startswith('.'):
                                    activity = activity[1:]
                                logger.debug(f"方法2找到Activity: {activity}")
                                return activity
            
            # 方法3: 使用pm获取启动Activity
            result = subprocess.run(
                adb_device._get_adb_prefix() + ["shell", "pm", "dump", package_name],
                capture_output=True, text=True, timeout=10
            )
            
            if result.returncode == 0:
                for line in result.stdout.split('\n'):
                    if 'android.intent.action.MAIN' in line and 'Activity' in line:
                        # 尝试提取Activity名称
                        if package_name in line:
                            parts = line.split()
                            for part in parts:
                                if package_name in part and '/' in part:
                                    activity = part.split('/')[-1]
                                    if activity.startswith('.'):
                                        activity = activity[1:]
                                    logger.debug(f"方法3找到Activity: {activity}")
                                    return activity
            
            # 方法4: 使用cmd package resolve-activity
            result = subprocess.run(
                adb_device._get_adb_prefix() + ["shell", "cmd", "package", "resolve-activity", "--brief", package_name],
                capture_output=True, text=True, timeout=10
            )
            
            if result.returncode == 0:
                lines = result.stdout.strip().split('\n')
                for line in lines:
                    if package_name in line:
                        activity = line.strip()
                        # 如果是完整包名，简化为相对路径
                        if activity.startswith(package_name):
                            activity = activity.replace(package_name, '')
                            if activity.startswith('.'):
                                activity = activity[1:]
                            else:
                                activity = '.' + activity
                        logger.debug(f"方法4找到Activity: {activity}")
                        return activity
            
            # 方法5: 使用monkey命令获取应用包信息
            result = subprocess.run(
                adb_device._get_adb_prefix() + ["shell", "monkey", "-p", package_name, "-c", "android.intent.category.LAUNCHER", "-v", "1"],
                capture_output=True, text=True, timeout=10
            )
            
            if result.returncode == 0:
                for line in result.stdout.split('\n'):
                    if 'Using:' in line and package_name in line:
                        # 提取Activity名称
                        parts = line.split()
                        for i, part in enumerate(parts):
                            if part == package_name and i+1 < len(parts):
                                activity = parts[i+1]
                                if activity.startswith('.'):
                                    activity = activity[1:]
                                logger.debug(f"方法5找到Activity: {activity}")
                                return activity
            
            # 所有方法都失败，记录警告并返回默认值
            logger.warning(f"无法确定应用 {package_name} 的主Activity，将使用默认值.MainActivity")
            return ".MainActivity"
            
        except Exception as e:
            logger.error(f"获取应用 {package_name} 的主Activity时出错: {str(e)}")
            return ".MainActivity"  # 出错时返回默认值
    
    def _get_app_info(self, adb_device, package_name: str, app_name: str) -> ApkInfo:
        """获取应用的详细信息，返回ApkInfo对象"""
        import logging
        from datetime import datetime
        logger = logging.getLogger(__name__)
        
        try:
            # 获取应用版本信息
            version_name = "Unknown"
            version_code = 0
            is_system = False
            installed_time = None
            
            # 使用dumpsys package获取应用详细信息
            result = subprocess.run(
                adb_device._get_adb_prefix() + ["shell", "dumpsys", "package", package_name],
                capture_output=True, text=True, timeout=10
            )
            
            if result.returncode == 0:
                output = result.stdout
                
                # 解析版本信息
                for line in output.split('\n'):
                    if 'versionName=' in line:
                        version_name = line.split('versionName=')[1].split()[0]
                    elif 'versionCode=' in line:
                        try:
                            version_code = int(line.split('versionCode=')[1].split()[0])
                        except (IndexError, ValueError):
                            pass
                    elif 'flags=' in line and 'SYSTEM' in line:
                        is_system = True
                
                # 解析安装时间
                for line in output.split('\n'):
                    if 'firstInstallTime=' in line:
                        try:
                            time_str = line.split('firstInstallTime=')[1].split()[0]
                            # 转换时间戳为datetime对象
                            installed_time = datetime.fromtimestamp(int(time_str) / 1000)
                            break
                        except (IndexError, ValueError):
                            pass
            
            # 创建ApkInfo对象
            app_info = ApkInfo(
                package_name=package_name,
                app_name=app_name,
                version=version_name,
                version_code=version_code,
                installed_time=installed_time,
                is_system=is_system,
                icon_path=None  # 暂时不获取图标路径
            )
            
            logger.debug(f"获取应用信息成功: {package_name}, 版本: {version_name}")
            return app_info
            
        except Exception as e:
            logger.error(f"获取应用 {package_name} 信息时出错: {str(e)}")
            # 出错时返回基本信息
            return ApkInfo(
                package_name=package_name,
                app_name=app_name,
                version="Unknown",
                version_code=0,
                installed_time=None,
                is_system=False,
                icon_path=None
            )
    
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
