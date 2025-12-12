"""
页面分析模块
负责页面识别、元素分析和页面变化检测
"""

import time
import hashlib
import re
from typing import List, Dict, Any, Optional, Tuple
from dataclasses import dataclass


@dataclass
class PageNode:
    """页面节点数据类"""
    page_id: str
    title: str
    activity_name: str
    element_count: int
    elements: List[Dict[str, Any]]


class PageAnalyzer:
    """页面分析器"""
    
    def __init__(self):
        self.page_history: List[PageNode] = []
    
    def parse_ui_hierarchy(self, ui_hierarchy: str) -> List[Dict[str, Any]]:
        """解析UI层次结构XML"""
        elements = []
        
        try:
            # 查找所有节点元素
            node_pattern = r'<node[^>]*>(.*?)</node>'
            nodes = re.findall(node_pattern, ui_hierarchy, re.DOTALL)
            
            for node_content in nodes:
                element = self._extract_element_attributes(node_content)
                
                # 只添加有意义的元素
                if element.get('text') or element.get('resource_id') or element.get('content_desc'):
                    elements.append(element)
            
        except Exception as e:
            print(f"❌ 解析UI层次结构失败: {e}")
        
        return elements
    
    def _extract_element_attributes(self, node_content: str) -> Dict[str, Any]:
        """提取元素属性"""
        element = {}
        
        # 提取text属性
        text_match = re.search(r'text="([^"]*)"', node_content)
        if text_match:
            element['text'] = text_match.group(1)
        
        # 提取resource-id属性
        resource_match = re.search(r'resource-id="([^"]*)"', node_content)
        if resource_match:
            element['resource_id'] = resource_match.group(1)
        
        # 提取content-desc属性
        desc_match = re.search(r'content-desc="([^"]*)"', node_content)
        if desc_match:
            element['content_desc'] = desc_match.group(1)
        
        # 提取class属性
        class_match = re.search(r'class="([^"]*)"', node_content)
        if class_match:
            element['class_name'] = class_match.group(1)
        
        # 提取clickable属性
        clickable_match = re.search(r'clickable="([^"]*)"', node_content)
        if clickable_match:
            element['clickable'] = clickable_match.group(1).lower() == 'true'
        
        # 提取editable属性
        editable_match = re.search(r'editable="([^"]*)"', node_content)
        if editable_match:
            element['editable'] = editable_match.group(1).lower() == 'true'
        
        # 提取bounds属性
        bounds_match = re.search(r'bounds="([^"]*)"', node_content)
        if bounds_match:
            bounds_str = bounds_match.group(1)
            bounds = re.findall(r'\d+', bounds_str)
            if len(bounds) == 4:
                element['bounds'] = [int(b) for b in bounds]
        
        return element
    
    def get_page_hash(self, ui_hierarchy: str) -> str:
        """获取页面哈希值（用于检测页面变化）"""
        try:
            return hashlib.md5(ui_hierarchy.encode()).hexdigest()
        except Exception as e:
            print(f"❌ 获取页面哈希失败: {e}")
            return ""
    
    def is_page_changed(self, current_hash: str, previous_hash: str, threshold: float = 0.8) -> bool:
        """检测页面是否发生变化"""
        if not current_hash or not previous_hash:
            return True
        
        # 简单的哈希比较（实际实现可能需要更复杂的相似度计算）
        return current_hash != previous_hash
    
    def get_interactive_elements(self, page_node: PageNode) -> List[Dict[str, Any]]:
        """获取可交互元素"""
        interactive_elements = []
        
        if not page_node.elements:
            return interactive_elements
        
        for element in page_node.elements:
            # 检查元素是否可交互
            if element.get('clickable', False) or element.get('editable', False):
                # 计算元素的重要性分数
                importance = self._calculate_element_importance(element)
                element['importance'] = importance
                interactive_elements.append(element)
        
        # 按重要性排序
        interactive_elements.sort(key=lambda x: x.get('importance', 0), reverse=True)
        
        return interactive_elements
    
    def _calculate_element_importance(self, element) -> float:
        """计算元素的重要性分数"""
        importance = 0.0
        
        # 基于文本内容的重要性
        text = element.get('text', '')
        if text:
            # 常见操作按钮文本
            important_texts = ['确定', '确认', '下一步', '继续', '登录', '注册', '搜索', '完成', '保存', '提交']
            if any(important in text for important in important_texts):
                importance += 0.8
            else:
                importance += 0.3
        
        # 基于元素类型的重要性
        class_name = element.get('class_name', '')
        if 'Button' in class_name:
            importance += 0.5
        elif 'EditText' in class_name:
            importance += 0.4
        elif 'TextView' in class_name:
            importance += 0.2
        
        # 基于可点击性
        if element.get('clickable', False):
            importance += 0.6
        
        # 基于可编辑性
        if element.get('editable', False):
            importance += 0.4
        
        return min(importance, 1.0)
    
    def elements_match(self, element1: Dict[str, Any], element2: Dict[str, Any]) -> bool:
        """判断两个元素是否匹配"""
        # 比较资源ID
        resource_id1 = element1.get('resource_id', '')
        resource_id2 = element2.get('resource_id', '')
        if resource_id1 and resource_id2 and resource_id1 == resource_id2:
            return True
        
        # 比较文本内容
        text1 = element1.get('text', '')
        text2 = element2.get('text', '')
        if text1 and text2 and text1 == text2:
            return True
        
        # 比较内容描述
        desc1 = element1.get('content_desc', '')
        desc2 = element2.get('content_desc', '')
        if desc1 and desc2 and desc1 == desc2:
            return True
        
        # 比较边界框（如果边界框相似，也认为是同一个元素）
        bounds1 = element1.get('bounds', [0, 0, 0, 0])
        bounds2 = element2.get('bounds', [0, 0, 0, 0])
        
        if len(bounds1) == 4 and len(bounds2) == 4:
            # 计算边界框中心点距离
            center1_x = (bounds1[0] + bounds1[2]) / 2
            center1_y = (bounds1[1] + bounds1[3]) / 2
            center2_x = (bounds2[0] + bounds2[2]) / 2
            center2_y = (bounds2[1] + bounds2[3]) / 2
            
            distance = ((center1_x - center2_x) ** 2 + (center1_y - center2_y) ** 2) ** 0.5
            
            # 如果中心点距离小于50像素，认为是同一个元素
            if distance < 50:
                return True
        
        return False
    
    def get_element_key(self, element) -> str:
        """获取元素的唯一标识键"""
        key_parts = []
        
        # 使用资源ID作为主要标识
        resource_id = element.get('resource_id', '')
        if resource_id:
            key_parts.append(resource_id)
        
        # 使用文本内容作为次要标识
        text = element.get('text', '')
        if text:
            key_parts.append(text)
        
        # 使用类名作为备用标识
        class_name = element.get('class_name', '')
        if class_name:
            key_parts.append(class_name)
        
        # 使用边界框作为最后标识
        bounds = element.get('bounds', [])
        if bounds:
            key_parts.append(str(bounds))
        
        return '_'.join(key_parts) if key_parts else str(id(element))
    
    def add_page_to_history(self, page_node: PageNode) -> None:
        """添加页面到历史记录"""
        self.page_history.append(page_node)
    
    def get_page_history(self) -> List[PageNode]:
        """获取页面历史记录"""
        return self.page_history.copy()
    
    def clear_page_history(self) -> None:
        """清空页面历史记录"""
        self.page_history.clear()