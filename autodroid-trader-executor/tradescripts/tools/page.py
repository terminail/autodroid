from typing import Dict, List, Optional, Tuple, Callable, Union
from pathlib import Path
import xml.etree.ElementTree as ET
import time
from collections import Counter
import math
import re
from pydantic import BaseModel
try:
    from .u2device import U2Device, ScreenUtils
    from .element import ElementInfo
except ImportError:
    # 当直接运行脚本时使用绝对导入
    from u2device import U2Device, ScreenUtils
    from element import ElementInfo


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
AUTODROID_FINGERPRINT = f"{{{AUTODROID_NS}}}fingerprint"


class PageInfo:
    page_id: str
    xml_path: Path
    steps: List[Dict]





class FingerprintElement(BaseModel):
    """指纹元素类型化类"""
    text: str = ""
    resource_id: str = ""
    class_name: str = ""
    bounds: str = ""
    content_desc: str = ""
    clickable: str = ""
    long_clickable: str = ""
    index: int = 0


class PageInfo(BaseModel):
    """页面信息类型化类"""
    page_id: str
    action_elements: List[ElementInfo] = []
    # 指纹元素（用于选择器方案）
    fingerprint_elements: List[FingerprintElement] = []  # 标记为autodroid:fingerprint="true"的元素





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


def build_page_info(root: ET.Element, page_id: str) -> PageInfo:
    action_elements = []
    fingerprint_elements = []  # 指纹元素

    all_elems = list(root.iter())

    for i, elem in enumerate(all_elems):
        if elem.tag == "hierarchy":
            continue

        action = elem.get(AUTODROID_ACTION) or elem.get("action")
        fingerprint = elem.get(AUTODROID_FINGERPRINT) or elem.get("fingerprint")

        # 提取指纹元素
        if fingerprint == "true":
            fingerprint_elements.append(FingerprintElement(
                text=elem.get("text", ""),
                resource_id=elem.get("resource-id", ""),
                class_name=elem.get("class", ""),
                bounds=elem.get("bounds", ""),
                content_desc=elem.get("content-desc", ""),
                clickable=elem.get("clickable", ""),
                long_clickable=elem.get("long-clickable", ""),
                index=i,
            ))

        if action:
            child_texts = get_child_texts(elem)
            bounds_str = elem.get("bounds", "")
            action_bounds = ScreenUtils.parse_bounds(bounds_str)

            action_type = action if action else "click"

            elem_info = ElementInfo(
                resource_id=elem.get("resource-id", ""),
                text=elem.get("text", ""),
                content_desc=elem.get("content-desc", ""),
                class_name=elem.get("class", ""),
                children=child_texts,
                action=action_type,
                step=elem.get(AUTODROID_STEP) or elem.get("step"),
                name=elem.get(AUTODROID_NAME) or elem.get("name"),
                value=elem.get(AUTODROID_VALUE) or elem.get("value"),
                save_to=elem.get(AUTODROID_SAVE_TO) or elem.get("save_to"),
                desc=elem.get(AUTODROID_DESC) or elem.get("desc"),
                wait_after=elem.get(AUTODROID_WAIT_AFTER) or elem.get("wait_after"),
                bounds=bounds_str,
            )

            action_elements.append(elem_info)

    step_counter = 1
    for elem in action_elements:
        if not elem.step:
            elem.step = step_counter
        step_counter += 1

    action_elements.sort(key=lambda x: int(x.step) if isinstance(x.step, int) else 0)

    return PageInfo(
        page_id=page_id,
        action_elements=action_elements,
        fingerprint_elements=fingerprint_elements,
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
        elem_bounds = ScreenUtils.parse_bounds(bounds_str)
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
        self._executed_steps_callback = None
        self._status_callback = None
        self._dump_dir = Path(__file__).parent / "dump-pages"
        self._dump_dir.mkdir(exist_ok=True)

    def set_executed_steps_callback(self, callback):
        """设置步骤执行回调函数"""
        self._executed_steps_callback = callback

    def set_status_callback(self, callback):
        """设置状态回调函数"""
        self._status_callback = callback

    def execute_steps(
        self,
        page_id: str,
        execute_action: Callable,
        device: U2Device,
        end_pages: Optional[List[str]] = None,
        refresh_page_callback: Optional[Callable[[], str]] = None
    ) -> bool:
        if page_id not in self._page_matcher._page_infos:
            print(f"❌ 页面不存在: {page_id}")
            return False

        page_data = self._page_matcher._page_infos[page_id]
        action_elements = page_data.action_elements
        total_steps = len(action_elements)

        if not action_elements:
            print(f"\n⚠️ 页面 {page_id} 已匹配，但无 autodroid:action 定义")
            return True

        print(f"\n📋 执行页面流程: {page_id}")
        print(f"   步骤数: {total_steps}")
        print("-" * 40)

        for idx, elem_info in enumerate(action_elements, 1):
            # 使用类型化的属性访问，确保step是整数
            step_raw = elem_info.step
            # 确保step是整数类型
            if isinstance(step_raw, int):
                step = step_raw
            elif isinstance(step_raw, str) and step_raw.isdigit():
                step = int(step_raw)
            else:
                step = idx
                print(f"  ⚠️ 警告: step值'{step_raw}'不是有效整数，使用索引值{idx}")
            
            action = elem_info.action
            desc = elem_info.desc
            name = elem_info.name
            value = elem_info.value
            save_to = elem_info.save_to
            wait_after = elem_info.wait_after

            print(f"\n[{idx}/{total_steps}] 步骤 {step}: {action}")
            if desc:
                print(f"   描述: {desc}")
            if name:
                print(f"   数据键: {name}")
            if value:
                print(f"   默认值: {value}")

            # 使用选择器方案执行步骤（不需要XML解析）
            # 构建选择器来定位实时元素
            live_elem = self._find_element_by_selector(device, elem_info)
            
            # 如果元素不在屏幕内，滚动到元素位置
            if live_elem and elem_info.bounds:
                screen_width = device.d.info.get('displayWidth', 1080)
                screen_height = device.d.info.get('displayHeight', 1920)
                self._scroll_to_element_by_bounds(device, elem_info.bounds, screen_width, screen_height)
            
            if execute_action(step, action, elem_info, live_elem):
                print(f"  ✓ {action} 完成")
                if self._executed_steps_callback:
                    self._executed_steps_callback(step, page_id)
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
        return True

    def _find_element_by_selector(self, device: U2Device, elem_info: ElementInfo) -> Optional[Dict]:
        """使用选择器定位实时UI元素"""
        if not device:
            return None
            
        resource_id = elem_info.resource_id.strip()
        text = elem_info.text.strip()
        content_desc = elem_info.content_desc.strip()
        
        # 优先使用resource-id定位
        if resource_id:
            selector = f'resourceId("{resource_id}")'
            if device.check_element_exists(selector):
                # 返回一个包含基本信息的字典，模拟XML元素
                return {
                    "resource-id": resource_id,
                    "text": text,
                    "content-desc": content_desc,
                    "bounds": elem_info.bounds
                }
        
        # 其次使用text定位
        if text:
            selector = f'text("{text}")'
            if device.check_element_exists(selector):
                return {
                    "resource-id": resource_id,
                    "text": text,
                    "content-desc": content_desc,
                    "bounds": elem_info.bounds
                }
        
        # 最后使用content-desc定位
        if content_desc:
            selector = f'description("{content_desc}")'
            if device.check_element_exists(selector):
                return {
                    "resource-id": resource_id,
                    "text": text,
                    "content-desc": content_desc,
                    "bounds": elem_info.bounds
                }
        
        print(f"  ⚠️ 无法通过选择器定位元素: resource_id='{resource_id}', text='{text}', content_desc='{content_desc}'")
        return None

    def _scroll_to_element_by_bounds(self, device: U2Device, bounds_str: str, screen_width: int, screen_height: int) -> bool:
        """根据bounds字符串滚动到元素位置"""
        if not bounds_str:
            return False
        
        bounds = ScreenUtils.parse_bounds(bounds_str)
        if not bounds:
            return False
        
        x1, y1, x2, y2 = bounds
        center_x = (x1 + x2) // 2
        center_y = (y1 + y2) // 2
        
        # 计算安全区域高度（考虑导航栏）
        safe_height = screen_height - 150
        
        # 如果元素已经在安全区域内，不需要滚动
        if y2 < safe_height:
            return True
        
        # 如果元素在屏幕下方，向上滚动
        if center_y > screen_height * 0.7:
            scroll_distance = y2 - safe_height + 100
            max_attempts = 5
            for attempt in range(max_attempts):
                start_x = screen_width // 2
                start_y = screen_height - 200
                end_x = screen_width // 2
                end_y = 200
                device.d.swipe(start_x, start_y, end_x, end_y, 0.5)
                time.sleep(0.5)
                
                if y2 - scroll_distance * (attempt + 1) < safe_height:
                    print(f"  🔄 向上滚动到元素位置")
                    return True
        
        # 如果元素在屏幕上方，向下滚动
        elif center_y < screen_height * 0.3:
            start_x = screen_width // 2
            start_y = screen_height * 0.3
            end_x = screen_width // 2
            end_y = screen_height * 0.7
            device.d.swipe(start_x, start_y, end_x, end_y, 0.5)
            print(f"  🔄 向下滚动到元素位置")
            return True
        
        return False


class PageMatcher:
    def __init__(self):
        self._page_infos: Dict[str, PageInfo] = {}

    @property
    def page_infos(self) -> Dict[str, PageInfo]:
        return self._page_infos

    def add_page_info(self, page_info: PageInfo):
        self._page_infos[page_info.page_id] = page_info

    def add_page_info_from_xml(self, root: ET.Element, page_id: str) -> PageInfo:
        page_info = build_page_info(root, page_id)
        self.add_page_info(page_info)
        return page_info

    def get_total_steps(self) -> int:
        """获取所有页面中action元素的总数"""
        total = 0
        for page_info in self._page_infos.values():
            total += len(page_info.action_elements)
        return total

    def has_page_steps(self, page_id: str) -> bool:
        """检查指定页面是否有可执行的动作元素"""
        if page_id not in self._page_infos:
            return False
        page_info = self._page_infos[page_id]
        return len(page_info.action_elements) > 0





    def identify_page(self, device: U2Device) -> Tuple[Optional[str], float, List[Tuple[str, float, Dict]]]:
        """使用选择器方案识别当前页面"""
        if not self._page_infos:
            return (None, 0.0, [])

        # 使用选择器方案进行页面匹配
        best_match_id = None
        best_score = 0.0
        all_scores = []

        for page_id, page_info in self._page_infos.items():
            if not page_info.action_elements:
                continue

            # 计算选择器匹配度
            matched_count = 0
            for elem_info in page_info.action_elements:
                # 使用选择器方案检查元素是否存在
                if self._check_element_exists_by_selector(device, elem_info):
                    matched_count += 1

            score = matched_count / len(page_info.action_elements) if page_info.action_elements else 0.0
            all_scores.append((page_id, score, {"method": "selector"}))

            if score > best_score:
                best_score = score
                best_match_id = page_id

        return (best_match_id if best_score > 0.4 else None, best_score, all_scores)

    def _check_element_exists_by_selector(self, device: U2Device, elem_info: ElementInfo) -> bool:
        """使用选择器检查元素是否存在"""
        if not device:
            return False
            
        resource_id = elem_info.resource_id.strip()
        text = elem_info.text.strip()
        content_desc = elem_info.content_desc.strip()
        
        # 优先使用resource-id定位
        if resource_id:
            selector = f'resourceId("{resource_id}")'
            if device.check_element_exists(selector):
                return True
        
        # 其次使用text定位
        if text:
            selector = f'text("{text}")'
            if device.check_element_exists(selector):
                return True
        
        # 最后使用content-desc定位
        if content_desc:
            selector = f'description("{content_desc}")'
            if device.check_element_exists(selector):
                return True
        
        return False

    def find_elements_by(self, all_elems: List[ET.Element], property_name: str, property_value: str) -> List[ET.Element]:
        results = []
        for elem in all_elems:
            if elem.tag == "hierarchy":
                continue
            attr_name = property_name
            if property_name == "resource_id":
                attr_name = "resource-id"
            elif property_name == "content_desc":
                attr_name = "content-desc"
            
            live_value = elem.get(attr_name, "").strip()
            if live_value == property_value:
                results.append(elem)
        return results

    def _find_in_live_xml(self, live_root: ET.Element, offline_elem: Dict) -> Tuple[Optional[ET.Element], str]:
        offline_resource_id = offline_elem.get("resource_id", "").strip()
        offline_text = offline_elem.get("text", "").strip()
        offline_content_desc = offline_elem.get("content_desc", "").strip()
        offline_class = offline_elem.get("class", "").strip()
        offline_children = offline_elem.get("children", [])
        offline_bounds = offline_elem.get("bounds", "").strip()

        print(f"   🔍 _find_in_live_xml: text='{offline_text}', bounds='{offline_bounds}', resource_id='{offline_resource_id}'")

        if not offline_resource_id and not offline_text and not offline_content_desc and not offline_children and not offline_bounds:
            return (None, "")

        all_elems = list(live_root.iter())
        candidates = None

        if offline_resource_id:
            print(f"   🔍 开始resource-id匹配: '{offline_resource_id}'")
            candidates = self.find_elements_by(all_elems, "resource_id", offline_resource_id)
            print(f"   🔍 resource-id匹配到{len(candidates)}个候选元素")
            if len(candidates) == 1:
                elem = candidates[0]
                print(f"   🔍 resource-id匹配成功: {offline_resource_id}")
                return (elem, f"resource-id定位: {offline_resource_id}")

        if offline_class:
            print(f"   🔍 开始class匹配: '{offline_class}'")
            if candidates:
                candidates = self.find_elements_by(candidates, "class", offline_class)
            else:
                candidates = self.find_elements_by(all_elems, "class", offline_class)
            print(f"   🔍 class匹配到{len(candidates)}个候选元素")
            if len(candidates) == 1:
                elem = candidates[0]
                print(f"   🔍 class匹配成功: {offline_class}")
                return (elem, f"class定位: {offline_class}")

        if offline_bounds:
            print(f"   🔍 开始bounds匹配: '{offline_bounds}'")
            if candidates:
                candidates = self.find_elements_by(candidates, "bounds", offline_bounds)
            else:
                candidates = self.find_elements_by(all_elems, "bounds", offline_bounds)

            print(f"   🔍 bounds匹配到{len(candidates)}个候选元素")
            if len(candidates) == 1:
                elem = candidates[0]
                print(f"   🔍 bounds匹配成功: {offline_bounds}")
                return (elem, f"bounds定位: {offline_bounds}")

        if offline_text:
            print(f"   🔍 开始text匹配: '{offline_text}'")
            if candidates:
                candidates = self.find_elements_by(candidates, "text", offline_text)
            else:
                candidates = self.find_elements_by(all_elems, "text", offline_text)
            print(f"   🔍 text匹配到{len(candidates)}个候选元素")
            if len(candidates) == 1:
                elem = candidates[0]
                print(f"   🔍 text匹配成功: {offline_text}")
                return (elem, f"text定位: {offline_text}")

        if offline_content_desc:
            print(f"   🔍 开始content-desc匹配: '{offline_content_desc}'")
            if candidates:
                candidates = self.find_elements_by(candidates, "content_desc", offline_content_desc)
            else:
                candidates = self.find_elements_by(all_elems, "content_desc", offline_content_desc)

            print(f"   🔍 content-desc匹配到{len(candidates)}个候选元素")
            if len(candidates) == 1:
                elem = candidates[0]
                print(f"   🔍 content-desc匹配成功: {offline_content_desc}")
                return (elem, f"content-desc定位: {offline_content_desc}")

        print(f"   ⚠️ 所有匹配策略都失败了")
        return (None, "")





    def find_element(self, live_root: ET.Element, elem_info) -> Tuple[Optional[ET.Element], str]:
        text = elem_info.text if hasattr(elem_info, 'text') else elem_info.get('text', '')
        bounds = elem_info.bounds if hasattr(elem_info, 'bounds') else elem_info.get('bounds', '')
        resource_id = elem_info.resource_id if hasattr(elem_info, 'resource_id') else elem_info.get('resource_id', '')
        print(f"   🔍 查找元素: text='{text}', bounds='{bounds}', resource_id='{resource_id}'")
        
        if isinstance(elem_info, dict):
            return self._find_in_live_xml(live_root, elem_info)
        else:
            elem_dict = {
                "resource_id": elem_info.resource_id,
                "text": elem_info.text,
                "content_desc": elem_info.content_desc,
                "class": elem_info.class_name,
                "bounds": elem_info.bounds,
                "children": elem_info.children,
            }
            return self._find_in_live_xml(live_root, elem_dict)

    def is_element_visible(self, elem: ET.Element, screen_width: int, screen_height: int) -> bool:
        """检查元素是否在屏幕可见区域内"""
        bounds_str = elem.get("bounds", "")
        if not bounds_str:
            return False
        
        bounds = ScreenUtils.parse_bounds(bounds_str)
        if not bounds:
            return False
        
        x1, y1, x2, y2 = bounds
        
        # 检查元素是否在屏幕边界内
        if x1 < 0 or y1 < 0 or x2 > screen_width or y2 > screen_height:
            return False
        
        # 检查元素是否太小（可能被遮挡或不可点击）
        width = x2 - x1
        height = y2 - y1
        if width < 10 or height < 10:
            return False
        
        # 检查元素是否在屏幕可见区域内（排除状态栏、导航栏等）
        visible_area_margin = 50  # 屏幕边缘留出50像素的边距
        if (x1 < visible_area_margin or y1 < visible_area_margin or 
            x2 > screen_width - visible_area_margin or y2 > screen_height - visible_area_margin):
            return False
            
        return True

    def scroll_to_element(self, device: U2Device, elem: ET.Element, screen_width: int, screen_height: int) -> bool:
        """滚动屏幕使元素可见"""
        bounds_str = elem.get("bounds", "")
        if not bounds_str:
            return False
        
        bounds = ScreenUtils.parse_bounds(bounds_str)
        if not bounds:
            return False
        
        x1, y1, x2, y2 = bounds
        center_x = (x1 + x2) // 2
        center_y = (y1 + y2) // 2
        
        # 如果元素在屏幕下方，向上滚动
        if center_y > screen_height * 0.7:
            # 从屏幕中间向上滑动
            start_x = screen_width // 2
            start_y = screen_height * 0.7
            end_x = screen_width // 2
            end_y = screen_height * 0.3
            device.d.swipe(start_x, start_y, end_x, end_y, 0.5)
            print(f"  🔄 向上滚动到元素位置")
            return True
        
        # 如果元素在屏幕上方，向下滚动
        elif center_y < screen_height * 0.3:
            # 从屏幕中间向下滑动
            start_x = screen_width // 2
            start_y = screen_height * 0.3
            end_x = screen_width // 2
            end_y = screen_height * 0.7
            device.d.swipe(start_x, start_y, end_x, end_y, 0.5)
            print(f"  🔄 向下滚动到元素位置")
            return True
        
        return False

