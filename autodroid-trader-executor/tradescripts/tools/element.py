from dataclasses import dataclass
from typing import Optional, Dict
from enum import Enum
import time
import re

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


@dataclass
class StepInfo:
    step: int
    action: str
    element: Dict
    name: Optional[str] = None
    value: Optional[str] = None
    save_to: Optional[str] = None
    desc: Optional[str] = None


class ElementExecutor:
    def __init__(self, device: U2Device):
        self.device = device

    def parse_bounds(self, bounds_str: str):
        if not bounds_str:
            return None
        try:
            parts = bounds_str.strip("[]").split("][")
            x1, y1 = map(int, parts[0].split(","))
            x2, y2 = map(int, parts[1].split(","))
            return (x1, y1, x2, y2)
        except:
            return None

    def calculate_center(self, bounds):
        x1, y1, x2, y2 = bounds
        return ((x1 + x2) // 2, (y1 + y2) // 2)

    def execute_action(self, step_info: StepInfo, live_elem) -> bool:
        action = step_info.action
        elem_name = step_info.element.get("desc") or step_info.element.get("text") or step_info.element.get("resource_id", "").split("/")[-1] or "unknown"

        if action == ActionType.CLICK.value:
            return self._execute_click(elem_name, step_info.element, live_elem)
        elif action == ActionType.INPUT.value:
            return self._execute_input(step_info, elem_name, live_elem)
        elif action == ActionType.GET_TEXT.value:
            return self._execute_get_text(step_info, live_elem)
        elif action == ActionType.WAIT.value:
            return self._execute_wait(step_info)
        elif action == ActionType.PRESS_KEY.value:
            return self._execute_press_key(step_info)
        else:
            print(f"  ⚠️ 不支持的动作: {action}")
            return False

    def _execute_click(self, elem_name: str, elem_info: Dict, live_elem) -> bool:
        try:
            overlap_info = elem_info.get("overlap", {})
            if overlap_info:
                overlap_bounds = tuple(overlap_info.get("overlap", []))
                if overlap_bounds and len(overlap_bounds) == 4:
                    cx, cy = self.calculate_center(overlap_bounds)
                    self.device.click(cx, cy)
                    overlap_text = overlap_info.get("text", "")
                    print(f"  ✓ 点击 [{elem_name}] @ ({cx}, {cy}) [基于重叠元素: '{overlap_text}']")
                    return True

            bounds = self.parse_bounds(live_elem.get("bounds", ""))
            if bounds:
                cx, cy = self.calculate_center(bounds)
                self.device.click(cx, cy)
                print(f"  ✓ 点击 [{elem_name}] @ ({cx}, {cy})")
                return True
            else:
                live_elem_elem = self.device(resourceId=live_elem.get("resource-id", "")) if live_elem.get("resource-id") else None
                if not live_elem_elem:
                    live_elem_elem = self.device(text=live_elem.get("text", ""))
                if live_elem_elem and live_elem_elem.exists:
                    live_elem_elem.click()
                    print(f"  ✓ 点击 [{elem_name}]")
                    return True
                print(f"  ⚠️ 无法获取元素坐标")
                return False
        except Exception as e:
            print(f"  ⚠️ 点击失败: {e}")
            return False

    def _execute_input(self, step_info: StepInfo, elem_name: str, live_elem, test_data: Dict = None) -> bool:
        target_value = step_info.name and test_data.get(step_info.name) if test_data else step_info.value
        if target_value:
            try:
                bounds = self.parse_bounds(live_elem.get("bounds", ""))
                if bounds:
                    cx, cy = self.calculate_center(bounds)
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

    def _execute_get_text(self, step_info: StepInfo, live_elem, runtime_context: Dict = None) -> bool:
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

    def _execute_wait(self, step_info: StepInfo) -> bool:
        wait_time = float(step_info.value) if step_info.value else 1.0
        try:
            time.sleep(wait_time)
            print(f"  ✓ 等待 {wait_time} 秒")
            return True
        except:
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
