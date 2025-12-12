"""应用结构分析工具，用于建立页面关系图和操作图谱"""

import json
import time
import sys
import os
from dataclasses import dataclass, field
from typing import Dict, List, Set, Optional, Any, Callable, Tuple
from pathlib import Path
import networkx as nx
import matplotlib.pyplot as plt
from datetime import datetime

from core.device.device_manager import DeviceManager


@dataclass
class HumanAssistanceRequest:
    """人工协助请求"""
    reason: str
    expected_action: str
    required_data: Optional[Dict[str, Any]] = None
    timeout: int = 300  # 超时时间（秒）


@dataclass
class PageNode:
    """页面节点"""
    page_id: str
    app_name: str
    activity_name: str
    title: str = ""
    elements: List[Dict[str, Any]] = field(default_factory=list)
    element_count: int = 0  # 元素数量
    screenshot_path: str = ""
    ui_hierarchy: str = ""
    timestamp: float = field(default_factory=time.time)
    requires_human_assistance: bool = False  # 是否需要人工协助
    human_assistance_reason: Optional[str] = None  # 人工协助原因


@dataclass
class UserOperation:
    """用户操作记录"""
    operation_id: str
    timestamp: float
    action_type: str  # "click", "input", "swipe", "back", "tap"
    target_element: Optional[Dict[str, Any]]
    input_text: Optional[str]
    coordinates: Optional[Tuple[int, int]]
    source_page: str
    target_page: Optional[str]
    success: bool = True


@dataclass
class OperationEdge:
    """操作边"""
    source_page: str
    target_page: str
    action_type: str  # tap, swipe, back, etc.
    action_target: str  # element description or coordinates
    success_rate: float = 1.0
    timestamp: float = field(default_factory=time.time)
    requires_human_assistance: bool = False  # 是否需要人工协助
    
    # 操作图相关属性
    user_operations: List[UserOperation] = field(default_factory=list)
    operation_count: int = 0
    average_duration: float = 0.0


class OperationGraph:
    """操作图构建模块"""
    
    def __init__(self):
        self.graph = nx.DiGraph()
        self.user_operations: List[UserOperation] = []
        self.page_nodes: Dict[str, PageNode] = {}
        
    def add_page_node(self, page_node: PageNode):
        """添加页面节点"""
        self.page_nodes[page_node.page_id] = page_node
        self.graph.add_node(page_node.page_id, 
                           label=page_node.title or page_node.activity_name,
                           type="page",
                           element_count=len(page_node.elements))
    
    def add_operation_edge(self, source_page: str, target_page: str, 
                          action_type: str, action_target: str, 
                          user_operation: Optional[UserOperation] = None):
        """添加操作边"""
        edge_key = f"{source_page}->{target_page}"
        
        if self.graph.has_edge(source_page, target_page):
            # 更新现有边
            edge_data = self.graph[source_page][target_page]
            edge_data["operation_count"] += 1
            if user_operation:
                if "user_operations" not in edge_data:
                    edge_data["user_operations"] = []
                edge_data["user_operations"].append(user_operation)
        else:
            # 添加新边
            edge_data = {
                "action_type": action_type,
                "action_target": action_target,
                "operation_count": 1,
                "user_operations": [user_operation] if user_operation else []
            }
            self.graph.add_edge(source_page, target_page, **edge_data)
    
    def add_user_operation(self, user_operation: UserOperation):
        """添加用户操作记录"""
        self.user_operations.append(user_operation)
        
        # 如果操作有目标页面，添加到操作图中
        if user_operation.target_page:
            self.add_operation_edge(
                user_operation.source_page,
                user_operation.target_page,
                user_operation.action_type,
                str(user_operation.target_element or user_operation.coordinates),
                user_operation
            )
    
    def get_operation_paths(self, start_page: str, max_depth: int = 10) -> List[List[str]]:
        """获取操作路径"""
        paths = []
        
        def dfs(current_path: List[str], depth: int):
            if depth >= max_depth:
                return
                
            current_page = current_path[-1]
            neighbors = list(self.graph.successors(current_page))
            
            if not neighbors:
                paths.append(current_path.copy())
                return
                
            for neighbor in neighbors:
                current_path.append(neighbor)
                dfs(current_path, depth + 1)
                current_path.pop()
        
        dfs([start_page], 0)
        return paths
    
    def calculate_path_coverage(self) -> Dict[str, float]:
        """计算路径覆盖率"""
        total_pages = len(self.page_nodes)
        if total_pages == 0:
            return {"coverage": 0.0, "visited_pages": 0, "total_pages": 0}
        
        # 获取所有可达的页面
        visited_pages = set()
        for node in self.graph.nodes():
            if self.graph.in_degree(node) > 0 or self.graph.out_degree(node) > 0:
                visited_pages.add(node)
        
        coverage = len(visited_pages) / total_pages
        
        return {
            "coverage": coverage,
            "visited_pages": len(visited_pages),
            "total_pages": total_pages
        }
    
    def generate_mermaid_diagram(self) -> str:
        """生成Mermaid格式的操作图"""
        mermaid_lines = ["graph TD"]
        
        # 添加节点
        for page_id, page_node in self.page_nodes.items():
            label = page_node.title or page_node.activity_name
            mermaid_lines.append(f"    {page_id}[{label}]")
        
        # 添加边
        for source, target, edge_data in self.graph.edges(data=True):
            action_type = edge_data.get("action_type", "操作")
            action_target = edge_data.get("action_target", "")
            
            # 简化目标描述
            if len(action_target) > 20:
                action_target = action_target[:20] + "..."
            
            label = f"{action_type}: {action_target}"
            mermaid_lines.append(f"    {source} -->|{label}| {target}")
        
        return "\n".join(mermaid_lines)
    
    def visualize_operation_graph(self, output_path: str):
        """可视化操作图"""
        try:
            plt.figure(figsize=(12, 8))
            
            # 使用spring布局
            pos = nx.spring_layout(self.graph, k=1, iterations=50)
            
            # 绘制节点
            nx.draw_networkx_nodes(self.graph, pos, node_size=500, 
                                 node_color='lightblue', alpha=0.9)
            
            # 绘制边
            nx.draw_networkx_edges(self.graph, pos, edge_color='gray', 
                                 arrows=True, arrowsize=20)
            
            # 添加标签
            labels = {node: self.graph.nodes[node].get('label', node) 
                     for node in self.graph.nodes()}
            nx.draw_networkx_labels(self.graph, pos, labels, font_size=8)
            
            # 添加边标签
            edge_labels = {(u, v): f"{d.get('action_type', '操作')}" 
                          for u, v, d in self.graph.edges(data=True)}
            nx.draw_networkx_edge_labels(self.graph, pos, edge_labels, font_size=6)
            
            plt.title("应用操作图")
            plt.axis('off')
            plt.tight_layout()
            plt.savefig(output_path, dpi=300, bbox_inches='tight')
            plt.close()
            
        except Exception as e:
            print(f"操作图可视化失败: {e}")
    
    def get_operation_statistics(self) -> Dict[str, Any]:
        """获取操作统计信息"""
        if not self.user_operations:
            return {"total_operations": 0, "operation_types": {}}
        
        # 统计操作类型
        operation_types = {}
        for operation in self.user_operations:
            op_type = operation.action_type
            operation_types[op_type] = operation_types.get(op_type, 0) + 1
        
        # 计算平均操作间隔
        timestamps = sorted([op.timestamp for op in self.user_operations])
        intervals = []
        for i in range(1, len(timestamps)):
            intervals.append(timestamps[i] - timestamps[i-1])
        
        avg_interval = sum(intervals) / len(intervals) if intervals else 0
        
        return {
            "total_operations": len(self.user_operations),
            "operation_types": operation_types,
            "average_interval": avg_interval,
            "first_operation": min(timestamps) if timestamps else 0,
            "last_operation": max(timestamps) if timestamps else 0
        }


class AppAnalyzer:
    """应用分析器，用于自动分析应用结构和操作流程"""
    
    def __init__(self, device_id: Optional[str] = None, output_dir: str = "app_analysis", 
                 human_assistance_callback: Optional[Callable] = None):
        self.device_id = device_id
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # 人工协助回调函数
        self.human_assistance_callback = human_assistance_callback
        
        # 存储分析结果
        self.pages: Dict[str, PageNode] = {}
        self.operations: List[OperationEdge] = []
        self.current_page: Optional[PageNode] = None
        
        # 分析状态
        self.visited_pages: Set[str] = set()
        self.analysis_history: List[Dict[str, Any]] = []
        
        # 人工协助相关
        self.pending_human_assistance = False
        self.human_assistance_timeout = 300
        
        # 操作图构建模块
        self.operation_graph = OperationGraph()
        self.user_operations: List[UserOperation] = []
        self.monitoring_enabled = False
        
        # 设备管理器
        self.device_manager = DeviceManager(device_id) if device_id else None
    
    def analyze_app_structure(self, app_package: str, max_depth: int = 10, 
                              enable_human_assistance: bool = True) -> Dict[str, Any]:
        """
        分析应用结构
        
        Args:
            app_package: 应用包名
            max_depth: 最大分析深度
            enable_human_assistance: 是否启用人工协助
            
        Returns:
            应用结构分析结果
        """
        print(f"🚀 开始分析应用: {app_package}")
        print(f"📋 人工协助模式: {'启用' if enable_human_assistance else '禁用'}")
        
        # 启动应用
        self._launch_app(app_package)
        
        # 深度优先遍历应用页面
        self._dfs_analyze(app_package, max_depth)
        
        # 生成分析报告
        report = self._generate_report(app_package, max_depth)
        
        # 保存分析结果
        self._save_analysis_results(app_package, report)
        
        return report
    
    def request_human_assistance(self, reason: str, expected_action: str, 
                                required_data: Optional[Dict[str, Any]] = None) -> bool:
        """请求人工协助"""
        if not self.human_assistance_callback:
            print(f"⚠️ 人工协助请求被忽略（未设置回调函数）: {reason}")
            return False
        
        request = HumanAssistanceRequest(
            reason=reason,
            expected_action=expected_action,
            required_data=required_data,
            timeout=self.human_assistance_timeout
        )
        
        print(f"🆘 请求人工协助: {reason}")
        print(f"   期望操作: {expected_action}")
        
        try:
            self.pending_human_assistance = True
            result = self.human_assistance_callback(request)
            self.pending_human_assistance = False
            
            if result:
                print("✅ 人工协助完成")
            else:
                print("❌ 人工协助失败或超时")
            
            return result
            
        except Exception as e:
            print(f"❌ 人工协助异常: {e}")
            self.pending_human_assistance = False
            return False
    
    def _detect_human_assistance_required(self, page: PageNode) -> bool:
        """检测是否需要人工协助"""
        # 检测指纹登录页面
        if self._is_fingerprint_login_page(page):
            page.requires_human_assistance = True
            page.human_assistance_reason = "需要指纹登录"
            return True
        
        # 检测人脸识别页面
        if self._is_face_recognition_page(page):
            page.requires_human_assistance = True
            page.human_assistance_reason = "需要人脸识别"
            return True
        
        # 检测验证码页面
        if self._is_captcha_page(page):
            page.requires_human_assistance = True
            page.human_assistance_reason = "需要输入验证码"
            return True
        
        # 检测硬件操作页面
        if self._is_hardware_operation_page(page):
            page.requires_human_assistance = True
            page.human_assistance_reason = "需要硬件操作（如NFC、扫码等）"
            return True
        
        return False
    
    def _is_fingerprint_login_page(self, page: PageNode) -> bool:
        """检测是否为指纹登录页面"""
        # 通过页面标题、元素文本等特征检测
        fingerprint_keywords = ["指纹", "指纹登录", "指纹验证", "fingerprint", "touch id"]
        
        for keyword in fingerprint_keywords:
            if keyword.lower() in page.title.lower():
                return True
        
        # 检查页面元素
        if page.ui_hierarchy:
            for keyword in fingerprint_keywords:
                if keyword.lower() in page.ui_hierarchy.lower():
                    return True
        
        return False
    
    def _is_face_recognition_page(self, page: PageNode) -> bool:
        """检测是否为人脸识别页面"""
        face_keywords = ["人脸", "面部", "刷脸", "face", "facial"]
        
        for keyword in face_keywords:
            if keyword.lower() in page.title.lower():
                return True
        
        if page.ui_hierarchy:
            for keyword in face_keywords:
                if keyword.lower() in page.ui_hierarchy.lower():
                    return True
        
        return False
    
    def _is_captcha_page(self, page: PageNode) -> bool:
        """检测是否为验证码页面"""
        captcha_keywords = ["验证码", "captcha", "验证", "code"]
        
        for keyword in captcha_keywords:
            if keyword.lower() in page.title.lower():
                return True
        
        if page.ui_hierarchy:
            for keyword in captcha_keywords:
                if keyword.lower() in page.ui_hierarchy.lower():
                    return True
        
        return False
    
    def _is_hardware_operation_page(self, page: PageNode) -> bool:
        """检测是否为硬件操作页面"""
        hardware_keywords = ["nfc", "扫码", "二维码", "蓝牙", "nfc", "qr", "scan"]
        
        for keyword in hardware_keywords:
            if keyword.lower() in page.title.lower():
                return True
        
        if page.ui_hierarchy:
            for keyword in hardware_keywords:
                if keyword.lower() in page.ui_hierarchy.lower():
                    return True
        
        return False
    
    def _launch_app(self, app_package: str):
        """启动应用并记录初始页面"""
        if self.device_manager:
            self.device_manager.launch_app(app_package)
        time.sleep(3)  # 等待应用启动
        
        # 记录初始页面
        self._capture_current_page("launch")
    
    def _capture_current_page(self, action_type: str = "unknown") -> PageNode:
        """捕获当前页面信息"""
        try:
            # 获取当前应用信息
            current_app = self.device_manager.get_current_app() if self.device_manager else "unknown"
            
            # 获取UI层次结构XML
            ui_hierarchy = self._get_ui_hierarchy()
            
            # 从UI层次结构中提取页面信息
            activity_name = self._extract_activity_name_from_ui_hierarchy(ui_hierarchy)
            title = self._extract_page_title_from_ui_hierarchy(ui_hierarchy)
            
            # 生成页面ID
            page_id = f"{current_app}_{int(time.time())}"
            
            # 保存截图
            screenshot_path = self.output_dir / f"{page_id}.png"
            if self.device_manager:
                self.device_manager.take_screenshot(str(screenshot_path))
            
            # 保存UI层次结构
            ui_hierarchy_path = self.output_dir / f"{page_id}_ui.xml"
            with open(ui_hierarchy_path, "w", encoding="utf-8") as f:
                f.write(ui_hierarchy)
            
            # 从UI层次结构中提取元素
            elements = self._extract_elements_from_ui_hierarchy(ui_hierarchy)
            
            # 创建页面节点
            page_node = PageNode(
                page_id=page_id,
                app_name=current_app,
                activity_name=activity_name,
                title=title,
                ui_hierarchy=ui_hierarchy,
                screenshot_path=str(screenshot_path)
            )
            
            # 设置元素信息
            page_node.elements = elements
            
            # 更新当前页面
            if self.current_page:
                # 记录操作边
                operation = OperationEdge(
                    source_page=self.current_page.page_id,
                    target_page=page_node.page_id,
                    action_type=action_type,
                    action_target="navigation"
                )
                self.operations.append(operation)
            
            self.current_page = page_node
            self.pages[page_id] = page_node
            self.visited_pages.add(page_id)
            
            return page_node
            
        except Exception as e:
            print(f"❌ 捕获页面信息失败: {e}")
            # 返回一个基本的页面节点
            page_node = PageNode(
                page_id="unknown",
                app_name="unknown",
                activity_name="unknown",
                title="未知页面",
                elements=[]
            )
            return page_node
    
    def _dfs_analyze(self, app_package: str, max_depth: int, current_depth: int = 0, 
                    enable_human_assistance: bool = True):
        """深度优先遍历分析应用页面"""
        if current_depth >= max_depth:
            return
        
        current_page = self._capture_current_page("analyze")
        
        # 检测是否需要人工协助
        if enable_human_assistance and self._detect_human_assistance_required(current_page):
            print(f"🆘 检测到需要人工协助: {current_page.human_assistance_reason}")
            
            # 请求人工协助
            success = self.request_human_assistance(
                reason=current_page.human_assistance_reason,
                expected_action=f"完成 {current_page.human_assistance_reason} 操作"
            )
            
            if not success:
                print("❌ 人工协助失败，跳过此页面")
                return
        
        # 分析当前页面的可点击元素
        clickable_elements = [e for e in current_page.elements if e.get("clickable", False)]
        
        for element in clickable_elements:
            # 尝试点击元素
            if self._try_click_element(element):
                # 等待页面变化
                time.sleep(2)
                
                # 检查是否进入新页面
                new_page = self._capture_current_page("tap")
                
                if new_page.page_id not in self.visited_pages:
                    # 新页面，继续分析
                    self._dfs_analyze(app_package, max_depth, current_depth + 1, enable_human_assistance)
                    
                    # 返回原页面
                    if self.device_manager:
                        self.device_manager.press_back()
                    time.sleep(2)
                    self._capture_current_page("back")
                
                # 记录分析历史
                self.analysis_history.append({
                    "depth": current_depth,
                    "action": f"click_{element.get('text', 'unknown')}",
                    "source_page": current_page.page_id,
                    "target_page": new_page.page_id if new_page else None
                })
    
    def _try_click_element(self, element: Dict[str, Any]) -> bool:
        """尝试点击元素"""
        try:
            bounds = element.get("bounds", "")
            if bounds:
                # 解析坐标并点击
                x, y = self._parse_bounds(bounds)
                if self.device_manager:
                    self.device_manager.tap(x, y)
                return True
        except Exception as e:
            print(f"点击元素失败: {e}")
        
        return False
    
    def _parse_bounds(self, bounds: str) -> tuple[int, int]:
        """解析元素边界坐标"""
        # 示例: "[0,0][1080,1920]" -> 中心点 (540, 960)
        import re
        
        match = re.search(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds)
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            return (x1 + x2) // 2, (y1 + y2) // 2
        
        return 540, 960  # 默认中心点
    
    def _extract_activity_name_from_app(self, app_info: str) -> str:
        """从应用信息中提取Activity名称"""
        # 简化实现，从应用信息中提取Activity
        if isinstance(app_info, dict) and 'activity' in app_info:
            return app_info['activity']
        elif ':' in str(app_info):
            # 假设格式为 "package/activity"
            parts = str(app_info).split('/')
            if len(parts) > 1:
                return parts[1]
        return "unknown"
    
    def _extract_page_title_from_app(self, app_info: str) -> str:
        """从应用信息中提取页面标题"""
        # 简化实现，基于应用包名生成标题
        if isinstance(app_info, dict) and 'package' in app_info:
            package = app_info['package']
            if 'autodroid' in package.lower():
                return "Autodroid Manager"
            elif 'dashboard' in package.lower():
                return "仪表板"
            elif 'settings' in package.lower():
                return "设置"
        
        # 默认标题
        return "应用页面"
    
    def _get_ui_hierarchy(self) -> str:
        """通过设备管理器获取UI层次结构XML"""
        if self.device_manager:
            return self.device_manager.get_ui_hierarchy()
        return "<hierarchy>设备管理器不可用</hierarchy>"
    
    def _extract_activity_name_from_ui_hierarchy(self, ui_hierarchy: str) -> str:
        """从UI层次结构中提取Activity名称"""
        try:
            import re
            # 查找包含activity信息的节点
            pattern = r'package="([^"]+)" activity="([^"]+)"'
            match = re.search(pattern, ui_hierarchy)
            if match:
                return match.group(2)
        except Exception as e:
            print(f"提取Activity名称失败: {e}")
        
        return "unknown"
    
    def _extract_page_title_from_ui_hierarchy(self, ui_hierarchy: str) -> str:
        """从UI层次结构中提取页面标题"""
        try:
            import re
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
    
    def _extract_elements_from_ui_hierarchy(self, ui_hierarchy: str) -> List[Dict[str, Any]]:
        """从UI层次结构中提取页面元素"""
        elements = []
        
        try:
            import re
            
            # 匹配UI元素节点
            element_pattern = r'<node[^>]*?>(?:.*?</node>)?'
            node_matches = re.findall(element_pattern, ui_hierarchy, re.DOTALL)
            
            for node_xml in node_matches:
                element = self._parse_ui_element(node_xml)
                if element:
                    elements.append(element)
            
        except Exception as e:
            print(f"解析UI元素失败: {e}")
        
        return elements
    
    def _parse_ui_element(self, node_xml: str) -> Dict[str, Any]:
        """解析单个UI元素节点"""
        try:
            import re
            
            element = {}
            
            # 提取属性
            attributes = {
                'text': r'text="([^"]*)"',
                'resource-id': r'resource-id="([^"]*)"',
                'class': r'class="([^"]*)"',
                'package': r'package="([^"]*)"',
                'content-desc': r'content-desc="([^"]*)"',
                'checkable': r'checkable="([^"]*)"',
                'checked': r'checked="([^"]*)"',
                'clickable': r'clickable="([^"]*)"',
                'enabled': r'enabled="([^"]*)"',
                'focusable': r'focusable="([^"]*)"',
                'focused': r'focused="([^"]*)"',
                'scrollable': r'scrollable="([^"]*)"',
                'long-clickable': r'long-clickable="([^"]*)"',
                'password': r'password="([^"]*)"',
                'selected': r'selected="([^"]*)"',
                'bounds': r'bounds="([^"]*)"'
            }
            
            for attr_name, pattern in attributes.items():
                match = re.search(pattern, node_xml)
                if match:
                    element[attr_name] = match.group(1)
            
            # 计算元素重要性
            element['important'] = self._calculate_element_importance(element)
            
            return element
            
        except Exception as e:
            print(f"解析UI元素节点失败: {e}")
            return None
    
    def _calculate_element_importance(self, element: Dict[str, Any]) -> bool:
        """计算元素重要性"""
        # 重要元素的标准：有文本、可点击、有资源ID等
        text = element.get('text', '')
        clickable = element.get('clickable', 'false') == 'true'
        resource_id = element.get('resource-id', '')
        
        # 过滤掉空文本和系统元素
        if text and text.strip() and not text.startswith('com.'):
            return True
        
        if clickable and resource_id:
            return True
        
        # 按钮、输入框等交互元素
        class_name = element.get('class', '').lower()
        if any(keyword in class_name for keyword in ['button', 'edittext', 'textview', 'imageview']):
            return True
        
        return False
    
    def _extract_page_title(self, ui_hierarchy: str) -> str:
        """提取页面标题"""
        # 简化实现
        lines = ui_hierarchy.split('\n')
        for line in lines:
            if 'text=' in line and 'title' in line.lower():
                import re
                match = re.search(r'text="([^"]+)"', line)
                if match:
                    return match.group(1)
        return ""
    
    def _extract_page_elements(self, ui_hierarchy: str) -> List[Dict[str, Any]]:
        """从UI层级中提取页面元素"""
        elements = []
        
        # 简化实现，实际需要解析XML
        lines = ui_hierarchy.split('\n')
        for line in lines:
            if any(attr in line for attr in ['text=', 'resource-id=', 'class=']):
                element = {
                    "text": self._extract_attribute(line, 'text'),
                    "resource_id": self._extract_attribute(line, 'resource-id'),
                    "class_name": self._extract_attribute(line, 'class'),
                    "clickable": 'clickable="true"' in line,
                    "bounds": self._extract_attribute(line, 'bounds')
                }
                elements.append(element)
        
        return elements
    
    def _extract_attribute(self, line: str, attr: str) -> str:
        """提取XML属性值"""
        import re
        match = re.search(f'{attr}="([^"]+)"', line)
        return match.group(1) if match else ""
    
    def start_user_operation_monitoring(self):
        """开始用户操作监控"""
        self.monitoring_enabled = True
        print("🔍 开始监控用户操作...")
    
    def stop_user_operation_monitoring(self):
        """停止用户操作监控"""
        self.monitoring_enabled = False
        print("⏹️ 停止监控用户操作")
    
    def record_user_operation(self, action_type: str, target_element: Optional[Dict[str, Any]] = None,
                            input_text: Optional[str] = None, coordinates: Optional[Tuple[int, int]] = None,
                            target_page: Optional[str] = None):
        """记录用户操作"""
        if not self.monitoring_enabled:
            return
        
        operation_id = f"op_{int(time.time() * 1000)}"
        user_operation = UserOperation(
            operation_id=operation_id,
            timestamp=time.time(),
            action_type=action_type,
            target_element=target_element,
            input_text=input_text,
            coordinates=coordinates,
            source_page=self.current_page.page_id if self.current_page else "unknown",
            target_page=target_page
        )
        
        self.user_operations.append(user_operation)
        self.operation_graph.add_user_operation(user_operation)
    
    def _generate_report(self, app_package: str, max_depth: int) -> Dict[str, Any]:
        """生成分析报告"""
        report = {
            "app_package": app_package,
            "analysis_time": datetime.now().isoformat(),
            "max_depth": max_depth,
            "pages_analyzed": len(self.pages),
            "operations_recorded": len(self.operations),
            "user_operations": len(self.user_operations),
            "path_coverage": self.operation_graph.calculate_path_coverage(),
            "operation_statistics": self.operation_graph.get_operation_statistics(),
            "pages": {},
            "operation_graph": self.operation_graph.generate_mermaid_diagram(),
            "analysis_history": self.analysis_history
        }
        
        # 添加页面详细信息
        for page_id, page in self.pages.items():
            report["pages"][page_id] = {
                "app_name": page.app_name,
                "activity_name": page.activity_name,
                "title": page.title,
                "element_count": len(page.elements),
                "requires_human_assistance": page.requires_human_assistance,
                "human_assistance_reason": page.human_assistance_reason,
                "screenshot_path": page.screenshot_path,
                "timestamp": page.timestamp
            }
        
        return report
    
    def _save_analysis_results(self, app_package: str, report: Dict[str, Any]):
        """保存分析结果"""
        # 保存JSON报告
        report_path = self.output_dir / f"{app_package}_analysis_report.json"
        with open(report_path, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        
        # 保存操作图为图片
        graph_image_path = self.output_dir / f"{app_package}_operation_graph.png"
        self.operation_graph.visualize_operation_graph(str(graph_image_path))
        
        # 保存Mermaid图
        mermaid_path = self.output_dir / f"{app_package}_operation_graph.mmd"
        with open(mermaid_path, "w", encoding="utf-8") as f:
            f.write(report["operation_graph"])
        
        print(f"✅ 分析结果已保存到: {self.output_dir}")
        print(f"   📊 报告文件: {report_path}")
        print(f"   📈 操作图: {graph_image_path}")
        print(f"   📋 Mermaid图: {mermaid_path}")
    
    def get_operation_graph(self) -> OperationGraph:
        """获取操作图"""
        return self.operation_graph
    
    def get_analysis_summary(self) -> Dict[str, Any]:
        """获取分析摘要"""
        return {
            "total_pages": len(self.pages),
            "total_operations": len(self.operations),
            "total_user_operations": len(self.user_operations),
            "path_coverage": self.operation_graph.calculate_path_coverage(),
            "operation_statistics": self.operation_graph.get_operation_statistics()
        }


def analyze_autodroid_manager(device_id: Optional[str] = None, output_dir: str = "autodroid_analysis") -> Dict[str, Any]:
    """分析Autodroid Manager应用"""
    analyzer = AppAnalyzer(device_id=device_id, output_dir=output_dir)
    
    # Autodroid Manager的包名
    autodroid_package = "com.autodroid.manager"
    
    # 分析应用结构
    report = analyzer.analyze_app_structure(autodroid_package, max_depth=5)
    
    return report


if __name__ == "__main__":
    # 示例用法
    print("🔍 Autodroid Analyzer - 应用结构分析工具")
    
    # 简化示例，直接使用默认设备ID
    device_id = "emulator-5554"  # 默认设备ID
    print(f"📱 使用设备: {device_id}")
    
    # 分析Autodroid Manager
    try:
        report = analyze_autodroid_manager(device_id)
        print("✅ 分析完成!")
        print(f"📊 分析页面数: {report['pages_analyzed']}")
        print(f"🔄 记录操作数: {report['operations_recorded']}")
        print(f"📈 路径覆盖率: {report['path_coverage']['coverage']:.1%}")
        
    except Exception as e:
        print(f"❌ 分析失败: {e}")
        import traceback
        traceback.print_exc()