"""
监控系统模块
负责用户操作监控、回调管理和监控状态控制
"""

import time
import threading
from typing import Callable, Dict, Any, Optional, List
from dataclasses import dataclass


@dataclass
class MonitoringConfig:
    """监控配置数据类"""
    monitoring_interval: float = 1.0
    page_change_threshold: float = 0.8
    max_monitoring_time: float = 300.0  # 5分钟


class MonitoringSystem:
    """监控系统"""
    
    def __init__(self, config: Optional[MonitoringConfig] = None):
        self.config = config or MonitoringConfig()
        self.is_monitoring = False
        self.monitoring_thread: Optional[threading.Thread] = None
        self.start_time: float = 0.0
        
        # 回调函数
        self.page_change_callback: Optional[Callable] = None
        self.user_action_callback: Optional[Callable] = None
        self.monitoring_stop_callback: Optional[Callable] = None
    
    def start_monitoring(self, 
                        page_change_callback: Optional[Callable] = None,
                        user_action_callback: Optional[Callable] = None,
                        monitoring_stop_callback: Optional[Callable] = None) -> bool:
        """开始监控"""
        if self.is_monitoring:
            print("⚠️ 监控已经在运行中")
            return False
        
        # 设置回调函数
        self.page_change_callback = page_change_callback
        self.user_action_callback = user_action_callback
        self.monitoring_stop_callback = monitoring_stop_callback
        
        # 启动监控线程
        self.is_monitoring = True
        self.start_time = time.time()
        self.monitoring_thread = threading.Thread(target=self._monitoring_loop)
        self.monitoring_thread.daemon = True
        self.monitoring_thread.start()
        
        print("🔍 开始用户操作监控")
        return True
    
    def stop_monitoring(self) -> bool:
        """停止监控"""
        if not self.is_monitoring:
            print("⚠️ 监控未在运行")
            return False
        
        self.is_monitoring = False
        
        if self.monitoring_thread and self.monitoring_thread.is_alive():
            self.monitoring_thread.join(timeout=5.0)
        
        print("🛑 停止用户操作监控")
        
        # 调用停止回调
        if self.monitoring_stop_callback:
            self.monitoring_stop_callback()
        
        return True
    
    def _monitoring_loop(self) -> None:
        """监控循环"""
        previous_page_hash = ""
        
        while self.is_monitoring:
            try:
                # 检查监控时间是否超时
                current_time = time.time()
                if current_time - self.start_time > self.config.max_monitoring_time:
                    print("⏰ 监控超时，自动停止")
                    self.stop_monitoring()
                    break
                
                # 获取当前页面哈希
                current_page_hash = self._get_current_page_hash()
                
                # 检测页面变化
                if current_page_hash and previous_page_hash:
                    if self._is_page_changed(current_page_hash, previous_page_hash):
                        if self.page_change_callback:
                            self.page_change_callback(current_page_hash, previous_page_hash)
                
                previous_page_hash = current_page_hash
                
                # 检测用户操作
                user_operations = self._detect_user_operations()
                if user_operations and self.user_action_callback:
                    for operation in user_operations:
                        self.user_action_callback(operation)
                
                # 等待下一次检测
                time.sleep(self.config.monitoring_interval)
                
            except Exception as e:
                print(f"❌ 监控循环异常: {e}")
                time.sleep(self.config.monitoring_interval)
    
    def _get_current_page_hash(self) -> str:
        """获取当前页面哈希（需要子类实现）"""
        # 这个方法需要在具体的设备交互类中实现
        return ""
    
    def _is_page_changed(self, current_hash: str, previous_hash: str) -> bool:
        """检测页面是否发生变化"""
        return current_hash != previous_hash
    
    def _detect_user_operations(self) -> List[Dict[str, Any]]:
        """检测用户操作（需要子类实现）"""
        # 这个方法需要在具体的设备交互类中实现
        return []
    
    def setup_callbacks(self, 
                       page_change_callback: Optional[Callable] = None,
                       user_action_callback: Optional[Callable] = None,
                       monitoring_stop_callback: Optional[Callable] = None) -> None:
        """设置回调函数"""
        if page_change_callback:
            self.page_change_callback = page_change_callback
        if user_action_callback:
            self.user_action_callback = user_action_callback
        if monitoring_stop_callback:
            self.monitoring_stop_callback = monitoring_stop_callback
    
    def get_monitoring_status(self) -> Dict[str, Any]:
        """获取监控状态"""
        return {
            "is_monitoring": self.is_monitoring,
            "monitoring_time": time.time() - self.start_time if self.is_monitoring else 0,
            "config": {
                "monitoring_interval": self.config.monitoring_interval,
                "max_monitoring_time": self.config.max_monitoring_time
            }
        }
    
    def is_running(self) -> bool:
        """检查监控是否在运行"""
        return self.is_monitoring