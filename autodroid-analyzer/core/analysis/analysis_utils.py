"""分析工具模块 - 包含元素匹配、边界相似性判断等分析相关工具方法"""

from typing import Dict, List, Any, Tuple
import numpy as np
import xml.etree.ElementTree as ET
import re


class AnalysisUtils:
    """分析工具类"""
    
    @staticmethod
    def elements_match(elem1: Dict[str, Any], elem2: Dict[str, Any]) -> bool:
        """判断两个元素是否匹配（改进版：多属性综合匹配）"""
        # 如果两个元素完全相同，直接返回True
        if elem1 == elem2:
            return True
        
        # 计算匹配分数
        match_score = 0
        total_weight = 0
        
        # 1. resource_id匹配（权重最高）
        if elem1.get('resource_id') and elem2.get('resource_id'):
            if elem1['resource_id'] == elem2['resource_id']:
                match_score += 10
            total_weight += 10
        
        # 2. 文本匹配（权重高）
        if elem1.get('text') and elem2.get('text'):
            if elem1['text'] == elem2['text']:
                match_score += 8
            total_weight += 8
        
        # 3. content_desc匹配（权重中）
        if elem1.get('content_desc') and elem2.get('content_desc'):
            if elem1['content_desc'] == elem2['content_desc']:
                match_score += 6
            total_weight += 6
        
        # 4. class_name匹配（权重中）
        if elem1.get('class_name') and elem2.get('class_name'):
            if elem1['class_name'] == elem2['class_name']:
                match_score += 5
            total_weight += 5
        
        # 5. bounds位置匹配（权重中）
        if elem1.get('bounds') and elem2.get('bounds'):
            if AnalysisUtils.bounds_similar(elem1['bounds'], elem2['bounds']):
                match_score += 7
            total_weight += 7
        
        # 6. 可点击性匹配（权重低）
        if elem1.get('clickable') == elem2.get('clickable'):
            match_score += 2
            total_weight += 2
        
        # 7. 可编辑性匹配（权重低）
        if elem1.get('editable') == elem2.get('editable'):
            match_score += 2
            total_weight += 2
        
        # 如果没有任何可匹配的属性，返回False
        if total_weight == 0:
            return False
        
        # 计算匹配率，超过60%认为匹配
        match_ratio = match_score / total_weight
        return match_ratio >= 0.6
    
    @staticmethod
    def bounds_similar(bounds1: List[int], bounds2: List[int], threshold: int = 50) -> bool:
        """判断两个bounds是否相似"""
        if len(bounds1) != 4 or len(bounds2) != 4:
            return False
        
        # 计算中心点距离
        center1_x = (bounds1[0] + bounds1[2]) // 2
        center1_y = (bounds1[1] + bounds1[3]) // 2
        center2_x = (bounds2[0] + bounds2[2]) // 2
        center2_y = (bounds2[1] + bounds2[3]) // 2
        
        distance = abs(center1_x - center2_x) + abs(center1_y - center2_y)
        return distance <= threshold
    
    @staticmethod
    def calculate_element_importance(element: Dict[str, Any]) -> float:
        """计算元素重要性分数"""
        importance = 0.0
        
        # 1. 文本内容（权重高）
        if element.get('text'):
            text = element['text'].strip()
            if len(text) > 0:
                importance += 3.0
                # 长文本可能更重要
                if len(text) > 10:
                    importance += 1.0
        
        # 2. 资源ID（权重高）
        if element.get('resource_id'):
            importance += 2.5
        
        # 3. 内容描述（权重中）
        if element.get('content_desc'):
            importance += 2.0
        
        # 4. 可点击性（权重中）
        if element.get('clickable', False):
            importance += 1.5
        
        # 5. 可编辑性（权重中）
        if element.get('editable', False):
            importance += 1.5
        
        # 6. 类名包含特定关键词（权重低）
        class_name = element.get('class_name', '').lower()
        important_classes = ['button', 'text', 'edit', 'input', 'menu', 'list']
        for important_class in important_classes:
            if important_class in class_name:
                importance += 0.5
        
        # 7. 元素大小（权重低）
        if element.get('bounds'):
            bounds = element['bounds']
            if len(bounds) == 4:
                width = bounds[2] - bounds[0]
                height = bounds[3] - bounds[1]
                area = width * height
                # 中等大小的元素可能更重要
                if 1000 < area < 10000:
                    importance += 0.5
        
        return importance
    
    @staticmethod
    def get_interactive_elements(elements: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """获取可交互元素列表"""
        interactive_elements = []
        
        for element in elements:
            # 计算元素重要性
            importance = AnalysisUtils.calculate_element_importance(element)
            
            # 只有重要性达到一定阈值的元素才被认为是可交互的
            if importance >= 2.0:
                element['importance'] = importance
                interactive_elements.append(element)
        
        # 按重要性排序
        interactive_elements.sort(key=lambda x: x.get('importance', 0), reverse=True)
        
        return interactive_elements
    
    @staticmethod
    def find_best_match_element(target_element: Dict[str, Any], 
                               candidate_elements: List[Dict[str, Any]]) -> Tuple[Dict[str, Any], float]:
        """在候选元素中寻找最佳匹配元素"""
        best_match = None
        best_score = 0.0
        
        for candidate in candidate_elements:
            # 计算匹配分数
            score = AnalysisUtils.calculate_element_similarity(target_element, candidate)
            
            if score > best_score:
                best_score = score
                best_match = candidate
        
        return best_match, best_score
    
    @staticmethod
    def parse_ui_hierarchy(ui_hierarchy: str) -> List[Dict[str, Any]]:
        """解析UI层次结构XML，提取页面元素"""
        elements = []
        
        try:
            # 使用正则表达式匹配UI元素节点
            element_pattern = r'<node[^>]*?>(?:.*?</node>)?'
            node_matches = re.findall(element_pattern, ui_hierarchy, re.DOTALL)
            
            for node_xml in node_matches:
                element = AnalysisUtils._parse_ui_element(node_xml)
                if element:
                    elements.append(element)
                    
        except Exception as e:
            print(f"解析UI层次结构失败: {e}")
        
        return elements
    
    @staticmethod
    def _parse_ui_element(node_xml: str) -> Dict[str, Any]:
        """解析单个UI元素节点"""
        try:
            element = {}
            
            # 提取属性
            attributes = re.findall(r'(\w+)="([^"]*)"', node_xml)
            for attr_name, attr_value in attributes:
                element[attr_name] = attr_value
            
            # 计算元素重要性
            element['importance'] = AnalysisUtils.calculate_element_importance(element)
            
            return element
            
        except Exception as e:
            print(f"解析UI元素节点失败: {e}")
            return None
    
    @staticmethod
    def calculate_element_similarity(elem1: Dict[str, Any], elem2: Dict[str, Any]) -> float:
        """计算两个元素的相似度分数"""
        similarity = 0.0
        total_weight = 0
        
        # 1. resource_id匹配（权重最高）
        if elem1.get('resource_id') and elem2.get('resource_id'):
            if elem1['resource_id'] == elem2['resource_id']:
                similarity += 10
            total_weight += 10
        
        # 2. 文本匹配（权重高）
        if elem1.get('text') and elem2.get('text'):
            if elem1['text'] == elem2['text']:
                similarity += 8
            total_weight += 8
        
        # 3. content_desc匹配（权重中）
        if elem1.get('content_desc') and elem2.get('content_desc'):
            if elem1['content_desc'] == elem2['content_desc']:
                similarity += 6
            total_weight += 6
        
        # 4. class_name匹配（权重中）
        if elem1.get('class_name') and elem2.get('class_name'):
            if elem1['class_name'] == elem2['class_name']:
                similarity += 5
            total_weight += 5
        
        # 5. bounds位置匹配（权重中）
        if elem1.get('bounds') and elem2.get('bounds'):
            if AnalysisUtils.bounds_similar(elem1['bounds'], elem2['bounds']):
                similarity += 7
            total_weight += 7
        
        # 6. 可点击性匹配（权重低）
        if elem1.get('clickable') == elem2.get('clickable'):
            similarity += 2
            total_weight += 2
        
        # 7. 可编辑性匹配（权重低）
        if elem1.get('editable') == elem2.get('editable'):
            similarity += 2
            total_weight += 2
        
        # 如果没有任何可匹配的属性，返回0
        if total_weight == 0:
            return 0.0
        
        # 计算相似度比率
        return similarity / total_weight
    
    @staticmethod
    def extract_page_title(ui_hierarchy: str) -> str:
        """从UI层次结构中提取页面标题"""
        try:
            # 查找包含title或text的节点
            patterns = [
                r'text="([^"]+)"[^>]*class=".*[Tt]itle.*"',
                r'text="([^"]+)"[^>]*resource-id=".*[Tt]itle.*"',
                r'text="([^"]+)"[^>]*class=".*[Tt]ext[Vv]iew.*"',
                r'text="([^"]+)"[^>]*class=".*[Ll]abel.*"'
            ]
            
            for pattern in patterns:
                matches = re.findall(pattern, ui_hierarchy)
                if matches:
                    # 返回第一个非空文本
                    for text in matches:
                        if text.strip():
                            return text.strip()
            
            # 如果没有找到标题，返回应用名称
            return "应用页面"
            
        except Exception as e:
            print(f"提取页面标题失败: {e}")
            return "未知页面"
    
    @staticmethod
    def extract_activity_name(ui_hierarchy: str) -> str:
        """从UI层次结构中提取Activity名称"""
        try:
            # 查找包含activity信息的节点
            pattern = r'package="([^"]+)" activity="([^"]+)"'
            match = re.search(pattern, ui_hierarchy)
            if match:
                return match.group(2)
        except Exception as e:
            print(f"提取Activity名称失败: {e}")
        
        return "unknown"
    
    @staticmethod
    def filter_important_elements(elements: List[Dict[str, Any]], 
                                 importance_threshold: float = 2.0) -> List[Dict[str, Any]]:
        """过滤重要元素"""
        important_elements = []
        
        for element in elements:
            importance = element.get('importance', 0.0)
            if importance >= importance_threshold:
                important_elements.append(element)
        
        # 按重要性排序
        important_elements.sort(key=lambda x: x.get('importance', 0), reverse=True)
        
        return important_elements
    
    @staticmethod
    def group_elements_by_type(elements: List[Dict[str, Any]]) -> Dict[str, List[Dict[str, Any]]]:
        """按类型分组元素"""
        grouped_elements = {}
        
        for element in elements:
            class_name = element.get('class_name', 'unknown')
            
            # 简化类名
            if 'button' in class_name.lower():
                group_key = 'buttons'
            elif 'text' in class_name.lower() and 'edit' in class_name.lower():
                group_key = 'input_fields'
            elif 'text' in class_name.lower():
                group_key = 'text_views'
            elif 'image' in class_name.lower():
                group_key = 'image_views'
            elif 'list' in class_name.lower() or 'recycler' in class_name.lower():
                group_key = 'lists'
            else:
                group_key = 'others'
            
            if group_key not in grouped_elements:
                grouped_elements[group_key] = []
            grouped_elements[group_key].append(element)
        
        return grouped_elements