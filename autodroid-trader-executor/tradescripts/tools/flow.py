from dataclasses import dataclass
from typing import Dict, List, Optional, Callable, Tuple
from pathlib import Path
import xml.etree.ElementTree as ET
import yaml

from page import PageMatcher, PageExecutor, PageFingerprint, preprocess_xml_for_parsing, parse_xml
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

    @property
    def page_matcher(self) -> PageMatcher:
        return self._page_matcher

    @property
    def end_pages(self) -> List[str]:
        return self._end_pages

    def get_flow_dir(self, apk_package: str, flow_name: str) -> Path:
        return self.apk_dir / apk_package / flow_name

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

    def load_and_build_fingerprints(
        self,
        apk_package: str = "com.tdx.androidCCZQ",
        flow_name: str = "general",
        preprocess_func=None,
        exclude_shared_elements: bool = False,
        min_shared_pages: int = 2
    ) -> LoadResult:
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
                fingerprint = self._page_matcher.add_fingerprint_from_xml(root, page_id)

                action_count = len(fingerprint.action_elements)
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

        return LoadResult(loaded_count=loaded_count, page_info=page_info)

    def identify_page(self, live_xml: str, method: int = 0) -> Tuple[Optional[str], float, List[Tuple[str, float, Dict]]]:
        """
        识别当前页面
        
        Args:
            live_xml: 实时页面XML
            method: 匹配方式 (0=混合方案, 1=快速精确匹配, 2=结构化特征匹配)
        
        Returns:
            (page_id, score, all_scores)
        """
        if not self._page_matcher.page_fingerprints:
            print("⚠️ 没有加载任何页面")
            return (None, 0.0, [])

        if method == 1:
            quick_result = self._page_matcher.quick_match(live_xml)
            if quick_result:
                page_id = quick_result.replace("exact:", "")
                return (page_id, 1.0, [(page_id, 1.0, {"method": "quick_match"})])
            return (None, 0.0, [])
        elif method == 2:
            page_id, score = self._page_matcher.structural_match(live_xml)
            if page_id and score >= 0.5:
                return (page_id, score, [(page_id, score, {"method": "structural_match"})])
            return (None, 0.0, [])
        else:
            quick_result = self._page_matcher.quick_match(live_xml)
            if quick_result:
                page_id = quick_result.replace("exact:", "")
                return (page_id, 1.0, [(page_id, 1.0, {"method": "quick_match"})])
            
            page_id, score = self._page_matcher.structural_match(live_xml)
            if page_id and score >= 0.5:
                return (page_id, score, [(page_id, score, {"method": "structural_match"})])
            
            live_root = parse_xml(live_xml)
            return self._page_matcher.identify_page(live_root)

    def execute_page_steps(self, page_id: str, live_xml: str, refresh_page_callback: Optional[Callable[[], str]] = None) -> bool:
        current_page_id, _, _ = self.identify_page(live_xml)
        is_end_page = current_page_id in self._end_pages if current_page_id else False
        
        if is_end_page:
            print(f"\n🏁 成功到达结束页面: {current_page_id}")
            print(f"✅ general 流程执行完成！\n")
            return True
        
        live_root = parse_xml(live_xml)
        return self._page_executor.execute_steps(page_id, live_root, self._execute_action_callback, self._end_pages, refresh_page_callback)

    def _execute_action_callback(self, step: int, action: str, elem_info: dict, live_elem) -> bool:
        if not self._element_executor:
            print(f"  ⚠️ 未初始化 ElementExecutor，无法执行动作")
            return False
        
        step_info = StepInfo(
            step=step,
            action=action,
            element=elem_info,
            name=elem_info.get("name", ""),
            value=elem_info.get("value", ""),
            save_to=elem_info.get("save_to", ""),
            desc=elem_info.get("desc", "")
        )
        return self._element_executor.execute_action(step_info, live_elem)

    def check_current_page(self, live_xml: str) -> Tuple[Optional[str], bool]:
        current_page_id, score, _ = self.identify_page(live_xml)
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
