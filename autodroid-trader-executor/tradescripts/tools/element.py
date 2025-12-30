from dataclasses import dataclass
from typing import Optional, Dict, Tuple, List, Union
from enum import Enum
import time
import re
import xml.etree.ElementTree as ET
from pydantic import BaseModel

from u2device import U2Device


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
    REDIRECT = "redirect"


class ElementInfo(BaseModel):
    """元素信息类型化类"""
    resource_id: str = ""
    text: str = ""
    content_desc: str = ""
    class_name: str = ""
    children: List[str] = []
    action: str = ""
    step: Optional[int] = None
    name: Optional[str] = None
    value: Optional[str] = None
    save_to: Optional[str] = None
    desc: Optional[str] = None
    wait_after: Optional[str] = None
    bounds: str = ""
    id: Optional[str] = None
    index: int = 0


@dataclass
class StepInfo:
    step: int
    action: str
    element: ElementInfo
    name: Optional[str] = None
    value: Optional[str] = None
    save_to: Optional[str] = None
    desc: Optional[str] = None


def _bounds_match(norm1: Tuple[float, float, float, float], norm2: Tuple[float, float, float, float], tolerance: float = 0.02) -> bool:
    x1_diff = abs(norm1[0] - norm2[0])
    y1_diff = abs(norm1[1] - norm2[1])
    x2_diff = abs(norm1[2] - norm2[2])
    y2_diff = abs(norm1[3] - norm2[3])
    return x1_diff <= tolerance and y1_diff <= tolerance and x2_diff <= tolerance and y2_diff <= tolerance


def structural_match(live_root: ET.Element, elem_info: Union[Dict, ElementInfo]) -> Tuple[Optional[ET.Element], str]:
    if isinstance(elem_info, ElementInfo):
        rid = elem_info.resource_id.strip()
        text = elem_info.text.strip()
        content_desc = elem_info.content_desc.strip()
        bounds = elem_info.bounds.strip()
        class_name = elem_info.class_name.strip()
        parent_rid = ""
        parent_text = ""
        parent_class = ""
        sibling_texts = []
        sibling_resource_ids = []
    else:
        rid = elem_info.get("resource_id", "").strip()
        text = elem_info.get("text", "").strip()
        content_desc = elem_info.get("content_desc", "").strip()
        bounds = elem_info.get("bounds", "").strip()
        class_name = elem_info.get("class", "").strip()
        parent_rid = elem_info.get("parent_resource_id", "").strip()
        parent_text = elem_info.get("parent_text", "").strip()
        parent_class = elem_info.get("parent_class", "").strip()
        sibling_texts = elem_info.get("sibling_texts", [])
        sibling_resource_ids = elem_info.get("sibling_resource_ids", [])

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

    threshold = max_possible_score * 0.6 if max_possible_score > 0 else 0.5

    parent_map = {child: elem for elem in live_root.iter() for child in elem}

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

            if sibling_texts:
                overlap = len(set(sibling_texts) & set(live_sibling_texts))
                if overlap > 0:
                    score += 0.1 * (overlap / len(sibling_texts))
                    reasons.append(f"兄弟元素text匹配({overlap}/{len(sibling_texts)})")

            if sibling_resource_ids:
                overlap = len(set(sibling_resource_ids) & set(live_sibling_resource_ids))
                if overlap > 0:
                    score += 0.1 * (overlap / len(sibling_resource_ids))
                    reasons.append(f"兄弟元素resource-id匹配({overlap}/{len(sibling_resource_ids)})")

        if score > best_score and score >= threshold:
            best_score = score
            best_elem = elem
            best_reason = ", ".join(reasons)

    if best_elem:
        return (best_elem, f"结构化匹配(得分:{best_score:.2f}): {best_reason}")
    return (None, "未找到结构化匹配")


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


def exact_match(live_root: ET.Element, elem_info: Union[Dict, ElementInfo]) -> Tuple[Optional[ET.Element], str]:
    if isinstance(elem_info, ElementInfo):
        rid = elem_info.resource_id.strip()
        text = elem_info.text.strip()
        content_desc = elem_info.content_desc.strip()
        bounds = elem_info.bounds.strip()
        normalized_bounds = None
    else:
        rid = elem_info.get("resource_id", "").strip()
        text = elem_info.get("text", "").strip()
        content_desc = elem_info.get("content_desc", "").strip()
        bounds = elem_info.get("bounds", "").strip()
        normalized_bounds = elem_info.get("normalized_bounds")

    if rid:
        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            live_rid = elem.get("resource-id", "").strip()
            if live_rid == rid:
                return elem, f"resource-id精确匹配: {rid}"

    if normalized_bounds and normalized_bounds != (0.0, 0.0, 0.0, 0.0):
        live_screen_width, live_screen_height = get_screen_size(live_root)
        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            live_bounds_str = elem.get("bounds", "").strip()
            live_bounds = parse_bounds(live_bounds_str)
            if live_bounds:
                live_normalized = normalize_bounds(live_bounds, live_screen_width, live_screen_height)
                if _bounds_match(normalized_bounds, live_normalized):
                    return elem, f"归一化bounds匹配: {bounds}"

    if text:
        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            live_text = elem.get("text", "").strip()
            if live_text == text:
                return (elem, f"text精确匹配: {text}")

    if content_desc:
        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            live_content_desc = elem.get("content-desc", "").strip()
            if live_content_desc == content_desc:
                return (elem, f"content-desc精确匹配: {content_desc}")

    return (None, "未找到精确匹配")


def find_element(live_root: ET.Element, elem_info: Dict) -> Tuple[Optional[ET.Element], str]:
    elem, reason = exact_match(live_root, elem_info)
    if elem:
        return (elem, reason)

    elem, reason = structural_match(live_root, elem_info)
    return (elem, reason)


class ElementMatcher:
    def __init__(self):
        pass


def calculate_center(bounds):
    x1, y1, x2, y2 = bounds
    return (x1 + x2) // 2, (y1 + y2) // 2


def _execute_get_text(step_info: StepInfo, live_elem, runtime_context: Dict = None) -> bool:
    try:
        text = live_elem.get("text", "")
        if step_info.save_to and runtime_context:
            runtime_context[step_info.save_to] = text
            print(f"  ✓ 获取文本: [{text}] -> {step_info.save_to}")
        else:
            print(f"  ✓ 获取文本: [{text}]")
        return True
    except Exception as e:
        print(f"  ⚠️ 获取文本失败: {e}")
        return False


def _execute_wait(step_info: StepInfo) -> bool:
    wait_time = float(step_info.value) if step_info.value else 1.0
    try:
        time.sleep(wait_time)
        print(f"  ✓ 等待 {wait_time} 秒")
        return True
    except:
        return False


def _execute_wait_for_user(step_info: StepInfo) -> bool:
    print(f"  ⏸️ 等待用户操作...")
    try:
        input("  按回车键继续...")
        print(f"  ✓ 用户继续")
        return True
    except:
        print(f"  ⚠️ 用户取消")
        return False


class ElementExecutor:
    def __init__(self, device: U2Device):
        self.device = device
        self.matcher = ElementMatcher()

    def find_element(self, live_root: ET.Element, elem_info: Dict) -> Tuple[Optional[ET.Element], str]:
        return find_element(live_root, elem_info)

    def execute_action(self, step_info: StepInfo, live_elem) -> bool:
        action = step_info.action
        elem_info = step_info.element
        elem_name = elem_info.desc or elem_info.text or (elem_info.resource_id.split("/")[-1] if elem_info.resource_id else "") or "unknown"

        if action == ActionType.CLICK.value:
            return self._execute_click(elem_name, elem_info, live_elem)
        elif action == ActionType.REDIRECT.value:
            return self._execute_redirect(elem_name, elem_info, live_elem)
        elif action == ActionType.INPUT.value:
            return self._execute_input(step_info, elem_name, live_elem)
        elif action == ActionType.GET_TEXT.value:
            return _execute_get_text(step_info, live_elem)
        elif action == ActionType.WAIT.value:
            return _execute_wait(step_info)
        elif action == ActionType.WAIT_FOR_USER.value:
            return _execute_wait_for_user(step_info)
        elif action == ActionType.PRESS_KEY.value:
            return self._execute_press_key(step_info)
        else:
            print(f"  ⚠️ 不支持的动作: {action}")
            return False

    def _execute_click(self, elem_name: str, elem_info: ElementInfo, live_elem) -> bool:
        try:
            bounds = parse_bounds(live_elem.get("bounds", ""))
            if bounds:
                cx, cy = calculate_center(bounds)
                self.device.click(cx, cy)
                print(f"  ✓ 点击 [{elem_name}] @ ({cx}, {cy})")
                return True
            else:
                live_elem_elem = self.device.d(resourceId=live_elem.get("resource-id", "")) if live_elem.get("resource-id") else None
                if not live_elem_elem or not live_elem_elem.exists:
                    live_elem_elem = self.device.d(text=live_elem.get("text", ""))
                if live_elem_elem and live_elem_elem.exists:
                    live_elem_elem.click()
                    print(f"  ✓ 点击 [{elem_name}]")
                    return True
                print(f"  ⚠️ 无法获取元素坐标")
                return False
        except Exception as e:
            print(f"  ⚠️ 点击失败: {e}")
            return False

    def _execute_redirect(self, elem_name: str, elem_info: ElementInfo, live_elem) -> bool:
        try:
            bounds = parse_bounds(live_elem.get("bounds", ""))
            if bounds:
                cx, cy = calculate_center(bounds)
                self.device.click(cx, cy)
                print(f"  ✓ 跳转 [{elem_name}] @ ({cx}, {cy})")
                return True
            else:
                live_elem_elem = self.device.d(resourceId=live_elem.get("resource-id", "")) if live_elem.get("resource-id") else None
                if not live_elem_elem or not live_elem_elem.exists:
                    live_elem_elem = self.device.d(text=live_elem.get("text", ""))
                if live_elem_elem and live_elem_elem.exists:
                    live_elem_elem.click()
                    print(f"  ✓ 跳转 [{elem_name}]")
                    return True
                print(f"  ⚠️ 无法获取元素坐标")
                return False
        except Exception as e:
            print(f"  ⚠️ 跳转失败: {e}")
            return False

    def _execute_input(self, step_info: StepInfo, elem_name: str, live_elem, test_data: Dict = None) -> bool:
        target_value = step_info.name and test_data.get(step_info.name) if test_data else step_info.value
        if target_value:
            try:
                bounds = parse_bounds(live_elem.get("bounds", ""))
                if bounds:
                    cx, cy = calculate_center(bounds)
                    self.device.click(cx, cy)
                else:
                    live_elem.click()
                time.sleep(0.3)
                self.device.clear_text()
                self.device.send_keys(str(target_value))
                print(f"  ✓ 输入: [{target_value}] -> [{elem_name}]")
                return True
            except Exception as e:
                print(f"  ⚠️ 输入失败: {e}")
                return False
        else:
            print(f"  ⚠️ 没有可用的输入值")
            return False

    def _execute_press_key(self, step_info: StepInfo) -> bool:
        key_map = {"BACK": 4, "HOME": 3, "MENU": 82}
        key_code = key_map.get(step_info.value, 4)
        try:
            self.device.press(key_code)
            print(f"  ✓ 按键: {step_info.value}")
            return True
        except Exception as e:
            print(f"  ⚠️ 按键失败: {e}")
            return False
