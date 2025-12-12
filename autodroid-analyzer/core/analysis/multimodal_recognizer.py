"""多模态页面识别器 - 简化实现"""

import os
import json
from pathlib import Path
from typing import Dict, List, Optional, Any


class MultiModalPageRecognizer:
    """多模态页面识别器简化实现"""
    
    def __init__(self, device_id: str, config: Dict[str, Any] = None):
        """
        初始化多模态页面识别器
        
        Args:
            device_id: 设备ID
            config: 配置参数
        """
        self.device_id = device_id
        self.config = config or {}
        self.analysis_modes = {
            "uiautomator2": True,
            "screenshot": True,
            "user_monitoring": True
        }
        
    def set_analysis_modes(self, modes: Dict[str, bool]):
        """设置分析模式"""
        self.analysis_modes.update(modes)
        print(f"🔧 设置分析模式: {modes}")
    
    def analyze_page_multimodal(self, 
                               screenshot_path: str, 
                               current_app: str, 
                               ui_hierarchy_path: str) -> Dict[str, Any]:
        """
        多模态页面分析
        
        Args:
            screenshot_path: 截图文件路径
            current_app: 当前应用包名
            ui_hierarchy_path: UI层次结构文件路径
            
        Returns:
            分析结果字典
        """
        try:
            print(f"🔍 开始多模态页面分析")
            print(f"   - 截图: {screenshot_path}")
            print(f"   - 应用: {current_app}")
            print(f"   - UI文件: {ui_hierarchy_path}")
            
            # 检查文件是否存在
            screenshot_file = Path(screenshot_path)
            ui_file = Path(ui_hierarchy_path)
            
            if not screenshot_file.exists():
                return {"error": f"截图文件不存在: {screenshot_path}"}
            
            if not ui_file.exists():
                return {"error": f"UI层次结构文件不存在: {ui_hierarchy_path}"}
            
            # 读取UI层次结构文件
            try:
                with open(ui_file, 'r', encoding='utf-8') as f:
                    ui_content = f.read()
            except Exception as e:
                return {"error": f"读取UI文件失败: {e}"}
            
            # 解析UI层次结构（简化实现）
            ui_elements = self._parse_ui_hierarchy(ui_content)
            
            # 生成页面特征
            combined_features = self._generate_combined_features(ui_elements, screenshot_path)
            
            # 生成分析结果
            analysis_result = {
                "ui_elements": ui_elements,
                "combined_features": combined_features,
                "page_type": "unknown",
                "confidence": 0.8,
                "analysis_modes": self.analysis_modes
            }
            
            print(f"✅ 多模态分析完成，发现 {len(ui_elements)} 个UI元素")
            return analysis_result
            
        except Exception as e:
            print(f"❌ 多模态页面分析失败: {e}")
            return {"error": str(e)}
    
    def _parse_ui_hierarchy(self, ui_content: str) -> List[Dict[str, Any]]:
        """解析UI层次结构（简化实现）"""
        elements = []
        
        try:
            # 简单的XML解析（假设是XML格式）
            lines = ui_content.split('\n')
            for i, line in enumerate(lines):
                line = line.strip()
                if not line or line.startswith('<?xml') or line.startswith('</'):
                    continue
                    
                # 提取元素信息（简化实现）
                if 'bounds=' in line and 'class=' in line:
                    element_info = {
                        "index": len(elements),
                        "class": self._extract_attribute(line, 'class'),
                        "text": self._extract_attribute(line, 'text'),
                        "resource_id": self._extract_attribute(line, 'resource-id'),
                        "bounds": self._extract_attribute(line, 'bounds'),
                        "clickable": self._extract_attribute(line, 'clickable', 'false').lower() == 'true',
                        "enabled": self._extract_attribute(line, 'enabled', 'true').lower() == 'true',
                        "visible": self._extract_attribute(line, 'visible', 'true').lower() == 'true'
                    }
                    elements.append(element_info)
        except Exception as e:
            print(f"⚠️ UI解析警告: {e}")
        
        return elements
    
    def _extract_attribute(self, line: str, attr_name: str, default: str = '') -> str:
        """从XML行中提取属性值"""
        try:
            start_idx = line.find(f'{attr_name}="')
            if start_idx == -1:
                return default
            
            start_idx += len(attr_name) + 2  # 跳过属性名和="
            end_idx = line.find('"', start_idx)
            
            if end_idx == -1:
                return default
            
            return line[start_idx:end_idx]
        except:
            return default
    
    def _generate_combined_features(self, ui_elements: List[Dict[str, Any]], screenshot_path: str) -> Dict[str, Any]:
        """生成组合特征"""
        try:
            # 统计元素信息
            element_count = len(ui_elements)
            clickable_count = sum(1 for elem in ui_elements if elem.get('clickable', False))
            text_count = sum(1 for elem in ui_elements if elem.get('text', '').strip())
            
            # 生成页面特征
            features = {
                "element_count": element_count,
                "clickable_elements": clickable_count,
                "text_elements": text_count,
                "activity_name": "unknown",
                "page_title": "未知页面",
                "screenshot_path": screenshot_path,
                "analysis_timestamp": self._get_current_timestamp()
            }
            
            # 尝试推断页面类型
            if text_count > 10 and clickable_count > 5:
                features["page_title"] = "列表页面"
            elif text_count > 20 and clickable_count < 3:
                features["page_title"] = "详情页面"
            elif clickable_count > 8:
                features["page_title"] = "导航页面"
                
            return features
            
        except Exception as e:
            print(f"⚠️ 特征生成警告: {e}")
            return {
                "element_count": len(ui_elements),
                "activity_name": "unknown",
                "page_title": "未知页面",
                "screenshot_path": screenshot_path
            }
    
    def _get_current_timestamp(self) -> str:
        """获取当前时间戳"""
        import time
        return time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())