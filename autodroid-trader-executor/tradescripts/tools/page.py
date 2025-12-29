from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple, Callable
from pathlib import Path
import xml.etree.ElementTree as ET
import time
from collections import Counter
import math
import re


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


@dataclass
class ElementFingerprint:
    element_id: str
    resource_id: str
    text: str
    content_desc: str
    class_name: str
    bounds: str
    normalized_bounds: Tuple[float, float, float, float]
    parent_resource_id: str
    parent_text: str
    parent_class: str
    sibling_texts: List[str]
    sibling_resource_ids: List[str]
    depth: int
    xpath: str
    context_xml: str


def preprocess_xml_for_parsing(xml_content: str) -> str:
    return xml_content


def parse_xml(xml_content: str) -> ET.Element:
    content = preprocess_xml_for_parsing(xml_content)
    return ET.fromstring(content.encode("utf-8"))


def find_autodroid_action_elements(offline_xml: str) -> List[Dict]:
    """在离线XML中找到所有带有autodroid:action的元素"""
    elements = []
    
    try:
        root = ET.fromstring(offline_xml)
    except ET.ParseError:
        return elements
    
    def dfs(node):
        if AUTODROID_ACTION in node.attrib:
            element_info = {
                'action': node.attrib[AUTODROID_ACTION],
                'resource_id': node.get('resource-id', ''),
                'text': node.get('text', ''),
                'bounds': node.get('bounds', ''),
                'class': node.get('class', ''),
                'line': ET.tostring(node, encoding='unicode')
            }
            elements.append(element_info)
        
        for child in node:
            dfs(child)
    
    dfs(root)
    return elements


def build_element_fingerprint(elem: ET.Element, root: ET.Element, element_id: str = None, context_size: int = 3) -> ElementFingerprint:
    all_elems = list(root.iter())
    parent_map = build_parent_map(root)
    
    rid = elem.get("resource-id", "").strip()
    text = elem.get("text", "").strip()
    content_desc = elem.get("content-desc", "").strip()
    class_name = elem.get("class", "").strip()
    bounds = elem.get("bounds", "").strip()
    
    screen_width, screen_height = get_screen_size(root)
    normalized_bounds = (0.0, 0.0, 0.0, 0.0)
    bounds_parsed = parse_bounds(bounds)
    if bounds_parsed:
        normalized_bounds = normalize_bounds(bounds_parsed, screen_width, screen_height)
    
    parent = parent_map.get(elem)
    parent_rid = ""
    parent_text = ""
    parent_class = ""
    if parent:
        pr = parent.get("resource-id", "").strip()
        pt = parent.get("text", "").strip()
        pc = parent.get("class", "").strip()
        if pr:
            parent_rid = pr
        if pt:
            parent_text = pt
        if pc:
            parent_class = pc
    
    sibling_texts = []
    sibling_resource_ids = []
    if parent:
        for sibling in parent:
            if sibling is not elem:
                sib_text = sibling.get("text", "").strip()
                sib_rid = sibling.get("resource-id", "").strip()
                if sib_text:
                    sibling_texts.append(sib_text)
                if sib_rid:
                    sibling_resource_ids.append(sib_rid)
    
    depth = 0
    current = elem
    while current is not None and current.tag != "hierarchy":
        depth += 1
        current = parent_map.get(current)
    
    xpath = build_xpath(elem, all_elems, parent_map)
    
    context_xml = ""
    parent_idx = all_elems.index(parent) if parent and parent in all_elems else -1
    if parent_idx >= 0:
        start_idx = max(0, parent_idx - context_size)
        end_idx = min(len(all_elems), parent_idx + context_size + 1)
        context_elems = all_elems[start_idx:end_idx]
        context_xml = "\n".join([ET.tostring(e, encoding="unicode") for e in context_elems if e.tag != "hierarchy"])
    
    return ElementFingerprint(
        element_id=element_id or f"elem_{id(elem)}",
        resource_id=rid,
        text=text,
        content_desc=content_desc,
        class_name=class_name,
        bounds=bounds,
        normalized_bounds=normalized_bounds,
        parent_resource_id=parent_rid,
        parent_text=parent_text,
        parent_class=parent_class,
        sibling_texts=sibling_texts,
        sibling_resource_ids=sibling_resource_ids,
        depth=depth,
        xpath=xpath,
        context_xml=context_xml
    )


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


def normalize_bounds(bounds: Tuple[int, int, int, int], screen_width: int, screen_height: int) -> Tuple[float, float, float, float]:
    x1, y1, x2, y2 = bounds
    return (round(x1 / screen_width, 4), round(y1 / screen_height, 4), 
            round(x2 / screen_width, 4), round(y2 / screen_height, 4))


def get_screen_size(root: ET.Element) -> Tuple[int, int]:
    for elem in root.iter():
        if elem.tag == "hierarchy":
            bounds_str = elem.get("bounds", "")
            if bounds_str:
                bounds = parse_bounds(bounds_str)
                if bounds:
                    return (bounds[2], bounds[3])
    return (1080, 1920)


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


def build_page_fingerprint(root: ET.Element, page_id: str, should_exclude_element: Optional[Callable[[Dict], bool]] = None) -> PageFingerprint:
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

        if should_exclude_element:
            elem_dict = {
                "resource-id": rid,
                "text": text,
                "content-desc": content_desc,
                "class": class_name,
                "bounds": bounds
            }
            if should_exclude_element(elem_dict):
                continue

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


class PageExecutor:
    def __init__(self, page_matcher: 'PageMatcher'):
        self._page_matcher = page_matcher

    def execute_steps(
        self,
        page_id: str,
        live_root: ET.Element,
        execute_action: Callable,
        end_pages: Optional[List[str]] = None,
        refresh_page_callback: Optional[Callable[[], str]] = None
    ) -> bool:
        if page_id not in self._page_matcher._page_fingerprints:
            print(f"❌ 页面不存在: {page_id}")
            return False

        page_data = self._page_matcher._page_fingerprints[page_id]
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

            live_elem, locate_method = self._page_matcher.find_element(live_root, elem_info)
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

            is_redirect = action == "redirect"
            if is_redirect and refresh_page_callback:
                latest_xml = refresh_page_callback()
                if latest_xml:
                    live_root = parse_xml(latest_xml)

        print("\n" + "=" * 40)
        if end_pages:
            if refresh_page_callback:
                latest_xml = refresh_page_callback()
                
                timestamp = int(time.time())
                dump_dir = Path(__file__).parent / "dump-pages"
                dump_dir.mkdir(exist_ok=True)
                dump_file = dump_dir / f"after_{page_id}_{timestamp}.xml"
                dump_file.write_text(latest_xml, encoding="utf-8")
                print(f"   💾 已保存跳转后页面XML: {dump_file}")
                
                live_root = parse_xml(latest_xml)
                current_page_id, score, all_scores = self._page_matcher.identify_page(live_root)
            else:
                current_page_id, score, all_scores = self._page_matcher.identify_page(live_root)
            
            print(f"   识别结果: {current_page_id} (分数: {score:.3f})")
            if score < 0.5:
                print(f"   ⚠️ 识别分数较低，显示所有候选页面:")
                for pid, s, details in sorted(all_scores, key=lambda x: x[1], reverse=True)[:5]:
                    print(f"      - {pid}: {s:.3f}")
            
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


class PageMatcher:
    def __init__(self):
        self._page_fingerprints: Dict[str, PageFingerprint] = {}
        self.feature_weights = {
            'resource_ids': 0.35,
            'text_content': 0.25,
            'class_distribution': 0.25,
            'clickable_ratio': 0.10,
            'depth_distribution': 0.05,
        }
        self._page_features_cache: Dict[str, dict] = {}
        self._should_exclude_element: Optional[Callable[[Dict], bool]] = None

    @property
    def page_fingerprints(self) -> Dict[str, PageFingerprint]:
        return self._page_fingerprints

    def set_element_filter(self, should_exclude_element: Optional[Callable[[Dict], bool]]):
        self._should_exclude_element = should_exclude_element

    def add_fingerprint(self, fingerprint: PageFingerprint):
        self._page_fingerprints[fingerprint.page_id] = fingerprint

    def add_fingerprint_from_xml(self, root: ET.Element, page_id: str) -> PageFingerprint:
        fingerprint = build_page_fingerprint(root, page_id, self._should_exclude_element)
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
                fingerprint = build_page_fingerprint(root, page_id, self._should_exclude_element)
                self._page_fingerprints[page_id] = fingerprint
            except ET.ParseError:
                pass

    def extract_features(self, xml: str) -> dict:
        """提取UI的多维度特征"""
        features = {
            'resource_ids': set(),
            'text_content': set(),
            'class_distribution': Counter(),
            'clickable_count': 0,
            'total_nodes': 0,
            'depth_levels': Counter(),
            'bounds_patterns': set(),
        }
        
        try:
            root = ET.fromstring(xml)
        except ET.ParseError:
            root = None
        
        if root is None:
            for line in xml.split('\n'):
                line = line.strip()
                if not line.startswith('<node'):
                    continue
                features['total_nodes'] += 1
                self._parse_node_line(line, features)
            return features
        
        def dfs(node, depth=0):
            features['total_nodes'] += 1
            features['depth_levels'][depth] += 1
            
            class_name = node.get('class', '')
            if class_name:
                simple_class = class_name.split('.')[-1]
                features['class_distribution'][simple_class] += 1
            
            rid = node.get('resource-id', '')
            if rid:
                features['resource_ids'].add(rid)
            
            text = node.get('text', '')
            if text and text.strip():
                features['text_content'].add(text.strip())
            
            if node.get('clickable') == 'true':
                features['clickable_count'] += 1
            
            bounds = node.get('bounds', '')
            if bounds:
                features['bounds_patterns'].add(bounds)
            
            for child in node:
                dfs(child, depth + 1)
        
        dfs(root)
        return features

    def _parse_node_line(self, line: str, features: dict):
        """解析node行提取特征"""
        for attr in line.split():
            if attr.startswith('resource-id='):
                rid = attr.split('=')[1].strip('"')
                if rid:
                    features['resource_ids'].add(rid)
            elif attr.startswith('text='):
                txt = attr.split('=')[1].strip('"')
                if txt and txt.strip():
                    features['text_content'].add(txt.strip())
            elif attr.startswith('class='):
                cls = attr.split('=')[1].strip('"')
                if cls:
                    simple_class = cls.split('.')[-1]
                    features['class_distribution'][simple_class] += 1
            elif attr.startswith('clickable='):
                if attr.split('=')[1].strip('"') == 'true':
                    features['clickable_count'] += 1
            elif attr.startswith('bounds='):
                bounds = attr.split('=')[1].strip('"')
                if bounds:
                    features['bounds_patterns'].add(bounds)
            features['total_nodes'] += 1

    def calculate_similarity(self, features1: dict, features2: dict) -> float:
        """计算两个特征集的相似度"""
        weights = self.feature_weights
        score = 0.0
        
        rid_sim = self._jaccard_similarity(features1['resource_ids'], features2['resource_ids'])
        score += weights['resource_ids'] * rid_sim
        
        text_sim = self._jaccard_similarity(features1['text_content'], features2['text_content'])
        score += weights['text_content'] * text_sim
        
        class_sim = self._cosine_similarity(features1['class_distribution'], features2['class_distribution'])
        score += weights['class_distribution'] * class_sim
        
        return score

    def _jaccard_similarity(self, set1: set, set2: set) -> float:
        """计算Jaccard相似度"""
        if not set1 and not set2:
            return 1.0
        if not set1 or not set2:
            return 0.0
        intersection = len(set1 & set2)
        union = len(set1 | set2)
        return intersection / union if union > 0 else 0.0

    def _cosine_similarity(self, counter1: Counter, counter2: Counter) -> float:
        """计算余弦相似度"""
        all_keys = set(counter1.keys()) | set(counter2.keys())
        if not all_keys:
            return 1.0
        
        dot_product = sum(counter1[k] * counter2[k] for k in all_keys)
        norm1 = math.sqrt(sum(counter1[k] ** 2 for k in counter1))
        norm2 = math.sqrt(sum(counter2[k] ** 2 for k in counter2))
        
        if norm1 == 0 or norm2 == 0:
            return 0.0
        return dot_product / (norm1 * norm2)

    def extract_page_fingerprint_features(self, page_fingerprint: PageFingerprint) -> dict:
        """从PageFingerprint对象提取特征"""
        return {
            'resource_ids': set(page_fingerprint.resource_ids),
            'text_content': set(page_fingerprint.texts),
            'class_distribution': Counter(page_fingerprint.classes),
            'clickable_count': page_fingerprint.clickable_count,
            'total_nodes': len(page_fingerprint.classes),
            'depth_levels': Counter(),
            'bounds_patterns': set(page_fingerprint.bounds_set),
        }

    def quick_match(self, live_xml: str) -> str:
        """快速精确匹配：使用autodroid:action元素的bounds进行匹配"""
        live_action_bounds = set()
        
        for line in live_xml.split('\n'):
            line = line.strip()
            if not line.startswith('<node'):
                continue
            
            autodroid_action_match = re.search(r'autodroid:action="([^"]*)"', line)
            if autodroid_action_match:
                bounds_match = re.search(r'bounds="([^"]*)"', line)
                if bounds_match:
                    bounds = bounds_match.group(1)
                    if bounds:
                        live_action_bounds.add(bounds)
        
        for page_id, fp in self._page_fingerprints.items():
            if not fp.action_elements:
                continue
            
            page_action_bounds = set()
            for action_elem in fp.action_elements:
                bounds = action_elem.get("bounds", "")
                if bounds:
                    page_action_bounds.add(bounds)
            
            if not page_action_bounds:
                continue
            
            overlap = len(live_action_bounds & page_action_bounds)
            
            if overlap >= max(1, len(page_action_bounds) * 0.5):
                return f"exact:{page_id}"
        
        return ''

    def structural_match(self, live_xml: str) -> tuple:
        """结构化特征匹配：计算多维度相似度"""
        live_features = None
        
        best_page_id = None
        best_score = 0.0
        
        live_features = self.extract_features(live_xml)
        
        for page_id, fp in self._page_fingerprints.items():
            if page_id not in self._page_features_cache:
                self._page_features_cache[page_id] = self.extract_page_fingerprint_features(fp)
            
            page_features = self._page_features_cache[page_id]
            score = self.calculate_similarity(live_features, page_features)
            
            if score > best_score:
                best_score = score
                best_page_id = page_id
        
        if best_score >= 0.5:
            return best_page_id, best_score
        return None, 0.0

    def calculate_multi_strategy_score(self, live_root: ET.Element, page_fingerprint: PageFingerprint) -> Dict[str, float]:
        live_bounds_list = []
        live_resource_ids = set()
        live_texts = set()
        live_content_descs = set()
        live_clickable_count = 0
        live_classes = set()

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
            if class_name:
                live_classes.add(class_name)

        page_bounds_list = [parse_bounds(b) for b in page_fingerprint.bounds_set if parse_bounds(b)]
        page_resource_ids = set(page_fingerprint.resource_ids)
        page_texts = set(page_fingerprint.texts)
        page_content_descs = set(page_fingerprint.content_descs)
        page_clickable_count = page_fingerprint.clickable_count
        page_classes = set(page_fingerprint.classes)

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
        class_overlap = len(live_classes & page_classes)

        bounds_score = bounds_overlap_count / len(page_bounds_list) if page_bounds_list else 0
        id_score = id_overlap / len(page_resource_ids) if page_resource_ids else 0
        text_score = text_overlap / len(page_texts) if page_texts else 0
        content_desc_score = content_desc_overlap / len(page_content_descs) if page_content_descs else 0
        class_score = class_overlap / len(page_classes) if page_classes else 0
        clickable_score = 1.0 - abs(live_clickable_count - page_clickable_count) / max(page_clickable_count, 1) if page_clickable_count > 0 else 0.5

        return {
            "bounds_score": bounds_score,
            "id_score": id_score,
            "text_score": text_score,
            "content_desc_score": content_desc_score,
            "class_score": class_score,
            "clickable_score": clickable_score,
            "bounds_overlap": bounds_overlap_count,
            "total_bounds": len(page_bounds_list),
            "id_overlap": id_overlap,
            "total_ids": len(page_resource_ids),
            "text_overlap": text_overlap,
            "total_texts": len(page_texts),
            "class_overlap": class_overlap,
            "total_classes": len(page_classes),
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
                "bounds_score": 0.30,
                "id_score": 0.15,
                "text_score": 0.10,
                "class_score": 0.35,
                "content_desc_score": 0.05,
                "clickable_score": 0.05,
            }

            combined_score = sum(
                multi_scores[key] * weight
                for key, weight in weights.items()
            )

            fine_score = self.calculate_page_similarity(live_root, page_data) if combined_score > 0.2 else 0.0
            final_score = combined_score

            all_scores.append((page_id, final_score, {
                "bounds_overlap": multi_scores.get('bounds_overlap', 0),
                "total_bounds": multi_scores.get('total_bounds', 0),
                "id_overlap": multi_scores.get('id_overlap', 0),
                "total_ids": multi_scores.get('total_ids', 0),
                "text_overlap": multi_scores.get('text_overlap', 0),
                "total_texts": multi_scores.get('total_texts', 0),
                "class_overlap": multi_scores.get('class_overlap', 0),
                "total_classes": multi_scores.get('total_classes', 0),
            }))

            if final_score > best_score:
                best_score = final_score
                best_match_id = page_id

        return (best_match_id if best_score > 0.4 else None, best_score, all_scores)

    def find_element(self, live_root: ET.Element, elem_info: Dict, page_fingerprint: PageFingerprint = None) -> Tuple[Optional[ET.Element], str]:
        return self._find_in_live_xml(live_root, elem_info)


class ElementMatcher:
    def __init__(self):
        self._element_fingerprints: Dict[str, ElementFingerprint] = {}

    @property
    def element_fingerprints(self) -> Dict[str, ElementFingerprint]:
        return self._element_fingerprints

    def add_fingerprint(self, fingerprint: ElementFingerprint):
        self._element_fingerprints[fingerprint.element_id] = fingerprint

    def add_fingerprint_from_elem(self, elem: ET.Element, root: ET.Element, element_id: str = None) -> ElementFingerprint:
        fingerprint = build_element_fingerprint(elem, root, element_id)
        self.add_fingerprint(fingerprint)
        return fingerprint

    def _bounds_match(self, bounds1: Tuple[float, float, float, float], bounds2: Tuple[float, float, float, float], tolerance: float = 0.005) -> bool:
        x1_diff = abs(bounds1[0] - bounds2[0])
        y1_diff = abs(bounds1[1] - bounds2[1])
        x2_diff = abs(bounds1[2] - bounds2[2])
        y2_diff = abs(bounds1[3] - bounds2[3])
        return x1_diff <= tolerance and y1_diff <= tolerance and x2_diff <= tolerance and y2_diff <= tolerance

    def exact_match(self, live_root: ET.Element, target_fingerprint: ElementFingerprint) -> Tuple[Optional[ET.Element], str, float]:
        rid = target_fingerprint.resource_id
        text = target_fingerprint.text
        content_desc = target_fingerprint.content_desc
        bounds = target_fingerprint.bounds
        normalized_bounds = target_fingerprint.normalized_bounds
        class_name = target_fingerprint.class_name

        if rid:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_rid = elem.get("resource-id", "").strip()
                if live_rid == rid:
                    return (elem, f"resource-id精确匹配: {rid}", 1.0)

        if normalized_bounds and normalized_bounds != (0.0, 0.0, 0.0, 0.0):
            live_screen_width, live_screen_height = get_screen_size(live_root)
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_bounds_str = elem.get("bounds", "").strip()
                live_bounds = parse_bounds(live_bounds_str)
                if live_bounds:
                    live_normalized = normalize_bounds(live_bounds, live_screen_width, live_screen_height)
                    if self._bounds_match(normalized_bounds, live_normalized):
                        return (elem, f"归一化bounds匹配: {bounds}", 1.0)

        if text:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_text = elem.get("text", "").strip()
                if live_text == text:
                    return (elem, f"text精确匹配: {text}", 1.0)

        if content_desc:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_content_desc = elem.get("content-desc", "").strip()
                if live_content_desc == content_desc:
                    return (elem, f"content-desc精确匹配: {content_desc}", 1.0)

        return (None, "未找到精确匹配", 0.0)

    def structural_match(self, live_root: ET.Element, target_fingerprint: ElementFingerprint) -> Tuple[Optional[ET.Element], str, float]:
        rid = target_fingerprint.resource_id
        text = target_fingerprint.text
        content_desc = target_fingerprint.content_desc
        bounds = target_fingerprint.bounds
        class_name = target_fingerprint.class_name
        parent_rid = target_fingerprint.parent_resource_id
        parent_text = target_fingerprint.parent_text
        parent_class = target_fingerprint.parent_class
        sibling_texts = target_fingerprint.sibling_texts
        sibling_resource_ids = target_fingerprint.sibling_resource_ids
        depth = target_fingerprint.depth

        best_elem = None
        best_score = 0.0
        best_reason = ""

        max_possible_score = 0.0
        if rid:
            max_possible_score += 0.4
        if text:
            max_possible_score += 0.3
        if content_desc:
            max_possible_score += 0.2
        if bounds:
            max_possible_score += 0.3
        if class_name:
            max_possible_score += 0.1
        if parent_rid:
            max_possible_score += 0.15
        if parent_text:
            max_possible_score += 0.1
        if parent_class:
            max_possible_score += 0.05
        if sibling_texts:
            max_possible_score += 0.1
        if sibling_resource_ids:
            max_possible_score += 0.1
        if depth > 0:
            max_possible_score += 0.05

        threshold = max_possible_score * 0.6 if max_possible_score > 0 else 0.5

        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue

            live_rid = elem.get("resource-id", "").strip()
            live_text = elem.get("text", "").strip()
            live_content_desc = elem.get("content-desc", "").strip()
            live_bounds = elem.get("bounds", "").strip()
            live_class = elem.get("class", "").strip()

            score = 0.0
            reasons = []

            if rid and live_rid == rid:
                score += 0.4
                reasons.append(f"resource-id匹配")

            if text and live_text == text:
                score += 0.3
                reasons.append(f"text匹配")

            if content_desc and live_content_desc == content_desc:
                score += 0.2
                reasons.append(f"content-desc匹配")

            if bounds and live_bounds == bounds:
                score += 0.3
                reasons.append(f"bounds匹配")

            if class_name and live_class == class_name:
                score += 0.1
                reasons.append(f"class匹配")

            parent_map = build_parent_map(live_root)
            parent = parent_map.get(elem)
            if parent:
                live_parent_rid = parent.get("resource-id", "").strip()
                live_parent_text = parent.get("text", "").strip()
                live_parent_class = parent.get("class", "").strip()

                if parent_rid and live_parent_rid == parent_rid:
                    score += 0.15
                    reasons.append(f"父元素resource-id匹配")

                if parent_text and live_parent_text == parent_text:
                    score += 0.1
                    reasons.append(f"父元素text匹配")

                if parent_class and live_parent_class == parent_class:
                    score += 0.05
                    reasons.append(f"父元素class匹配")

                live_sibling_texts = []
                live_sibling_resource_ids = []
                for sibling in parent:
                    if sibling is not elem:
                        sib_text = sibling.get("text", "").strip()
                        sib_rid = sibling.get("resource-id", "").strip()
                        if sib_text:
                            live_sibling_texts.append(sib_text)
                        if sib_rid:
                            live_sibling_resource_ids.append(sib_rid)

                if sibling_texts and live_sibling_texts:
                    text_intersection = set(sibling_texts) & set(live_sibling_texts)
                    if text_intersection:
                        score += 0.1 * (len(text_intersection) / len(sibling_texts))
                        reasons.append(f"兄弟元素text匹配")

                if sibling_resource_ids and live_sibling_resource_ids:
                    rid_intersection = set(sibling_resource_ids) & set(live_sibling_resource_ids)
                    if rid_intersection:
                        score += 0.1 * (len(rid_intersection) / len(sibling_resource_ids))
                        reasons.append(f"兄弟元素resource-id匹配")

            live_depth = 0
            current = elem
            while current is not None and current.tag != "hierarchy":
                live_depth += 1
                current = parent_map.get(current)

            if depth > 0 and live_depth > 0:
                depth_diff = abs(depth - live_depth)
                if depth_diff == 0:
                    score += 0.05
                elif depth_diff == 1:
                    score += 0.03

            if score > best_score:
                best_score = score
                best_elem = elem
                best_reason = ", ".join(reasons)

        if best_score >= threshold:
            return (best_elem, f"结构化匹配({best_reason}): {best_score:.2f}", best_score)
        else:
            return (None, f"结构化匹配得分不足: {best_score:.2f} (阈值: {threshold:.2f})", best_score)

    def match(self, live_root: ET.Element, target_fingerprint: ElementFingerprint, method: int = 0) -> Tuple[Optional[ET.Element], str, float]:
        if method == 1:
            return self.exact_match(live_root, target_fingerprint)
        elif method == 2:
            return self.structural_match(live_root, target_fingerprint)
        else:
            elem, reason, score = self.exact_match(live_root, target_fingerprint)
            if elem:
                return (elem, reason, score)

            return self.structural_match(live_root, target_fingerprint)
