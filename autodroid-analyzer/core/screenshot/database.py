"""
截屏模块数据库服务类
"""

import json
from typing import List, Optional, Dict, Any
from peewee import DoesNotExist

from core.database.base import BaseDatabase
from core.database.models import Screenshot, PageElement, Apk


class ScreenshotDatabase(BaseDatabase):
    """截屏模块数据库服务类"""
    
    def __init__(self):
        """初始化截屏数据库服务"""
        super().__init__()
    
    def save_screenshot(self, screenshot_data: Dict[str, Any]) -> Optional[Screenshot]:
        """保存截屏信息"""
        try:
            with Screenshot._meta.database.atomic():
                screenshot = Screenshot.create(
                    id=screenshot_data['screenshot_id'],
                    apk=screenshot_data['apk_id'],
                    timestamp=screenshot_data['timestamp'],
                    file_path=screenshot_data['file_path'],
                    page_title=screenshot_data.get('page_title'),
                    analysis_status=screenshot_data.get('analysis_status', 'pending')
                )
                
                # 更新APK的截屏计数
                apk = Apk.get(Apk.id == screenshot_data['apk_id'])
                Apk.update(total_screenshots=apk.total_screenshots + 1).where(
                    Apk.id == screenshot_data['apk_id']
                ).execute()
                
                return screenshot
                
        except Exception as e:
            print(f"保存截屏失败: {str(e)}")
            return None
    
    def get_screenshot(self, screenshot_id: str) -> Optional[Screenshot]:
        """获取特定截屏信息"""
        try:
            return Screenshot.get(Screenshot.id == screenshot_id)
        except DoesNotExist:
            return None
    
    def get_screenshots_by_apk(self, apk_id: str, limit: int = 50) -> List[Screenshot]:
        """获取特定APK的截屏列表"""
        try:
            return list(Screenshot
                       .select()
                       .where(Screenshot.apk == apk_id)
                       .order_by(Screenshot.timestamp.desc())
                       .limit(limit))
        except Exception as e:
            print(f"获取APK截屏列表失败: {str(e)}")
            return []
    
    def get_recent_screenshots(self, limit: int = 20) -> List[Screenshot]:
        """获取最近的截屏"""
        try:
            return list(Screenshot
                       .select()
                       .order_by(Screenshot.timestamp.desc())
                       .limit(limit))
        except Exception as e:
            print(f"获取最近截屏失败: {str(e)}")
            return []
    
    def update_screenshot(self, screenshot_id: str, update_data: Dict[str, Any]) -> bool:
        """更新截屏信息"""
        try:
            valid_fields = {"page_title", "analysis_status"}
            update_fields = {k: v for k, v in update_data.items() if k in valid_fields and v is not None}
            
            if not update_fields:
                return False
            
            query = Screenshot.update(**update_fields).where(Screenshot.id == screenshot_id)
            return query.execute() > 0
            
        except Exception as e:
            print(f"更新截屏失败: {str(e)}")
            return False
    
    def delete_screenshot(self, screenshot_id: str) -> bool:
        """删除截屏记录"""
        try:
            # 先获取截屏信息以更新APK计数
            screenshot = self.get_screenshot(screenshot_id)
            if screenshot:
                apk_id = screenshot.apk.id
                
                # 删除截屏（级联删除关联的页面元素）
                deleted_count = Screenshot.delete().where(Screenshot.id == screenshot_id).execute()
                
                if deleted_count > 0:
                    # 更新APK的截屏计数
                    apk = Apk.get(Apk.id == apk_id)
                    new_count = max(0, apk.total_screenshots - 1)
                    Apk.update(total_screenshots=new_count).where(Apk.id == apk_id).execute()
                    return True
            
            return False
        except Exception as e:
            print(f"删除截屏失败: {str(e)}")
            return False
    
    def save_page_element(self, element_data: Dict[str, Any]) -> Optional[PageElement]:
        """保存页面元素"""
        try:
            with PageElement._meta.database.atomic():
                element = PageElement.create(
                    id=element_data['element_id'],
                    screenshot=element_data['screenshot_id'],
                    element_type=element_data['element_type'],
                    text_content=element_data.get('text_content'),
                    bounds=json.dumps(element_data.get('bounds', {}), ensure_ascii=False),
                    importance=element_data.get('importance', 3),
                    custom_tags=json.dumps(element_data.get('custom_tags', []), ensure_ascii=False)
                )
                return element
        except Exception as e:
            print(f"保存页面元素失败: {str(e)}")
            return None
    
    def get_page_element(self, element_id: str) -> Optional[PageElement]:
        """获取特定页面元素"""
        try:
            return PageElement.get(PageElement.id == element_id)
        except DoesNotExist:
            return None
    
    def get_page_elements_by_screenshot(self, screenshot_id: str) -> List[PageElement]:
        """获取特定截屏的页面元素"""
        try:
            return list(PageElement
                       .select()
                       .where(PageElement.screenshot == screenshot_id)
                       .order_by(PageElement.importance.desc(), PageElement.element_type))
        except Exception as e:
            print(f"获取页面元素失败: {str(e)}")
            return []
    
    def update_page_element(self, element_id: str, update_data: Dict[str, Any]) -> bool:
        """更新页面元素"""
        try:
            valid_fields = {"element_type", "text_content", "bounds", "importance", "custom_tags"}
            update_fields = {}
            
            for field in valid_fields:
                if field in update_data and update_data[field] is not None:
                    if field in ['bounds', 'custom_tags']:
                        update_fields[field] = json.dumps(update_data[field], ensure_ascii=False)
                    else:
                        update_fields[field] = update_data[field]
            
            if not update_fields:
                return False
            
            query = PageElement.update(**update_fields).where(PageElement.id == element_id)
            return query.execute() > 0
            
        except Exception as e:
            print(f"更新页面元素失败: {str(e)}")
            return False
    
    def delete_page_element(self, element_id: str) -> bool:
        """删除页面元素"""
        try:
            deleted_count = PageElement.delete().where(PageElement.id == element_id).execute()
            return deleted_count > 0
        except Exception as e:
            print(f"删除页面元素失败: {str(e)}")
            return False
    
    def get_elements_by_type(self, screenshot_id: str, element_type: str) -> List[PageElement]:
        """按类型获取页面元素"""
        try:
            return list(PageElement
                       .select()
                       .where((PageElement.screenshot == screenshot_id) & 
                              (PageElement.element_type == element_type))
                       .order_by(PageElement.importance.desc()))
        except Exception as e:
            print(f"按类型获取页面元素失败: {str(e)}")
            return []
    
    def get_important_elements(self, screenshot_id: str, min_importance: int = 4) -> List[PageElement]:
        """获取重要页面元素"""
        try:
            return list(PageElement
                       .select()
                       .where((PageElement.screenshot == screenshot_id) & 
                              (PageElement.importance >= min_importance))
                       .order_by(PageElement.importance.desc()))
        except Exception as e:
            print(f"获取重要页面元素失败: {str(e)}")
            return []
    
    def search_screenshots(self, **kwargs) -> List[Screenshot]:
        """搜索截屏"""
        try:
            query = Screenshot.select()
            
            if kwargs.get('apk_id'):
                query = query.where(Screenshot.apk == kwargs['apk_id'])
            
            if kwargs.get('page_title'):
                query = query.where(Screenshot.page_title.contains(kwargs['page_title']))
            
            if kwargs.get('analysis_status'):
                query = query.where(Screenshot.analysis_status == kwargs['analysis_status'])
            
            # 时间范围查询
            if kwargs.get('start_timestamp'):
                query = query.where(Screenshot.timestamp >= kwargs['start_timestamp'])
            
            if kwargs.get('end_timestamp'):
                query = query.where(Screenshot.timestamp <= kwargs['end_timestamp'])
            
            # 应用分页
            limit = kwargs.get('limit', 50)
            offset = kwargs.get('offset', 0)
            query = query.limit(limit).offset(offset)
            
            return list(query.order_by(Screenshot.timestamp.desc()))
            
        except Exception as e:
            print(f"搜索截屏失败: {str(e)}")
            return []
    
    def get_screenshot_count_by_apk(self, apk_id: str) -> int:
        """获取特定APK的截屏数量"""
        try:
            return Screenshot.select().where(Screenshot.apk == apk_id).count()
        except Exception as e:
            print(f"获取截屏数量失败: {str(e)}")
            return 0