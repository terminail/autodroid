#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
调试工具包 - Debug Tools Package
提供统一的ADB调试和XML分析功能

主要模块:
- adb_operator: 统一的ADB页面分析和元素定位工具
- adb_dumper: 交互式页面转储工具（截图+XML）

使用方法:
    from tools.adb_operator import ADBAutoOpTool
    from tools.adb_dumper import ADBDumper
"""

from .adb_operator import ADBAutoOpTool
from .adb_dumper import ADBDumper

__all__ = ['ADBAutoOpTool', 'ADBDumper']
