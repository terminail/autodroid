import time
import re
import logging
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass
from pathlib import Path
import xml.etree.ElementTree as ET

from .adb_driver import ADBManager

logger = logging.getLogger(__name__)


@dataclass
class StepInfo:
    step_number: int
    element: ET.Element
    action: str
    name: Optional[str]
    value: Optional[str]
    save_to: Optional[str]
    wait_after: float
    description: str


@dataclass
class ExecutionResult:
    success: bool
    step_number: int
    action: str
    message: str
    captured_data: Optional[Dict[str, Any]] = None


class ActionHandler:
    ACTION_CLICK = "click"
    ACTION_INPUT = "input"
    ACTION_SELECT = "select"
    ACTION_GET_TEXT = "get_text"
    ACTION_WAIT = "wait"
    ACTION_SWIPE = "swipe"
    ACTION_VERIFY = "verify"
    ACTION_PRESS_KEY = "press_key"

    ID_COMPATIBILITY_MAP = {
        "entry-xzsg": {
            "text": "条件单",
            "alt_ids": ["entry-tjd"]
        },
        "entry-tjd": {
            "alt_ids": []
        },
    }

    def __init__(self, adb_manager: ADBManager, device_id: str, wait_timeout: int = 30):
        self.adb_manager = adb_manager
        self.device_id = device_id
        self.wait_timeout = wait_timeout

    def get_bounds_from_xml(self, elem: ET.Element) -> Optional[Tuple[int, int, int, int]]:
        """从 XML 元素中提取边界坐标"""
        bounds = elem.get("bounds", "")
        if bounds:
            match = re.findall(r'\[(\d+),(\d+)\]', bounds)
            if len(match) >= 2:
                x1, y1 = int(match[0][0]), int(match[0][1])
                x2, y2 = int(match[1][0]), int(match[1][1])
                return (x1, y1, x2, y2)
        return None

    def calculate_center(self, bounds: Tuple[int, int, int, int]) -> Tuple[int, int]:
        """计算边界中心点"""
        x1, y1, x2, y2 = bounds
        return ((x1 + x2) // 2, (y1 + y2) // 2)

    def adb_click(self, x: int, y: int) -> bool:
        """使用 ADB 点击指定坐标"""
        return self.adb_manager.tap(self.device_id, x, y)

    def adb_input_text(self, text: str) -> bool:
        """使用 ADB 输入文本"""
        return self.adb_manager.input_text(self.device_id, text)

    def adb_clear_input(self, x: int, y: int) -> bool:
        """使用 ADB 清除输入框内容（通过全选删除）"""
        self.adb_manager.tap(self.device_id, x, y)
        time.sleep(0.1)
        self.adb_manager.run_shell(self.device_id, "input keyevent KEYCODE_SELECT_ALL")
        time.sleep(0.1)
        self.adb_manager.run_shell(self.device_id, "input keyevent KEYCODE_DELETE")
        time.sleep(0.1)
        return True

    def adb_swipe(self, x1: int, y1: int, x2: int, y2: int) -> bool:
        """使用 ADB 执行滑动操作"""
        return self.adb_manager.swipe(self.device_id, x1, y1, x2, y2)

    def adb_press_key(self, keycode: str) -> bool:
        """使用 ADB 按键"""
        key_map = {
            "BACK": "4",
            "HOME": "3",
            "ENTER": "66",
            "DEL": "67",
            "TAB": "61",
        }
        key = key_map.get(keycode.upper(), keycode)
        return self.adb_manager.press_keycode(self.device_id, key)

    def find_element_in_xml(
        self,
        root: ET.Element,
        attrs: Dict[str, str]
    ) -> Optional[ET.Element]:
        """在 XML 树中根据属性查找元素"""
        resource_id = attrs.get("resource-id", "").split("/")[-1]
        text = attrs.get("text", "")
        content_desc = attrs.get("content-desc", "")
        class_name = attrs.get("class", "")

        for elem in root.iter():
            if elem.tag == "hierarchy":
                continue

            match = True

            if resource_id and elem.get("resource-id", "").split("/")[-1] != resource_id:
                match = False
            elif text and elem.get("text", "") != text:
                match = False
            elif content_desc and elem.get("content-desc", "") != content_desc:
                match = False
            elif class_name and elem.get("class", "") != class_name:
                match = False

            if match:
                return elem

        return None

    def resolve_value(
        self, name: Optional[str], hardcoded_value: Optional[str], test_data: Dict[str, Any]
    ) -> Optional[str]:
        if name and name in test_data:
            return test_data[name]
        return hardcoded_value

    def execute_click(self, elem: ET.Element, description: str = "") -> bool:
        bounds = self.get_bounds_from_xml(elem)
        if not bounds:
            logger.error(f"  [点击失败] 无法获取元素边界: {description}")
            return False

        x, y = self.calculate_center(bounds)
        click_start = time.time()

        if self.adb_click(x, y):
            click_time = time.time() - click_start
            logger.info(f"  [点击执行耗时 {click_time:.2f}s] 坐标=({x}, {y}) - {description}")
            return True
        else:
            click_time = time.time() - click_start
            logger.error(f"  [点击失败耗时 {click_time:.2f}s] - {description}")
            return False

    def execute_input(
        self, elem: ET.Element, value: str, description: str = ""
    ) -> bool:
        bounds = self.get_bounds_from_xml(elem)
        if not bounds:
            logger.error(f"  [输入失败] 无法获取输入框边界: {description}")
            return False

        x, y = self.calculate_center(bounds)

        self.adb_clear_input(x, y)

        if self.adb_input_text(value):
            logger.info(f"  [输入执行成功] 值=[{value}] - {description}")
            return True
        else:
            logger.error(f"  [输入执行失败] 值=[{value}] - {description}")
            return False

    def execute_get_text(
        self, elem: ET.Element, save_to: Optional[str], description: str = ""
    ) -> Optional[str]:
        text = elem.get("text", "")
        if text:
            logger.info(f"  [获取文本成功] [{save_to}] = '{text}' - {description}")
            return text
        else:
            logger.error(f"  [获取文本失败] 元素无文本属性 - {description}")
            return None

    def execute_wait(self, value: str) -> bool:
        try:
            wait_seconds = float(value)
            time.sleep(wait_seconds)
            logger.info(f"  [等待执行成功] {wait_seconds} 秒")
            return True
        except ValueError:
            logger.error(f"  [等待执行失败] 无效的等待时间值: {value}")
            return False

    def execute_swipe(self, elem: ET.Element, value: str) -> bool:
        bounds = self.get_bounds_from_xml(elem)
        if not bounds:
            logger.error(f"  [滑动失败] 无法获取元素边界")
            return False

        try:
            direction, percent = value.split(":")
            percent = float(percent)
        except (ValueError, TypeError):
            logger.error(f"  [滑动失败] 无效的滑动参数: {value}")
            return False

        x1, y1, x2, y2 = bounds
        center_x = (x1 + x2) // 2
        center_y = (y1 + y2) // 2

        width = x2 - x1
        height = y2 - y1

        distance = height * percent

        if direction == "up":
            end_x, end_y = center_x, int(center_y - distance)
        elif direction == "down":
            end_x, end_y = center_x, int(center_y + distance)
        elif direction == "left":
            end_x, end_y = int(center_x - distance), center_y
        elif direction == "right":
            end_x, end_y = int(center_x + distance), center_y
        else:
            logger.error(f"  [滑动失败] 未知的滑动方向: {direction}")
            return False

        if self.adb_swipe(center_x, center_y, end_x, end_y):
            logger.info(f"  [滑动执行成功] {direction} {percent * 100}%")
            return True
        else:
            logger.error(f"  [滑动执行失败]")
            return False

    def execute_press_key(self, value: str) -> bool:
        if self.adb_press_key(value):
            logger.info(f"  [按键执行成功] {value}")
            return True
        else:
            logger.error(f"  [按键执行失败] {value}")
            return False

    def execute_action(
        self,
        action: str,
        element: ET.Element,
        value: Optional[str] = None,
        save_to: Optional[str] = None,
        description: str = "",
    ) -> Tuple[bool, Optional[str]]:
        if action == self.ACTION_CLICK:
            return self.execute_click(element, description), None
        elif action == self.ACTION_INPUT:
            if value:
                return self.execute_input(element, value, description), None
            return False, None
        elif action == self.ACTION_GET_TEXT:
            text = self.execute_get_text(element, save_to, description)
            return text is not None, text
        elif action == self.ACTION_WAIT:
            if value:
                return self.execute_wait(value), None
            return False, None
        elif action == self.ACTION_SWIPE:
            if value:
                return self.execute_swipe(element, value), None
            return False, None
        elif action == self.ACTION_PRESS_KEY:
            if value:
                return self.execute_press_key(value), None
            return False, None
        else:
            logger.error(f"  [执行失败] 未知的动作类型: {action}")
            return False, None


class DataDrivenExecutor:
    def __init__(
        self,
        adb_manager: ADBManager,
        device_id: str,
        apks_dir: str,
        wait_timeout: int = 30,
    ):
        self.adb_manager = adb_manager
        self.device_id = device_id
        self.apks_dir = Path(apks_dir)
        self.wait_timeout = wait_timeout
        self.action_handler = ActionHandler(adb_manager, device_id, wait_timeout)
        self.runtime_context: Dict[str, Any] = {}
        self.current_xml_root: Optional[ET.Element] = None

    def set_test_data(self, test_data: Dict[str, Any]):
        self.test_data = test_data

    @staticmethod
    def _sanitize_xml(xml_content: str) -> str:
        xml_content = re.sub(r'\sautodroid:[a-z_.-]+="[^"]*"', '', xml_content)
        xml_content = re.sub(r'autodroid:[a-z_.-]+="[^"]*"', '', xml_content)
        return xml_content

    @staticmethod
    def _extract_autodroid_attrs(xml_content: str) -> Dict[int, Dict[str, str]]:
        attrs_by_line = {}
        for match in re.finditer(r'<([^\s>]+)([^>]*)index="(\d+)"([^>]*)>', xml_content):
            elem_tag = match.group(1)
            before_index = match.group(2)
            index = int(match.group(3))
            after_index = match.group(4)
            attr_str = before_index + after_index
            attrs = {}
            for attr_match in re.finditer(r'autodroid:([a-z_.-]+)="([^"]*)"', attr_str):
                attrs[attr_match.group(1)] = attr_match.group(2)
            if attrs:
                attrs_by_line[index] = attrs
        return attrs_by_line

    def execute_page_flow(self, page_id: str) -> List[ExecutionResult]:
        xml_path = self.apks_dir / f"{page_id}.xml"

        if not xml_path.exists():
            logger.error(f"Page XML not found: {xml_path}")
            return [
                ExecutionResult(
                    success=False,
                    step_number=0,
                    action="load",
                    message=f"Page XML not found: {xml_path}",
                )
            ]

        try:
            with open(xml_path, 'r', encoding='utf-8') as f:
                xml_content = f.read()
            autodroid_attrs = self._extract_autodroid_attrs(xml_content)
            clean_xml_content = self._sanitize_xml(xml_content)
            self.current_xml_root = ET.fromstring(clean_xml_content)
        except ET.ParseError as e:
            logger.error(f"Failed to parse XML: {e}")
            return [
                ExecutionResult(
                    success=False,
                    step_number=0,
                    action="parse",
                    message=f"Failed to parse XML: {e}",
                )
            ]

        steps = self._collect_steps(self.current_xml_root, autodroid_attrs)

        results = []
        for step_info in steps:
            result = self._execute_single_step(step_info)
            results.append(result)

            if result.success:
                if step_info.wait_after > 0:
                    logger.info(f"Waiting after step {step_info.step_number}: {step_info.wait_after}s")
                    time.sleep(step_info.wait_after)
            else:
                logger.warning(
                    f"Step {step_info.step_number} failed: {result.message}"
                )

        return results

    def _collect_steps(self, root: ET.Element, autodroid_attrs: Dict[int, Dict[str, str]] = None) -> List[StepInfo]:
        if autodroid_attrs is None:
            autodroid_attrs = {}
        steps = []
        step_counter = 0

        for elem in root.iter():
            if elem.tag == "hierarchy":
                continue

            index = elem.get("index")
            elem_attrs = {}
            if index is not None:
                elem_attrs = autodroid_attrs.get(int(index), {})

            action = elem_attrs.get("action") or elem.get("autodroid:action")
            if not action:
                continue

            step_str = elem.get("autodroid:step")
            if step_str:
                try:
                    step_number = int(step_str)
                except ValueError:
                    step_number = step_counter
            else:
                step_number = step_counter
                step_counter += 1

            step_info = StepInfo(
                step_number=step_number,
                element=elem,
                action=action,
                name=elem.get("autodroid:name"),
                value=elem.get("autodroid:value"),
                save_to=elem.get("autodroid:save_to"),
                wait_after=float(elem.get("autodroid:wait_after", 0)),
                description=elem.get("autodroid:desc", f"Step {step_number}"),
            )

            steps.append(step_info)

        steps.sort(key=lambda s: s.step_number)
        return steps

    def _execute_single_step(self, step_info: StepInfo) -> ExecutionResult:
        elem = step_info.element
        resource_id = elem.get("resource-id", "").split("/")[-1]
        element_desc = resource_id if resource_id else step_info.description

        logger.info(f"[Step {step_info.step_number}] 开始执行: action={step_info.action}, 目标元素={element_desc}")

        if not self.current_xml_root:
            logger.error(f"[Step {step_info.step_number}] XML 根元素未初始化")
            return ExecutionResult(
                success=False,
                step_number=step_info.step_number,
                action=step_info.action,
                message="XML root not initialized",
            )

        success, captured = self.action_handler.execute_action(
            action=step_info.action,
            element=elem,
            value=self.action_handler.resolve_value(
                step_info.name, step_info.value, getattr(self, "test_data", {})
            ),
            save_to=step_info.save_to,
            description=f"Step {step_info.step_number}: {element_desc}",
        )

        if captured and step_info.save_to:
            self.runtime_context[step_info.save_to] = captured

        status = "成功" if success else "失败"
        logger.info(f"[Step {step_info.step_number}] 执行{status}: {step_info.action} - {element_desc}")

        return ExecutionResult(
            success=success,
            step_number=step_info.step_number,
            action=step_info.action,
            message=f"Step {step_info.step_number} {status}",
            captured_data={step_info.save_to: captured} if captured else None,
        )

    def get_runtime_context(self) -> Dict[str, Any]:
        return self.runtime_context.copy()

    def clear_runtime_context(self):
        self.runtime_context.clear()
