#!/usr/bin/env python3
"""
工作脚本引擎 - 动态加载和执行工作脚本
"""

import os
import sys
import importlib.util
import logging
import json
from typing import Dict, Any, Optional, Type
from datetime import datetime
import traceback

from .base import BaseWorkScript


class WorkScriptEngine:
    """工作脚本引擎"""
    
    def __init__(self, workscripts_dir: str = None, reports_dir: str = None):
        """
        初始化工作脚本引擎
        
        Args:
            workscripts_dir: 工作脚本目录路径
            reports_dir: 报告输出目录路径
        """
        self.workscripts_dir = workscripts_dir or os.getenv(
            'AUTODROID_WORKSCRIPTS_DIR', 
            './autodroid-container/workscripts'
        )
        self.reports_dir = reports_dir or os.getenv(
            'AUTODROID_REPORTS_DIR', 
            './reports'
        )
        
        self.logger = logging.getLogger(self.__class__.__name__)
        self.loaded_scripts = {}  # 缓存已加载的脚本
        
        # 设置日志
        self._setup_logging()
        
        self.logger.info(f"工作脚本引擎初始化完成")
        self.logger.info(f"脚本目录: {self.workscripts_dir}")
        self.logger.info(f"报告目录: {self.reports_dir}")
    
    def _setup_logging(self):
        """设置日志配置"""
        # 确保报告目录存在
        os.makedirs(self.reports_dir, exist_ok=True)
        
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.StreamHandler(),
                logging.FileHandler(
                    os.path.join(self.reports_dir, 'workscript_engine.log')
                )
            ]
        )
    
    def load_script(self, script_name: str) -> Type[BaseWorkScript]:
        """
        动态加载工作脚本
        
        Args:
            script_name: 脚本名称（不含.py后缀）
            
        Returns:
            工作脚本类
            
        Raises:
            FileNotFoundError: 脚本文件不存在
            ImportError: 脚本导入失败
            ValueError: 脚本格式错误
        """
        # 检查缓存
        if script_name in self.loaded_scripts:
            self.logger.info(f"从缓存加载脚本: {script_name}")
            return self.loaded_scripts[script_name]
        
        # 构建脚本文件路径
        script_path = os.path.join(self.workscripts_dir, f"{script_name}.py")
        
        if not os.path.exists(script_path):
            raise FileNotFoundError(f"脚本文件不存在: {script_path}")
        
        try:
            self.logger.info(f"加载脚本: {script_name}")
            
            # 动态导入模块
            spec = importlib.util.spec_from_file_location(script_name, script_path)
            if spec is None or spec.loader is None:
                raise ImportError(f"无法创建模块规范: {script_name}")
            
            module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(module)
            
            # 查找工作脚本类
            script_class = None
            for attr_name in dir(module):
                attr = getattr(module, attr_name)
                if (isinstance(attr, type) and 
                    issubclass(attr, BaseWorkScript) and 
                    attr != BaseWorkScript):
                    script_class = attr
                    break
            
            if script_class is None:
                raise ValueError(f"脚本中未找到继承BaseWorkScript的类: {script_name}")
            
            # 验证类名是否与文件名匹配
            if script_class.__name__ != script_name:
                self.logger.warning(
                    f"脚本类名 '{script_class.__name__}' 与文件名 '{script_name}' 不匹配"
                )
            
            # 缓存脚本类
            self.loaded_scripts[script_name] = script_class
            
            self.logger.info(f"脚本加载成功: {script_name}")
            return script_class
            
        except Exception as e:
            self.logger.error(f"脚本加载失败: {script_name} - {e}")
            self.logger.error(traceback.format_exc())
            raise ImportError(f"脚本加载失败: {script_name} - {e}")
    
    def execute_script(self, workplan: Dict[str, Any], device_udid: str = None) -> Dict[str, Any]:
        """
        执行工作脚本
        
        Args:
            workplan: 工作计划数据
            device_udid: 设备UDID
            
        Returns:
            执行结果
        """
        start_time = datetime.now()
        
        try:
            # 验证工作计划格式
            self._validate_workplan(workplan)
            
            script_name = workplan['workscript']
            self.logger.info(f"开始执行工作脚本: {script_name}")
            self.logger.info(f"工作计划ID: {workplan.get('id', 'unknown')}")
            self.logger.info(f"设备UDID: {device_udid}")
            
            # 加载脚本类
            script_class = self.load_script(script_name)
            
            # 创建脚本实例
            script_instance = script_class(workplan, device_udid)
            
            # 执行脚本
            result = script_instance.execute()
            
            # 添加执行元数据
            result.update({
                'execution_start_time': start_time.isoformat(),
                'execution_end_time': datetime.now().isoformat(),
                'engine_version': '1.0.0'
            })
            
            self.logger.info(f"工作脚本执行完成: {result['status']}")
            return result
            
        except Exception as e:
            self.logger.error(f"工作脚本执行失败: {e}")
            self.logger.error(traceback.format_exc())
            
            error_result = {
                'status': 'error',
                'message': str(e),
                'error_type': type(e).__name__,
                'execution_start_time': start_time.isoformat(),
                'execution_end_time': datetime.now().isoformat(),
                'engine_version': '1.0.0',
                'workplan_id': workplan.get('id') if isinstance(workplan, dict) else None,
                'script_name': workplan.get('workscript') if isinstance(workplan, dict) else None,
                'device_udid': device_udid
            }
            
            return error_result
    
    def _validate_workplan(self, workplan: Dict[str, Any]):
        """
        验证工作计划格式
        
        Args:
            workplan: 工作计划数据
            
        Raises:
            ValueError: 工作计划格式错误
        """
        if not isinstance(workplan, dict):
            raise ValueError("工作计划必须是字典类型")
        
        if 'workscript' not in workplan:
            raise ValueError("工作计划必须包含 'workscript' 字段")
        
        if 'data' not in workplan:
            raise ValueError("工作计划必须包含 'data' 字段")
        
        if not isinstance(workplan['data'], dict):
            raise ValueError("工作计划 'data' 字段必须是字典类型")
    
    def list_available_scripts(self) -> list:
        """
        列出可用的工作脚本
        
        Returns:
            脚本名称列表
        """
        scripts = []
        
        try:
            for filename in os.listdir(self.workscripts_dir):
                if filename.endswith('.py') and not filename.startswith('__'):
                    script_name = filename[:-3]  # 移除.py后缀
                    
                    try:
                        # 尝试加载脚本以验证其有效性
                        self.load_script(script_name)
                        scripts.append(script_name)
                    except Exception as e:
                        self.logger.warning(f"脚本验证失败: {script_name} - {e}")
            
            self.logger.info(f"发现 {len(scripts)} 个可用脚本")
            return sorted(scripts)
            
        except Exception as e:
            self.logger.error(f"列出脚本失败: {e}")
            return []
    
    def list_scripts(self) -> list:
        """
        列出所有可用的工作脚本（兼容旧方法名）
        
        Returns:
            脚本名称列表
        """
        return self.list_available_scripts()
    
    def get_script_info(self, script_name: str) -> Dict[str, Any]:
        """
        获取脚本信息
        
        Args:
            script_name: 脚本名称
            
        Returns:
            脚本信息
        """
        try:
            script_class = self.load_script(script_name)
            
            info = {
                'name': script_name,
                'class_name': script_class.__name__,
                'module_path': os.path.join(self.workscripts_dir, f"{script_name}.py"),
                'docstring': script_class.__doc__ or '',
                'available': True
            }
            
            return info
            
        except Exception as e:
            return {
                'name': script_name,
                'error': str(e),
                'available': False
            }