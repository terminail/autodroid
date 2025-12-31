from dataclasses import dataclass
from typing import Optional, Dict, Tuple, List, Union
from enum import Enum
import time
import re
import xml.etree.ElementTree as ET
from pydantic import BaseModel

from u2device import U2Device, ScreenUtils, calculate_center


class ActionType(Enum):
    CLICK = "click"
    INPUT = "input"
    SELECT = "select"
    GET_TEXT = "get_text"
    SWIPE = "swipe"
    VERIFY = "verify"
    PRESS_KEY = "press_key"


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
        live_screen_width, live_screen_height = ScreenUtils.get_screen_size(live_root)
        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            live_bounds_str = elem.get("bounds", "").strip()
            live_bounds = ScreenUtils.parse_bounds(live_bounds_str)
            if live_bounds:
                live_normalized = ScreenUtils.normalize_bounds(live_bounds, live_screen_width, live_screen_height)
                if ScreenUtils.bounds_match(normalized_bounds, live_normalized):
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


class ElementMatcher:
    def __init__(self):
        pass


class ElementExecutor:
    def __init__(self, device: U2Device):
        self.device = device
        self.matcher = ElementMatcher()

    def execute_action(self, step_info: StepInfo, live_elem_obj) -> bool:
        action = step_info.action
        elem_info = step_info.element
        elem_name = elem_info.desc or elem_info.text or (elem_info.resource_id.split("/")[-1] if elem_info.resource_id else "") or "unknown"

        if action == ActionType.CLICK.value:
            return self._execute_click(elem_name, elem_info, live_elem_obj)
        elif action == ActionType.INPUT.value:
            return self._execute_input(elem_name, elem_info, live_elem_obj, step_info.value)
        elif action == ActionType.PRESS_KEY.value:
            return self._execute_press_key(elem_name, elem_info, live_elem_obj, step_info.value)
        elif action == ActionType.GET_TEXT.value:
            return self._execute_get_text(step_info, live_elem_obj)
        else:
            print(f"  ⚠️ 不支持的动作: {action}")
            return False

    def _execute_get_text(self, step_info: StepInfo, live_elem_obj, runtime_context: Dict = None) -> bool:
        try:
            if not live_elem_obj or not live_elem_obj.exists:
                print(f"  ⚠️ 元素不存在")
                return False
            
            text = live_elem_obj.info.get('text', '')
            if step_info.save_to and runtime_context:
                runtime_context[step_info.save_to] = text
                print(f"  ✓ 获取文本: [{text}] -> {step_info.save_to}")
            else:
                print(f"  ✓ 获取文本: [{text}]")
            return True
        except Exception as e:
            print(f"  ⚠️ 获取文本失败: {e}")
            return False

    def _execute_click(self, elem_name: str, elem_info: ElementInfo, live_elem_obj) -> bool:
        try:
            live_elem_obj.click()
            if elem_info.name:
                print(f"  ✓ 点击 [{elem_info.name}] [{elem_name}]")
            else:
                print(f"  ✓ 点击 [{elem_name}]")
            return True
        except Exception as e:
            print(f"  ⚠️ 点击失败: {e}")
            return False

    def _execute_input(self, elem_name: str, elem_info: ElementInfo, live_elem_obj, value: str) -> bool:
        try:
            live_elem_obj.set_text(value)
            if elem_info.name:
                print(f"  ✓ 输入 [{elem_info.name}] [{elem_name}]: {value}")
            else:
                print(f"  ✓ 输入 [{elem_name}]: {value}")
            return True
        except Exception as e:
            print(f"  ⚠️ 输入失败: {e}")
            return False

    def _execute_press_key(self, elem_name: str, elem_info: ElementInfo, live_elem_obj, value: str) -> bool:
        try:
            self.device.d.press(value)
            if elem_info.name:
                print(f"  ✓ 按键 [{elem_info.name}] [{elem_name}]: {value}")
            else:
                print(f"  ✓ 按键 [{elem_name}]: {value}")
            return True
        except Exception as e:
            print(f"  ⚠️ 按键失败: {e}")
            return False


