#!/usr/bin/env python3
"""
工作脚本基类 - 所有工作脚本必须继承此类
"""

from abc import ABC, abstractmethod
from typing import Dict, Any, Optional
import logging
import os
import time
from datetime import datetime

# 导入设备连接模块
try:
    import sys
    sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(__file__))))
    from workscripts.device_connection import DeviceConnectionPool, ADBDeviceController
    DEVICE_CONNECTION_AVAILABLE = True
except ImportError as e:
    DEVICE_CONNECTION_AVAILABLE = False
    logging.warning(f"设备连接模块导入失败: {e}，将使用模拟模式")


class BaseWorkScript(ABC):
    """工作脚本抽象基类"""
    
    def __init__(self, workplan: Dict[str, Any], device_serialno: Optional[str] = None):
        """
        初始化工作脚本
        
        Args:
            workplan: 工作计划数据，包含脚本执行所需的所有参数
            device_serialno: 设备序列号
        """
        self.workplan = workplan
        self.device_serialno = device_serialno
        self.logger = logging.getLogger(self.__class__.__name__)
        self.start_time = None
        self.end_time = None
        self.report_dir = None
        
        # 设置报告目录
        self._setup_report_directory()
        
        # 初始化设备对象
        self.device = self._initialize_device()
        
        self.logger.info(f"初始化工作脚本: {self.__class__.__name__}")
        self.logger.info(f"工作计划ID: {workplan.get('id', 'unknown')}")
        self.logger.info(f"设备序列号: {device_serialno}")
        if self.device:
            self.logger.info(f"设备连接状态: {'已连接' if self.device.is_connected() else '未连接'}")
        else:
            self.logger.warning("未找到设备对象，将使用模拟模式")
    
    def _setup_report_directory(self):
        """设置报告目录"""
        reports_base = os.getenv('AUTODROID_REPORTS_DIR', './reports')
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        script_name = self.__class__.__name__.lower()
        
        self.report_dir = os.path.join(
            reports_base, 
            script_name, 
            f"{timestamp}_{self.workplan.get('id', 'unknown')}"
        )
        
        os.makedirs(self.report_dir, exist_ok=True)
        self.logger.info(f"报告目录: {self.report_dir}")
    
    @abstractmethod
    def run(self) -> Dict[str, Any]:
        """
        执行工作脚本的主要逻辑
        
        Returns:
            Dict 包含执行结果，必须包含 'status' 字段
            示例: {
                'status': 'success',  # 或 'failed'
                'message': '执行成功',
                'data': {...},        # 可选，额外数据
                'report_path': '...'  # 可选，报告路径
            }
        """
        pass
    
    def execute(self) -> Dict[str, Any]:
        """
        执行工作脚本的包装方法
        
        Returns:
            执行结果字典
        """
        self.start_time = time.time()
        self.logger.info(f"开始执行工作脚本: {self.__class__.__name__}")
        
        try:
            # 调用子类的 run 方法
            result = self.run()
            
            # 验证结果格式
            if not isinstance(result, dict):
                raise ValueError("run() 方法必须返回字典类型")
            
            if 'status' not in result:
                raise ValueError("结果字典必须包含 'status' 字段")
            
            if result['status'] not in ['success', 'failed', 'error']:
                raise ValueError("status 字段必须是 'success', 'failed' 或 'error'")
            
            self.end_time = time.time()
            execution_time = self.end_time - self.start_time
            
            # 添加执行时间和设备信息
            result.update({
                'execution_time': execution_time,
                'device_serialno': self.device_serialno,
                'workplan_id': self.workplan.get('id'),
                'script_name': self.__class__.__name__,
                'report_directory': self.report_dir
            })
            
            self.logger.info(f"工作脚本执行完成: {result['status']}")
            self.logger.info(f"执行时间: {execution_time:.2f}秒")
            
            return result
            
        except Exception as e:
            self.end_time = time.time()
            execution_time = self.end_time - self.start_time
            
            error_result = {
                'status': 'error',
                'message': str(e),
                'error_type': type(e).__name__,
                'execution_time': execution_time,
                'device_serialno': self.device_serialno,
                'workplan_id': self.workplan.get('id'),
                'script_name': self.__class__.__name__,
                'report_directory': self.report_dir
            }
            
            self.logger.error(f"工作脚本执行失败: {e}")
            self.logger.error(f"执行时间: {execution_time:.2f}秒")
            
            return error_result
    
    def get_workplan_param(self, key: str, default: Any = None) -> Any:
        """
        获取工作计划参数
        
        Args:
            key: 参数键名
            default: 默认值
            
        Returns:
            参数值
        """
        data = self.workplan.get('data', {})
        return data.get(key, default)
    
    def save_report(self, filename: str, content: str) -> str:
        """
        保存报告文件
        
        Args:
            filename: 文件名
            content: 文件内容
            
        Returns:
            文件完整路径
        """
        filepath = os.path.join(self.report_dir, filename)
        
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            
            self.logger.info(f"报告已保存: {filepath}")
            return filepath
            
        except Exception as e:
            self.logger.error(f"保存报告失败: {e}")
            raise
    
    def log_step(self, step_name: str, message: str = ""):
        """
        记录执行步骤
        
        Args:
            step_name: 步骤名称
            message: 步骤描述
        """
        self.logger.info(f"[步骤] {step_name}: {message}")
    
    def log_success(self, message: str):
        """记录成功信息"""
        self.logger.info(f"✅ {message}")
    
    def log_error(self, message: str):
        """记录错误信息"""
        self.logger.error(f"❌ {message}")
    
    def log_warning(self, message: str):
        """记录警告信息"""
        self.logger.warning(f"⚠️ {message}")
    
    def _initialize_device(self) -> Optional[Any]:
        """
        初始化设备对象
        
        Returns:
            设备对象或None
        """
        if not self.device_serialno:
            self.logger.warning("未提供设备序列号，无法初始化设备对象")
            return None
        
        if not DEVICE_CONNECTION_AVAILABLE:
            self.logger.warning("设备连接模块不可用，无法初始化设备对象")
            return None
        
        try:
            # 创建设备连接池
            device_pool = DeviceConnectionPool()
            
            # 获取设备对象
            device = device_pool.get_device(self.device_serialno)
            
            # 连接设备
            if device.connect():
                self.logger.info(f"设备 {self.device_serialno} 初始化成功")
                return device
            else:
                self.logger.error(f"设备 {self.device_serialno} 连接失败")
                return None
                
        except Exception as e:
            self.logger.error(f"设备初始化失败: {str(e)}")
            return None