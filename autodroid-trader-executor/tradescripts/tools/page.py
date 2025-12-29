from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple, Callable
from pathlib import Path
import xml.etree.ElementTree as ET
import time


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


@dataclass
class PageInfo:
    page_id: str
    xml_path: Path
    steps: List[Dict]


@dataclass
class PageFingerprint:
    page_id: str
    action_elements: List[Dict]
    resource_ids: List[str]
    texts: List[str]
    content_descs: List[str]
    classes: List[str]
    bounds_set: List[str]
    clickable_count: int
    long_clickable_count: int
    help_elements: List[Dict]
    help_to_action: Dict
    webview_texts: List[str]


def preprocess_xml_for_parsing(xml_content: str) -> str:
    return xml_content


def parse_xml(xml_content: str) -> ET.Element:
    content = preprocess_xml_for_parsing(xml_content)
    return ET.fromstring(content.encode("utf-8"))


def parse_bounds(bounds_str: str) -> Optional[Tuple[int, int, int, int]]:
    if not bounds_str:
        return None
    try:
        parts = bounds_str.strip("[]").split("][")
        x1, y1 = map(int, parts[0].split(","))
        x2, y2 = map(int, parts[1].split(","))
        return (x1, y1, x2, y2)
    except:
        return None


def calculate_center(bounds: Tuple[int, int, int, int]) -> Tuple[int, int]:
    x1, y1, x2, y2 = bounds
    return ((x1 + x2) // 2, (y1 + y2) // 2)


def calculate_overlap(bounds1: Tuple[int, int, int, int], bounds2: Tuple[int, int, int, int]) -> Optional[Tuple[int, int, int, int]]:
    x1 = max(bounds1[0], bounds2[0])
    y1 = max(bounds1[1], bounds2[1])
    x2 = min(bounds1[2], bounds2[2])
    y2 = min(bounds1[3], bounds2[3])

    if x1 < x2 and y1 < y2:
        return (x1, y1, x2, y2)
    return None


def build_parent_map(root: ET.Element) -> Dict[ET.Element, ET.Element]:
    return {child: elem for elem in root.iter() for child in elem}


def build_xpath(elem: ET.Element, all_elems: List[ET.Element], parent_map: Dict) -> str:
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


def get_child_texts(elem: ET.Element) -> List[str]:
    child_texts = []
    for child in elem.iter():
        if child.tag == "hierarchy":
            continue
        text = child.get("text", "")
        if text:
            child_texts.append(text)
    return child_texts


def compute_relative_path(all_elems: List[ET.Element], from_idx: int, to_idx: int, parent_map: Dict[ET.Element, ET.Element]) -> Optional[List[str]]:
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


def follow_relative_path(from_elem: ET.Element, path: List[str], parent_map: Dict[ET.Element, ET.Element]) -> Optional[ET.Element]:
    current = from_elem

    for step in path:
        if step == "..":
            current = parent_map.get(current)
            if current is None or current.tag == "hierarchy":
                return None
        else:
            import re
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


def build_page_fingerprint(root: ET.Element, page_id: str) -> PageFingerprint:
    action_elements = []
    help_elements = []
    id_to_elem = {}
    help_to_action = {}

    all_elems = list(root.iter())
    parent_map = build_parent_map(root)

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
            child_texts = get_child_texts(elem)
            bounds_str = elem.get("bounds", "")
            action_bounds = parse_bounds(bounds_str)

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
                overlap_info = find_overlapping_visible_element(all_elems, action_bounds)
                if overlap_info:
                    elem_info["overlap"] = overlap_info

            action_elements.append(elem_info)

    for help_elem in help_elements:
        help_id = help_elem.get("help")
        if help_id and help_id in id_to_elem:
            action_elem, action_idx = id_to_elem[help_id]
            help_idx = help_elem.get("index")

            path_from_help = compute_relative_path(all_elems, help_idx, action_idx, parent_map)
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
    clickable_count = 0
    long_clickable_count = 0
    all_bounds_set = set()
    webview_texts = []

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
        if content_desc:
            all_content_descs.append(content_desc)
        if class_name:
            all_classes.append(class_name)
        if bounds and clickable:
            all_bounds_set.add(bounds)

        if clickable:
            clickable_count += 1
        if long_clickable:
            long_clickable_count += 1

        if class_name == "android.webkit.WebView" and text:
            webview_texts.append(text)

    return PageFingerprint(
        page_id=page_id,
        action_elements=action_elements,
        resource_ids=all_resource_ids,
        texts=all_texts,
        content_descs=all_content_descs,
        classes=all_classes,
        bounds_set=list(all_bounds_set),
        clickable_count=clickable_count,
        long_clickable_count=long_clickable_count,
        help_elements=help_elements,
        help_to_action=help_to_action,
        webview_texts=webview_texts,
    )


def find_overlapping_visible_element(all_elems: List[ET.Element], action_bounds: Tuple[int, int, int, int]) -> Optional[Dict]:
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
        elem_bounds = parse_bounds(bounds_str)
        if not elem_bounds:
            continue

        overlap = calculate_overlap(action_bounds, elem_bounds)
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


class PageMatcher:
    def __init__(self):
        self._page_fingerprints: Dict[str, PageFingerprint] = {}

    @property
    def page_fingerprints(self) -> Dict[str, PageFingerprint]:
        return self._page_fingerprints

    def add_fingerprint(self, fingerprint: PageFingerprint):
        self._page_fingerprints[fingerprint.page_id] = fingerprint

    def add_fingerprint_from_xml(self, root: ET.Element, page_id: str) -> PageFingerprint:
        fingerprint = build_page_fingerprint(root, page_id)
        self.add_fingerprint(fingerprint)
        return fingerprint

    def load_pages_from_dir(self, flow_dir: Path, preprocess_func=None):
        if not flow_dir.exists():
            return

        for xml_file in sorted(flow_dir.glob("*.xml")):
            try:
                with open(xml_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                if preprocess_func:
                    content = preprocess_func(content)
                root = ET.fromstring(content)
                page_id = xml_file.stem
                fingerprint = build_page_fingerprint(root, page_id)
                self._page_fingerprints[page_id] = fingerprint
            except ET.ParseError:
                pass

    def calculate_multi_strategy_score(self, live_root: ET.Element, page_fingerprint: PageFingerprint) -> Dict[str, float]:
        live_bounds_list = []
        live_resource_ids = set()
        live_texts = set()
        live_content_descs = set()
        live_clickable_count = 0
        live_webview_texts = set()

        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            bounds = elem.get("bounds", "").strip()
            rid = elem.get("resource-id", "").strip()
            text = elem.get("text", "").strip()
            content_desc = elem.get("content-desc", "").strip()
            clickable = elem.get("clickable", "").strip() == "true"
            class_name = elem.get("class", "").strip()

            if bounds:
                parsed = parse_bounds(bounds)
                if parsed:
                    live_bounds_list.append(parsed)
            if rid:
                live_resource_ids.add(rid)
            if text:
                live_texts.add(text)
            if content_desc:
                live_content_descs.add(content_desc)
            if clickable:
                live_clickable_count += 1
            if class_name == "android.webkit.WebView" and text:
                live_webview_texts.add(text)

        page_bounds_list = [parse_bounds(b) for b in page_fingerprint.bounds_set if parse_bounds(b)]
        page_resource_ids = set(page_fingerprint.resource_ids)
        page_texts = set(page_fingerprint.texts)
        page_content_descs = set(page_fingerprint.content_descs)
        page_clickable_count = page_fingerprint.clickable_count
        page_webview_texts = set(page_fingerprint.webview_texts)

        bounds_overlap_count = 0
        matched_live_indices = set()
        for i, page_bounds in enumerate(page_bounds_list):
            page_area = (page_bounds[2] - page_bounds[0]) * (page_bounds[3] - page_bounds[1])
            for j, live_bounds in enumerate(live_bounds_list):
                if j in matched_live_indices:
                    continue
                live_area = (live_bounds[2] - live_bounds[0]) * (live_bounds[3] - live_bounds[1])
                overlap = calculate_overlap(live_bounds, page_bounds)
                if overlap:
                    overlap_area = (overlap[2] - overlap[0]) * (overlap[3] - overlap[1])
                    if overlap_area >= live_area * 0.8 and overlap_area >= page_area * 0.8:
                        bounds_overlap_count += 1
                        matched_live_indices.add(j)
                        break
                    if overlap_area >= page_area * 0.9:
                        bounds_overlap_count += 1
                        matched_live_indices.add(j)
                        break

        id_overlap = len(live_resource_ids & page_resource_ids)
        text_overlap = len(live_texts & page_texts)
        content_desc_overlap = len(live_content_descs & page_content_descs)
        webview_text_overlap = len(live_webview_texts & page_webview_texts)

        bounds_score = bounds_overlap_count / len(page_bounds_list) if page_bounds_list else 0
        id_score = id_overlap / len(page_resource_ids) if page_resource_ids else 0
        text_score = text_overlap / len(page_texts) if page_texts else 0
        content_desc_score = content_desc_overlap / len(page_content_descs) if page_content_descs else 0
        if page_webview_texts:
            webview_text_score = webview_text_overlap / len(page_webview_texts)
        elif live_webview_texts:
            webview_text_score = -0.15
        else:
            webview_text_score = 0
        clickable_score = 1.0 - abs(live_clickable_count - page_clickable_count) / max(page_clickable_count, 1) if page_clickable_count > 0 else 0.5

        return {
            "bounds_score": bounds_score,
            "id_score": id_score,
            "text_score": text_score,
            "content_desc_score": content_desc_score,
            "webview_text_score": webview_text_score,
            "clickable_score": clickable_score,
            "bounds_overlap": bounds_overlap_count,
            "total_bounds": len(page_bounds_list),
            "webview_text_overlap": webview_text_overlap,
            "total_webview_texts": len(page_webview_texts),
        }

    def calculate_page_similarity(self, live_root: ET.Element, page_fingerprint: PageFingerprint) -> float:
        action_elements = page_fingerprint.action_elements
        if not action_elements:
            return 0.0

        matched_count = 0
        for offline_elem in action_elements:
            if self._find_in_live_xml(live_root, offline_elem, page_fingerprint):
                matched_count += 1

        return matched_count / len(action_elements)

    def _find_in_live_xml(self, live_root: ET.Element, offline_elem: Dict, page_fingerprint: PageFingerprint = None) -> Tuple[Optional[ET.Element], str]:
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
                live_children = get_child_texts(elem)
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

    def _find_action_via_help(self, live_root: ET.Element, action_id: str, page_fingerprint: PageFingerprint) -> Optional[ET.Element]:
        help_to_action = page_fingerprint.help_to_action
        if action_id not in help_to_action:
            return None

        path_info = help_to_action[action_id]
        relative_path = path_info.get("path", [])
        if not relative_path:
            return None

        target_help = None
        for help_info in page_fingerprint.help_elements:
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
            parent_map = build_parent_map(live_root)
            action_elem = follow_relative_path(help_elem, relative_path, parent_map)
            if action_elem:
                return action_elem

        return None

    def _find_action_via_overlap(self, live_root: ET.Element, offline_elem: Dict, page_fingerprint: PageFingerprint) -> Optional[ET.Element]:
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
            return help_elem

        return None

    def identify_page(self, live_root: ET.Element) -> Tuple[Optional[str], float, List[Tuple[str, float, Dict]]]:
        if not self._page_fingerprints:
            return (None, 0.0, [])

        live_bounds_set = set()
        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            bounds = elem.get("bounds", "").strip()
            if bounds:
                live_bounds_set.add(bounds)

        for page_id, page_data in self._page_fingerprints.items():
            page_bounds_set = set(page_data.bounds_set)
            if page_bounds_set and page_bounds_set == live_bounds_set:
                return (page_id, 1.0, [(page_id, 1.0, {})])

        best_match_id = None
        best_score = 0.0
        all_scores = []

        for page_id, page_data in self._page_fingerprints.items():
            multi_scores = self.calculate_multi_strategy_score(live_root, page_data)

            weights = {
                "bounds_score": 0.45,
                "id_score": 0.20,
                "text_score": 0.10,
                "webview_text_score": 0.15,
                "content_desc_score": 0.05,
                "clickable_score": 0.05,
            }

            combined_score = sum(
                multi_scores[key] * weight
                for key, weight in weights.items()
            )

            fine_score = self.calculate_page_similarity(live_root, page_data) if combined_score > 0.2 else 0.0
            final_score = (combined_score * 0.6) + (fine_score * 0.4)

            all_scores.append((page_id, final_score, {
                "bounds_overlap": multi_scores.get('bounds_overlap', 0),
                "total_bounds": multi_scores.get('total_bounds', 0),
                "id_overlap": multi_scores.get('id_overlap', 0),
                "total_ids": multi_scores.get('total_ids', 0),
                "text_overlap": multi_scores.get('text_overlap', 0),
                "total_texts": multi_scores.get('total_texts', 0),
                "webview_overlap": multi_scores.get('webview_text_overlap', 0),
                "webview_total": multi_scores.get('total_webview_texts', 0),
            }))

            if final_score > best_score:
                best_score = final_score
                best_match_id = page_id

        return (best_match_id if best_score > 0.4 else None, best_score, all_scores)

    def find_element(self, live_root: ET.Element, elem_info: Dict, page_fingerprint: PageFingerprint = None) -> Tuple[Optional[ET.Element], str]:
        return self._find_in_live_xml(live_root, elem_info)

    def execute_steps(
        self,
        page_id: str,
        live_root: ET.Element,
        execute_action: Callable,
        end_pages: Optional[List[str]] = None,
        refresh_page_callback: Optional[Callable[[], str]] = None
    ) -> bool:
        if page_id not in self._page_fingerprints:
            print(f"❌ 页面不存在: {page_id}")
            return False

        page_data = self._page_fingerprints[page_id]
        action_elements = page_data.action_elements
        total_steps = len(action_elements)

        if not action_elements:
            print(f"\n⚠️ 页面 {page_id} 已匹配，但无 autodroid:action 定义")
            return True

        print(f"\n📋 执行页面流程: {page_id}")
        print(f"   步骤数: {total_steps}")
        print("-" * 40)

        for idx, elem_info in enumerate(action_elements, 1):
            step = elem_info.get("step", idx)
            action = elem_info.get("action", "")
            desc = elem_info.get("desc", "")
            name = elem_info.get("name", "")
            value = elem_info.get("value", "")
            save_to = elem_info.get("save_to", "")
            wait_after = elem_info.get("wait_after", "")

            print(f"\n[{idx}/{total_steps}] 步骤 {step}: {action}")
            if desc:
                print(f"   描述: {desc}")
            if name:
                print(f"   数据键: {name}")
            if value:
                print(f"   默认值: {value}")

            live_elem, locate_method = self.find_element(live_root, elem_info)
            if not live_elem:
                elem_name = elem_info.get("text") or elem_info.get("resource_id", "").split("/")[-1] or "unknown"
                print(f"  ⚠️ 未找到元素: {elem_name}")
                return False

            elem_name = elem_info.get("desc") or elem_info.get("text") or elem_info.get("resource_id", "").split("/")[-1] or "unknown"
            print(f"  🔍 {locate_method}")
            print(f"  ✅ 找到元素: text='{live_elem.get('text', '')}', class='{live_elem.get('class', '')}'")

            if execute_action(step, action, elem_info, live_elem):
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
        if end_pages:
            if refresh_page_callback:
                latest_xml = refresh_page_callback()
                current_page_id, _ = self.identify_page(parse_xml(latest_xml))
            else:
                current_page_id, _ = self.identify_page(live_root)
            if current_page_id in end_pages:
                print(f"🏁 已到达结束页面: {current_page_id}")
                print(f"✅ general 流程执行完成！\n")
            else:
                print(f"✅ 页面 {page_id} 执行完成 ({idx}/{total_steps})")
                print(f"   当前页面: {current_page_id}")
                print(f"   继续执行下一步...\n")
        else:
            print(f"✅ 页面 {page_id} 执行完成 ({idx}/{total_steps})\n")
        return True
