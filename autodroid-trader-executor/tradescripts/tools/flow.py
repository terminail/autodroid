from dataclasses import dataclass
from typing import Dict, List, Optional, Callable, Tuple
from pathlib import Path
import xml.etree.ElementTree as ET

from page import PageMatcher, PageFingerprint, preprocess_xml_for_parsing, parse_xml


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
    def __init__(self, apk_dir: Path):
        self.apk_dir = apk_dir
        self._page_matcher = PageMatcher()

    @property
    def page_matcher(self) -> PageMatcher:
        return self._page_matcher

    def get_flow_dir(self, apk_package: str, flow_name: str) -> Path:
        return self.apk_dir / apk_package / flow_name

    def load_flow_pages(self, apk_package: str = "com.tdx.androidCCZQ", flow_name: str = "general") -> List[Path]:
        flow_dir = self.get_flow_dir(apk_package, flow_name)
        if not flow_dir.exists():
            return []
        return sorted(flow_dir.glob("*.xml"))

    def load_and_build_fingerprints(
        self,
        apk_package: str = "com.tdx.androidCCZQ",
        flow_name: str = "general",
        preprocess_func=None
    ) -> LoadResult:
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
        return LoadResult(loaded_count=loaded_count, page_info=page_info)

    def identify_page(self, live_xml: str) -> Tuple[Optional[str], float]:
        if not self._page_matcher.page_fingerprints:
            print("⚠️ 没有加载任何页面")
            return (None, 0.0)

        live_root = parse_xml(live_xml)
        return self._page_matcher.identify_page(live_root)

    def execute_page_steps(self, page_id: str, live_xml: str, execute_action: Callable) -> bool:
        live_root = parse_xml(live_xml)
        return self._page_matcher.execute_steps(page_id, live_root, execute_action)
