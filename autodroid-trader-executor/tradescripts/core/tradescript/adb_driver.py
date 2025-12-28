import subprocess
import time
import logging
from typing import Optional, Dict, Any, List
from dataclasses import dataclass
from pathlib import Path

logger = logging.getLogger(__name__)


@dataclass
class DeviceInfo:
    device_id: str
    platform_version: str
    device_name: str
    manufacturer: str
    model: str


class ADBManager:
    def __init__(self, adb_path: Optional[str] = None):
        self.adb_path = adb_path or self._find_adb()

    def _find_adb(self) -> str:
        try:
            result = subprocess.run(
                ["where", "adb"],
                capture_output=True,
                text=True,
                timeout=10,
            )
            if result.returncode == 0:
                adb_paths = result.stdout.strip().split("\n")
                if adb_paths:
                    return adb_paths[0]
        except Exception:
            pass

        common_paths = [
            "adb",
            "${ANDROID_HOME}/platform-tools/adb",
            "${ANDROID_SDK_ROOT}/platform-tools/adb",
            "C:\\Android\\sdk\\platform-tools\\adb.exe",
            "C:\\Program Files\\Android\\android-sdk\\platform-tools\\adb.exe",
        ]

        for path in common_paths:
            path = Path(path.expandvars())
            if path.exists():
                return str(path)

        return "adb"

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
                if len(parts) >= 2 and parts[1] == "device":
                    devices.append(parts[0])
        return devices

    def get_device_info(self, device_id: str) -> Optional[DeviceInfo]:
        result = self.run_command(["-s", device_id, "shell", "getprop", "ro.build.version.release"])
        platform_version = result.stdout.strip()

        result = self.run_command(["-s", device_id, "shell", "getprop", "ro.product.manufacturer"])
        manufacturer = result.stdout.strip()

        result = self.run_command(["-s", device_id, "shell", "getprop", "ro.product.model"])
        model = result.stdout.strip()

        result = self.run_command(["-s", device_id, "shell", "getprop", "ro.product.name"])
        device_name = result.stdout.strip()

        return DeviceInfo(
            device_id=device_id,
            platform_version=platform_version,
            device_name=device_name,
            manufacturer=manufacturer,
            model=model,
        )

    def start_app(
        self,
        device_id: str,
        app_package: str,
        app_activity: Optional[str] = None,
        wait: bool = True,
        timeout: int = 30,
    ) -> bool:
        logger.info(f"Starting app: {app_package}")

        if app_activity:
            result = self.run_command(
                [
                    "-s",
                    device_id,
                    "shell",
                    "am",
                    "start",
                    "-n",
                    f"{app_package}/{app_activity}",
                ]
            )
        else:
            result = self.run_command(
                ["-s", device_id, "shell", "am", "start", "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER"]
            )

        if result.returncode != 0:
            logger.error(f"Failed to start app: {result.stderr}")
            return False

        if wait:
            time.sleep(timeout)

        logger.info(f"App started successfully: {app_package}")
        return True

    def stop_app(self, device_id: str, app_package: str) -> bool:
        logger.info(f"Stopping app: {app_package}")
        result = self.run_command(
            ["-s", device_id, "shell", "am", "force-stop", app_package]
        )
        if result.returncode != 0:
            logger.error(f"Failed to stop app: {result.stderr}")
            return False

        logger.info(f"App stopped successfully: {app_package}")
        return True

    def cold_start_app(
        self,
        device_id: str,
        app_package: str,
        app_activity: Optional[str] = None,
        clear_data: bool = False,
        wait_timeout: int = 30,
    ) -> bool:
        logger.info(f"Cold starting app: {app_package}")

        if clear_data:
            self.run_command(["-s", device_id, "shell", "pm", "clear", app_package])

        self.stop_app(device_id, app_package)

        time.sleep(1)

        success = self.start_app(device_id, app_package, app_activity, wait=True, timeout=wait_timeout)

        return success

    def install_app(self, device_id: str, app_path: str) -> bool:
        logger.info(f"Installing app: {app_path}")
        result = self.run_command(["-s", device_id, "install", "-r", app_path])
        if result.returncode != 0:
            logger.error(f"Failed to install app: {result.stderr}")
            return False

        logger.info(f"App installed successfully: {app_path}")
        return True

    def uninstall_app(self, device_id: str, app_package: str) -> bool:
        logger.info(f"Uninstalling app: {app_package}")
        result = self.run_command(["-s", device_id, "uninstall", app_package])
        if result.returncode != 0:
            logger.error(f"Failed to uninstall app: {result.stderr}")
            return False

        logger.info(f"App uninstalled successfully: {app_package}")
        return True

    def get_app_version(self, device_id: str, app_package: str) -> Optional[str]:
        result = self.run_command(
            ["-s", device_id, "shell", "dumpsys", "package", app_package]
        )
        for line in result.stdout.split("\n"):
            if "versionName" in line:
                try:
                    return line.split("=")[1].strip()
                except IndexError:
                    pass
        return None

    def press_key(self, device_id: str, key_code: int) -> bool:
        result = self.run_command(["-s", device_id, "shell", "input", "keyevent", str(key_code)])
        return result.returncode == 0

    def tap(self, device_id: str, x: int, y: int) -> bool:
        result = self.run_command(["-s", device_id, "shell", "input", "tap", str(x), str(y)])
        return result.returncode == 0

    def swipe(
        self, device_id: str, x1: int, y1: int, x2: int, y2: int, duration: int = 300
    ) -> bool:
        result = self.run_command(
            ["-s", device_id, "shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(duration)]
        )
        return result.returncode == 0

    def input_text(self, device_id: str, text: str) -> bool:
        escaped_text = text.replace(" ", "%s").replace("'", "")
        result = self.run_command(["-s", device_id, "shell", "input", "text", escaped_text])
        return result.returncode == 0

    def get_screen_resolution(self, device_id: str) -> Optional[tuple]:
        result = self.run_command(["-s", device_id, "shell", "wm", "size"])
        for line in result.stdout.split("\n"):
            if "Physical size:" in line:
                try:
                    size = line.split(":")[1].strip()
                    width, height = size.split("x")
                    return int(width), int(height)
                except (IndexError, ValueError):
                    pass
        return None

    def take_screenshot(self, device_id: str, save_path: str) -> bool:
        result = self.run_command(["-s", device_id, "shell", "screencap", "-p", "/sdcard/screen.png"])
        if result.returncode != 0:
            return False

        result = self.run_command(["-s", device_id, "pull", "/sdcard/screen.png", save_path])
        if result.returncode != 0:
            return False

        self.run_command(["-s", device_id, "shell", "rm", "/sdcard/screen.png"])
        return True

    def dump_page(self, device_id: str, local_path: str = "temp_dump.xml") -> bool:
        logger.info(f"Dumping page UI hierarchy for device: {device_id}")
        try:
            self.run_command(["-s", device_id, "shell", "uiautomator", "dump"])
            self.run_command(["-s", device_id, "pull", "/sdcard/window_dump.xml", local_path])
            logger.info(f"Page dumped to: {local_path}")
            return True
        except Exception as e:
            logger.error(f"Failed to dump page: {e}")
            return False

    def dump_page_xml(self, device_id: str) -> str:
        logger.info(f"Dumping page UI hierarchy for device: {device_id}")
        try:
            self.run_command(["-s", device_id, "shell", "uiautomator", "dump"])

            result = self.run_command(["-s", device_id, "shell", "cat", "/sdcard/window_dump.xml"])

            self.run_command(["-s", device_id, "shell", "rm", "/sdcard/window_dump.xml"])

            if result.returncode == 0 and result.stdout:
                return result.stdout.strip()

            return ""

        except Exception as e:
            logger.error(f"Failed to dump page XML: {e}")
            return ""
