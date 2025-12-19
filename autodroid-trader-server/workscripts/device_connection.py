"""
ADB设备连接管理器
提供真实的Android设备UI自动化能力
"""

from adb_device import ADBDevice, quick_connect
from typing import Optional, Dict, Any
import time
import logging
import subprocess
import tempfile
import os

logger = logging.getLogger(__name__)


class ADBDeviceController:
    """
    ADB设备控制类 - 纯ADB实现，无需Appium
    提供start_app、find_element_by_id、click、send_keys等UI自动化方法
    完全基于ADB命令和UIAutomator，移除所有Appium依赖
    """
    
    def __init__(self, device_udid: str = None):
        """
        初始化设备连接
        
        Args:
            device_udid: 设备UDID（可选）
        """
        self.device_udid = device_udid
        self.adb_device = None
        self._is_connected = False
        
    def connect(self, app_package: str = None, app_activity: str = None, timeout: int = 30) -> bool:
        """
        连接设备并创建ADB会话 - 纯ADB实现，无需Appium服务器
        
        Args:
            app_package: 应用包名
            app_activity: 应用启动Activity
            timeout: 连接超时时间
            
        Returns:
            连接成功返回True，失败返回False
        """
        try:
            # 初始化ADB设备连接
            self.adb_device = quick_connect(self.device_udid)
            self._is_connected = self.adb_device.is_connected()
            
            if self._is_connected:
                device_info = self.adb_device.get_device_info()
                logger.info(f"设备 {device_info.get('device_id', 'unknown')} 连接成功")
                logger.info(f"Model: {device_info.get('model', 'unknown')}")
                logger.info(f"Android Version: {device_info.get('android_version', 'unknown')}")
                
                # 如果指定了应用包名和Activity，启动应用
                if app_package:
                    if app_activity:
                        self.adb_device.start_activity(app_package, app_activity)
                    else:
                        self.adb_device.start_app(app_package)
                    time.sleep(2)  # 等待应用启动
                    
                return True
            else:
                logger.error("设备连接失败")
                return False
                
        except Exception as e:
            logger.error(f"设备 {self.device_udid} 连接失败：{str(e)}")
            self._is_connected = False
            return False
    
    def disconnect(self):
        """断开设备连接"""
        try:
            if self.adb_device:
                self.adb_device.disconnect()
                logger.info(f"设备 {self.device_udid} 连接已断开")
        except Exception as e:
            logger.error(f"断开设备 {self.device_udid} 连接时出错：{str(e)}")
        finally:
            self.adb_device = None
            self._is_connected = False
    
    def is_connected(self) -> bool:
        """检查设备是否已连接"""
        return self._is_connected and self.adb_device is not None
    
    def start_app(self, package_name: str, activity_name: str = None) -> bool:
        """
        启动应用 - 纯ADB实现，无需Appium
        
        Args:
            package_name: 应用包名
            activity_name: 启动Activity名称（可选）
            
        Returns:
            启动成功返回True
        """
        if not self.is_connected():
            logger.error(f"设备 {self.device_udid} 未连接，无法启动应用")
            return False
            
        try:
            if activity_name:
                # 使用ADB启动指定Activity
                result = self.adb_device.launch_app(package_name, activity_name)
                logger.info(f"设备 {self.device_udid} 启动Activity: {package_name}/{activity_name}")
            else:
                # 使用ADB启动应用
                result = self.adb_device.launch_app(package_name)
                logger.info(f"设备 {self.device_udid} 启动应用: {package_name}")
                
            if result:
                time.sleep(2)  # 等待应用启动
                return True
            else:
                logger.error(f"设备 {self.device_udid} 启动应用失败")
                return False
                
        except Exception as e:
            logger.error(f"设备 {self.device_udid} 启动应用失败：{str(e)}")
            return False
    
    def find_element_by_id(self, element_id: str, timeout: int = 10):
        """
        通过ID查找元素 - 使用ADB UIAutomator，无需Appium
        
        Args:
            element_id: 元素ID
            timeout: 查找超时时间
            
        Returns:
            元素信息字典，包含bounds坐标信息
            直接通过ADB命令获取UI层级结构，比Appium更快
        """
        if not self.is_connected():
            logger.error(f"设备 {self.device_udid} 未连接，无法查找元素")
            return None
            
        try:
            import subprocess
            import json
            import time
            
            start_time = time.time()
            while time.time() - start_time < timeout:
                # 使用uiautomator dump获取UI层次结构
                result = subprocess.run(
                    self.adb_device._get_adb_prefix() + ["shell", "uiautomator", "dump", "/sdcard/ui_dump.xml"],
                    capture_output=True, text=True
                )
                
                if result.returncode == 0:
                    # 拉取UI dump文件到临时目录
                    temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.xml', delete=False)
                    temp_file.close()
                    subprocess.run(
                        self.adb_device._get_adb_prefix() + ["pull", "/sdcard/ui_dump.xml", temp_file.name],
                        capture_output=True
                    )
                    
                    # 解析XML查找元素
                    import xml.etree.ElementTree as ET
                    try:
                        tree = ET.parse(temp_file.name)
                        root = tree.getroot()
                        
                        # 查找包含指定ID的元素
                        for elem in root.iter():
                            if elem.get("resource-id") == element_id:
                                bounds = elem.get("bounds")
                                if bounds:
                                    # 解析bounds坐标 [x1,y1][x2,y2]
                                    import re
                                    match = re.search(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds)
                                    if match:
                                        x1, y1, x2, y2 = map(int, match.groups())
                                        center_x = (x1 + x2) // 2
                                        center_y = (y1 + y2) // 2
                                        
                                        logger.info(f"设备 {self.device_udid} 找到元素: {element_id}, 坐标: ({center_x}, {center_y})")
                                        return {
                                            "element_id": element_id,
                                            "bounds": bounds,
                                            "center_x": center_x,
                                            "center_y": center_y,
                                            "x1": x1,
                                            "y1": y1,
                                            "x2": x2,
                                            "y2": y2
                                        }
                    except Exception as parse_error:
                        logger.warning(f"解析UI dump失败: {parse_error}")
                    finally:
                        # 清理临时文件
                        try:
                            if temp_file and os.path.exists(temp_file.name):
                                os.unlink(temp_file.name)
                        except:
                            pass
                
                time.sleep(1)  # 等待1秒后重试
            
            logger.error(f"设备 {self.device_udid} 在{timeout}秒内未找到元素: {element_id}")
            return None
            
        except Exception as e:
            logger.error(f"设备 {self.device_udid} 查找元素 {element_id} 失败：{str(e)}")
            return None
    
    def click(self, element_id: str = None, x: int = None, y: int = None, timeout: int = 10) -> bool:
        """
        点击元素或坐标 - 使用ADB命令，无Appium依赖
        
        Args:
            element_id: 元素ID（优先使用）
            x, y: 坐标（当element_id为None时使用）
            timeout: 查找超时时间
            
        Returns:
            点击成功返回True
            直接ADB tap命令，比Appium更快速可靠
        """
        if not self.is_connected():
            logger.error(f"设备 {self.device_udid} 未连接，无法点击")
            return False
            
        try:
            # 检查是否提供了坐标参数（x和y都不为None）
            if x is not None and y is not None:
                # 通过坐标点击（优先处理坐标点击）
                self.adb_device.tap(x, y)
                logger.info(f"设备 {self.device_udid} 点击坐标: ({x}, {y})")
                return True
            elif element_id:
                # 通过元素ID点击
                element = self.find_element_by_id(element_id, timeout)
                if element:
                    # 使用ADB点击元素中心坐标
                    self.adb_device.tap(element["center_x"], element["center_y"])
                    logger.info(f"设备 {self.device_udid} 点击元素: {element_id} 坐标: ({element['center_x']}, {element['center_y']})")
                    return True
                else:
                    return False
            else:
                logger.error("必须提供element_id或坐标(x, y)")
                return False
                
        except Exception as e:
            logger.error(f"设备 {self.device_udid} 点击失败：{str(e)}")
            return False
    
    def send_keys(self, element_id: str, text: str, timeout: int = 10) -> bool:
        """
        向元素输入文本 - 纯ADB实现，无需Appium
        
        Args:
            element_id: 元素ID
            text: 输入的文本
            timeout: 查找超时时间
            
        Returns:
            输入成功返回True
            使用ADB input text命令，比Appium更稳定
        """
        if not self.is_connected():
            logger.error(f"设备 {self.device_udid} 未连接，无法输入文本")
            return False
            
        try:
            # 先点击元素获取焦点
            if self.click(element_id, timeout=timeout):
                time.sleep(0.5)  # 等待焦点
                
                # 使用ADB input text命令输入文本
                import subprocess
                result = subprocess.run(
                    self.adb_device._get_adb_prefix() + ["shell", "input", "text", text],
                    capture_output=True
                )
                
                if result.returncode == 0:
                    logger.info(f"设备 {self.device_udid} 向元素 {element_id} 输入文本: {text}")
                    return True
                else:
                    logger.error(f"设备 {self.device_udid} ADB输入文本失败: {result.stderr}")
                    return False
            else:
                return False
                
        except Exception as e:
            logger.error(f"设备 {self.device_udid} 输入文本失败：{str(e)}")
            return False
    
    def get_current_activity(self) -> str:
        """获取当前Activity名称"""
        if not self.is_connected():
            logger.error(f"设备 {self.device_udid} 未连接，无法获取当前Activity")
            return ""
            
        try:
            activity = self.adb_device.get_current_app()
            logger.info(f"设备 {self.device_udid} 当前Activity: {activity}")
            return activity
        except Exception as e:
            logger.error(f"设备 {self.device_udid} 获取当前Activity失败：{str(e)}")
            return ""
    
    def get_current_package(self) -> str:
        """获取当前包名"""
        if not self.is_connected():
            logger.error(f"设备 {self.device_udid} 未连接，无法获取当前包名")
            return ""
            
        try:
            package = self.adb_device.get_current_package()
            logger.info(f"设备 {self.device_udid} 当前包名: {package}")
            return package
        except Exception as e:
            logger.error(f"设备 {self.device_udid} 获取当前包名失败：{str(e)}")
            return ""
    
    def back(self) -> bool:
        """返回键"""
        if not self.is_connected():
            logger.error(f"设备 {self.device_udid} 未连接，无法执行返回操作")
            return False
            
        try:
            self.adb_device.back()
            logger.info(f"设备 {self.device_udid} 执行返回操作")
            return True
        except Exception as e:
            logger.error(f"设备 {self.device_udid} 执行返回操作失败：{str(e)}")
            return False
    
    def home(self) -> bool:
        """Home键"""
        if not self.is_connected():
            logger.error(f"设备 {self.device_udid} 未连接，无法执行Home操作")
            return False
            
        try:
            self.adb_device.home()
            logger.info(f"设备 {self.device_udid} 执行Home操作")
            return True
        except Exception as e:
            logger.error(f"设备 {self.device_udid} 执行Home操作失败：{str(e)}")
            return False
    
    def wait_for_element(self, element_id: str, timeout: int = 10):
        """
        等待元素出现
        
        Args:
            element_id: 元素ID
            timeout: 等待超时时间
            
        Returns:
            找到的元素对象
        """
        if not self.is_connected():
            logger.error(f"设备 {self.device_udid} 未连接，无法等待元素")
            return None
            
        try:
            element = self.adb_device.wait_for_element(element_id, timeout)
            logger.info(f"设备 {self.device_udid} 等待并找到元素: {element_id}")
            return element
            
        except Exception as e:
            logger.error(f"设备 {self.device_udid} 等待元素 {element_id} 失败：{str(e)}")
            return None


class DeviceConnectionPool:
    """
    设备连接池管理器 - ADB-Based，无Appium依赖
    管理多个设备的ADB连接
    """
    
    def __init__(self, max_connections: int = 10):
        self.max_connections = max_connections
        self.connections: Dict[str, ADBDeviceController] = {}
        
    def get_device(self, device_udid: str) -> ADBDeviceController:
        """
        获取设备连接 - 纯ADB实现
        
        Args:
            device_udid: 设备UDID
            
        Returns:
            ADBDeviceController对象 - 基于ADB，无需Appium服务器
        """
        # 如果已存在连接，直接返回
        if device_udid in self.connections:
            device = self.connections[device_udid]
            if device.is_connected():
                return device
            else:
                # 连接已断开，移除旧连接
                del self.connections[device_udid]
        
        # 创建新连接
        device = ADBDeviceController(device_udid)
        self.connections[device_udid] = device
        
        return device
        
    def close_all_connections(self):
        """关闭所有设备连接"""
        for device in self.connections.values():
            device.disconnect()
        self.connections.clear()
        logger.info("所有ADB设备连接已关闭")
    
    def remove_device(self, device_udid: str):
        """移除设备连接"""
        if device_udid in self.connections:
            device = self.connections[device_udid]
            device.disconnect()
            del self.connections[device_udid]
    
    def disconnect_all(self):
        """断开所有设备连接"""
        for device in self.connections.values():
            device.disconnect()
        self.connections.clear()
    
    def get_connected_devices(self) -> list:
        """获取已连接的设备列表"""
        return [udid for udid, device in self.connections.items() if device.is_connected()]