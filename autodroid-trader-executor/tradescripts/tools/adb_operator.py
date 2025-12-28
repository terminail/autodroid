import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Optional, Dict, List, Tuple, Set
from dataclasses import dataclass
from enum import Enum
import uiautomator2 as u2

AUTODROID_NS = "https://autodroid.example.com"
AUTODROID_ACTION = f"{{{AUTODROID_NS}}}action"
AUTODROID_STEP = f"{{{AUTODROID_NS}}}step"
AUTODROID_NAME = f"{{{AUTODROID_NS}}}name"
AUTODROID_VALUE = f"{{{AUTODROID_NS}}}value"
AUTODROID_SAVE_TO = f"{{{AUTODROID_NS}}}save_to"
AUTODROID_DESC = f"{{{AUTODROID_NS}}}desc"
AUTODROID_WAIT_AFTER = f"{{{AUTODROID_NS}}}wait_after"
AUTODROID_ID = f"{{{AUTODROID_NS}}}id"
AUTODROID_HELP = f"{{{AUTODROID_NS}}}help"


class ActionType(Enum):
    CLICK = "click"
    INPUT = "input"
    SELECT = "select"
    GET_TEXT = "get_text"
    WAIT = "wait"
    SWIPE = "swipe"
    VERIFY = "verify"
    WAIT_FOR_USER = "wait_for_user"
    PRESS_KEY = "press_key"


@dataclass
class StepInfo:
    step: int
    action: str
    element: Dict
    name: Optional[str] = None
    value: Optional[str] = None
    save_to: Optional[str] = None
    desc: Optional[str] = None


@dataclass
class PageInfo:
    page_id: str
    xml_path: Path
    steps: List[StepInfo]


def _preprocess_xml_for_parsing(xml_content: str) -> str:
    return xml_content


class ADBAutoOpTool:
    def __init__(
        self,
        device_id: str = "TDCDU17905004388",
        config_path: str = r"d:\git\autodroid\autodroid-trader-executor\tradescripts\config.yaml"
    ):
        self.device_id = device_id
        self.config_path = Path(config_path)
        self.test_data: Dict[str, str] = {}
        self.runtime_context: Dict[str, str] = {}
        self.page_fingerprints: Dict[str, Dict] = {}
        self._d: Optional[u2.Device] = None

        self._load_config()
        self._load_flow_pages()

    def _load_config(self):
        import yaml
        if self.config_path.exists():
            with open(self.config_path, 'r', encoding='utf-8') as f:
                config = yaml.safe_load(f)
            apk_dir = config.get("apk_dir", "")
            if not apk_dir:
                apk_dir = config.get("apks", {}).get("path", "")
            if apk_dir:
                if not Path(apk_dir).is_absolute():
                    apk_dir = self.config_path.parent / apk_dir
                self.apk_dir = Path(apk_dir)
            else:
                self.apk_dir = self.config_path.parent / "apks"
        else:
            self.apk_dir = self.config_path.parent.parent / "apks"
        print(f"📂 APK目录: {self.apk_dir}")

    def _get_flow_dir(self, apk_package: str, flow_name: str) -> Path:
        return self.apk_dir / apk_package / flow_name

    def _load_flow_pages(self, apk_package: str = "com.tdx.androidCCZQ", flow_name: str = "general"):
        flow_dir = self._get_flow_dir(apk_package, flow_name)
        print(f"📂 加载业务流程页面: {flow_dir}")

        if not flow_dir.exists():
            print(f"❌ 目录不存在: {flow_dir}")
            return

        loaded_count = 0
        for xml_file in sorted(flow_dir.glob("*.xml")):
            try:
                with open(xml_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                content = _preprocess_xml_for_parsing(content)
                root = ET.fromstring(content)

                page_id = xml_file.stem

                fingerprint = self._build_page_fingerprint(root, page_id)
                self.page_fingerprints[page_id] = fingerprint

                action_count = len(fingerprint['action_elements'])
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

    def _build_page_fingerprint(self, root: ET.Element, page_id: str) -> Dict:
        action_elements = []
        help_elements = []
        id_to_elem = {}
        help_to_action = {}

        all_elems = list(root.iter())
        parent_map = {child: elem for elem in all_elems for child in elem}

        for i, elem in enumerate(all_elems):
            if elem.tag == "hierarchy":
                continue

            elem_id = elem.get(AUTODROID_ID) or elem.get("id")
            elem_help = elem.get(AUTODROID_HELP) or elem.get("help")
            action = elem.get(AUTODROID_ACTION) or elem.get("action")

            if elem_id:
                id_to_elem[elem_id] = (elem, i)

            if elem_help:
                help_elements.append({
                    "help": elem_help,
                    "text": elem.get("text", ""),
                    "resource_id": elem.get("resource-id", ""),
                    "content_desc": elem.get("content-desc", ""),
                    "index": i,
                })

            if action:
                child_texts = self._get_child_texts(elem)
                bounds_str = elem.get("bounds", "")
                action_bounds = self._parse_bounds(bounds_str)

                action_type = action if action else "click"

                elem_info = {
                    "resource_id": elem.get("resource-id", ""),
                    "text": elem.get("text", ""),
                    "content_desc": elem.get("content-desc", ""),
                    "class": elem.get("class", ""),
                    "children": child_texts,
                    "action": action_type,
                    "step": elem.get(AUTODROID_STEP) or elem.get("step"),
                    "name": elem.get(AUTODROID_NAME) or elem.get("name"),
                    "value": elem.get(AUTODROID_VALUE) or elem.get("value"),
                    "save_to": elem.get(AUTODROID_SAVE_TO) or elem.get("save_to"),
                    "desc": elem.get(AUTODROID_DESC) or elem.get("desc"),
                    "wait_after": elem.get(AUTODROID_WAIT_AFTER) or elem.get("wait_after"),
                    "id": elem_id,
                    "index": i,
                    "bounds": bounds_str,
                }

                if action_bounds:
                    overlap_info = self._find_overlapping_visible_element(all_elems, action_bounds)
                    if overlap_info:
                        elem_info["overlap"] = overlap_info

                action_elements.append(elem_info)

        for help_elem in help_elements:
            help_id = help_elem.get("help")
            if help_id and help_id in id_to_elem:
                action_elem, action_idx = id_to_elem[help_id]
                help_idx = help_elem.get("index")

                path_from_help = self._compute_relative_path(all_elems, help_idx, action_idx, parent_map)
                if path_from_help:
                    help_to_action[help_id] = {
                        "path": path_from_help,
                        "action_index": action_idx,
                    }

        step_counter = 1
        for elem in action_elements:
            if not elem.get("step"):
                elem["step"] = step_counter
            step_counter += 1

        action_elements.sort(key=lambda x: int(x.get("step", 0)))

        all_resource_ids = []
        all_texts = []
        all_content_descs = []
        all_classes = []
        clickable_elements = []
        long_clickable_elements = []
        visible_texts = []
        all_bounds = []
        all_bounds_set = set()
        clickable_count = 0
        long_clickable_count = 0

        for i, elem in enumerate(all_elems):
            if elem.tag == "hierarchy":
                continue

            rid = elem.get("resource-id", "").strip()
            text = elem.get("text", "").strip()
            content_desc = elem.get("content-desc", "").strip()
            class_name = elem.get("class", "").strip()
            bounds = elem.get("bounds", "")
            clickable = elem.get("clickable", "").strip() == "true"
            long_clickable = elem.get("long-clickable", "").strip() == "true"

            if rid:
                all_resource_ids.append(rid)
            if text:
                all_texts.append(text)
                visible_texts.append(text)
            if content_desc:
                all_content_descs.append(content_desc)
            if class_name:
                all_classes.append(class_name)
            if bounds:
                all_bounds.append(bounds)
                all_bounds_set.add(bounds)

            if clickable:
                clickable_count += 1
                if bounds:
                    xpath = self._build_xpath(elem, all_elems, parent_map)
                    clickable_elements.append({
                        "index": i,
                        "bounds": bounds,
                        "xpath": xpath,
                        "text": text,
                        "resource_id": rid,
                    })

            if long_clickable:
                long_clickable_count += 1
                if bounds:
                    xpath = self._build_xpath(elem, all_elems, parent_map)
                    long_clickable_elements.append({
                        "index": i,
                        "bounds": bounds,
                        "xpath": xpath,
                        "text": text,
                        "resource_id": rid,
                    })

        return {
            "page_id": page_id,
            "action_elements": action_elements,
            "help_elements": help_elements,
            "help_to_action": help_to_action,
            "resource_ids": all_resource_ids,
            "texts": all_texts,
            "content_descs": all_content_descs,
            "classes": all_classes,
            "clickable_count": clickable_count,
            "long_clickable_count": long_clickable_count,
            "total_clickable": clickable_count + long_clickable_count,
            "clickable_elements": clickable_elements,
            "long_clickable_elements": long_clickable_elements,
            "visible_texts": visible_texts,
            "total_bounds": len(all_bounds),
            "bounds_set": list(all_bounds_set),
        }

    def _build_xpath(self, elem: ET.Element, all_elems: List[ET.Element], parent_map: Dict) -> str:
        path_parts = []
        current = elem
        while current is not None and current.tag != "hierarchy":
            parent = parent_map.get(current)
            if parent is None:
                break

            siblings = list(parent)
            for idx, sibling in enumerate(siblings):
                if sibling is current:
                    path_parts.append(f"{current.tag}[{idx}]")
                    break
            current = parent

        path_parts.reverse()
        return "//" + "/".join(path_parts) if path_parts else ""

    def _compute_relative_path(self, all_elems: List[ET.Element], from_idx: int, to_idx: int, parent_map: Dict[ET.Element, ET.Element]) -> Optional[List[str]]:
        if from_idx >= len(all_elems) or to_idx >= len(all_elems):
            return None

        from_elem = all_elems[from_idx]
        to_elem = all_elems[to_idx]

        from_ancestors = []
        elem = from_elem
        while elem is not None and elem.tag != "hierarchy":
            from_ancestors.append(elem)
            elem = parent_map.get(elem)

        to_ancestors = []
        elem = to_elem
        while elem is not None and elem.tag != "hierarchy":
            to_ancestors.append(elem)
            elem = parent_map.get(elem)

        lca_idx = -1
        for i in range(min(len(from_ancestors), len(to_ancestors))):
            if from_ancestors[i] is to_ancestors[i]:
                lca_idx = i
            else:
                break

        if lca_idx == -1:
            return None

        steps_up = len(from_ancestors) - lca_idx - 1
        steps_down = len(to_ancestors) - lca_idx - 1

        path = []
        for _ in range(steps_up):
            path.append("..")

        to_reversed = list(reversed(to_ancestors[lca_idx + 1:]))
        for elem in to_reversed:
            siblings = list(elem.parent)
            for i, sibling in enumerate(siblings):
                if sibling is elem:
                    path.append(f"[{i}]")
                    break

        return path if path else None

    def _get_child_texts(self, elem: ET.Element) -> List[str]:
        child_texts = []
        for child in elem.iter():
            if child.tag == "hierarchy":
                continue
            text = child.get("text", "")
            if text:
                child_texts.append(text)
        return child_texts

    @property
    def d(self) -> u2.Device:
        if self._d is None:
            self._d = u2.connect(self.device_id)
        return self._d

    def dump_hierarchy(self) -> str:
        return self.d.dump_hierarchy()

    def _parse_live_xml(self, live_xml: str) -> ET.Element:
        content = _preprocess_xml_for_parsing(live_xml)
        return ET.fromstring(content.encode("utf-8"))

    def _find_in_live_xml(self, live_root: ET.Element, offline_elem: Dict, page_fingerprint: Dict = None) -> Tuple[Optional[ET.Element], str]:
        offline_resource_id = offline_elem.get("resource_id", "").strip()
        offline_text = offline_elem.get("text", "").strip()
        offline_content_desc = offline_elem.get("content_desc", "").strip()
        offline_children = offline_elem.get("children", [])
        offline_bounds = offline_elem.get("bounds", "").strip()

        if not offline_resource_id and not offline_text and not offline_content_desc and not offline_children and not offline_bounds:
            return (None, "")

        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            live_bounds = elem.get("bounds", "").strip()
            if offline_bounds and live_bounds == offline_bounds:
                return (elem, f"bounds定位: {offline_bounds}")

        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue

            live_resource_id = elem.get("resource-id", "").strip()
            if offline_resource_id and live_resource_id == offline_resource_id:
                return (elem, f"resource-id定位: {offline_resource_id}")

        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue

            live_text = elem.get("text", "").strip()
            if offline_text and live_text == offline_text:
                return (elem, f"text定位: {offline_text}")

        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue

            live_content_desc = elem.get("content-desc", "").strip()
            if offline_content_desc and live_content_desc == offline_content_desc:
                return (elem, f"content-desc定位: {offline_content_desc}")

        if offline_children:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue

                live_children = self._get_child_texts(elem)
                if live_children:
                    if set(offline_children) & set(live_children):
                        return (elem, f"children文本定位: {offline_children}")

        if page_fingerprint and offline_elem.get("id"):
            found = self._find_action_via_help(live_root, offline_elem.get("id"), page_fingerprint)
            if found:
                return (found, f"help相对路径定位")

        if page_fingerprint and offline_elem.get("overlap"):
            found = self._find_action_via_overlap(live_root, offline_elem, page_fingerprint)
            if found:
                return (found, f"bounds重叠定位")

        return (None, "")

    def _find_action_via_help(self, live_root: ET.Element, action_id: str, page_fingerprint: Dict) -> Optional[ET.Element]:
        help_to_action = page_fingerprint.get("help_to_action", {})
        if action_id not in help_to_action:
            return None

        path_info = help_to_action[action_id]
        relative_path = path_info.get("path", [])
        if not relative_path:
            return None

        help_elements = page_fingerprint.get("help_elements", [])
        target_help = None
        for help_info in help_elements:
            if help_info.get("help") == action_id:
                target_help = help_info
                break

        if not target_help:
            return None

        help_text = target_help.get("text", "").strip()
        help_resource_id = target_help.get("resource_id", "").strip()
        help_content_desc = target_help.get("content_desc", "").strip()

        help_elem = None
        if help_resource_id:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_resource_id = elem.get("resource-id", "").strip()
                if live_resource_id == help_resource_id:
                    help_elem = elem
                    break

        if not help_elem and help_text:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_text = elem.get("text", "").strip()
                if live_text == help_text:
                    help_elem = elem
                    break

        if not help_elem and help_content_desc:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_content_desc = elem.get("content-desc", "").strip()
                if live_content_desc == help_content_desc:
                    help_elem = elem
                    break

        if help_elem:
            parent_map = self._build_parent_map(live_root)
            action_elem = self._follow_relative_path(help_elem, relative_path, parent_map)
            if action_elem:
                return action_elem

        return None

    def _build_parent_map(self, root: ET.Element) -> Dict[ET.Element, ET.Element]:
        return {child: elem for elem in root.iter() for child in elem}

    def _follow_relative_path(self, from_elem: ET.Element, path: List[str], parent_map: Dict[ET.Element, ET.Element]) -> Optional[ET.Element]:
        current = from_elem

        for step in path:
            if step == "..":
                current = parent_map.get(current)
                if current is None or current.tag == "hierarchy":
                    return None
            else:
                match = re.match(r'\[(\d+)\]', step)
                if not match:
                    continue
                child_idx = int(match.group(1))
                children = list(current)
                if child_idx < len(children):
                    current = children[child_idx]
                else:
                    return None

        return current

    def _find_action_via_overlap(self, live_root: ET.Element, offline_elem: Dict, page_fingerprint: Dict) -> Optional[ET.Element]:
        overlap_info = offline_elem.get("overlap", {})
        if not overlap_info:
            return None

        target_text = overlap_info.get("text", "").strip()
        target_resource_id = overlap_info.get("resource_id", "").strip()
        target_content_desc = overlap_info.get("content_desc", "").strip()
        target_overlap = overlap_info.get("overlap")

        if not target_text and not target_resource_id and not target_content_desc:
            return None

        help_elem = None
        if target_resource_id:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_resource_id = elem.get("resource-id", "").strip()
                if live_resource_id == target_resource_id:
                    help_elem = elem
                    break

        if not help_elem and target_text:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_text = elem.get("text", "").strip()
                if live_text == target_text:
                    help_elem = elem
                    break

        if not help_elem and target_content_desc:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_content_desc = elem.get("content-desc", "").strip()
                if live_content_desc == target_content_desc:
                    help_elem = elem
                    break

        if help_elem and target_overlap:
            overlap_bounds = tuple(target_overlap)
            center_x, center_y = self._calculate_center(overlap_bounds)
            return help_elem

        return None

    def _calculate_page_similarity(self, live_root: ET.Element, page_fingerprint: Dict) -> float:
        action_elements = page_fingerprint["action_elements"]
        if not action_elements:
            return 0.0

        matched_count = 0
        for offline_elem in action_elements:
            if self._find_in_live_xml(live_root, offline_elem, page_fingerprint):
                matched_count += 1

        return matched_count / len(action_elements)

    def _calculate_multi_strategy_score(self, live_root: ET.Element, page_fingerprint: Dict) -> Dict[str, float]:
        live_bounds_set = set()
        live_resource_ids = set()
        live_texts = set()
        live_content_descs = set()
        live_clickable_count = 0

        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            bounds = elem.get("bounds", "").strip()
            rid = elem.get("resource-id", "").strip()
            text = elem.get("text", "").strip()
            content_desc = elem.get("content-desc", "").strip()
            clickable = elem.get("clickable", "").strip() == "true"

            if bounds:
                live_bounds_set.add(bounds)
            if rid:
                live_resource_ids.add(rid)
            if text:
                live_texts.add(text)
            if content_desc:
                live_content_descs.add(content_desc)
            if clickable:
                live_clickable_count += 1

        page_bounds_set = set(page_fingerprint.get("bounds_set", []))
        page_resource_ids = set(page_fingerprint.get("resource_ids", []))
        page_texts = set(page_fingerprint.get("texts", []))
        page_content_descs = set(page_fingerprint.get("content_descs", []))
        page_clickable_count = page_fingerprint.get("clickable_count", 0)

        bounds_overlap = len(live_bounds_set & page_bounds_set)
        id_overlap = len(live_resource_ids & page_resource_ids)
        text_overlap = len(live_texts & page_texts)
        content_desc_overlap = len(live_content_descs & page_content_descs)

        bounds_score = bounds_overlap / len(page_bounds_set) if page_bounds_set else 0
        id_score = id_overlap / len(page_resource_ids) if page_resource_ids else 0
        text_score = text_overlap / len(page_texts) if page_texts else 0
        content_desc_score = content_desc_overlap / len(page_content_descs) if page_content_descs else 0
        clickable_score = 1.0 - abs(live_clickable_count - page_clickable_count) / max(page_clickable_count, 1) if page_clickable_count > 0 else 0.5

        return {
            "bounds_score": bounds_score,
            "id_score": id_score,
            "text_score": text_score,
            "content_desc_score": content_desc_score,
            "clickable_score": clickable_score,
            "bounds_overlap": bounds_overlap,
            "total_bounds": len(page_bounds_set),
        }

    def identify_page(self, live_xml: Optional[str] = None) -> Tuple[Optional[str], float]:
        if live_xml is None:
            live_xml = self.dump_hierarchy()

        try:
            live_root = self._parse_live_xml(live_xml)
        except ET.ParseError as e:
            print(f"❌ XML解析错误: {e}")
            return (None, 0.0)

        if not self.page_fingerprints:
            print("⚠️ 没有加载任何页面")
            return (None, 0.0)

        live_bounds_set = set()
        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            bounds = elem.get("bounds", "").strip()
            if bounds:
                live_bounds_set.add(bounds)

        print("=" * 70)
        print("📋 页面匹配分析")
        print("=" * 70)
        print(f"已加载 {len(self.page_fingerprints)} 个离线页面")
        print(f"当前页面 Bounds 数量: {len(live_bounds_set)}")
        print("-" * 70)

        exact_match_id = None
        for page_id, page_data in self.page_fingerprints.items():
            page_bounds_set = set(page_data.get("bounds_set", []))
            if page_bounds_set and page_bounds_set == live_bounds_set:
                print(f"✅ Bounds 精确匹配: {page_id}")
                print("=" * 70)
                return (page_id, 1.0)

        print("未找到 Bounds 精确匹配，使用多策略评分...")
        print("-" * 70)
        print(f"{'页面':<20} {'综合分':>8} {'Bounds':>12} {'ID':>6} {'文本':>6} {'内容':>6} {'可点':>6}")
        print("-" * 70)

        print("未找到 Bounds 精确匹配，使用多策略评分...")
        print("-" * 70)
        print(f"{'页面':<20} {'综合分':>8} {'Bounds':>12} {'ID':>6} {'文本':>6} {'内容':>6} {'可点':>6}")
        print("-" * 70)

        best_match_id = None
        best_score = 0.0
        all_scores: Dict[str, Dict] = {}

        for page_id, page_data in self.page_fingerprints.items():
            multi_scores = self._calculate_multi_strategy_score(live_root, page_data)

            weights = {
                "bounds_score": 0.40,
                "id_score": 0.20,
                "text_score": 0.25,
                "content_desc_score": 0.10,
                "clickable_score": 0.05,
            }

            combined_score = sum(
                multi_scores[key] * weight
                for key, weight in weights.items()
            )

            fine_score = self._calculate_page_similarity(live_root, page_data) if combined_score > 0.2 else 0.0
            final_score = (combined_score * 0.6) + (fine_score * 0.4)

            all_scores[page_id] = {
                "combined": final_score,
                "details": multi_scores,
            }

            if final_score > best_score:
                best_score = final_score
                best_match_id = page_id

        for page_id in sorted(all_scores.keys()):
            scores = all_scores[page_id]
            d = scores["details"]
            bounds_info = f"{d['bounds_overlap']}/{d['total_bounds']}"
            score_bar = "█" * int(scores["combined"] * 20) + "░" * (20 - int(scores["combined"] * 20))
            match_status = "✓" if scores["combined"] > 0.4 else " "
            print(f"  {match_status} {page_id:<18} [{score_bar}] {scores['combined']:.3f}")
            print(f"    ├── Bounds: {d['bounds_score']:.2f} ({bounds_info}) | ID: {d['id_score']:.2f} | 文本: {d['text_score']:.2f}")
            print(f"    └── 内容描述: {d['content_desc_score']:.2f} | 可点击: {d['clickable_score']:.2f}")

        print("-" * 70)
        if best_match_id and best_score > 0.4:
            print(f"✅ 最佳匹配: {best_match_id} (分数: {best_score:.3f})")
            print("=" * 70)
            return (best_match_id, best_score)
        else:
            print("❌ 未匹配到任何离线页面")
            print(f"   最高分数: {best_match_id} ({best_score:.3f})" if best_match_id else "   没有可用的页面指纹")
            print("=" * 70)
            return (None, best_score)

    def _calculate_coarse_score(self, live_root: ET.Element, page_fingerprint: Dict) -> float:
        live_resource_ids = set()
        live_texts = set()
        live_visible_texts = set()
        live_clickable_count = 0

        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            rid = elem.get("resource-id", "").strip()
            text = elem.get("text", "").strip()
            clickable = elem.get("clickable", "").strip() == "true"

            if rid:
                live_resource_ids.add(rid)
            if text:
                live_texts.add(text)
                live_visible_texts.add(text)
            if clickable:
                live_clickable_count += 1

        page_resource_ids = set(page_fingerprint.get("resource_ids", []))
        page_texts = set(page_fingerprint.get("texts", []))
        page_clickable_count = page_fingerprint.get("clickable_count", 0)

        id_overlap = len(live_resource_ids & page_resource_ids) if page_resource_ids else 0
        text_overlap = len(live_texts & page_texts) if page_texts else 0

        id_score = id_overlap / len(page_resource_ids) if page_resource_ids else 0
        text_score = text_overlap / len(page_texts) if page_texts else 0

        clickable_score = 1.0 - abs(live_clickable_count - page_clickable_count) / max(page_clickable_count, 1) if page_clickable_count > 0 else 0.5

        combined_score = (id_score * 0.4) + (text_score * 0.4) + (clickable_score * 0.2)

        return combined_score

    def _parse_bounds(self, bounds_str: str) -> Optional[Tuple[int, int, int, int]]:
        if not bounds_str:
            return None
        try:
            parts = bounds_str.strip("[]").split("][")
            x1, y1 = map(int, parts[0].split(","))
            x2, y2 = map(int, parts[1].split(","))
            return (x1, y1, x2, y2)
        except:
            return None

    def _calculate_center(self, bounds: Tuple[int, int, int, int]) -> Tuple[int, int]:
        x1, y1, x2, y2 = bounds
        return ((x1 + x2) // 2, (y1 + y2) // 2)

    def _calculate_overlap(self, bounds1: Tuple[int, int, int, int], bounds2: Tuple[int, int, int, int]) -> Optional[Tuple[int, int, int, int]]:
        x1 = max(bounds1[0], bounds2[0])
        y1 = max(bounds1[1], bounds2[1])
        x2 = min(bounds1[2], bounds2[2])
        y2 = min(bounds1[3], bounds2[3])

        if x1 < x2 and y1 < y2:
            return (x1, y1, x2, y2)
        return None

    def _find_overlapping_visible_element(self, all_elems: List[ET.Element], action_bounds: Tuple[int, int, int, int]) -> Optional[Dict]:
        overlapping_elements = []

        for elem in all_elems:
            if elem.tag == "hierarchy":
                continue

            text = elem.get("text", "").strip()
            content_desc = elem.get("content-desc", "").strip()
            resource_id = elem.get("resource-id", "").strip()

            if not (text or content_desc or resource_id):
                continue

            bounds_str = elem.get("bounds", "")
            elem_bounds = self._parse_bounds(bounds_str)
            if not elem_bounds:
                continue

            overlap = self._calculate_overlap(action_bounds, elem_bounds)
            if overlap and text:
                overlapping_elements.append({
                    "text": text,
                    "content_desc": content_desc,
                    "resource_id": resource_id,
                    "bounds": bounds_str,
                    "overlap": overlap
                })

        if not overlapping_elements:
            return None

        best_match = overlapping_elements[0]
        for elem in overlapping_elements:
            overlap_area = (elem['overlap'][2] - elem['overlap'][0]) * (elem['overlap'][3] - elem['overlap'][1])
            best_area = (best_match['overlap'][2] - best_match['overlap'][0]) * (best_match['overlap'][3] - best_match['overlap'][1])
            if overlap_area > best_area:
                best_match = elem

        return best_match

    def _execute_action_on_live_element(self, step_info: StepInfo, live_elem: ET.Element) -> bool:
        action = step_info.action
        elem_name = step_info.element.get("desc") or step_info.element.get("text") or step_info.element.get("resource_id", "").split("/")[-1] or "unknown"

        if action == "click":
            try:
                overlap_info = step_info.element.get("overlap", {})
                if overlap_info:
                    overlap_bounds = tuple(overlap_info.get("overlap", []))
                    if overlap_bounds and len(overlap_bounds) == 4:
                        cx, cy = self._calculate_center(overlap_bounds)
                        self.d.click(cx, cy)
                        overlap_text = overlap_info.get("text", "")
                        print(f"  ✓ 点击 [{elem_name}] @ ({cx}, {cy}) [基于重叠元素: '{overlap_text}']")
                        return True

                bounds = self._parse_bounds(live_elem.get("bounds", ""))
                if bounds:
                    cx, cy = self._calculate_center(bounds)
                    self.d.click(cx, cy)
                    print(f"  ✓ 点击 [{elem_name}] @ ({cx}, {cy})")
                    return True
                else:
                    live_elem_elem = self.d(resourceId=live_elem.get("resource-id", "")) if live_elem.get("resource-id") else None
                    if not live_elem_elem:
                        live_elem_elem = self.d(text=live_elem.get("text", ""))
                    if live_elem_elem and live_elem_elem.exists:
                        live_elem_elem.click()
                        print(f"  ✓ 点击 [{elem_name}]")
                        return True
                    print(f"  ⚠️ 无法获取元素坐标")
                    return False
            except Exception as e:
                print(f"  ⚠️ 点击失败: {e}")
                return False

        elif action == "input":
            target_value = self.test_data.get(step_info.name, step_info.value) if step_info.name else step_info.value
            if target_value:
                try:
                    bounds = self._parse_bounds(live_elem.get("bounds", ""))
                    if bounds:
                        cx, cy = self._calculate_center(bounds)
                        self.d.click(cx, cy)
                    else:
                        live_elem.click()
                    time.sleep(0.3)
                    self.d.clear_text()
                    self.d.send_keys(str(target_value))
                    print(f"  ✓ 输入: [{target_value}] -> [{elem_name}]")
                    return True
                except Exception as e:
                    print(f"  ⚠️ 输入失败: {e}")
                    return False
            else:
                print(f"  ⚠️ 没有可用的输入值")
                return False

        elif action == "get_text":
            try:
                text = live_elem.get("text", "")
                if step_info.save_to:
                    self.runtime_context[step_info.save_to] = text
                    print(f"  ✓ 获取文本: [{text}] -> {step_info.save_to}")
                else:
                    print(f"  ✓ 获取文本: [{text}]")
                return True
            except Exception as e:
                print(f"  ⚠️ 获取文本失败: {e}")
                return False

        elif action == "wait":
            wait_time = float(step_info.value) if step_info.value else 1.0
            try:
                time.sleep(wait_time)
                print(f"  ✓ 等待 {wait_time} 秒")
                return True
            except:
                return False

        elif action == "press_key":
            key_map = {"BACK": 4, "HOME": 3, "MENU": 82}
            key_code = key_map.get(step_info.value, 4)
            try:
                self.d.press(key_code)
                print(f"  ✓ 按键: {step_info.value}")
                return True
            except Exception as e:
                print(f"  ⚠️ 按键失败: {e}")
                return False

        else:
            print(f"  ⚠️ 不支持的动作: {action}")
            return False

    def execute_page_steps(self, page_id: str, live_xml: str) -> bool:
        if page_id not in self.page_fingerprints:
            print(f"❌ 页面不存在: {page_id}")
            return False

        page_data = self.page_fingerprints[page_id]
        action_elements = page_data["action_elements"]

        if not action_elements:
            print(f"\n⚠️ 页面 {page_id} 已匹配，但无 autodroid:action 定义")
            return True

        try:
            live_root = self._parse_live_xml(live_xml)
        except ET.ParseError as e:
            print(f"❌ XML解析错误: {e}")
            return False

        print(f"\n📋 执行页面流程: {page_id}")
        print(f"   步骤数: {len(action_elements)}")
        print("-" * 40)

        for idx, elem_info in enumerate(action_elements, 1):
            step = elem_info.get("step", idx)
            action = elem_info.get("action", "")
            desc = elem_info.get("desc", "")
            name = elem_info.get("name", "")
            value = elem_info.get("value", "")
            save_to = elem_info.get("save_to", "")
            wait_after = elem_info.get("wait_after", "")

            print(f"\n步骤 {step}: {action}")
            if desc:
                print(f"   描述: {desc}")
            if name:
                print(f"   数据键: {name}")
            if value:
                print(f"   默认值: {value}")

            live_elem, locate_method = self._find_in_live_xml(live_root, elem_info)
            if not live_elem:
                elem_name = elem_info.get("text") or elem_info.get("resource_id", "").split("/")[-1] or "unknown"
                print(f"  ⚠️ 未找到元素: {elem_name}")
                return False

            elem_name = elem_info.get("desc") or elem_info.get("text") or elem_info.get("resource_id", "").split("/")[-1] or "unknown"
            print(f"  🔍 {locate_method}")
            print(f"  ✅ 找到元素: text='{live_elem.get('text', '')}', class='{live_elem.get('class', '')}'")

            step_info = StepInfo(
                step=step,
                action=action,
                element=elem_info,
                name=name,
                value=value,
                save_to=save_to,
                desc=desc
            )

            if self._execute_action_on_live_element(step_info, live_elem):
                print(f"  ✓ {action} 完成")
            else:
                print(f"\n⚠️ 步骤 {step} 执行失败")
                return False

            if wait_after:
                try:
                    wait_time = float(wait_after)
                    time.sleep(wait_time)
                    print(f"  ⏸️ 等待 {wait_time} 秒后继续")
                except:
                    pass

        print("\n" + "=" * 40)
        print(f"✅ 页面流程完成: {page_id}")
        return True

    def _print_menu(self):
        print("\n" + "=" * 60)
        print("🚀 ADB Auto Operator")
        print("=" * 60)

    def run_interactive_mode(self):
        self._print_menu()

        while True:
            try:
                user_input = input("\n[?] 是否自动操作当前页面? (y/n/q): ").strip().lower()

                if user_input == 'q':
                    print("\n👋 再见!")
                    break

                elif user_input == 'n':
                    print("⏸️ 等待手动操作...")
                    time.sleep(2)

                elif user_input == 'y':
                    live_xml = self.dump_hierarchy()
                    page_id, score = self.identify_page(live_xml)

                    if page_id:
                        print(f"\n📄 识别页面: {page_id} (匹配度: {score:.2%})")
                        print("-" * 60)
                        self.execute_page_steps(page_id, live_xml)
                    else:
                        print("\n⚠️ 未能识别当前页面")
                        time.sleep(2)
                else:
                    print("\n⚠️ 无效输入，请输入 y/n/q")
                    time.sleep(1)

            except KeyboardInterrupt:
                print("\n👋 再见!")
                break
            except Exception as e:
                print(f"❌ 错误: {e}")
                time.sleep(2)


def main():
    tool = ADBAutoOpTool()
    tool.run_interactive_mode()


if __name__ == "__main__":
    main()
