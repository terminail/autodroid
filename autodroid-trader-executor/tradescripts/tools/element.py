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
        # 只使用Appium IME方法，禁用其他fallback方法以避免对话框
        try:
            return self._safe_input_using_appium_ime(elem_name, elem_info, live_elem_obj, value)
        except Exception as e:
            print(f"  ⚠️ Appium IME输入失败: {e}")
            # 不再尝试其他方法，直接返回失败
            return False
    
    def _is_text_input_acceptable(self, actual_text: str, expected_text: str) -> bool:
        """检查实际输入文本是否可以接受（允许应用自动格式化）"""
        # 如果实际文本等于期望文本，直接接受
        if actual_text == expected_text:
            return True
        
        # 处理数字格式化：允许应用去掉小数点（如 100.0 -> 100）
        try:
            # 尝试将文本转换为数字进行比较
            actual_num = float(actual_text) if actual_text else 0
            expected_num = float(expected_text) if expected_text else 0
            
            # 如果数值相等，即使格式不同也接受
            if abs(actual_num - expected_num) < 0.0001:  # 允许小的浮点误差
                return True
        except (ValueError, TypeError):
            pass
        
        # 处理空格和格式差异
        actual_clean = actual_text.strip()
        expected_clean = expected_text.strip()
        if actual_clean == expected_clean:
            return True
        
        # 如果实际文本包含期望文本（部分匹配）
        if expected_text in actual_text:
            return True
            
        return False

    def _encode_modified_utf7(self, text: str) -> str:
        """将文本编码为Modified UTF-7格式（Appium Unicode IME要求）"""
        # Modified UTF-7编码规则：
        # - 可打印ASCII字符（0x20-0x7E）保持不变，除了'&'
        # - '&' 编码为 '&-' 
        # - 非ASCII字符使用Base64编码，并用'&'和'-'包围
        
        if not text:
            return ""
        
        result = []
        i = 0
        n = len(text)
        
        while i < n:
            ch = text[i]
            # 处理'&'字符
            if ch == '&':
                result.append('&-')
                i += 1
            # 处理可打印ASCII字符（除了'&'）
            elif 0x20 <= ord(ch) <= 0x7E:
                result.append(ch)
                i += 1
            # 处理非ASCII字符序列
            else:
                # 收集连续的非ASCII字符
                non_ascii_chars = []
                j = i
                while j < n and (ord(text[j]) < 0x20 or ord(text[j]) > 0x7E or text[j] == '&'):
                    if text[j] == '&':
                        break  # '&'需要单独处理
                    non_ascii_chars.append(text[j])
                    j += 1
                
                if non_ascii_chars:
                    # 将非ASCII字符编码为UTF-16BE字节序列
                    utf16_bytes = ''.join(non_ascii_chars).encode('utf-16be')
                    # Base64编码，移除末尾的'='填充
                    import base64
                    base64_encoded = base64.b64encode(utf16_bytes).decode('ascii').rstrip('=')
                    # 替换Base64中的'/'为','（Modified UTF-7要求）
                    base64_encoded = base64_encoded.replace('/', ',')
                    result.append('&' + base64_encoded + '-')
                    i = j
                else:
                    # 如果没有非ASCII字符，继续处理
                    result.append(ch)
                    i += 1
        
        return ''.join(result)

    def _safe_input_using_appium_ime(self, elem_name: str, elem_info: ElementInfo, live_elem_obj, value: str) -> bool:
        """使用Appium Settings的Unicode IME进行安全文本输入"""
        try:
            # 首先确保编辑框获得焦点（使用温和的方式）
            if live_elem_obj.exists:
                bounds = live_elem_obj.info.get('bounds', {})
                if bounds:
                    x = (bounds.get('left', 0) + bounds.get('right', 0)) // 2
                    y = (bounds.get('top', 0) + bounds.get('bottom', 0)) // 2
                    # 使用坐标点击而非元素点击
                    self.device.d.click(x, y)
                    import time
                    time.sleep(0.5)  # 等待较短时间确保焦点稳定
            
            # 方法1: 尝试直接使用Appium IME的set_text方法
            # 由于系统默认输入法已经是Appium IME，直接输入应该安全
            try:
                live_elem_obj.set_text(value)
                time.sleep(0.3)
                actual_text = live_elem_obj.get_text()
                # 更灵活的验证：允许应用自动格式化文本（如去掉小数点）
                if actual_text == value or self._is_text_input_acceptable(actual_text, value):
                    if elem_info.name:
                        print(f"  ✓ 使用Appium IME直接输入 [{elem_info.name}] [{elem_name}]: {value} -> {actual_text}")
                    else:
                        print(f"  ✓ 使用Appium IME直接输入 [{elem_name}]: {value} -> {actual_text}")
                    return True
                else:
                    print(f"  ⚠️ 直接输入验证失败: 期望 '{value}', 实际 '{actual_text}'")
            except Exception as e1:
                print(f"  ⚠️ 直接输入失败: {e1}")
            
            # 方法2: 如果直接输入失败，尝试使用Modified UTF-7编码
            # 将文本编码为Modified UTF-7格式
            encoded_text = self._encode_modified_utf7(value)
            
            # 清除原有文本并输入编码后的文本
            live_elem_obj.clear_text()
            live_elem_obj.set_text(encoded_text)
            
            # 验证文本是否成功输入
            time.sleep(0.3)
            actual_text = live_elem_obj.get_text()
            # 更灵活的验证：允许应用自动格式化文本
            if actual_text == value or self._is_text_input_acceptable(actual_text, value):
                if elem_info.name:
                    print(f"  ✓ 使用Modified UTF-7编码输入 [{elem_info.name}] [{elem_name}]: {value} -> {actual_text}")
                else:
                    print(f"  ✓ 使用Modified UTF-7编码输入 [{elem_name}]: {value} -> {actual_text}")
                return True
            else:
                print(f"  ⚠️ 编码输入验证失败: 期望 '{value}', 实际 '{actual_text}'")
                return False
            
        except Exception as e:
            print(f"  ⚠️ Appium IME输入失败: {e}")
            # 如果Appium IME不可用，抛出异常让上层处理
            raise

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


