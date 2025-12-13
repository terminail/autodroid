"""
设备管理服务类 - 按照server-database-model模式实现
"""

import os
import time
import subprocess
from pathlib import Path
from typing import Dict, List, Optional, Any
from datetime import datetime

from core.database.models import Device
from .models import DeviceInfo, DeviceCreateRequest, DeviceConnectionLogInfo, DeviceAppInfo, DeviceInfoFromADB


class DeviceManager:
    """设备管理服务类"""
    
    def __init__(self):
        """初始化设备管理服务"""
        # 延迟导入以避免循环导入问题
        from .database import DeviceDatabase
        self.db = DeviceDatabase()
    
    def register_device(self, device_data: Dict[str, Any]) -> Optional[DeviceInfo]:
        """注册设备信息"""
        return self.db.register_device(device_data)
    
    def get_device(self, device_id: str) -> Optional[DeviceInfo]:
        """获取特定设备信息"""
        return self.db.get_device(device_id)
    
    def get_all_devices(self) -> List[DeviceInfo]:
        """获取所有设备信息"""
        return self.db.get_all_devices()
    
    def update_device(self, device_id: str, update_data: Dict[str, Any]) -> bool:
        """更新设备信息"""
        return self.db.update_device(device_id, update_data)
    
    def delete_device(self, device_id: str) -> bool:
        """删除设备记录"""
        return self.db.delete_device(device_id)
    
    def get_connected_devices(self) -> List[DeviceInfo]:
        """获取已连接的设备 - 从adb实时检测"""
        try:
            # 使用adb devices命令获取实时连接的设备
            result = subprocess.run(
                ["adb", "devices"], 
                capture_output=True, 
                text=True, 
                timeout=10
            )
            
            if result.returncode != 0:
                print(f"❌ 获取设备列表失败: {result.stderr}")
                return []
            
            devices = []
            device_ids = []
            
            # 解析adb devices输出
            for line in result.stdout.strip().split('\n')[1:]:  # 跳过第一行标题
                if line.strip() and '\tdevice' in line:
                    device_id = line.split('\t')[0].strip()
                    if device_id:
                        device_ids.append(device_id)
                        
                        # 获取设备详细信息
                        device_info = self._get_device_info_from_adb(device_id)
                        if device_info:
                            # 更新或创建设备记录
                            existing_device = self.db.get_device(device_id)
                            if existing_device:
                                # 更新现有设备信息
                                self.db.update_device(device_id, {
                                    'device_name': device_info.device_name,
                                    'device_model': device_info.device_model,
                                    'android_version': device_info.android_version,
                                    'api_level': device_info.api_level,
                                    'connection_type': device_info.connection_type,
                                    'battery_level': device_info.battery_level,
                                    'is_connected': True,
                                    'last_connected': datetime.now()
                                })
                            else:
                                # 创建新设备记录
                                self.db.create_device({
                                    'device_id': device_id,
                                    'device_name': device_info.device_name,
                                    'device_model': device_info.device_model,
                                    'android_version': device_info.android_version,
                                    'api_level': device_info.api_level,
                                    'connection_type': device_info.connection_type,
                                    'battery_level': device_info.battery_level,
                                    'is_connected': True,
                                    'created_at': datetime.now(),
                                    'last_connected': datetime.now()
                                })
                            
                            # 获取更新后的设备信息 - 现在 _get_device_info_from_adb 直接返回 DeviceInfo
                            devices.append(device_info)
            
            # 更新所有不在连接列表中的设备状态为断开
            all_db_devices = self.db.get_all_devices()
            for db_device in all_db_devices:
                if db_device.id not in device_ids and db_device.is_connected:
                    self.db.set_device_connection_status(db_device.id, False)
            
            print(f"📱 检测到 {len(devices)} 个连接的设备")
            return devices
            
        except Exception as e:
            print(f"❌ 获取连接设备失败: {e}")
            # 如果adb命令失败，返回数据库中的已连接设备
            devices = self.db.get_connected_devices()
            return devices
    
    def _get_device_info_from_adb(self, device_id: str) -> Optional[DeviceInfo]:
        """从adb获取设备详细信息 - 统一模型"""
        try:
            device_model = 'Unknown'
            android_version = 'Unknown'
            api_level = 0
            device_name = device_id
            battery_level = 0
            battery_status = 'Unknown'
            is_charging = False
            connection_type = 'USB'
            
            # 获取设备型号
            try:
                result = subprocess.run(
                    ["adb", "-s", device_id, "shell", "getprop", "ro.product.model"],
                    capture_output=True, text=True, timeout=5
                )
                if result.returncode == 0:
                    device_model = result.stdout.strip()
            except:
                pass
            
            # 获取Android版本
            try:
                result = subprocess.run(
                    ["adb", "-s", device_id, "shell", "getprop", "ro.build.version.release"],
                    capture_output=True, text=True, timeout=5
                )
                if result.returncode == 0:
                    android_version = result.stdout.strip()
            except:
                pass
            
            # 获取API级别
            try:
                result = subprocess.run(
                    ["adb", "-s", device_id, "shell", "getprop", "ro.build.version.sdk"],
                    capture_output=True, text=True, timeout=5
                )
                if result.returncode == 0:
                    api_level = int(result.stdout.strip())
            except:
                pass
            
            # 获取电池信息
            try:
                result = subprocess.run(
                    ["adb", "-s", device_id, "shell", "dumpsys", "battery"],
                    capture_output=True, text=True, timeout=5
                )
                if result.returncode == 0:
                    battery_output = result.stdout
                    # 解析电池电量
                    for line in battery_output.split('\n'):
                        if 'level:' in line:
                            try:
                                battery_level = int(line.split(':')[1].strip())
                            except:
                                pass
                        elif 'status:' in line:
                            try:
                                status_code = int(line.split(':')[1].strip())
                                if status_code == 2:  # 充电中
                                    is_charging = True
                                    battery_status = 'Charging'
                                elif status_code == 5:  # 充满
                                    battery_status = 'Full'
                                else:
                                    battery_status = 'Discharging'
                            except:
                                pass
                        elif 'USB powered:' in line and 'true' in line:
                            connection_type = 'USB'
                        elif 'Wireless powered:' in line and 'true' in line:
                            connection_type = 'WiFi'
            except:
                pass
            
            # 获取设备名称（使用型号作为名称）
            device_name = device_model if device_model != 'Unknown' else device_id
            
            # 获取数据库中的设备信息（如果存在）
            from datetime import datetime
            existing_device = self.db.get_device(device_id)
            if existing_device:
                created_at = existing_device.created_at
                last_connected = existing_device.last_connected
                last_updated = existing_device.last_updated
            else:
                created_at = datetime.now()
                last_connected = datetime.now()
                last_updated = datetime.now()
            
            return DeviceInfo(
                id=device_id,
                device_name=device_name,
                device_model=device_model,
                android_version=android_version,
                api_level=api_level,
                is_connected=True,  # 因为能被adb检测到，所以是已连接状态
                connection_type=connection_type,
                battery_level=battery_level,
                battery_status=battery_status,
                is_charging=is_charging,
                created_at=created_at,
                last_updated=last_updated,
                last_connected=last_connected
            )
            
        except Exception as e:
            print(f"❌ 获取设备 {device_id} 信息失败: {e}")
            return None
    
    def set_device_connection_status(self, device_id: str, is_connected: bool) -> bool:
        """设置设备连接状态"""
        return self.db.set_device_connection_status(device_id, is_connected)
    
    def search_devices(self, **kwargs) -> List[DeviceInfo]:
        """搜索设备 - 现在数据库方法直接返回 DeviceInfo 对象"""
        return self.db.search_devices(**kwargs)
    
    def get_device_count(self) -> int:
        """获取设备总数"""
        return self.db.get_device_count()
    
    def get_recently_connected_devices(self, days: int = 7) -> List[DeviceInfo]:
        """获取最近连接的设备 - 现在数据库方法直接返回 DeviceInfo 对象"""
        return self.db.get_recently_connected_devices(days)
    
    def check_device_connection(self, device_id: str) -> bool:
        """检查设备连接状态"""
        try:
            result = subprocess.run(
                ["adb", "devices"], 
                capture_output=True, 
                text=True, 
                timeout=10
            )
            
            if device_id in result.stdout:
                self.set_device_connection_status(device_id, True)
                return True
            else:
                self.set_device_connection_status(device_id, False)
                return False
                
        except Exception as e:
            print(f"检查设备连接失败: {e}")
            self.set_device_connection_status(device_id, False)
            return False
    
    def get_current_app(self, device_id: str) -> Optional[str]:
        """获取当前前台应用包名"""
        try:
            if not self.check_device_connection(device_id):
                print(f"❌ 设备未连接，无法获取当前应用")
                return None
            
            # 使用adb命令获取当前前台应用
            cmd = f'adb -s {device_id} shell dumpsys window windows | grep -E "mCurrentFocus|mFocusedApp"'
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=10)
            
            if result.returncode == 0:
                output = result.stdout.strip()
                # 解析输出获取包名
                if "mCurrentFocus" in output:
                    import re
                    match = re.search(r'[\w\.]+/\w+', output)
                    if match:
                        full_name = match.group(0)
                        return full_name.split('/')[0]
            
            return None
        except Exception as e:
            print(f"❌ 获取当前应用失败: {e}")
            return None
    
    def launch_app(self, device_id: str, app_package: str) -> bool:
        """启动应用"""
        try:
            if not self.check_device_connection(device_id):
                print(f"❌ 设备未连接，无法启动应用 {app_package}")
                return False
            
            # 检查应用是否已安装
            check_result = subprocess.run(
                ["adb", "-s", device_id, "shell", "pm", "list", "packages", app_package],
                capture_output=True,
                text=True,
                timeout=10
            )
            
            if app_package not in check_result.stdout:
                print(f"❌ 应用 {app_package} 未安装在设备上")
                return False
            
            # 检查应用是否已在前台
            current_app = self.get_current_app(device_id)
            if current_app and app_package in current_app:
                print("✅ 应用已在前台运行")
                return True
            
            print("⚠️ 应用未在前台运行，尝试自动启动...")
            print(f"   目标应用包名: {app_package}")
            
            # 尝试启动应用
            try:
                cmd = f'adb -s {device_id} shell am start -n {app_package}/.MainActivity'
                result = subprocess.run(cmd, shell=True, capture_output=True, timeout=10)
                
                if result.returncode == 0:
                    print("✅ 应用启动命令已发送")
                    
                    # 等待应用启动
                    time.sleep(3)
                    
                    # 再次检查应用是否在前台
                    current_app = self.get_current_app(device_id)
                    if current_app and app_package in current_app:
                        print("✅ 应用已成功启动并运行在前台")
                        return True
                    else:
                        print("⚠️ 应用可能未完全启动，请稍等...")
                        return True
                else:
                    print(f"❌ 应用启动失败: {result.stderr.decode('utf-8', errors='ignore')}")
                    
                    # 尝试备用启动方式
                    print("💡 尝试备用启动方式...")
                    cmd = f'adb -s {device_id} shell monkey -p {app_package} -c android.intent.category.LAUNCHER 1'
                    result = subprocess.run(cmd, shell=True, capture_output=True, timeout=10)
                    
                    if result.returncode == 0:
                        print("✅ 备用启动方式成功")
                        time.sleep(3)
                        return True
                    else:
                        print(f"❌ 备用启动方式也失败: {result.stderr.decode('utf-8', errors='ignore')}")
                        return False
                        
            except Exception as e:
                print(f"❌ 应用启动异常: {e}")
                return False
                
        except Exception as e:
            print(f"❌ 启动应用失败: {e}")
            return False
    
    def take_screenshot(self, device_id: str, output_path: str) -> bool:
        """截取屏幕截图"""
        try:
            if not self.check_device_connection(device_id):
                print(f"❌ 设备未连接，无法截取截图")
                return False
            
            # 确保输出目录存在
            output_dir = Path(output_path).parent
            output_dir.mkdir(parents=True, exist_ok=True)
            
            # 截取截图
            screenshot_result = subprocess.run(
                ["adb", "-s", device_id, "shell", "screencap", "-p", "/sdcard/screenshot.png"],
                capture_output=True,
                text=True,
                timeout=10
            )
            
            if screenshot_result.returncode != 0:
                print(f"❌ 截取截图失败")
                return False
            
            # 拉取截图到本地
            pull_result = subprocess.run(
                ["adb", "-s", device_id, "pull", "/sdcard/screenshot.png", output_path],
                capture_output=True,
                text=True,
                timeout=10
            )
            
            if pull_result.returncode == 0 and Path(output_path).exists():
                print(f"✅ 截图已保存: {output_path}")
                
                # 删除设备上的临时文件
                subprocess.run(
                    ["adb", "-s", device_id, "shell", "rm", "/sdcard/screenshot.png"],
                    capture_output=True,
                    timeout=5
                )
                
                return True
            else:
                print(f"❌ 拉取截图失败")
                return False
        except Exception as e:
            print(f"❌ 截取截图失败: {e}")
            return False
    
    def _device_to_dict(self, device: Device) -> Dict[str, Any]:
        """将Device模型转换为字典"""
        return {
            'device_id': device.device_id,
            'device_name': device.device_name,
            'device_model': device.device_model,
            'android_version': device.android_version,
            'api_level': device.api_level,
            'screen_width': device.screen_width,
            'screen_height': device.screen_height,
            'density': device.density,
            'is_connected': device.is_connected,
            'last_connected': device.last_connected.isoformat() if device.last_connected else None
        }
    



def main():
    """主函数 - 用于测试"""
    manager = DeviceManager()
    
    # 测试设备注册
    device_data = {
        'device_id': 'test_device_001',
        'device_name': '测试设备',
        'device_model': 'Test Model',
        'android_version': '12.0',
        'api_level': 31,
        'screen_width': 1080,
        'screen_height': 1920,
        'density': 420,
        'is_connected': True
    }
    
    device = manager.register_device(device_data)
    if device:
        print(f"✅ 设备注册成功: {device}")
    else:
        print("❌ 设备注册失败")


if __name__ == "__main__":
    main()