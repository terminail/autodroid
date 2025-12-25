"""ADB-based device utilities for Android automation.

This module provides basic ADB operations that Appium doesn't directly expose:
- Device discovery (adb devices)
- Device information retrieval (getprop, wm, settings)
- Debug status checking
- App installation check

For UI automation, use Appium WebDriver API instead.
"""

import subprocess
import re
import logging
from typing import Optional, List, Dict, Any


logger = logging.getLogger(__name__)


def _run_adb_command(args: List[str], timeout: int = 10) -> subprocess.CompletedProcess:
    """Run an ADB command.
    
    Args:
        args: Command arguments
        timeout: Command timeout in seconds
        
    Returns:
        CompletedProcess result
    """
    return subprocess.run(
        ["adb"] + args,
        capture_output=True,
        text=True,
        timeout=timeout
    )


def _run_shell_command(device_id: Optional[str], args: List[str], timeout: int = 10) -> subprocess.CompletedProcess:
    """Run an ADB shell command.
    
    Args:
        device_id: Optional device serial number
        args: Shell command arguments
        timeout: Command timeout in seconds
        
    Returns:
        CompletedProcess result
    """
    cmd = ["adb"]
    if device_id:
        cmd.extend(["-s", device_id])
    cmd.extend(["shell"] + args)
    
    return subprocess.run(
        cmd,
        capture_output=True,
        text=True,
        timeout=timeout
    )


def list_devices() -> List[str]:
    """List all connected Android devices.
    
    Returns:
        List of device serial numbers
    """
    result = _run_adb_command(["devices"])
    
    devices = []
    for line in result.stdout.strip().split('\n')[1:]:
        if line.strip() and '\t' in line:
            device_id, status = line.strip().split('\t')
            if status == 'device':
                devices.append(device_id)
    
    return devices


def quick_connect(device_id: Optional[str] = None) -> 'ADBDevice':
    """Quick connect to an Android device.
    
    Args:
        device_id: Optional device ID for multi-device setups
        
    Returns:
        ADBDevice instance
    """
    return ADBDevice(device_id=device_id)


class ADBDevice:
    """Android device utilities using ADB commands."""
    
    def __init__(self, device_id: Optional[str] = None):
        """Initialize ADB device connection.
        
        Args:
            device_id: Optional device ID for multi-device setups
        """
        self.device_id = device_id
        self._connected = False
        self._check_connection()
    
    def _check_connection(self) -> None:
        """Check if ADB is available and device is connected."""
        try:
            result = _run_adb_command(["version"])
            if result.returncode != 0:
                raise RuntimeError("ADB not available")
            
            devices = list_devices()
            if not devices:
                raise RuntimeError("No Android devices found")
            
            if self.device_id and self.device_id not in devices:
                raise RuntimeError(f"Device {self.device_id} not found")
            
            if not self.device_id and len(devices) == 1:
                self.device_id = devices[0]
            
            self._connected = True
            
        except (subprocess.TimeoutExpired, subprocess.SubprocessError, FileNotFoundError) as e:
            raise RuntimeError(f"ADB connection failed: {e}")
    
    def is_connected(self) -> bool:
        """Check if device is connected."""
        return self._connected
    
    def is_usb_debug_enabled(self) -> bool:
        """Check if USB debugging is enabled on the device."""
        try:
            result = _run_shell_command(
                self.device_id,
                ["settings", "get", "global", "adb_enabled"]
            )
            if result.returncode == 0:
                return result.stdout.strip() == "1"
            
            result = _run_shell_command(self.device_id, ["echo", "test"])
            return result.returncode == 0 and "test" in result.stdout
        except (subprocess.TimeoutExpired, subprocess.SubprocessError):
            return False
    
    def is_wifi_debug_enabled(self) -> bool:
        """Check if WiFi debugging is enabled on the device."""
        try:
            result = _run_shell_command(
                self.device_id,
                ["settings", "get", "global", "adb_wifi_enabled"]
            )
            if result.returncode == 0:
                return result.stdout.strip() == "1"
            
            result = _run_shell_command(self.device_id, ["netstat", "-an"])
            if result.returncode == 0:
                return "5555" in result.stdout and "LISTEN" in result.stdout
            
            return False
        except (subprocess.TimeoutExpired, subprocess.SubprocessError):
            return False
    
    def is_app_installed(self, package_name: str) -> bool:
        """Check if an app is installed on the device.
        
        Args:
            package_name: Android package name (e.g., 'com.tdx.androidCCZQ')
            
        Returns:
            True if app is installed, False otherwise
        """
        result = _run_shell_command(
            self.device_id,
            ["pm", "path", package_name]
        )
        
        return result.returncode == 0 and result.stdout.strip() != ""
    
    def get_current_package(self) -> str:
        """Get the currently focused package name.
        
        Returns:
            Package name or empty string
        """
        result = _run_shell_command(
            self.device_id,
            ["dumpsys", "window", "windows"]
        )
        
        for line in result.stdout.split("\n"):
            if "mCurrentFocus" in line:
                if "}" in line:
                    focus = line.split("}")[0].split(" ")[-1]
                    if "/" in focus:
                        return focus.split("/")[0]
        
        return ""
    
    def get_device_info(self) -> Dict[str, Any]:
        """Get device information.
        
        Returns:
            Device info dictionary
        """
        info = {}
        
        for prop, key in [
            ("ro.product.model", "model"),
            ("ro.product.manufacturer", "manufacturer"),
            ("ro.product.brand", "brand"),
            ("ro.product.device", "device"),
            ("ro.product.name", "product"),
            ("ro.build.version.release", "android_version"),
            ("ro.build.version.sdk", "api_level"),
        ]:
            result = _run_shell_command(self.device_id, ["getprop", prop])
            if result.returncode == 0 and result.stdout.strip():
                value = result.stdout.strip()
                info[key] = int(value) if key == "api_level" else value
        
        result = _run_shell_command(self.device_id, ["wm", "size"])
        if result.returncode == 0:
            size_output = result.stdout.strip()
            if "Physical size:" in size_output:
                size = size_output.split(": ")[1]
                try:
                    width, height = size.split("x")
                    info["screen_width"] = int(width)
                    info["screen_height"] = int(height)
                except (ValueError, IndexError):
                    pass
        
        result = _run_shell_command(self.device_id, ["ip", "addr", "show", "wlan0"])
        if result.returncode == 0:
            for line in result.stdout.split("\n"):
                if "inet " in line:
                    match = re.search(r'inet (\d+\.\d+\.\d+\.\d+)', line)
                    if match:
                        info["ip"] = match.group(1)
                        break
        
        info["platform"] = "Android"
        info["device_id"] = self.device_id
        
        for setting in ["settings", "get", "secure", "bluetooth_name"]:
            result = _run_shell_command(self.device_id, ["settings", "get", "secure", "bluetooth_name"])
            if result.returncode == 0 and result.stdout.strip():
                info["name"] = result.stdout.strip()
                break
        else:
            result = _run_shell_command(self.device_id, ["settings", "get", "global", "device_name"])
            if result.returncode == 0 and result.stdout.strip():
                info["name"] = result.stdout.strip()
            else:
                info["name"] = info.get("model", "Unknown Device")
        
        return info
    
    def disconnect(self) -> None:
        """Disconnect from the device."""
        self._connected = False
