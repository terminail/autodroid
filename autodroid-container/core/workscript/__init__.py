#!/usr/bin/env python3
"""
工作脚本引擎核心模块

提供动态加载和执行工作脚本的功能
"""

from .base import BaseWorkScript
from .engine import WorkScriptEngine

__version__ = '1.0.0'
__all__ = ['BaseWorkScript', 'WorkScriptEngine']