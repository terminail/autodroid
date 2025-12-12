"""
截图管理模块
包含截图捕获、页面分析、元素识别等功能
"""

from .screenshot_manager import ScreenshotManager, ScreenshotInfo
from .page_analyzer import PageAnalyzer
from .page_recognizer import PageRecognizer

__all__ = [
    'ScreenshotManager',
    'ScreenshotInfo', 
    'PageAnalyzer',
    'PageRecognizer'
]