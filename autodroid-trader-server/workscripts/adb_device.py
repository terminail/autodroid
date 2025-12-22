"""ADB-based device connection module for Android automation."""

import subprocess
import time
from typing import Optional, List, Dict, Any


class ADBDevice:
    """Android device controller using ADB commands."""
    
    def __init__(self, device_id: Optional[str] = None):
        """Initialize ADB device connection.
        
        Args:
            device_id: Optional device ID for multi-device setups
        """
        self.device_id = device_id
        self._connected = False
        self._check_adb_available()
        
    def _get_adb_prefix(self) -> List[str]:
        """Get ADB command prefix with optional device specifier."""
        if self.device_id:
            return ["adb", "-s", self.device_id]
        return ["adb"]
    
    def _check_adb_available(self) -> None:
        """Check if ADB is available and device is connected."""
        try:
            # Check if ADB is available
            result = subprocess.run(
                ["adb", "version"], 
                capture_output=True, 
                text=True, 
                timeout=5
            )
            if result.returncode != 0:
                raise RuntimeError("ADB not available")
            
            # Check device connection
            result = subprocess.run(
                ["adb", "devices"], 
                capture_output=True, 
                text=True, 
                timeout=5
            )
            
            devices = []
            for line in result.stdout.strip().split('\n')[1:]:  # Skip header
                if line.strip() and '\t' in line:
                    device_id, status = line.strip().split('\t')
                    if status == 'device':
                        devices.append(device_id)
            
            if not devices:
                raise RuntimeError("No Android devices found")
            
            if self.device_id and self.device_id not in devices:
                raise RuntimeError(f"Device {self.device_id} not found")
            
            if not self.device_id and len(devices) == 1:
                self.device_id = devices[0]
            
            self._connected = True
            
        except (subprocess.TimeoutExpired, subprocess.SubprocessError, FileNotFoundError) as e:
            raise RuntimeError(f"ADB connection failed: {e}")
    
    def tap(self, x: int, y: int, delay: float = 1.0) -> None:
        """Tap at the specified coordinates.
        
        Args:
            x: X coordinate
            y: Y coordinate  
            delay: Delay in seconds after tap
        """
        subprocess.run(
            self._get_adb_prefix() + ["shell", "input", "tap", str(x), str(y)], 
            capture_output=True
        )
        time.sleep(delay)
    
    def swipe(self, start_x: int, start_y: int, end_x: int, end_y: int, 
              duration_ms: Optional[int] = None, delay: float = 1.0) -> None:
        """Swipe from start to end coordinates.
        
        Args:
            start_x: Starting X coordinate
            start_y: Starting Y coordinate
            end_x: Ending X coordinate
            end_y: Ending Y coordinate
            duration_ms: Duration of swipe in milliseconds
            delay: Delay in seconds after swipe
        """
        if duration_ms is None:
            # Calculate duration based on distance
            dist_sq = (start_x - end_x) ** 2 + (start_y - end_y) ** 2
            duration_ms = int(dist_sq / 1000)
            duration_ms = max(500, min(duration_ms, 2000))  # Clamp between 500-2000ms
        
        subprocess.run(
            self._get_adb_prefix() + [
                "shell", "input", "swipe",
                str(start_x), str(start_y), str(end_x), str(end_y), str(duration_ms)
            ],
            capture_output=True
        )
        time.sleep(delay)
    
    def type_text(self, text: str, delay: float = 1.0) -> None:
        """Type text on the device.
        
        Args:
            text: Text to type
            delay: Delay in seconds after typing
        """
        # Replace spaces with %s for ADB input
        text = text.replace(' ', '%s')
        subprocess.run(
            self._get_adb_prefix() + ["shell", "input", "text", text],
            capture_output=True
        )
        time.sleep(delay)
    
    def press_key(self, keycode: str, delay: float = 1.0) -> None:
        """Press a key on the device.
        
        Args:
            keycode: Android keycode (e.g., '4' for back, 'KEYCODE_HOME' for home)
            delay: Delay in seconds after pressing key
        """
        subprocess.run(
            self._get_adb_prefix() + ["shell", "input", "keyevent", keycode],
            capture_output=True
        )
        time.sleep(delay)
    
    def back(self, delay: float = 1.0) -> None:
        """Press the back button."""
        self.press_key("4", delay)
    
    def home(self, delay: float = 1.0) -> None:
        """Press the home button."""
        self.press_key("KEYCODE_HOME", delay)
    
    def get_current_app(self) -> str:
        """Get the currently focused app name.
        
        Returns:
            The app name if recognized, otherwise "System Home"
        """
        result = subprocess.run(
            self._get_adb_prefix() + ["shell", "dumpsys", "window"],
            capture_output=True, text=True
        )
        
        # Parse window focus info
        for line in result.stdout.split("\n"):
            if "mCurrentFocus" in line or "mFocusedApp" in line:
                # Extract package name
                if "}" in line:
                    package = line.split("}")[0].split(" ")[-1]
                    return package
        
        return "System Home"
    
    def launch_app(self, package_name: str, delay: float = 2.0) -> bool:
        """Launch an app by package name.
        
        Args:
            package_name: Android package name (e.g., 'com.android.chrome')
            delay: Delay in seconds after launching
            
        Returns:
            True if app was launched, False otherwise
        """
        result = subprocess.run(
            self._get_adb_prefix() + [
                "shell", "monkey",
                "-p", package_name,
                "-c", "android.intent.category.LAUNCHER",
                "1"
            ],
            capture_output=True
        )
        
        time.sleep(delay)
        return result.returncode == 0
    
    def get_screenshot(self, filename: str = "screenshot.png") -> bool:
        """Take a screenshot of the device.
        
        Args:
            filename: Output filename
            
        Returns:
            True if screenshot was taken successfully
        """
        # Take screenshot on device
        result = subprocess.run(
            self._get_adb_prefix() + ["shell", "screencap", "-p", "/sdcard/screenshot.png"],
            capture_output=True
        )
        
        if result.returncode != 0:
            return False
        
        # Pull screenshot to local
        result = subprocess.run(
            self._get_adb_prefix() + ["pull", "/sdcard/screenshot.png", filename],
            capture_output=True
        )
        
        return result.returncode == 0
    
    def is_connected(self) -> bool:
        """Check if device is connected."""
        return self._connected
    
    def is_usb_debug_enabled(self) -> bool:
        """Check if USB debugging is enabled on the device."""
        try:
            # Check if USB debugging is enabled by checking if we can run adb commands
            result = subprocess.run(
                self._get_adb_prefix() + ["shell", "settings", "get", "global", "adb_enabled"],
                capture_output=True, text=True, timeout=5
            )
            if result.returncode == 0:
                # adb_enabled returns 1 if USB debugging is enabled
                return result.stdout.strip() == "1"
            
            # Fallback: try to run a simple command to check if debugging is working
            result = subprocess.run(
                self._get_adb_prefix() + ["shell", "echo", "test"],
                capture_output=True, text=True, timeout=5
            )
            return result.returncode == 0 and "test" in result.stdout
        except (subprocess.TimeoutExpired, subprocess.SubprocessError):
            return False
    
    def is_wifi_debug_enabled(self) -> bool:
        """Check if WiFi debugging is enabled on the device."""
        try:
            # Check if wireless debugging is enabled
            result = subprocess.run(
                self._get_adb_prefix() + ["shell", "settings", "get", "global", "adb_wifi_enabled"],
                capture_output=True, text=True, timeout=5
            )
            if result.returncode == 0:
                # adb_wifi_enabled returns 1 if WiFi debugging is enabled
                return result.stdout.strip() == "1"
            
            # Alternative method: check if adbd is listening on a network port
            result = subprocess.run(
                self._get_adb_prefix() + ["shell", "netstat", "-an"],
                capture_output=True, text=True, timeout=5
            )
            if result.returncode == 0:
                # Look for adbd listening on port 5555 (default ADB over WiFi port)
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
        result = subprocess.run(
            self._get_adb_prefix() + ["shell", "pm", "path", package_name],
            capture_output=True, text=True
        )
        
        # If the app is installed, pm path will return the package path
        # If not installed, it will return empty
        return result.returncode == 0 and result.stdout.strip() != ""
    
    def get_device_info(self) -> Dict[str, Any]:
        """Get device information.
        
        Returns:
            Device info dictionary
        """
        info = {}
        
        # Get device model
        result = subprocess.run(
            self._get_adb_prefix() + ["shell", "getprop", "ro.product.model"],
            capture_output=True, text=True
        )
        if result.returncode == 0:
            info["model"] = result.stdout.strip()
        
        # Get manufacturer
        result = subprocess.run(
            self._get_adb_prefix() + ["shell", "getprop", "ro.product.manufacturer"],
            capture_output=True, text=True
        )
        if result.returncode == 0:
            info["manufacturer"] = result.stdout.strip()
        
        # Get brand
        result = subprocess.run(
            self._get_adb_prefix() + ["shell", "getprop", "ro.product.brand"],
            capture_output=True, text=True
        )
        if result.returncode == 0:
            info["brand"] = result.stdout.strip()
        
        # Get device
        result = subprocess.run(
            self._get_adb_prefix() + ["shell", "getprop", "ro.product.device"],
            capture_output=True, text=True
        )
        if result.returncode == 0:
            info["device"] = result.stdout.strip()
        
        # Get product
        result = subprocess.run(
            self._get_adb_prefix() + ["shell", "getprop", "ro.product.name"],
            capture_output=True, text=True
        )
        if result.returncode == 0:
            info["product"] = result.stdout.strip()
        
        # Get Android version
        result = subprocess.run(
            self._get_adb_prefix() + ["shell", "getprop", "ro.build.version.release"],
            capture_output=True, text=True
        )
        if result.returncode == 0:
            info["android_version"] = result.stdout.strip()
        
        # Get API level
        result = subprocess.run(
            self._get_adb_prefix() + ["shell", "getprop", "ro.build.version.sdk"],
            capture_output=True, text=True
        )
        if result.returncode == 0:
            try:
                info["api_level"] = int(result.stdout.strip())
            except ValueError:
                pass
        
        # Get screen dimensions
        result = subprocess.run(
            self._get_adb_prefix() + ["shell", "wm", "size"],
            capture_output=True, text=True
        )
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
        
        # Get IP address
        result = subprocess.run(
            self._get_adb_prefix() + ["shell", "ip", "addr", "show", "wlan0"],
            capture_output=True, text=True
        )
        if result.returncode == 0:
            for line in result.stdout.split("\n"):
                if "inet " in line:
                    parts = line.strip().split()
                    for i, part in enumerate(parts):
                        if part == "inet" and i + 1 < len(parts):
                            ip_address = parts[i + 1].split("/")[0]
                            info["ip"] = ip_address
                            break
        
        # Set platform
        info["platform"] = "Android"
        
        # Get device ID
        info["device_id"] = self.device_id
        
        return info


def list_devices() -> List[str]:
    """List all connected Android devices.
    
    Returns:
        List of device IDs
    """
    try:
        result = subprocess.run(
            ["adb", "devices"], 
            capture_output=True, 
            text=True, 
            timeout=5
        )
        
        devices = []
        for line in result.stdout.strip().split('\n')[1:]:  # Skip header
            if line.strip() and '\t' in line:
                device_id, status = line.strip().split('\t')
                if status == 'device':
                    devices.append(device_id)
        
        return devices
    
    except (subprocess.TimeoutExpired, subprocess.SubprocessError):
        return []


def quick_connect(device_id: Optional[str] = None) -> ADBDevice:
    """Quickly connect to an Android device.
    
    Args:
        device_id: Optional device ID, auto-select if only one device
        
    Returns:
        ADBDevice instance
        
    Raises:
        RuntimeError: If connection fails
    """
    return ADBDevice(device_id)