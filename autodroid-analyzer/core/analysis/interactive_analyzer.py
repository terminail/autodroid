"""交互式应用分析器，支持用户操作监控和多种分析方式"""

import time
import json
import subprocess
import multiprocessing
from pathlib import Path
from typing import Dict, List, Optional, Any
from dataclasses import dataclass
from datetime import datetime

from .app_analyzer import AppAnalyzer, PageNode, OperationEdge
from core.database import get_database_manager
from core.device.device_manager import DeviceManager
from .multimodal_recognizer import MultiModalPageRecognizer


@dataclass
class UserAction:
    """用户操作记录"""
    timestamp: float
    action_type: str  # "click", "input", "swipe", "back"
    target_element: Optional[Dict[str, Any]]
    input_text: Optional[str]
    coordinates: Optional[tuple[int, int]]
    result_page: Optional[str]


class InteractiveAppAnalyzer(AppAnalyzer):
    """交互式应用分析器"""
    
    def __init__(self, device_id: str = None, app_package: str = None, output_dir: str = "analysis_output"):
        """
        初始化交互式应用分析器
        
        Args:
            device_id: 设备ID（必须提供）
            app_package: 应用包名，如果为None则从config.yaml加载
            output_dir: 输出目录
        """
        # 验证必要参数
        if not device_id:
            raise ValueError("设备ID未提供，请通过参数传递")
        if not app_package:
            raise ValueError("应用包名未提供，请通过参数传递")
        
        super().__init__(device_id, output_dir)
        self.app_package = app_package
        self.user_actions: List[UserAction] = []
        self.current_analysis_mode = "auto"  # "auto", "interactive", "monitor"
        self.monitoring_enabled = False
        self.last_page_hash = ""
        
        # 初始化设备管理器
        self.device_manager = DeviceManager(device_id)
        
        # 初始化多模态页面识别器
        self.multimodal_recognizer = MultiModalPageRecognizer(device_id, {})
        
        # 设置分析模式
        self.multimodal_recognizer.set_analysis_modes({
            "uiautomator2": True,  # DOM树分析
            "screenshot": True,    # 截图分析
            "user_monitoring": True  # 用户操作监控
        })
        
        # 初始化数据库管理器
        self.db_service = get_database_manager()
    
    def launch_app(self) -> bool:
        """检查并启动应用（如果未启动则自动启动）"""
        return self.device_manager.launch_app(self.app_package)
    
    def get_current_page(self) -> Optional[PageNode]:
        """获取当前页面（覆盖父类方法）"""
        try:
            # 调用父类的私有捕获页面方法
            return self._capture_current_page("current")
        except Exception as e:
            print(f"❌ 获取当前页面失败: {e}")
            return None
            
    def analyze_with_user_interaction(self, max_depth: int = 5, enable_monitoring: bool = True):
        """使用用户交互进行分析 - 用户操作，程序监控，最后分析"""
        print("🚀 开始交互式应用分析")
        print("=" * 50)
        print("💡 模式: 用户操作，程序监控，最后分析")
        print("💡 请在手机上操作应用，程序将自动监控和记录")
        print("💡 输入 'stop' 停止监控并开始分析")
        
        # 启动应用
        if not self.launch_app():
            print("❌ 应用启动失败")
            return
        
        # 等待应用启动
        time.sleep(3)
        
        # 开始监控用户操作
        if enable_monitoring:
            self.start_user_operation_monitoring()
        
        # 只记录初始页面，不进行分析
        initial_page = self._capture_current_page("launch")
        print(f"📱 初始页面: {initial_page.title}")
        print("💡 监控模式已启动，请开始操作应用...")
        
        # 启动用户操作监控线程
        self._start_user_monitoring_mode()
        
        # 等待用户输入停止
        while True:
            user_input = input("\n📝 继续监控(Y/n): ").strip().lower()
            
            # 默认值为Y（回车或输入y/Y继续监控）
            if user_input in ['', 'y', 'yes']:
                # 显示当前监控状态
                print(f"📊 已记录 {len(self.user_actions)} 个用户操作")
                print(f"📄 已监控 {len(self.pages)} 个页面")
                print("💡 继续操作应用，程序持续监控中...")
                continue
            elif user_input in ['n', 'no']:
                break
            else:
                # 无效输入，继续监控
                print("💡 输入无效，继续监控...")
                continue
        
        # 停止监控
        if enable_monitoring:
            self.stop_user_operation_monitoring()
        
        print("\n🔍 开始分析监控到的内容...")
        print("=" * 50)
        
        # 分析阶段：对监控到的所有页面进行多模态分析
        self._analyze_monitored_content()
        
        # 生成分析报告
        self.generate_analysis_report()
        
        print("✅ 分析完成，报告已生成")
    
    def analyze_page_multimodal(self, page_node: PageNode) -> Dict[str, Any]:
        """多模态页面分析"""
        try:
            # 尝试两种可能的UI层次结构文件命名格式
            ui_hierarchy_path1 = self.output_dir / f"ui_hierarchy_{page_node.page_id}.xml"
            ui_hierarchy_path2 = self.output_dir / f"{page_node.page_id}_ui.xml"
            
            # 获取截图文件路径
            screenshot_path = self.output_dir / f"screenshot_{page_node.page_id}.png"
            
            # 检查文件是否存在
            if not ui_hierarchy_path1.exists() and not ui_hierarchy_path2.exists():
                print(f"❌ UI层次结构文件不存在: {ui_hierarchy_path1} 或 {ui_hierarchy_path2}")
                return {"error": "UI层次结构文件不存在"}
            
            if not screenshot_path.exists():
                print(f"❌ 截图文件不存在: {screenshot_path}")
                return {"error": "截图文件不存在"}
            
            # 使用存在的文件路径
            ui_hierarchy_path = ui_hierarchy_path1 if ui_hierarchy_path1.exists() else ui_hierarchy_path2
            
            # 执行多模态分析
            analysis_results = self.multimodal_recognizer.analyze_page_multimodal(
                screenshot_path=str(screenshot_path),
                current_app=self.app_package,
                ui_hierarchy_path=str(ui_hierarchy_path)
            )
            
            # 更新页面节点信息
            if analysis_results.get("ui_elements"):
                page_node.elements = analysis_results["ui_elements"]
            
            if analysis_results.get("combined_features"):
                features = analysis_results["combined_features"]
                page_node.activity_name = features.get("activity_name", "")
                page_node.title = features.get("page_title", "")
                page_node.element_count = features.get("element_count", 0)
            
            return analysis_results
            
        except Exception as e:
            print(f"❌ 多模态页面分析失败: {e}")
            return {"error": str(e)}
    
    def get_detailed_element_analysis(self, page_node: PageNode) -> Dict[str, Any]:
        """获取详细的元素分析"""
        try:
            # 尝试两种可能的文件命名格式
            ui_hierarchy_path1 = self.output_dir / f"ui_hierarchy_{page_node.page_id}.xml"
            ui_hierarchy_path2 = self.output_dir / f"{page_node.page_id}_ui.xml"
            
            if ui_hierarchy_path1.exists():
                return self.multimodal_recognizer.get_detailed_element_analysis(str(ui_hierarchy_path1))
            elif ui_hierarchy_path2.exists():
                return self.multimodal_recognizer.get_detailed_element_analysis(str(ui_hierarchy_path2))
            else:
                print(f"❌ UI层次结构文件不存在: {ui_hierarchy_path1} 或 {ui_hierarchy_path2}")
                return {"error": "UI层次结构文件不存在"}
                
        except Exception as e:
            print(f"❌ 详细元素分析失败: {e}")
            return {"error": f"详细元素分析失败: {e}"}
    
    def _display_multimodal_analysis(self, page_node: PageNode):
        """显示多模态分析结果"""
        print("\n🔍 多模态分析结果:")
        print("-" * 40)
        
        # 执行多模态分析
        analysis_results = self.analyze_page_multimodal(page_node)
        
        if "error" in analysis_results:
            print(f"❌ 分析失败: {analysis_results['error']}")
            return
        
        # 显示DOM树分析结果
        dom_analysis = analysis_results["analysis_modes"].get("dom_tree", {})
        if "error" not in dom_analysis:
            print("🌳 DOM树分析:")
            print(f"   页面标题: {dom_analysis.get('page_title', '未知')}")
            print(f"   Activity: {dom_analysis.get('activity_name', '未知')}")
            print(f"   元素数量: {dom_analysis.get('element_count', 0)}")
            
            # 显示关键元素
            elements = dom_analysis.get("elements", [])
            if elements:
                print("   关键元素:")
                for i, element in enumerate(elements[:5], 1):  # 显示前5个元素
                    text = element.get("text", "")
                    if text:
                        print(f"     {i}. {text}")
        
        # 显示截图分析结果
        screenshot_analysis = analysis_results["analysis_modes"].get("screenshot", {})
        if "error" not in screenshot_analysis:
            print("📸 截图分析:")
            print(f"   布局复杂度: {screenshot_analysis.get('layout_complexity', 0):.2f}")
            
        # 显示页面匹配结果
        page_match = analysis_results.get("page_match")
        if page_match:
            print(f"🎯 页面匹配: {page_match.page_id} (置信度: {page_match.confidence:.2f})")
        
        # 显示详细元素分析
        detailed_analysis = self.get_detailed_element_analysis(page_node)
        if "error" not in detailed_analysis:
            print("📊 详细元素分析:")
            print(f"   可点击元素: {detailed_analysis.get('clickable_elements', 0)}")
            print(f"   文本元素: {detailed_analysis.get('text_elements', 0)}")
            
            # 显示交互点
            interaction_points = detailed_analysis.get("interaction_points", [])
            if interaction_points:
                print("   推荐交互点:")
                for i, point in enumerate(interaction_points[:3], 1):
                    text = point.get("element", {}).get("text", "")
                    importance = point.get("importance", 0)
                    if text:
                        print(f"     {i}. {text} (重要性: {importance:.2f})")
    
    def _enhanced_display_page_elements(self, page_node: PageNode):
        """增强的页面元素显示（包含多模态分析）"""
        print(f"\n📄 页面: {page_node.title}")
        print(f"📱 Activity: {page_node.activity_name}")
        print(f"🔢 元素数量: {page_node.element_count}")
        
        # 显示多模态分析
        self._display_multimodal_analysis(page_node)
        
        # 显示可操作元素
        interactive_elements = self._get_interactive_elements(page_node)
        if interactive_elements:
            print("\n🖱️ 可操作元素:")
            for i, element in enumerate(interactive_elements, 1):
                # 尝试获取多种可能的文本描述
                element_text = element.get('text', '')
                if not element_text:
                    element_text = element.get('content_desc', '')
                if not element_text:
                    element_text = element.get('resource_id', '')
                if not element_text:
                    element_text = element.get('class_name', '')
                if not element_text:
                    element_text = '未知元素'
                
                importance = element.get("importance", 0)
                print(f"   {i}. {element_text} (重要性: {importance:.2f})")
        else:
            print("\n⚠️ 未找到可操作元素")
        
    def start_user_operation_monitoring(self):
        """开始监控用户操作"""
        print("🔍 开始监控用户操作...")
        self.monitoring_enabled = True
        self.last_page_hash = self._get_current_page_hash()
        self.last_page_id = None
        self.monitor_thread = None
        
        # 启动监控线程
        import threading
        self.monitor_thread = threading.Thread(target=self.monitor_user_operations, daemon=True)
        self.monitor_thread.start()
        
    def stop_user_operation_monitoring(self):
        """停止监控用户操作"""
        print("🛑 停止监控用户操作")
        self.monitoring_enabled = False
        if self.monitor_thread:
            self.monitor_thread.join(timeout=5)
    
    def _start_user_monitoring_mode(self):
        """启动用户监控模式 - 真正的'用户操作，程序监控'"""
        print("\n📡 启动用户操作监控模式")
        print("💡 请在手机上操作应用，程序将自动记录和分析")
        
        # 确保监控已启动
        if not self.monitoring_enabled:
            self.start_user_operation_monitoring()
        
        # 设置监控回调，在检测到用户操作时实时显示
        self._setup_monitoring_callbacks()
        
        print("✅ 监控模式已启动，开始记录用户操作...")
    
    def _setup_monitoring_callbacks(self):
        """设置监控回调函数"""
        # 设置页面变化时的回调
        self.on_page_change = self._on_page_change_callback
        # 设置用户操作检测时的回调
        self.on_user_action = self._on_user_action_callback
    
    def _on_page_change_callback(self, old_page_id: str, new_page: PageNode):
        """页面变化回调函数"""
        print(f"\n🔄 检测到页面变化: {old_page_id} → {new_page.page_id}")
        print(f"📄 新页面: {new_page.title}")
        
        # 分析新页面
        analysis_result = self.analyze_page_multimodal(new_page)
        if "error" not in analysis_result:
            print(f"✅ 页面分析完成: {new_page.title}")
            
            # 显示页面元素
            interactive_elements = self._get_interactive_elements(new_page)
            if interactive_elements:
                print(f"🖱️ 可操作元素 ({len(interactive_elements)}个):")
                for i, element in enumerate(interactive_elements[:5], 1):  # 显示前5个
                    element_text = element.get('text', '')
                    if not element_text:
                        element_text = element.get('content_desc', '')
                    if not element_text:
                        element_text = element.get('resource_id', '')
                    if not element_text:
                        element_text = element.get('class_name', '未知元素')
                    
                    importance = element.get('importance', 0)
                    print(f"   {i}. {element_text} (重要性: {importance:.2f})")
        else:
            print(f"❌ 页面分析失败: {analysis_result['error']}")
    
    def _on_user_action_callback(self, action_type: str, target_element: Dict[str, Any], result_page: str, input_text: str = None):
        """用户操作回调函数"""
        # 获取元素信息
        bounds = target_element.get('bounds', [0, 0, 0, 0])
        element_text = target_element.get('text', '')
        if not element_text:
            element_text = target_element.get('content_desc', '')
        if not element_text:
            element_text = target_element.get('resource_id', '')
        if not element_text:
            element_text = target_element.get('class_name', '未知元素')
        
        # 计算点击坐标（元素中心点）
        click_x = (bounds[0] + bounds[2]) // 2
        click_y = (bounds[1] + bounds[3]) // 2
        
        # 格式化显示用户操作信息
        if action_type == "click":
            print(f"\n👆 用户点击[{click_x},{click_y}]元素\"{element_text}\"")
        elif action_type == "input":
            print(f"\n⌨️  用户输入[{click_x},{click_y}]元素\"{element_text}\"：{input_text}")
        elif action_type == "back":
            print(f"\n↩️  用户返回操作")
        else:
            print(f"\n👆 用户操作[{click_x},{click_y}]元素\"{element_text}\"：{action_type}")
        
        # 记录用户操作
        action = UserAction(
            timestamp=time.time(),
            action_type=action_type,
            target_element=target_element,
            input_text=input_text,
            coordinates=(click_x, click_y),
            result_page=result_page
        )
        self.user_actions.append(action)
        
        print(f"📝 已记录用户操作 #{len(self.user_actions)}")
    
    def _analyze_monitored_content(self):
        """分析监控到的所有内容"""
        print(f"📊 开始分析监控内容...")
        print(f"   📄 需要分析的页面数量: {len(self.pages)}")
        print(f"   🎯 需要分析的用户操作数量: {len(self.user_actions)}")
        
        # 对每个页面进行多模态分析
        for i, page in enumerate(self.pages):
            print(f"\n🔍 分析页面 {i+1}/{len(self.pages)}: {page.title}")
            
            # 进行多模态页面分析
            analyzed_page = self.analyze_page_multimodal(page)
            
            if "error" in analyzed_page:
                print(f"❌ 页面分析失败: {analyzed_page['error']}")
            else:
                print(f"✅ 页面分析完成")
        
        print("\n✅ 所有页面分析完成")
        
        # 分析用户操作序列
        self._analyze_user_operation_sequence()
    
    def _analyze_user_operation_sequence(self):
        """分析用户操作序列"""
        print("\n📈 分析用户操作序列...")
        
        if not self.user_actions:
            print("⚠️  没有用户操作记录")
            return
        
        # 统计操作类型
        action_types = {}
        for action in self.user_actions:
            action_type = action.action_type
            action_types[action_type] = action_types.get(action_type, 0) + 1
        
        print("📊 操作类型统计:")
        for action_type, count in action_types.items():
            print(f"   {action_type}: {count}次")
        
        # 分析操作路径
        print("\n🛣️  用户操作路径:")
        for i, action in enumerate(self.user_actions):
            element_text = "未知元素"
            if action.target_element:
                element_text = action.target_element.get('text', '')
                if not element_text:
                    element_text = action.target_element.get('content_desc', '')
                if not element_text:
                    element_text = action.target_element.get('resource_id', '')
                if not element_text:
                    element_text = action.target_element.get('class_name', '未知元素')
            
            print(f"   {i+1}. {action.action_type} -> {element_text}")
    
    def generate_analysis_report(self):
        """生成分析报告"""
        print("\n📄 生成分析报告...")
        
        # 创建报告目录
        report_dir = Path("analysis_reports")
        report_dir.mkdir(exist_ok=True)
        
        # 生成报告文件名
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_file = report_dir / f"analysis_report_{timestamp}.json"
        
        # 准备报告数据
        report_data = {
            "app_package": self.app_package,
            "device_id": self.device_id,
            "analysis_time": datetime.now().isoformat(),
            "total_pages": len(self.pages),
            "total_operations": len(self.user_actions),
            "pages": {},
            "operations": []
        }
        
        # 添加页面信息
        for page in self.pages:
            report_data["pages"][page.page_id] = {
                "title": page.title,
                "activity_name": page.activity_name,
                "element_count": page.element_count,
                "elements": page.elements
            }
        
        # 添加操作信息
        for action in self.user_actions:
            report_data["operations"].append({
                "timestamp": action.timestamp,
                "action_type": action.action_type,
                "target_element": action.target_element,
                "input_text": action.input_text,
                "coordinates": action.coordinates,
                "result_page": action.result_page
            })
        
        # 保存报告
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(report_data, f, ensure_ascii=False, indent=2)
        
        print(f"✅ 分析报告已生成: {report_file}")
        
        return report_file
    
    def monitor_user_operations(self, interval: float = 2.0):
        """监控用户操作"""
        print("🔍 开始监控用户操作...")
        
        last_elements = []
        last_page_id = None
        
        while self.monitoring_enabled:
            try:
                # 获取当前页面
                current_page = self.get_current_page()
                if not current_page:
                    time.sleep(interval)
                    continue
                
                # 检查页面是否变化
                current_page_hash = self._get_current_page_hash()
                
                if current_page_hash != self.last_page_hash:
                    # 页面变化，记录新页面
                    if hasattr(self, 'on_page_change') and last_page_id:
                        self.on_page_change(last_page_id, current_page)
                    
                    self.last_page_hash = current_page_hash
                    last_page_id = current_page.page_id
                    
                    # 添加页面到分析列表
                    if current_page.page_id not in [p.page_id for p in self.pages]:
                        self.pages.append(current_page)
                
                # 获取当前页面元素
                current_elements = current_page.elements if current_page.elements else []
                
                # 批量检测用户操作
                operation_count = self._batch_detect_user_operations(current_page, current_elements, last_elements)
                
                if operation_count > 0:
                    print(f"🔍 检测到 {operation_count} 个用户操作")
                
                # 更新上次元素状态
                last_elements = current_elements.copy()
                
                time.sleep(interval)
                
            except Exception as e:
                print(f"❌ 监控过程中出现错误: {e}")
                time.sleep(interval)
    
    def _batch_detect_user_operations(self, current_page, current_elements, last_elements) -> int:
        """批量检测用户操作"""
        operation_count = 0
        
        # 检测点击操作
        for element in current_elements:
            if element.get('clickable', False):
                # 检查是否是新增的可点击元素
                if not self._element_exists_in_list(element, last_elements):
                    # 检测到可能的点击操作
                    if hasattr(self, 'on_user_action'):
                        self.on_user_action("click", element, current_page.page_id)
                    operation_count += 1
        
        # 检测输入操作
        for element in current_elements:
            if element.get('editable', False):
                # 检查输入内容是否变化
                last_element = self._find_matching_element(element, last_elements)
                if last_element:
                    current_text = element.get('text', '')
                    last_text = last_element.get('text', '')
                    if current_text != last_text and current_text:
                        # 检测到输入操作
                        if hasattr(self, 'on_user_action'):
                            self.on_user_action("input", element, current_page.page_id, current_text)
                        operation_count += 1
        
        return operation_count
    
    def _element_exists_in_list(self, element, element_list) -> bool:
        """检查元素是否存在于列表中"""
        for existing_element in element_list:
            if self._elements_match(element, existing_element):
                return True
        return False
    
    def _find_matching_element(self, element, element_list):
        """在列表中查找匹配的元素"""
        for existing_element in element_list:
            if self._elements_match(element, existing_element):
                return existing_element
        return None
    
    def _elements_match(self, element1, element2) -> bool:
        """判断两个元素是否匹配"""
        if not element1 or not element2:
            return False
        
        # 比较关键属性
        key_attrs = ['resource_id', 'text', 'content_desc', 'class_name']
        
        for attr in key_attrs:
            val1 = element1.get(attr, '')
            val2 = element2.get(attr, '')
            
            if val1 and val2 and val1 == val2:
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
    
    def _get_element_key(self, element) -> str:
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
    
    def _get_interactive_elements(self, page_node: PageNode) -> List[Dict[str, Any]]:
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
    
    def _record_user_operation(self, operation: Dict[str, Any], current_page: PageNode):
        """记录用户操作"""
        try:
            # 创建用户操作记录
            action = UserAction(
                timestamp=time.time(),
                action_type=operation['type'],
                target_element=operation.get('element'),
                input_text=operation.get('input_text'),
                coordinates=operation.get('coordinates'),
                result_page=current_page.page_id
            )
            
            # 添加到操作列表
            self.user_actions.append(action)
            
            # 保存到数据库（如果数据库服务可用）
            if hasattr(self, 'db_service') and self.db_service:
                operation_data = {
                    'timestamp': action.timestamp,
                    'action_type': action.action_type,
                    'target_element': action.target_element,
                    'input_text': action.input_text,
                    'coordinates': action.coordinates,
                    'result_page': action.result_page
                }
                
                # 获取或创建会话ID
                if not hasattr(self, 'session_id') or not self.session_id:
                    self.session_id = self.db_service.user_operation.create_session(
                        device_id=self.device_id,
                        app_package=self.app_package,
                        start_time=time.time()
                    )
                
                if self.session_id:
                    self.db_service.user_operation.save_user_operation(self.session_id, operation_data)
            
            print(f"📝 记录用户操作: {operation['type']}")
            
        except Exception as e:
            print(f"❌ 记录用户操作失败: {e}")
    
    def _get_current_page_hash(self) -> str:
        """获取当前页面的哈希值（用于检测页面变化）"""
        try:
            # 使用设备管理器获取UI层次结构
            ui_hierarchy = self.device_manager.get_ui_hierarchy()
            if ui_hierarchy:
                # 计算UI层次结构的哈希值
                import hashlib
                return hashlib.md5(ui_hierarchy.encode()).hexdigest()
        except Exception as e:
            print(f"❌ 获取页面哈希失败: {e}")
        
        return ""
    
    def check_device_connection(self) -> bool:
        """检查设备连接状态"""
        return self.device_manager.check_device_connection()
    
    def _try_input_text(self, element: Dict[str, Any], text: str) -> bool:
        """尝试输入文本"""
        return self.device_manager.input_text(element, text)
    
    def _capture_current_page(self, page_type: str = "current") -> Optional[PageNode]:
        """捕获当前页面（私有方法）"""
        try:
            # 使用设备管理器获取UI层次结构
            ui_hierarchy = self.device_manager.get_ui_hierarchy()
            if not ui_hierarchy:
                return None
            
            # 保存UI层次结构到文件
            page_id = f"{page_type}_{int(time.time())}"
            ui_file = self.output_dir / f"ui_hierarchy_{page_id}.xml"
            with open(ui_file, 'w', encoding='utf-8') as f:
                f.write(ui_hierarchy)
            
            # 获取截图
            screenshot_file = self.output_dir / f"screenshot_{page_id}.png"
            if not self.device_manager.take_screenshot(str(screenshot_file)):
                print(f"❌ 截图失败: {screenshot_file}")
                return None
            
            # 创建页面节点
            page_node = PageNode(
                page_id=page_id,
                title=f"{page_type.capitalize()} Page",
                activity_name="",
                element_count=0,
                elements=[]
            )
            
            # 解析UI层次结构获取元素信息
            elements = self._parse_ui_hierarchy(ui_hierarchy)
            page_node.elements = elements
            page_node.element_count = len(elements)
            
            return page_node
            
        except Exception as e:
            print(f"❌ 捕获页面失败: {e}")
            return None
    
    def _parse_ui_hierarchy(self, ui_hierarchy: str) -> List[Dict[str, Any]]:
        """解析UI层次结构XML"""
        elements = []
        
        try:
            # 简单的XML解析（实际实现可能需要更复杂的解析）
            import re
            
            # 查找所有节点元素
            node_pattern = r'<node[^>]*>(.*?)</node>'
            nodes = re.findall(node_pattern, ui_hierarchy, re.DOTALL)
            
            for node_content in nodes:
                # 提取属性
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
                    # 解析边界框 [left,top,right,bottom]
                    bounds = re.findall(r'\d+', bounds_str)
                    if len(bounds) == 4:
                        element['bounds'] = [int(b) for b in bounds]
                
                # 只添加有意义的元素
                if element.get('text') or element.get('resource_id') or element.get('content_desc'):
                    elements.append(element)
            
        except Exception as e:
            print(f"❌ 解析UI层次结构失败: {e}")
        
        return elements
    
    def _load_config(self) -> Dict[str, Any]:
        """从config.yaml加载配置"""
        try:
            import yaml
            import os
            
            # 获取当前文件所在目录
            current_dir = os.path.dirname(os.path.abspath(__file__))
            # 获取项目根目录
            project_root = os.path.dirname(current_dir)
            config_path = os.path.join(project_root, 'config.yaml')
            
            if not os.path.exists(config_path):
                print(f"⚠️ 配置文件不存在: {config_path}")
                return {}
            
            with open(config_path, 'r', encoding='utf-8') as f:
                config = yaml.safe_load(f)
            
            print("✅ 配置加载成功")
            return config or {}
            
        except Exception as e:
            print(f"❌ 加载配置失败: {e}")
            return {}