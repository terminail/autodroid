"""导航系统模块，负责页面导航和路径规划"""

import time
import json
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass
import networkx as nx

from core.analysis.analysis_utils import AnalysisUtils


@dataclass
class NavigationNode:
    """导航节点数据类"""
    page_id: str
    page_title: str
    activity_name: str
    element_count: int
    timestamp: float
    screenshot_path: Optional[str] = None
    ui_hierarchy_path: Optional[str] = None


@dataclass
class NavigationEdge:
    """导航边数据类"""
    source_page: str
    target_page: str
    action_type: str  # "click", "input", "swipe", "back", "menu"
    target_element: Optional[Dict[str, Any]] = None
    input_text: Optional[str] = None
    coordinates: Optional[Tuple[int, int]] = None
    timestamp: float = 0.0


class NavigationSystem:
    """导航系统类"""
    
    def __init__(self, device_id: str, app_package: str, output_dir: Path):
        self.device_id = device_id
        self.app_package = app_package
        self.output_dir = output_dir
        
        # 创建导航图
        self.navigation_graph = nx.DiGraph()
        
        # 页面节点映射
        self.page_nodes: Dict[str, NavigationNode] = {}
        
        # 导航路径记录
        self.navigation_path: List[Tuple[str, str, str]] = []  # (source_page, target_page, action)
        
        # 创建导航输出目录
        self.navigation_dir = output_dir / "navigation"
        self.navigation_dir.mkdir(exist_ok=True)
        
        # 分析工具
        self.analysis_utils = AnalysisUtils()
    
    def add_page_node(self, page_data: Dict[str, Any]) -> str:
        """添加页面节点到导航图"""
        try:
            page_id = page_data.get('page_id', f"page_{int(time.time())}")
            
            # 创建导航节点
            node = NavigationNode(
                page_id=page_id,
                page_title=page_data.get('title', '未知页面'),
                activity_name=page_data.get('activity_name', ''),
                element_count=page_data.get('element_count', 0),
                timestamp=page_data.get('timestamp', time.time()),
                screenshot_path=page_data.get('screenshot_path'),
                ui_hierarchy_path=page_data.get('ui_hierarchy_path')
            )
            
            # 添加到节点映射
            self.page_nodes[page_id] = node
            
            # 添加到导航图
            self.navigation_graph.add_node(page_id, **page_data)
            
            print(f"🗺️  已添加页面节点: {page_id} ({node.page_title})")
            return page_id
            
        except Exception as e:
            print(f"❌ 添加页面节点失败: {e}")
            return ""
    
    def add_navigation_edge(self, source_page: str, target_page: str, action_data: Dict[str, Any]):
        """添加导航边"""
        try:
            if source_page not in self.page_nodes or target_page not in self.page_nodes:
                print(f"❌ 页面节点不存在: {source_page} -> {target_page}")
                return
            
            # 创建导航边
            edge = NavigationEdge(
                source_page=source_page,
                target_page=target_page,
                action_type=action_data.get('action_type', 'unknown'),
                target_element=action_data.get('target_element'),
                input_text=action_data.get('input_text'),
                coordinates=action_data.get('coordinates'),
                timestamp=action_data.get('timestamp', time.time())
            )
            
            # 添加到导航图
            edge_data = {
                'action_type': edge.action_type,
                'target_element': edge.target_element,
                'input_text': edge.input_text,
                'coordinates': edge.coordinates,
                'timestamp': edge.timestamp
            }
            
            self.navigation_graph.add_edge(source_page, target_page, **edge_data)
            
            # 记录导航路径
            self.navigation_path.append((source_page, target_page, edge.action_type))
            
            print(f"🔄 已添加导航边: {source_page} -> {target_page} ({edge.action_type})")
            
        except Exception as e:
            print(f"❌ 添加导航边失败: {e}")
    
    def get_page_reachability(self, source_page: str, target_page: str) -> bool:
        """检查页面可达性"""
        try:
            if source_page not in self.navigation_graph or target_page not in self.navigation_graph:
                return False
            
            return nx.has_path(self.navigation_graph, source_page, target_page)
            
        except Exception as e:
            print(f"❌ 检查页面可达性失败: {e}")
            return False
    
    def find_shortest_path(self, source_page: str, target_page: str) -> List[str]:
        """查找最短路径"""
        try:
            if source_page not in self.navigation_graph or target_page not in self.navigation_graph:
                return []
            
            if not self.get_page_reachability(source_page, target_page):
                return []
            
            return nx.shortest_path(self.navigation_graph, source_page, target_page)
            
        except Exception as e:
            print(f"❌ 查找最短路径失败: {e}")
            return []
    
    def get_navigation_suggestions(self, current_page: str) -> List[Dict[str, Any]]:
        """获取导航建议"""
        try:
            if current_page not in self.navigation_graph:
                return []
            
            suggestions = []
            
            # 获取当前页面的所有出边
            for target_page in self.navigation_graph.successors(current_page):
                edge_data = self.navigation_graph.get_edge_data(current_page, target_page)
                
                if edge_data:
                    suggestion = {
                        'target_page': target_page,
                        'action_type': edge_data.get('action_type', 'unknown'),
                        'target_element': edge_data.get('target_element'),
                        'input_text': edge_data.get('input_text'),
                        'confidence': self._calculate_navigation_confidence(current_page, target_page)
                    }
                    suggestions.append(suggestion)
            
            # 按置信度排序
            suggestions.sort(key=lambda x: x['confidence'], reverse=True)
            
            return suggestions
            
        except Exception as e:
            print(f"❌ 获取导航建议失败: {e}")
            return []
    
    def _calculate_navigation_confidence(self, source_page: str, target_page: str) -> float:
        """计算导航置信度"""
        try:
            # 基于导航频率计算置信度
            edge_count = 0
            total_edges = 0
            
            for edge in self.navigation_graph.edges():
                if edge[0] == source_page:
                    total_edges += 1
                    if edge[1] == target_page:
                        edge_count += 1
            
            if total_edges == 0:
                return 0.0
            
            return edge_count / total_edges
            
        except Exception as e:
            print(f"❌ 计算导航置信度失败: {e}")
            return 0.0
    
    def analyze_navigation_patterns(self) -> Dict[str, Any]:
        """分析导航模式"""
        try:
            analysis_result = {
                "total_pages": len(self.page_nodes),
                "total_navigations": len(self.navigation_path),
                "navigation_density": 0.0,
                "most_visited_pages": [],
                "navigation_loops": [],
                "dead_end_pages": []
            }
            
            if not self.page_nodes:
                return analysis_result
            
            # 计算导航密度
            max_possible_edges = len(self.page_nodes) * (len(self.page_nodes) - 1)
            if max_possible_edges > 0:
                analysis_result["navigation_density"] = len(self.navigation_path) / max_possible_edges
            
            # 查找最常访问的页面
            page_visits = {}
            for path in self.navigation_path:
                source_page, target_page, _ = path
                page_visits[source_page] = page_visits.get(source_page, 0) + 1
                page_visits[target_page] = page_visits.get(target_page, 0) + 1
            
            if page_visits:
                sorted_pages = sorted(page_visits.items(), key=lambda x: x[1], reverse=True)
                analysis_result["most_visited_pages"] = [
                    {"page_id": page_id, "visit_count": count} 
                    for page_id, count in sorted_pages[:5]
                ]
            
            # 查找导航循环
            analysis_result["navigation_loops"] = self._find_navigation_loops()
            
            # 查找死胡同页面
            analysis_result["dead_end_pages"] = self._find_dead_end_pages()
            
            return analysis_result
            
        except Exception as e:
            print(f"❌ 分析导航模式失败: {e}")
            return {"error": f"分析导航模式失败: {e}"}
    
    def _find_navigation_loops(self) -> List[List[str]]:
        """查找导航循环"""
        try:
            loops = []
            
            # 使用强连通分量查找循环
            sccs = list(nx.strongly_connected_components(self.navigation_graph))
            
            for scc in sccs:
                if len(scc) > 1:  # 只考虑包含多个节点的强连通分量
                    # 获取子图
                    subgraph = self.navigation_graph.subgraph(scc)
                    
                    # 查找简单循环
                    try:
                        simple_cycles = list(nx.simple_cycles(subgraph))
                        loops.extend(simple_cycles)
                    except:
                        pass
            
            return loops
            
        except Exception as e:
            print(f"❌ 查找导航循环失败: {e}")
            return []
    
    def _find_dead_end_pages(self) -> List[str]:
        """查找死胡同页面"""
        try:
            dead_ends = []
            
            for page_id in self.page_nodes:
                # 检查是否有出边
                if self.navigation_graph.out_degree(page_id) == 0:
                    dead_ends.append(page_id)
            
            return dead_ends
            
        except Exception as e:
            print(f"❌ 查找死胡同页面失败: {e}")
            return []
    
    def export_navigation_data(self) -> bool:
        """导出导航数据"""
        try:
            # 导出导航图数据
            graph_data = {
                "nodes": {},
                "edges": [],
                "navigation_path": self.navigation_path,
                "analysis_result": self.analyze_navigation_patterns()
            }
            
            # 节点数据
            for page_id, node in self.page_nodes.items():
                graph_data["nodes"][page_id] = {
                    "page_title": node.page_title,
                    "activity_name": node.activity_name,
                    "element_count": node.element_count,
                    "timestamp": node.timestamp
                }
            
            # 边数据
            for edge in self.navigation_graph.edges(data=True):
                source, target, data = edge
                graph_data["edges"].append({
                    "source": source,
                    "target": target,
                    "action_type": data.get('action_type', 'unknown'),
                    "timestamp": data.get('timestamp', 0)
                })
            
            # 保存到文件
            graph_file = self.navigation_dir / "navigation_graph.json"
            with open(graph_file, 'w', encoding='utf-8') as f:
                json.dump(graph_data, f, ensure_ascii=False, indent=2)
            
            print(f"💾 导航数据已导出到: {graph_file}")
            return True
            
        except Exception as e:
            print(f"❌ 导出导航数据失败: {e}")
            return False
    
    def visualize_navigation_graph(self) -> Optional[str]:
        """可视化导航图"""
        try:
            import matplotlib.pyplot as plt
            
            # 创建图形
            plt.figure(figsize=(12, 8))
            
            # 使用spring布局
            pos = nx.spring_layout(self.navigation_graph)
            
            # 绘制节点
            nx.draw_networkx_nodes(self.navigation_graph, pos, node_color='lightblue', 
                                 node_size=500, alpha=0.9)
            
            # 绘制边
            nx.draw_networkx_edges(self.navigation_graph, pos, edge_color='gray', 
                                  arrows=True, arrowsize=20)
            
            # 绘制标签
            labels = {}
            for page_id in self.navigation_graph.nodes():
                node = self.page_nodes.get(page_id)
                if node:
                    labels[page_id] = f"{node.page_title}\n({page_id[:8]})"
                else:
                    labels[page_id] = page_id[:8]
            
            nx.draw_networkx_labels(self.navigation_graph, pos, labels, font_size=8)
            
            # 保存图像
            image_path = self.navigation_dir / "navigation_graph.png"
            plt.title(f"{self.app_package} 导航图")
            plt.axis('off')
            plt.tight_layout()
            plt.savefig(image_path, dpi=300, bbox_inches='tight')
            plt.close()
            
            print(f"📊 导航图已保存到: {image_path}")
            return str(image_path)
            
        except ImportError:
            print("⚠️  matplotlib未安装，无法生成导航图")
            return None
        except Exception as e:
            print(f"❌ 可视化导航图失败: {e}")
            return None


def create_navigation_system(device_id: str, app_package: str, output_dir: Path) -> NavigationSystem:
    """创建导航系统"""
    return NavigationSystem(device_id, app_package, output_dir)