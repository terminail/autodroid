from dataclasses import dataclass
from typing import Dict, List, Optional, Callable, Tuple
from pathlib import Path
import xml.etree.ElementTree as ET
import yaml

try:
    from .page import PageMatcher, PageExecutor, preprocess_xml_for_parsing, parse_xml, PageInfo
    from .element import ElementExecutor, StepInfo
    from .u2device import U2Device
except ImportError:
    # 当直接运行脚本时使用绝对导入
    from page import PageMatcher, PageExecutor, preprocess_xml_for_parsing, parse_xml, PageInfo
    from element import ElementExecutor, StepInfo
    from u2device import U2Device


@dataclass
class FlowInfo:
    apk_package: str
    flow_name: str
    flow_dir: Path


@dataclass
class LoadResult:
    loaded_count: int
    page_info: Dict[str, int]


class FlowManager:
    def __init__(self, apk_dir: Path, device: Optional[U2Device] = None):
        self.apk_dir = apk_dir
        self.device = device
        self._page_matcher = PageMatcher()
        self._page_executor = PageExecutor(self._page_matcher)
        self._element_executor = None
        if device:
            self._element_executor = ElementExecutor(device)
        self._end_pages: List[str] = []
        self._executed_steps: set = set()
        self._total_steps: int = 0
        self._page_infos: Dict[str, PageInfo] = {}
        self._page_executor.set_executed_steps_callback(self._on_step_executed)
        self._page_executor.set_status_callback(self.get_execution_status)

    @property
    def page_matcher(self) -> PageMatcher:
        return self._page_matcher

    @property
    def end_pages(self) -> List[str]:
        return self._end_pages

    def get_flow_dir(self, apk_package: str, flow_name: str) -> Path:
        return self.apk_dir / apk_package / flow_name

    def _is_page_matched_by_selectors(self, page_id: str, page_info: PageInfo) -> bool:
        """使用选择器检查页面是否匹配，优先使用fingerprint元素进行精确匹配"""
        if not self.device:
            return False
        
        # 优先使用fingerprint元素进行精确匹配
        fingerprint_elements = page_info.fingerprint_elements
        if fingerprint_elements:
            print(f"  🔍 页面 {page_id}: 使用 {len(fingerprint_elements)} 个fingerprint元素进行精确匹配")
            
            # 检查所有fingerprint元素，只要有一个存在就认为页面匹配
            for fp_elem in fingerprint_elements:
                resource_id = fp_elem.resource_id.strip()
                text = fp_elem.text.strip()
                content_desc = fp_elem.content_desc.strip()
                
                print(f"    - 检查fingerprint元素: resource_id='{resource_id}', text='{text}', content_desc='{content_desc}'")
                
                # 使用选择器检查元素是否存在
                if resource_id:
                    selector = f'resourceId("{resource_id}")'
                    if self.device.check_element_exists(selector):
                        print(f"  ✓ 页面 {page_id}: 找到fingerprint元素 {selector}")
                        return True
                    else:
                        print(f"    ✗ 未找到fingerprint元素: {selector}")
                elif text:
                    selector = f'text("{text}")'
                    if self.device.check_element_exists(selector):
                        print(f"  ✓ 页面 {page_id}: 找到fingerprint元素 {selector}")
                        return True
                    else:
                        print(f"    ✗ 未找到fingerprint元素: {selector}")
                elif content_desc:
                    selector = f'description("{content_desc}")'
                    if self.device.check_element_exists(selector):
                        print(f"  ✓ 页面 {page_id}: 找到fingerprint元素 {selector}")
                        return True
                    else:
                        print(f"    ✗ 未找到fingerprint元素: {selector}")
            
            # 如果有fingerprint元素但都没找到，页面不匹配
            print(f"  ✗ 页面 {page_id}: 所有fingerprint元素都未找到")
            return False
        
        # 如果没有fingerprint元素，回退到action元素匹配
        action_elements = page_info.action_elements
        if not action_elements:
            print(f"  ⚠️ 页面 {page_id}: 没有fingerprint元素和action元素")
            return False
        
        print(f"  🔍 页面 {page_id}: 检查 {len(action_elements)} 个action元素（回退方案）")
        
        # 检查所有action元素，只要有任何一个存在就认为页面匹配
        for elem_info in action_elements:
            resource_id = elem_info.resource_id.strip()
            text = elem_info.text.strip()
            content_desc = elem_info.content_desc.strip()
            
            print(f"    - 检查元素: resource_id='{resource_id}', text='{text}', content_desc='{content_desc}'")
            
            # 使用选择器检查元素是否存在
            if resource_id:
                selector = f'resourceId("{resource_id}")'
                if self.device.check_element_exists(selector):
                    print(f"  ✓ 页面 {page_id}: 找到元素 {selector}")
                    return True
                else:
                    print(f"    ✗ 未找到元素: {selector}")
            elif text:
                selector = f'text("{text}")'
                if self.device.check_element_exists(selector):
                    print(f"  ✓ 页面 {page_id}: 找到元素 {selector}")
                    return True
                else:
                    print(f"    ✗ 未找到元素: {selector}")
            elif content_desc:
                selector = f'description("{content_desc}")'
                if self.device.check_element_exists(selector):
                    print(f"  ✓ 页面 {page_id}: 找到元素 {selector}")
                    return True
                else:
                    print(f"    ✗ 未找到元素: {selector}")
        
        return False

    def _load_flow_config(self, apk_package: str, flow_name: str) -> List[str]:
        flow_dir = self.get_flow_dir(apk_package, flow_name)
        config_path = flow_dir / "config.yaml"

        end_pages = []
        if config_path.exists():
            try:
                with open(config_path, 'r', encoding='utf-8') as f:
                    config = yaml.safe_load(f)
                if config and 'ends' in config:
                    for end in config['ends']:
                        layout = end.get('layout', '')
                        if layout:
                            end_pages.append(layout.replace('.xml', ''))
            except Exception as e:
                print(f"  ⚠️ 加载流程配置失败: {e}")

        return end_pages

    def load_flow_pages(self, apk_package: str = "com.tdx.androidCCZQ", flow_name: str = "general") -> List[Path]:
        flow_dir = self.get_flow_dir(apk_package, flow_name)
        if not flow_dir.exists():
            return []
        return sorted(flow_dir.glob("*.xml"))

    def load_and_build_pages(
        self,
        apk_package: str = "com.tdx.androidCCZQ",
        flow_name: str = "general",
        preprocess_func=None
    ) -> LoadResult:
        self.reset_execution_state()
        flow_dir = self.get_flow_dir(apk_package, flow_name)
        print(f"📂 加载业务流程页面: {flow_dir}")

        if not flow_dir.exists():
            print(f"❌ 目录不存在: {flow_dir}")
            return LoadResult(loaded_count=0, page_info={})

        loaded_count = 0
        page_info: Dict[str, int] = {}

        for xml_file in sorted(flow_dir.glob("*.xml")):
            try:
                with open(xml_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                if preprocess_func:
                    content = preprocess_func(content)
                content = preprocess_xml_for_parsing(content)
                root = ET.fromstring(content)

                page_id = xml_file.stem
                # 使用选择器方案加载页面
                self._page_matcher.add_page_info_from_xml(root, page_id)
                
                # 获取动作元素数量
                action_elements = [elem for elem in root.iter() 
                                 if elem.get("{{{}}}action".format("https://autodroid.example.com"))]
                action_count = len(action_elements)
                page_info[page_id] = action_count
                
                if action_count > 0:
                    print(f"  ✓ {xml_file.name} -> {page_id} ({action_count} 个动作)")
                else:
                    print(f"  ⚠️ {xml_file.name} -> {page_id} (无 autodroid:action)")
                loaded_count += 1

            except ET.ParseError as e:
                print(f"  ✗ {xml_file.name}: XML解析错误 - {e}")
            except Exception as e:
                print(f"  ✗ {xml_file.name}: {e}")

        print(f"\n✅ 加载完成: {loaded_count} 个页面\n")

        self._end_pages = self._load_flow_config(apk_package, flow_name)
        if self._end_pages:
            print(f"🏁 结束页面: {self._end_pages}")

        self._total_steps = self._calculate_total_steps()
        print(f"📊 流程总步骤数: {self._total_steps}")

        # 从PageMatcher获取页面信息
        self._page_infos = self._page_matcher.page_infos

        return LoadResult(loaded_count=loaded_count, page_info=page_info)

    def identify_page(self) -> Tuple[Optional[str], float, List[Tuple[str, float, Dict]]]:
        """
        识别当前页面（flow层职责）- 使用选择器方案
        
        Returns:
            (page_id, score, all_scores)
        """
        if not self.device:
            print("⚠️ 未初始化设备，无法进行页面识别")
            return (None, 0.0, [])
        
        try:
            print("🔍 开始选择器方案页面识别...")
            
            # 遍历所有页面，检查是否有匹配的元素
            for page_id, page_info in self._page_infos.items():
                print(f"  🔍 检查页面: {page_id}")
                if self._is_page_matched_by_selectors(page_id, page_info):
                    print(f"✅ 识别到页面: {page_id} (选择器方案)")
                    return (page_id, 1.0, [(page_id, 1.0, {"method": "selector"})])
            
            # 未找到匹配的页面
            print("❌ 未找到匹配的页面")
            print("⚠️ 请用户手动操作到已知页面，或检查页面XML是否定义了fingerprint元素")
            print("   提示：可以使用 autodroid:fingerprint='true' 标记页面唯一元素以提高识别准确率")
            return (None, 0.0, [])
        except Exception as e:
            print(f"⚠️ 页面识别失败: {e}")
            import traceback
            traceback.print_exc()
            return (None, 0.0, [])

    def execute_page_steps(self, page_id: str, refresh_page_callback: Optional[Callable[[], str]] = None) -> bool:
        current_page_id, _, _ = self.identify_page()
        
        if not current_page_id:
            print(f"\n⚠️ 未能识别当前页面")
            return False
        
        is_end_page = current_page_id in self._end_pages if current_page_id else False
        
        # 检查页面是否有可执行的动作元素
        has_steps = self._page_matcher.has_page_steps(current_page_id)
        
        if not has_steps:
            if is_end_page:
                if len(self._executed_steps) == 0:
                    print(f"\n⚠️ 检测到结束页面 {current_page_id}，但尚未执行任何步骤")
                    print(f"   流程尚未启动，需要先执行步骤才能判断流程完成\n")
                    return False
                
                status = self.get_execution_status()
                print(f"\n🏁 成功到达结束页面: {current_page_id}")
                print(f"📊 流程执行状态:")
                print(f"   已执行步骤: {status['executed_steps']}/{status['total_steps']}")
                print(f"   完成率: {status['completion_rate']:.1%}")
                
                if status['is_complete']:
                    print(f"✅ 流程执行完整正确！\n")
                else:
                    print(f"⚠️ 流程未完整执行，部分步骤未完成\n")
                
                return True
            else:
                print(f"\n⚠️ 页面 {current_page_id} 没有定义步骤，且不是结束页面")
                return False
        
        # 使用选择器方案执行步骤（不需要XML解析）
        return self._page_executor.execute_steps(current_page_id, self._execute_action_callback, self.device, self._end_pages, refresh_page_callback)

    def _execute_action_callback(self, step: int, action: str, elem_info, live_elem) -> bool:
        if not self._element_executor:
            print(f"  ⚠️ 未初始化 ElementExecutor，无法执行动作")
            return False
        
        # elem_info是ElementInfo对象，直接使用
        step_info = StepInfo(
            step=step,
            action=action,
            element=elem_info,
            name=elem_info.name,
            value=elem_info.value,
            save_to=elem_info.save_to,
            desc=elem_info.desc
        )
        return self._element_executor.execute_action(step_info, live_elem)

    def refresh_current_page(self) -> str:
        """刷新当前页面识别"""
        page_id, _, _ = self.identify_page()
        return page_id if page_id else ""

    def check_current_page(self) -> Tuple[Optional[str], bool]:
        current_page_id, score, _ = self.identify_page()
        is_end_page = current_page_id in self._end_pages if current_page_id else False
        return current_page_id, is_end_page

    def reset_execution_state(self):
        """重置执行状态"""
        self._executed_steps = set()
        self._total_steps = 0

    def _on_step_executed(self, step: int, page_id: str = None):
        """步骤执行回调"""
        print(f"  🔍 调试 _on_step_executed: step类型={type(step)}, step值='{step}', page_id='{page_id}'")
        # 确保step是整数
        if not isinstance(step, int):
            print(f"  ⚠️ 警告: step不是整数类型，类型={type(step)}, 值='{step}'")
            try:
                step = int(step)
            except (ValueError, TypeError):
                print(f"  ❌ 错误: 无法将step转换为整数")
                return
        self._executed_steps.add(step)

    def _calculate_total_steps(self) -> int:
        """计算流程中所有页面的总步骤数"""
        total = 0
        # 使用PageMatcher的方法获取总步骤数
        return self._page_matcher.get_total_steps()

    def get_execution_status(self) -> Dict:
        """获取流程执行状态"""
        return {
            "executed_steps": len(self._executed_steps),
            "total_steps": self._total_steps,
            "completion_rate": len(self._executed_steps) / self._total_steps if self._total_steps > 0 else 0.0,
            "is_complete": len(self._executed_steps) == self._total_steps and self._total_steps > 0
        }
