"""
Server information and utility API endpoints for Autodroid system.
Handles health checks, server info, configuration, WiFi scanning, and QR code generation.
"""

from fastapi import APIRouter, HTTPException
from fastapi.responses import Response
import os
import yaml
import socket
import platform
import subprocess
import re
import asyncio
import json
import base64
import io
from datetime import datetime, timedelta
import qrcode

from .models import WiFiList

# Initialize router
router = APIRouter(prefix="/api", tags=["server"])

# Load configuration
def load_config():
    """Load configuration from config.yaml"""
    config_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "config.yaml")
    try:
        with open(config_path, 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        return config
    except FileNotFoundError:
        return {}
    except Exception as e:
        print(f"Error loading config: {e}")
        return {}

config = load_config()

@router.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": asyncio.get_event_loop().time(),
        "services": {
            "device_manager": "running",
            "scheduler": "running"
        }
    }

@router.get("/server")
async def get_server_info():
    """Get full server information"""
    # 获取服务器IP地址
    hostname = socket.gethostname()
    ip_address = socket.gethostbyname(hostname)
    
    # 从配置文件获取端口
    server_config = config.get('server', {})
    backend_config = server_config.get('backend', {})
    fastapi_port = backend_config.get('port', 8004)
    
    return {
        "name": "Autodroid Server",
        "hostname": hostname,
        "ipAddress": ip_address,
        "platform": platform.platform(),
        "apiEndpoint": f"http://{ip_address}:{fastapi_port}/api",
        "services": {
            "device_manager": "running",
            "scheduler": "running"
        },
        "capabilities": {
            "device_registration": True,
            "test_scheduling": True,
            "event_triggering": True
        }
    }

@router.get("/config")
async def get_config():
    """Get the unified configuration for frontend and other services"""
    return config

@router.get("/server/wifis", response_model=WiFiList)
async def get_server_wifis():
    """Get WiFi networks available to the server and suggest which one autodroid-app should use"""
    
    def get_windows_wifis():
        """Get WiFi networks on Windows using netsh"""
        try:
            result = subprocess.run(["netsh", "wlan", "show", "networks"], 
                                  capture_output=True, text=False, check=True)
            # Decode with proper encoding for Chinese Windows
            output = result.stdout.decode('utf-8', errors='ignore')
            
            wifis = []
            current_wifi = None
            
            for line in output.splitlines():
                line = line.strip()
                if line.startswith("SSID") and ":" in line:
                    ssid = line.split(":", 1)[1].strip()
                    if ssid:
                        if current_wifi:
                            wifis.append(current_wifi)
                        current_wifi = {
                            "name": ssid,
                            "signal_strength": 0,
                            "security": "Unknown",
                            "ip_address": None
                        }
                elif line.startswith("Signal") and current_wifi:
                    signal = line.split(":", 1)[1].strip()
                    signal_value = re.search(r"(\d+)%", signal)
                    if signal_value:
                        current_wifi["signal_strength"] = int(signal_value.group(1))
                elif line.startswith("Authentication") and current_wifi:
                    auth = line.split(":", 1)[1].strip()
                    current_wifi["security"] = auth
            
            if current_wifi:
                wifis.append(current_wifi)
            
            # Try to get the IP address for the currently connected WiFi
            try:
                connected_result = subprocess.run(["netsh", "wlan", "show", "interfaces"], 
                                               capture_output=True, text=False, check=True)
                connected_output = connected_result.stdout.decode('utf-8', errors='ignore')
                
                connected_ssid = None
                for line in connected_output.splitlines():
                    line = line.strip()
                    if line.startswith("SSID") and ":" in line:
                        connected_ssid = line.split(":", 1)[1].strip()
                        break
                
                if connected_ssid:
                    # Get the IP address for the connected WiFi
                    ip_result = subprocess.run(["ipconfig"], 
                                             capture_output=True, text=False, check=True)
                    ip_output = ip_result.stdout.decode('utf-8', errors='ignore')
                    
                    in_wifi_section = False
                    for line in ip_output.splitlines():
                        line = line.strip()
                        if "Wireless LAN adapter" in line:
                            in_wifi_section = True
                        elif "Ethernet adapter" in line or "Tunnel adapter" in line:
                            in_wifi_section = False
                        
                        if in_wifi_section and line.startswith("IPv4 Address") and ":" in line:
                            ip_address = line.split(":", 1)[1].strip().split()[0]
                            # Find the matching WiFi in the list and add the IP
                            for wifi in wifis:
                                if wifi["name"] == connected_ssid:
                                    wifi["ipAddress"] = ip_address
                                    break
                            break
            except Exception as e:
                print(f"Error getting connected WiFi IP on Windows: {e}")
            
            return wifis
        except Exception as e:
            print(f"Error getting Windows WiFi networks: {e}")
            return []
    
    def get_linux_wifis():
        """Get WiFi networks on Linux using nmcli"""
        try:
            result = subprocess.run(["nmcli", "-t", "-f", "SSID,SIGNAL,SECURITY", "dev", "wifi"], 
                                  capture_output=True, text=False, check=True)
            output = result.stdout.decode('utf-8', errors='ignore')
            
            wifis = []
            for line in output.splitlines():
                if line:
                    parts = line.split(":")
                    if len(parts) >= 3:
                        ssid = parts[0]
                        signal = int(parts[1])
                        security = parts[2]
                        if ssid:
                            wifis.append({
                                "name": ssid,
                                "signal_strength": signal,
                                "security": security,
                                "ipAddress": None
                            })
            
            # Try to get the IP address for the currently connected WiFi
            try:
                # Get connected WiFi SSID
                connected_ssid_result = subprocess.run(["nmcli", "-t", "-f", "active,ssid", "dev", "wifi"], 
                                                    capture_output=True, text=True, check=True)
                connected_ssid_output = connected_ssid_result.stdout
                
                connected_ssid = None
                for line in connected_ssid_output.splitlines():
                    if line.startswith("yes:"):
                        connected_ssid = line.split(":", 1)[1]
                        break
                
                if connected_ssid:
                    # Get the IP address for the connected WiFi
                    ip_result = subprocess.run(["ip", "addr", "show", "wlan0"], 
                                             capture_output=True, text=True, check=True)
                    ip_output = ip_result.stdout
                    
                    ip_match = re.search(r"inet\s+(\d+\.\d+\.\d+\.\d+)/\d+", ip_output)
                    if ip_match:
                        ip_address = ip_match.group(1)
                        # Find the matching WiFi in the list and add the IP
                        for wifi in wifis:
                            if wifi["name"] == connected_ssid:
                                wifi["ipAddress"] = ip_address
                                break
            except Exception as e:
                print(f"Error getting connected WiFi IP on Linux: {e}")
            
            return wifis
        except Exception as e:
            print(f"Error getting Linux WiFi networks: {e}")
            return []
    
    # Get server IP and subnet
    hostname = socket.gethostname()
    server_ip = socket.gethostbyname(hostname)
    server_subnet = ".".join(server_ip.split(".")[:-1])
    
    # Get WiFi networks based on platform
    wifis = []
    platform_system = platform.system()
    
    if platform_system == "Windows":
        wifis = get_windows_wifis()
    elif platform_system == "Linux":
        wifis = get_linux_wifis()
    else:
        print(f"WiFi scanning not supported on {platform_system}")
    
    # Suggest the best WiFi for autodroid-app (highest signal strength)
    suggested_wifi = None
    if wifis:
        suggested_wifi = max(wifis, key=lambda x: x["signal_strength"])
    
    return {
        "server_ip": server_ip,
        "server_subnet": server_subnet,
        "wifis": wifis,
        "suggested_wifi": suggested_wifi,
        "platform": platform_system
    }

@router.get("/qr-code")
async def generate_qr_code():
    """Generate a QR code containing server connection information"""
    try:
        # Get server IP address
        hostname = socket.gethostname()
        ip_address = socket.gethostbyname(hostname)
        
        # Get server configuration
        server_config = config.get('server', {})
        server_backend_config = server_config.get('backend', {})
        port = server_backend_config.get('port', 8003)
        use_https = server_backend_config.get('use_https', False)
        
        # Create connection data
        protocol = 'https' if use_https else 'http'
        api_endpoint = f"{protocol}://{ip_address}:{port}/api"
        
        # Generate expiry timestamp (24 hours from now)
        expiry_time = (datetime.now() + timedelta(hours=24)).isoformat()
        
        # Create QR code data
        qr_data = {
            "server_name": "Autodroid Server",
            "apiEndpoint": api_endpoint,
            "ipAddress": ip_address,
            "port": port,
            "protocol": protocol,
            "version": "1.0",
            "timestamp": datetime.now().isoformat(),
            "expiry": expiry_time
        }
        
        # Convert to JSON string
        qr_json = json.dumps(qr_data, separators=(',', ':'))
        
        # Generate QR code
        qr = qrcode.QRCode(
            version=1,
            error_correction=qrcode.constants.ERROR_CORRECT_L,
            box_size=10,
            border=4,
        )
        qr.add_data(qr_json)
        qr.make(fit=True)
        
        # Create an image from the QR Code instance
        img = qr.make_image(fill_color="black", back_color="white")
        
        # Convert image to bytes
        buffer = io.BytesIO()
        img.save(buffer, format="PNG")
        img_bytes = buffer.getvalue()
        
        # Return the image as a response
        return Response(
            content=img_bytes,
            media_type="image/png",
            headers={
                "Content-Disposition": "inline; filename=autodroid-qr.png",
                "X-QR-Data": base64.b64encode(qr_json.encode()).decode()
            }
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to generate QR code: {str(e)}")

@router.get("/qr-code/data")
async def get_qr_code_data():
    """Get the QR code data as JSON (for debugging or alternative display)"""
    try:
        # Get server IP address
        hostname = socket.gethostname()
        ip_address = socket.gethostbyname(hostname)
        
        # Get server configuration
        server_config = config.get('server', {})
        server_backend_config = server_config.get('backend', {})
        port = server_backend_config.get('port', 8003)
        use_https = server_backend_config.get('use_https', False)
        
        # Create connection data
        protocol = 'https' if use_https else 'http'
        api_endpoint = f"{protocol}://{ip_address}:{port}/api"
        
        # Generate expiry timestamp (24 hours from now)
        expiry_time = (datetime.now() + timedelta(hours=24)).isoformat()
        
        # Create QR code data
        qr_data = {
            "server_name": "Autodroid Server",
            "apiEndpoint": api_endpoint,
            "ipAddress": ip_address,
            "port": port,
            "protocol": protocol,
            "version": "1.0",
            "timestamp": datetime.now().isoformat(),
            "expiry": expiry_time
        }
        
        return {
            "qr_data": qr_data,
            "apiEndpoint": api_endpoint
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to generate QR code data: {str(e)}")