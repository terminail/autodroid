from dataclasses import dataclass
from typing import Dict, List, Optional, Callable, Tuple
from pathlib import Path
import xml.etree.ElementTree as ET
import yaml

try:
    from .page import PageMatcher, PageExecutor, preprocess_xml_for_parsing, parse_xml
    from .element import ElementExecutor, StepInfo
    from .u2device import U2Device
except ImportError:
    # 当直接运行脚本时使用绝对导入
    from page import PageMatcher, PageExecutor, preprocess_xml_for_parsing, parse_xml
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
        self._shared_elements: Dict[str, Dict] = {}
        self._executed_steps: set = set()
        self._total_steps: int = 0
        self._page_infos: Dict = {}
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

    def _quick_identify_page(self) -> Optional[Tuple[str, float, Dict]]:
        """快速页面识别（选择器方案）"""
        if not self.device:
            print("⚠️ 未初始化设备，无法进行快速页面识别")
            return None
        
        try:
            # 使用选择器方案进行快速页面识别
            print("🔍 开始选择器方案页面识别...")
            
            # 遍历所有页面，检查是否有匹配的元素
            for page_id, page_info in self._page_infos.items():
                if self._is_page_matched_by_selectors(page_id, page_info):
                    print(f"✅ 识别到页面: {page_id} (选择器方案)")
                    return (page_id, 1.0, {"method": "selector"})
            
            print("❌ 未找到匹配的页面")
            return None
        except Exception as e:
            print(f"⚠️ 快速页面识别失败: {e}")
            return None

    def _is_page_matched_by_selectors(self, page_id: str, page_info) -> bool:
        """使用选择器检查页面是否匹配"""
        if not self.device:
            return False
        
        # 检查页面是否有action_elements
        action_elements = page_info.action_elements
        if not action_elements:
            return False
        
        # 检查第一个action元素是否存在
        first_elem = action_elements[0]
        resource_id = first_elem.resource_id.strip()
        text = first_elem.text.strip()
        content_desc = first_elem.content_desc.strip()
        
        # 使用选择器检查元素是否存在
        if resource_id:
            if self.device.check_element_exists(f'resourceId("{resource_id}")'):
                return True
        elif text:
            if self.device.check_element_exists(f'text("{text}")'):
                return True
        elif content_desc:
            if self.device.check_element_exists(f'description("{content_desc}")'):
                return True
        
        return False

    def _execute_steps_with_selectors(self, page_id: str) -> bool:
        """使用选择器方案执行页面步骤（flow层职责）"""
        if not self.device:
            print("⚠️ 未初始化设备，无法执行选择器步骤")
            return False
        
        try:
            # 使用PageExecutor执行页面步骤（不需要live_xml）
            return self._page_executor.execute_steps(
                page_id=page_id,
                execute_action=self._execute_action_callback,
                device=self.device,
                end_pages=self._end_pages,
                refresh_page_callback=self.refresh_current_page
            )
        except Exception as e:
            print(f"⚠️ 选择器方案执行失败: {e}")
            return False

    def _build_selector_from_element(self, element: dict) -> Optional[str]:
        """从元素构建选择器（element层职责）"""
        if not self._element_executor:
            print("⚠️ 未初始化ElementExecutor，无法构建选择器")
            return None
        
        try:
            return self._element_executor.build_selector(element)
        except Exception as e:
            print(f"⚠️ 构建选择器失败: {e}")
            return None

    def _execute_selector_action(self, selector: str, action: str, element: dict) -> bool:
        """执行选择器动作"""
        try:
            if action == 'click':
                return self.device.d(selector).click()
            elif action == 'input':
                text = element.get('value', '')
                if text:
                    return self.device.d(selector).set_text(text)
            elif action == 'get_text':
                text = self.device.d(selector).get_text()
                save_to = element.get('save_to', '')
                if save_to:
                    # 保存到运行时上下文（需要实现运行时上下文管理）
                    print(f"📝 保存文本到 {save_to}: {text}")
                return True
            # 其他动作类型...
            return True
        except Exception as e:
            print(f"⚠️ 执行选择器动作失败: {action} - {e}")
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
        preprocess_func=None,
        exclude_shared_elements: bool = False,
        min_shared_pages: int = 2
    ) -> LoadResult:
        self.reset_execution_state()
        flow_dir = self.get_flow_dir(apk_package, flow_name)
        print(f"📂 加载业务流程页面: {flow_dir}")

        if not flow_dir.exists():
            print(f"❌ 目录不存在: {flow_dir}")
            return LoadResult(loaded_count=0, page_info={})

        if exclude_shared_elements:
            print(f"🔍 识别共享元素...")
            self.identify_and_mark_shared_elements(apk_package, flow_name, min_shared_pages)
            self._page_matcher.set_element_filter(self.is_shared_element)
            print(f"✅ 共享元素过滤已启用")

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
        # 使用选择器方案进行页面识别
        if self._page_infos:
            page_id = list(self._page_infos.keys())[0]
            return (page_id, 1.0, [(page_id, 1.0, {"method": "selector"})])
        else:
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
        result = self._quick_identify_page()
        if result:
            page_id, score, _ = result
            return page_id
        return ""

    def check_current_page(self) -> Tuple[Optional[str], bool]:
        current_page_id, score, _ = self.identify_page()
        is_end_page = current_page_id in self._end_pages if current_page_id else False
        return current_page_id, is_end_page

    def identify_and_mark_shared_elements(
        self,
        apk_package: str = "com.tdx.androidCCZQ",
        flow_name: str = "general",
        min_shared_pages: int = 2
    ) -> Dict[str, Dict]:
        """
        识别并标记流程中多个页面共享的元素
        
        Args:
            apk_package: APK包名
            flow_name: 流程名称
            min_shared_pages: 最少在多少个页面中出现才认为是共享元素
        
        Returns:
            共享元素字典 {element_key: {page_ids: [], count: int, ...}}
        """
        flow_dir = self.get_flow_dir(apk_package, flow_name)
        if not flow_dir.exists():
            print(f"❌ 目录不存在: {flow_dir}")
            return {}

        print(f"🔍 扫描共享元素: {flow_dir}")

        element_occurrences: Dict[str, Dict] = {}

        for xml_file in sorted(flow_dir.glob("*.xml")):
            try:
                with open(xml_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                content = preprocess_xml_for_parsing(content)
                root = ET.fromstring(content)

                page_id = xml_file.stem

                for elem in root.iter():
                    if elem.tag == "hierarchy":
                        continue
                    
                    rid = elem.get("resource-id", "").strip()
                    text = elem.get("text", "").strip()
                    bounds = elem.get("bounds", "").strip()
                    class_name = elem.get("class", "").strip()

                    if not rid and not text:
                        continue

                    element_key = f"{rid}|{text}|{bounds}|{class_name}"

                    if element_key not in element_occurrences:
                        element_occurrences[element_key] = {
                            "page_ids": [],
                            "count": 0,
                            "resource_id": rid,
                            "text": text,
                            "bounds": bounds,
                            "class": class_name
                        }

                    if page_id not in element_occurrences[element_key]["page_ids"]:
                        element_occurrences[element_key]["page_ids"].append(page_id)
                        element_occurrences[element_key]["count"] += 1

            except Exception as e:
                print(f"  ⚠️ 处理 {xml_file.name} 时出错: {e}")

        shared_elements = {}
        for key, info in element_occurrences.items():
            if info["count"] >= min_shared_pages:
                shared_elements[key] = info

        self._shared_elements = shared_elements

        print(f"✅ 识别完成: 找到 {len(shared_elements)} 个共享元素")
        if shared_elements:
            print(f"   共享元素出现在 {min_shared_pages} 个或更多页面中")

        return shared_elements

    def is_shared_element(self, element: Dict) -> bool:
        """
        检查元素是否是共享元素
        
        Args:
            element: 元素字典，包含 resource-id, text, bounds, class 等字段
        
        Returns:
            True 如果是共享元素，否则 False
        """
        rid = element.get("resource_id", "").strip()
        text = element.get("text", "").strip()
        bounds = element.get("bounds", "").strip()
        class_name = element.get("class", "").strip()

        element_key = f"{rid}|{text}|{bounds}|{class_name}"
        return element_key in self._shared_elements

    @property
    def shared_elements(self) -> Dict[str, Dict]:
        return self._shared_elements

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
