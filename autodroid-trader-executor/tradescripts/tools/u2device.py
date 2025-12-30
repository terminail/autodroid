from typing import Optional, Tuple, List
from pathlib import Path
import time
import uiautomator2 as u2
import xml.etree.ElementTree as ET
import subprocess
import logging

logger = logging.getLogger(__name__)


class ScreenUtils:
    @staticmethod
    def parse_bounds(bounds_str: str) -> Optional[Tuple[int, int, int, int]]:
        """Parse bounds string from Android UI dump"""
        if not bounds_str:
            return None
        try:
            parts = bounds_str.strip("[]").split("][")
            x1, y1 = map(int, parts[0].split(","))
            x2, y2 = map(int, parts[1].split(","))
            return (x1, y1, x2, y2)
        except:
            return None

    @staticmethod
    def normalize_bounds(bounds: Tuple[int, int, int, int], screen_width: int, screen_height: int) -> Tuple[float, float, float, float]:
        """Normalize bounds to relative coordinates (0-1)"""
        x1, y1, x2, y2 = bounds
        return (x1,y1,x2,y2)
        # for now, return absolute coordinates
        # return (round(x1 / screen_width, 4), round(y1 / screen_height, 4), 
        #         round(x2 / screen_width, 4), round(y2 / screen_height, 4))

    @staticmethod
    def get_screen_size(root: ET.Element) -> Tuple[int, int]:
        """Get screen size from hierarchy element bounds"""
        for elem in root.iter():
            if elem.tag == "hierarchy":
                bounds_str = elem.get("bounds", "")
                if bounds_str:
                    bounds = ScreenUtils.parse_bounds(bounds_str)
                    if bounds:
                        return (bounds[2], bounds[3])
        return (1080, 1920)

    @staticmethod
    def bounds_match(norm1: Tuple[float, float, float, float], 
                     norm2: Tuple[float, float, float, float], 
                     tolerance: float = 0.02) -> bool:
        """Check if two normalized bounds match within tolerance"""
        x1_diff = abs(norm1[0] - norm2[0])
        y1_diff = abs(norm1[1] - norm2[1])
        x2_diff = abs(norm1[2] - norm2[2])
        y2_diff = abs(norm1[3] - norm2[3])
        return x1_diff <= tolerance and y1_diff <= tolerance and x2_diff <= tolerance and y2_diff <= tolerance




class U2Device:
    def __init__(self, device_id: str = None, apk_dir: Optional[Path] = None, adb_path: str = "adb"):
        self._device_id: Optional[str] = device_id
        self._d: Optional[u2.Device] = None
        self.apk_dir = apk_dir
        self.adb_path = adb_path

    @property
    def device_id(self) -> Optional[str]:
        return self._device_id

    @property
    def d(self) -> u2.Device:
        if self._d is None:
            try:
                # 尝试连接指定设备
                if self._device_id:
                    self._d = u2.connect(self._device_id)
                else:
                    # 如果没有指定设备ID，连接第一个可用设备
                    self._d = u2.connect()
            except Exception as e:
                # 如果是UiAutomator2服务冲突，等待后重试
                error_msg = str(e)
                if "already registered" in error_msg or "UiAutomationService" in error_msg:
                    print("⚠️ UiAutomator2服务冲突，等待2秒后重试...")
                    import time
                    time.sleep(2)
                    try:
                        if self._device_id:
                            self._d = u2.connect(self._device_id)
                        else:
                            self._d = u2.connect()
                        print("✅ 重试连接成功")
                    except Exception as retry_error:
                        print(f"❌ 重试连接失败: {retry_error}")
                        raise
                else:
                    print(f"❌ 设备连接失败: {e}")
                    raise
        return self._d

    @property
    def info(self) -> dict:
        return self.d.info

    @property
    def xml(self) -> str:
        return self.d.dump_hierarchy()

    def dump_hierarchy(self) -> str:
        return self.d.dump_hierarchy()

    def check_element_exists(self, selector: str, timeout: float = 1.0) -> bool:
        """检查元素是否存在（无需下载XML）"""
        try:
            # 解析选择器并转换为uiautomator2的关键字参数调用方式
            if selector.startswith('text("') and selector.endswith('")'):
                # 处理 text("value") 格式
                text_value = selector[6:-2]  # 提取 "value" 部分
                return self.d(text=text_value).exists(timeout=timeout)
            elif selector.startswith('resourceId("') and selector.endswith('")'):
                # 处理 resourceId("value") 格式
                resource_id = selector[12:-2]  # 提取 "value" 部分
                return self.d(resourceId=resource_id).exists(timeout=timeout)
            elif selector.startswith('description("') and selector.endswith('")'):
                # 处理 description("value") 格式
                desc_value = selector[12:-2]  # 提取 "value" 部分
                return self.d(description=desc_value).exists(timeout=timeout)
            else:
                # 其他选择器格式，尝试直接使用
                logger.warning(f"不支持的选择器格式: {selector}")
                return False
        except Exception as e:
            logger.warning(f"检查元素失败 {selector}: {e}")
            return False

    def get_element_count(self, selector: str) -> int:
        """获取匹配元素的数量"""
        try:
            # 解析选择器并转换为uiautomator2的关键字参数调用方式
            if selector.startswith('text("') and selector.endswith('")'):
                # 处理 text("value") 格式
                text_value = selector[6:-2]  # 提取 "value" 部分
                return len(self.d(text=text_value))
            elif selector.startswith('resourceId("') and selector.endswith('")'):
                # 处理 resourceId("value") 格式
                resource_id = selector[12:-2]  # 提取 "value" 部分
                return len(self.d(resourceId=resource_id))
            elif selector.startswith('description("') and selector.endswith('")'):
                # 处理 description("value") 格式
                desc_value = selector[12:-2]  # 提取 "value" 部分
                return len(self.d(description=desc_value))
            else:
                # 其他选择器格式，尝试直接使用
                logger.warning(f"不支持的选择器格式: {selector}")
                return 0
        except Exception as e:
            logger.warning(f"获取元素数量失败 {selector}: {e}")
            return 0

    def save_current_page(self, page_id: str, prefix: str = "live") -> str:
        live_xml = self.dump_hierarchy()
        timestamp = int(time.time())
        filename = f"{prefix}_{page_id}_{timestamp}.xml"
        if self.apk_dir:
            filepath = Path(self.apk_dir).parent.parent / "debug_xmls" / filename
        else:
            filepath = Path(__file__).parent.parent / "debug_xmls" / filename
        filepath.parent.mkdir(exist_ok=True)
        filepath.write_text(live_xml, encoding="utf-8")
        print(f"💾 已保存当前页面: {filepath}")
        return str(filepath)

    def set_apk_dir(self, apk_dir: Path):
        self.apk_dir = apk_dir

    def click(self, x: int, y: int) -> bool:
        try:
            self.d.click(x, y)
            return True
        except Exception:
            return False

    def long_click(self, x: int, y: int, duration: float = 0.5) -> bool:
        try:
            self.d.long_click(x, y, duration)
            return True
        except Exception:
            return False

    def swipe(self, x1: int, y1: int, x2: int, y2: int, duration: float = 0.5) -> bool:
        try:
            self.d.swipe(x1, y1, x2, y2, duration)
            return True
        except Exception:
            return False

    def send_keys(self, text: str) -> bool:
        try:
            self.d.send_keys(text)
            return True
        except Exception:
            return False

    def clear_text(self) -> bool:
        try:
            self.d.clear_text()
            return True
        except Exception:
            return False

    def press(self, key: int) -> bool:
        try:
            self.d.press(key)
            return True
        except Exception:
            return False

    def press_back(self) -> bool:
        return self.press(4)

    def press_home(self) -> bool:
        return self.press(3)

    def press_menu(self) -> bool:
        return self.press(82)

    def exists(self, **kwargs) -> bool:
        return self.d.exists(**kwargs)

    def __getattr__(self, name):
        return getattr(self.d, name)

    def run_command(self, command: List[str], timeout: int = 60) -> subprocess.CompletedProcess:
        full_command = [self.adb_path] + command
        try:
            result = subprocess.run(
                full_command,
                capture_output=True,
                text=True,
                encoding='utf-8',
                timeout=timeout,
            )
            return result
        except subprocess.TimeoutExpired:
            logger.error(f"ADB command timed out: {command}")
            raise

    def get_devices(self) -> List[str]:
        result = self.run_command(["devices", "-l"])
        devices = []
        for line in result.stdout.strip().split("\n")[1:]:
            if line.strip():
                parts = line.split()
                if len(parts) > 0:
                    devices.append(parts[0])
        return devices

    def take_screenshot(self, device_id: str, local_path: str) -> bool:
        logger.info(f"Taking screenshot for device: {device_id}")
        try:
            self.run_command(["-s", device_id, "shell", "screencap", "-p", "/sdcard/screenshot.png"])
            self.run_command(["-s", device_id, "pull", "/sdcard/screenshot.png", local_path])
            logger.info(f"Screenshot saved to: {local_path}")
            return True
        except Exception as e:
            logger.error(f"Failed to take screenshot: {e}")
            return False

    def tap(self, device_id: str, x: int, y: int) -> bool:
        logger.info(f"Tapping at ({x}, {y}) on device: {device_id}")
        try:
            result = self.run_command(["-s", device_id, "shell", "input", "tap", str(x), str(y)])
            return result.returncode == 0
        except Exception as e:
            logger.error(f"Failed to tap: {e}")
            return False

    def swipe(self, device_id: str, x1: int, y1: int, x2: int, y2: int, duration: int = 500) -> bool:
        logger.info(f"Swiping from ({x1}, {y1}) to ({x2}, {y2}) on device: {device_id}")
        try:
            result = self.run_command([
                "-s", device_id, "shell", "input", "swipe",
                str(x1), str(y1), str(x2), str(y2), str(duration)
            ])
            return result.returncode == 0
        except Exception as e:
            logger.error(f"Failed to swipe: {e}")
            return False

    def input_text(self, device_id: str, text: str) -> bool:
        logger.info(f"Inputting text on device: {device_id}")
        try:
            # 转义特殊字符
            escaped_text = text.replace(' ', '%s').replace('"', '\\"')
            result = self.run_command(["-s", device_id, "shell", "input", "text", escaped_text])
            return result.returncode == 0
        except Exception as e:
            logger.error(f"Failed to input text: {e}")
            return False

    def key_event(self, device_id: str, key_code: int) -> bool:
        logger.info(f"Sending key event {key_code} to device: {device_id}")
        try:
            result = self.run_command(["-s", device_id, "shell", "input", "keyevent", str(key_code)])
            return result.returncode == 0
        except Exception as e:
            logger.error(f"Failed to send key event: {e}")
            return False

    def dump_page_xml(self, device_id: str, local_path: str = None) -> str:
        """
        导出页面XML结构
        
        Args:
            device_id: 设备ID
            local_path: 可选参数，如果提供则保存到文件并返回文件路径
        
        Returns:
            如果local_path为None，返回XML字符串；否则返回文件路径或空字符串
        """
        logger.info(f"Dumping page UI hierarchy for device: {device_id}")
        try:
            # 执行uiautomator dump命令
            dump_result = self.run_command(["-s", device_id, "shell", "uiautomator", "dump"])
            if dump_result.returncode != 0:
                logger.error(f"uiautomator dump failed: {dump_result.stderr}")
                return "" if local_path is None else ""
            
            # 如果指定了本地文件路径，使用pull方式
            if local_path:
                pull_result = self.run_command(["-s", device_id, "pull", "/sdcard/window_dump.xml", local_path])
                if pull_result.returncode != 0:
                    logger.error(f"pull failed: {pull_result.stderr}")
                    return ""
                logger.info(f"Page dumped to: {local_path}")
                return local_path
            
            # 否则使用cat方式读取内容
            result = self.run_command(["-s", device_id, "shell", "cat", "/sdcard/window_dump.xml"])
            
            # 清理临时文件
            self.run_command(["-s", device_id, "shell", "rm", "/sdcard/window_dump.xml"])

            if result.returncode == 0 and result.stdout:
                # 尝试多种编码方式处理特殊字符
                xml_content = result.stdout.strip()
                
                # 改进的Unicode字符处理逻辑
                try:
                    # 首先尝试直接使用UTF-8解码
                    xml_content = xml_content.encode('utf-8').decode('utf-8')
                except UnicodeDecodeError:
                    # 如果UTF-8失败，尝试使用更宽松的处理方式
                    try:
                        # 使用errors='replace'来保留特殊字符
                        xml_content = xml_content.encode('utf-8', errors='replace').decode('utf-8')
                    except:
                        # 如果所有方法都失败，使用原始内容
                        pass
                
                return xml_content

            return ""

        except Exception as e:
            logger.error(f"Failed to dump page XML: {e}")
            return "" if local_path is None else ""
