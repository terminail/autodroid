"""
编辑管理模块
负责用户操作记录、截屏管理、页面元素关联和自动化脚本生成
"""

__version__ = "1.0.0"
__author__ = "AutoDroid Team"

# 只导入实际存在的模块
from .operation_recorder import OperationRecorder

__all__ = [
    "OperationRecorder"
]