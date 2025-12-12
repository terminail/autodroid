"""
截屏管理服务类 - 按照server-database-model模式实现
"""

import json
import os
from typing import Dict, List, Optional, Any

from .database import ScreenshotDatabase
from core.database.models import Screenshot, PageElement


class ScreenshotManager:
    """截屏管理服务类"""
    
    def __init__(self):
        """初始化截屏管理服务"""
        self.db = ScreenshotDatabase()
    
    def save_screenshot(self, screenshot_data: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """保存截屏信息"""
        screenshot = self.db.save_screenshot(screenshot_data)
        if screenshot:
            return self._screenshot_to_dict(screenshot)
        return None
    
    def get_screenshot(self, screenshot_id: str) -> Optional[Dict[str, Any]]:
        """获取特定截屏信息"""
        screenshot = self.db.get_screenshot(screenshot_id)
        if screenshot:
            return self._screenshot_to_dict(screenshot)
        return None
    
    def get_screenshots_by_apk(self, apk_id: str, limit: int = 50) -> List[Dict[str, Any]]:
        """获取特定APK的截屏列表"""
        screenshots = self.db.get_screenshots_by_apk(apk_id, limit)
        return [self._screenshot_to_dict(screenshot) for screenshot in screenshots]
    
    def get_recent_screenshots(self, limit: int = 20) -> List[Dict[str, Any]]:
        """获取最近的截屏"""
        screenshots = self.db.get_recent_screenshots(limit)
        return [self._screenshot_to_dict(screenshot) for screenshot in screenshots]
    
    def update_screenshot(self, screenshot_id: str, update_data: Dict[str, Any]) -> bool:
        """更新截屏信息"""
        return self.db.update_screenshot(screenshot_id, update_data)
    
    def delete_screenshot(self, screenshot_id: str) -> bool:
        """删除截屏记录"""
        return self.db.delete_screenshot(screenshot_id)
    
    def save_page_element(self, element_data: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """保存页面元素"""
        element = self.db.save_page_element(element_data)
        if element:
            return self._page_element_to_dict(element)
        return None
    
    def get_page_element(self, element_id: str) -> Optional[Dict[str, Any]]:
        """获取特定页面元素"""
        element = self.db.get_page_element(element_id)
        if element:
            return self._page_element_to_dict(element)
        return None
    
    def get_page_elements_by_screenshot(self, screenshot_id: str) -> List[Dict[str, Any]]:
        """获取特定截屏的页面元素"""
        elements = self.db.get_page_elements_by_screenshot(screenshot_id)
        return [self._page_element_to_dict(element) for element in elements]
    
    def update_page_element(self, element_id: str, update_data: Dict[str, Any]) -> bool:
        """更新页面元素"""
        return self.db.update_page_element(element_id, update_data)
    
    def delete_page_element(self, element_id: str) -> bool:
        """删除页面元素"""
        return self.db.delete_page_element(element_id)
    
    def get_elements_by_type(self, screenshot_id: str, element_type: str) -> List[Dict[str, Any]]:
        """按类型获取页面元素"""
        elements = self.db.get_elements_by_type(screenshot_id, element_type)
        return [self._page_element_to_dict(element) for element in elements]
    
    def get_important_elements(self, screenshot_id: str, min_importance: int = 4) -> List[Dict[str, Any]]:
        """获取重要页面元素"""
        elements = self.db.get_important_elements(screenshot_id, min_importance)
        return [self._page_element_to_dict(element) for element in elements]
    
    def analyze_screenshot(self, screenshot_id: str) -> Dict[str, Any]:
        """分析截屏内容"""
        try:
            screenshot = self.get_screenshot(screenshot_id)
            if not screenshot:
                return {"error": "截屏不存在"}
            
            # 检查截屏文件是否存在
            if not os.path.exists(screenshot['file_path']):
                return {"error": "截屏文件不存在"}
            
            # 这里可以集成页面分析器
            # 例如：调用page_analyzer.py中的功能
            
            # 更新分析状态
            self.update_screenshot(screenshot_id, {"analysis_status": "analyzed"})
            
            return {
                "screenshot_id": screenshot_id,
                "status": "analyzed",
                "elements_count": len(self.get_page_elements_by_screenshot(screenshot_id))
            }
            
        except Exception as e:
            print(f"分析截屏失败: {e}")
            return {"error": str(e)}
    
    def recognize_page_structure(self, screenshot_id: str) -> Dict[str, Any]:
        """识别页面结构"""
        try:
            screenshot = self.get_screenshot(screenshot_id)
            if not screenshot:
                return {"error": "截屏不存在"}
            
            # 这里可以集成页面识别器
            # 例如：调用page_recognizer.py中的功能
            
            # 模拟识别结果
            recognition_result = {
                "screenshot_id": screenshot_id,
                "page_title": screenshot.get('page_title', '未知页面'),
                "layout_type": "LinearLayout",
                "widgets_count": 15,
                "text_elements": 8,
                "button_elements": 4,
                "input_elements": 2,
                "image_elements": 1
            }
            
            return recognition_result
            
        except Exception as e:
            print(f"识别页面结构失败: {e}")
            return {"error": str(e)}
    
    def _screenshot_to_dict(self, screenshot: Screenshot) -> Dict[str, Any]:
        """将Screenshot模型转换为字典"""
        return {
            'screenshot_id': screenshot.id,
            'apk_id': screenshot.apk.id if screenshot.apk else None,
            'timestamp': screenshot.timestamp.isoformat() if screenshot.timestamp else None,
            'file_path': screenshot.file_path,
            'page_title': screenshot.page_title,
            'analysis_status': screenshot.analysis_status
        }
    
    def _page_element_to_dict(self, element: PageElement) -> Dict[str, Any]:
        """将PageElement模型转换为字典"""
        return {
            'element_id': element.id,
            'screenshot_id': element.screenshot.id if element.screenshot else None,
            'element_type': element.element_type,
            'text_content': element.text_content,
            'bounds': json.loads(element.bounds) if element.bounds else {},
            'importance': element.importance,
            'custom_tags': json.loads(element.custom_tags) if element.custom_tags else []
        }


def main():
    """主函数 - 用于测试"""
    manager = ScreenshotManager()
    
    # 测试截屏保存
    screenshot_data = {
        'screenshot_id': 'test_screenshot_001',
        'apk_id': 'test_apk_001',
        'timestamp': '2024-01-01T10:00:00',
        'file_path': '/path/to/screenshot.png',
        'page_title': '测试页面',
        'analysis_status': 'pending'
    }
    
    screenshot = manager.save_screenshot(screenshot_data)
    if screenshot:
        print(f"✅ 截屏保存成功: {screenshot}")
    else:
        print("❌ 截屏保存失败")


if __name__ == "__main__":
    main()