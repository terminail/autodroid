#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
调试工具包 - Debug Tools Package
提供统一的ADB调试和XML分析功能

主要模块:
- adb_debug_tool: 统一的ADB页面分析和元素定位工具
- adb_dumper: 交互式页面转储工具（截图+XML）
- xml_analyzer: XML页面对比和元素分析工具
- verify_tradescript: Tradescript Engine 完整执行流程验证

使用方法:
    from tools.adb_debug_tool import ADBDebugTool
    from tools.adb_dumper import ADBDumper
    from tools.xml_analyzer import XMLAnalyzer
    from tools.verify_tradescript import TradeScriptVerifier
"""

from .adb_debug_tool import ADBDebugTool
from .adb_dumper import ADBDumper
from .xml_analyzer import XMLAnalyzer
from .verify_tradescript import TradeScriptVerifier

__all__ = ['ADBDebugTool', 'ADBDumper', 'XMLAnalyzer', 'TradeScriptVerifier']
