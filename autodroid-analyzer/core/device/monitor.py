"""
设备监控进程 - 定期更新设备连接状态

该模块提供后台设备监控功能，定期执行adb devices命令，
检测设备连接状态变化并更新数据库。
"""

import asyncio
import logging
import time
from datetime import datetime
from typing import List, Dict, Set
import subprocess
import threading
from pathlib import Path

# 添加项目根目录到Python路径
import sys
project_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(project_root))

from autodroid_analyzer.core.device.service import DeviceManager
from autodroid_analyzer.core.device.database import DeviceDatabase

logger = logging.getLogger(__name__)


class DeviceMonitor:
    """设备监控器 - 定期更新设备连接状态"""
    
    def __init__(self, check_interval: int = 30):
        """
        初始化设备监控器
        
        Args:
            check_interval: 检查间隔时间（秒）
        """
        self.check_interval = check_interval
        self.device_manager = DeviceManager()
        self.device_db = DeviceDatabase()
        self._running = False
        self._thread = None
        self._last_connected_devices: Set[str] = set()
        
    def start(self):
        """启动设备监控进程"""
        if self._running:
            logger.warning("设备监控器已在运行中")
            return
            
        self._running = True
        self._thread = threading.Thread(target=self._monitor_loop, daemon=True)
        self._thread.start()
        logger.info(f"设备监控器已启动，检查间隔：{self.check_interval}秒")
        
    def stop(self):
        """停止设备监控进程"""
        if not self._running:
            return
            
        self._running = False
        if self._thread:
            self._thread.join(timeout=5)
        logger.info("设备监控器已停止")
        
    def _monitor_loop(self):
        """监控循环"""
        logger.info("设备监控循环开始")
        
        while self._running:
            try:
                self._check_devices()
            except Exception as e:
                logger.error(f"设备检查过程中出错: {e}")
                
            # 等待下一次检查
            time.sleep(self.check_interval)
            
        logger.info("设备监控循环结束")
        
    def _check_devices(self):
        """检查设备连接状态"""
        try:
            # 获取当前连接的设备
            current_devices = self._get_connected_devices()
            current_device_ids = set(device['id'] for device in current_devices)
            
            # 检测新连接的设备
            newly_connected = current_device_ids - self._last_connected_devices
            if newly_connected:
                logger.info(f"检测到新设备连接: {newly_connected}")
                for device_id in newly_connected:
                    device_info = self._get_device_detail_info(device_id)
                    if device_info:
                        self._update_device_in_db(device_info)
                        
            # 检测断开的设备
            disconnected_devices = self._last_connected_devices - current_device_ids
            if disconnected_devices:
                logger.info(f"检测到设备断开: {disconnected_devices}")
                for device_id in disconnected_devices:
                    self._mark_device_disconnected(device_id)
                    
            # 更新已连接设备的信息
            for device in current_devices:
                if device['id'] not in newly_connected:
                    # 定期更新已连接设备的信息
                    device_info = self._get_device_detail_info(device['id'])
                    if device_info:
                        self._update_device_in_db(device_info)
                        
            self._last_connected_devices = current_device_ids
            
        except Exception as e:
            logger.error(f"设备检查失败: {e}")
            
    def _get_connected_devices(self) -> List[Dict[str, str]]:
        """获取当前连接的设备列表"""
        try:
            # 执行adb devices命令
            result = subprocess.run(
                ['adb', 'devices'], 
                capture_output=True, 
                text=True, 
                timeout=10
            )
            
            if result.returncode != 0:
                logger.error(f"adb devices命令失败: {result.stderr}")
                return []
                
            devices = []
            lines = result.stdout.strip().split('\n')[1:]  # 跳过标题行
            
            for line in lines:
                if '\t' in line:
                    device_id, status = line.split('\t')
                    if status == 'device':
                        devices.append({
                            'id': device_id.strip(),
                            'status': status.strip()
                        })
                        
            logger.debug(f"发现 {len(devices)} 个连接的设备")
            return devices
            
        except subprocess.TimeoutExpired:
            logger.error("adb devices命令超时")
            return []
        except Exception as e:
            logger.error(f"获取设备列表失败: {e}")
            return []
            
    def _get_device_detail_info(self, device_id: str) -> Dict:
        """获取设备的详细信息"""
        try:
            # 使用DeviceManager获取设备详细信息
            device_info = self.device_manager._get_device_info_from_adb(device_id)
            
            if device_info:
                return {
                    'id': device_id,
                    'device_name': device_info.get('device_name', ''),
                    'android_version': device_info.get('android_version', ''),
                    'api_level': device_info.get('api_level', 0),
                    'connection_type': device_info.get('connection_type', 'USB'),
                    'battery_level': device_info.get('battery_level', 0),
                    'device_model': device_info.get('device_model', ''),
                    'is_connected': True,
                    'last_connected': datetime.now()
                }
            else:
                return {
                    'id': device_id,
                    'device_name': device_id[:8],  # 使用设备ID前8位作为名称
                    'android_version': 'Unknown',
                    'api_level': 0,
                    'connection_type': 'USB',
                    'battery_level': 0,
                    'device_model': 'Unknown',
                    'is_connected': True,
                    'last_connected': datetime.now()
                }
                
        except Exception as e:
            logger.error(f"获取设备详细信息失败 {device_id}: {e}")
            return None
            
    def _update_device_in_db(self, device_info: Dict):
        """更新设备信息到数据库"""
        try:
            # 检查设备是否已存在
            existing_device = self.device_db.get_device_by_id(device_info['id'])
            
            if existing_device:
                # 更新现有设备信息
                self.device_db.update_device_connection_status(
                    device_info['id'],
                    device_info['is_connected'],
                    device_info['connection_type']
                )
                
                # 更新其他设备信息
                self.device_db.update_device_info(
                    device_info['id'],
                    device_name=device_info['device_name'],
                    android_version=device_info['android_version'],
                    api_level=device_info['api_level'],
                    battery_level=device_info['battery_level'],
                    device_model=device_info['device_model'],
                    last_connected=device_info['last_connected']
                )
                
                logger.debug(f"更新设备信息: {device_info['id']}")
            else:
                # 注册新设备
                self.device_db.register_device(
                    device_id=device_info['id'],
                    device_name=device_info['device_name'],
                    android_version=device_info['android_version'],
                    api_level=device_info['api_level'],
                    connection_type=device_info['connection_type'],
                    battery_level=device_info['battery_level'],
                    device_model=device_info['device_model']
                )
                
                logger.info(f"注册新设备: {device_info['id']}")
                
        except Exception as e:
            logger.error(f"更新数据库失败: {e}")
            
    def _mark_device_disconnected(self, device_id: str):
        """标记设备为断开状态"""
        try:
            self.device_db.update_device_connection_status(
                device_id,
                is_connected=False,
                connection_type='Unknown'
            )
            logger.debug(f"标记设备为断开状态: {device_id}")
            
        except Exception as e:
            logger.error(f"标记设备断开状态失败 {device_id}: {e}")


def start_device_monitor(check_interval: int = 30) -> DeviceMonitor:
    """
    启动设备监控器
    
    Args:
        check_interval: 检查间隔时间（秒）
        
    Returns:
        DeviceMonitor实例
    """
    monitor = DeviceMonitor(check_interval)
    monitor.start()
    return monitor


if __name__ == "__main__":
    # 配置日志
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # 启动设备监控器
    monitor = start_device_monitor(check_interval=30)
    
    try:
        # 保持运行
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        logger.info("收到中断信号，正在停止监控器...")
        monitor.stop()
        logger.info("监控器已停止")