from fastapi import FastAPI, HTTPException, BackgroundTasks, Depends, Header
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.security import HTTPBearer
from typing import List, Dict, Any, Optional
from pydantic import BaseModel
import os
import asyncio
import socket
from zeroconf import ServiceInfo, Zeroconf

from core.device.device_manager import DeviceManager, DeviceInfo
from core.workflow.engine import WorkflowEngine, WorkflowConfig
from core.scheduling.scheduler import SmartScheduler, DeviceWorkflowPlan
from core.auth.models import UserCreate, UserLogin, UserResponse, Token
from core.auth.service import AuthService

# Initialize FastAPI app
app = FastAPI(
    title="Autodroid API",
    description="RESTful API for Autodroid Android Automation System",
    version="1.0.0"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount static files for frontend (only if frontend build exists)
frontend_build_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "frontend", "build")
if os.path.exists(frontend_build_dir):
    # Mount frontend at a specific path, not root, to avoid overriding API routes
    app.mount("/app", StaticFiles(directory=frontend_build_dir, html=True), name="frontend")
    print(f"✓ Frontend mounted at /app from {frontend_build_dir}")
else:
    print("⚠ Frontend build directory not found, API routes will be available at root")

# Initialize core components
device_manager = DeviceManager()
workflow_engine = WorkflowEngine()
scheduler = SmartScheduler()

# Initialize authentication service
auth_service = AuthService(
    secret_key="your-secret-key-change-in-production",  # 生产环境请修改此密钥
    token_expire_minutes=60
)

# Security scheme
security = HTTPBearer()

# Configuration
APPIUM_URL = os.getenv("APPIUM_URL", "http://appium-server:4723")
# Use absolute path to workflows directory in current project
WORKFLOWS_DIR = os.getenv("WORKFLOWS_DIR", os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "workflows"))
MAX_CONCURRENT_TASKS = int(os.getenv("MAX_CONCURRENT_TASKS", "5"))

# Pydantic models
class WorkflowCreate(BaseModel):
    name: str
    description: str
    metadata: Dict[str, str]
    device_selection: Dict[str, Any]
    steps: List[Dict[str, Any]]
    schedule: Optional[Dict[str, Any]] = None

class WorkflowPlanCreate(BaseModel):
    workflow_id: str
    device_udid: str
    enabled: bool = True
    schedule: Dict[str, Any]
    priority: int = 0

class EventTrigger(BaseModel):
    event_type: str
    event_data: Dict[str, Any]

# Background tasks
async def start_scheduler():
    """Start the scheduling service"""
    asyncio.create_task(scheduler.schedule_tasks())

async def register_mdns_service():
    """Register the server with mDNS for discovery using correct async API"""
    try:
        # Implement with correct async API for zeroconf
        service_type = "_autodroid._tcp.local."
        service_name = "Autodroid Server._autodroid._tcp.local."
        port = 8001
        properties = {
            'version': '1.0',
            'description': 'Autodroid Test Server'
        }
        
        # Get server IP address
        hostname = socket.gethostname()
        server_ip = socket.gethostbyname(hostname)
        print(f"Server IP: {server_ip}")
        
        # Create ServiceInfo
        info = ServiceInfo(
            service_type,
            service_name,
            addresses=[socket.inet_aton(server_ip)],
            port=port,
            properties=properties
        )
        
        # Register service using correct async API
        from zeroconf.asyncio import AsyncZeroconf
        
        # Create and keep the AsyncZeroconf instance alive for persistent registration
        aiozc = AsyncZeroconf()
        await aiozc.async_register_service(info)
        
        # Keep zeroconf instance alive
        app.state.zeroconf = aiozc
        app.state.service_info = info
        
        print(f"✓ mDNS service registered: {service_name} at {server_ip}:{port}")
        print("✓ Using correct async zeroconf API")
        
    except Exception as e:
        import traceback
        print(f"✗ Failed to register mDNS service: {e}")
        print(f"Full error traceback:")
        traceback.print_exc()
        # This is a critical failure - mDNS is required for the system to work
        print("⚠ WARNING: mDNS registration failed. The app will not be able to discover this server.")
        # Continue without mDNS - don't crash the server
        print("⚠ Continuing without mDNS service...")

# API Endpoints
@app.on_event("startup")
async def startup_event():
    """Initialize services on startup"""
    await start_scheduler()
    await register_mdns_service()

@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup services on shutdown"""
    if hasattr(app.state, 'zeroconf') and hasattr(app.state, 'service_info'):
        try:
            await app.state.zeroconf.async_unregister_service(app.state.service_info)
            await app.state.zeroconf.async_close()
            print("✓ mDNS service unregistered")
        except Exception as e:
            print(f"✗ Failed to unregister mDNS service: {e}")

@app.get("/api/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": asyncio.get_event_loop().time(),
        "services": {
            "device_manager": "running",
            "workflow_engine": "running",
            "scheduler": "running"
        }
    }

@app.get("/api/server")
async def get_server_info():
    """Get full server information"""
    import socket
    import platform
    
    # 获取服务器IP地址
    hostname = socket.gethostname()
    ip_address = socket.gethostbyname(hostname)
    
    # FastAPI 端口配置
    fastapi_port = 8000
    
    return {
        "name": "Autodroid Server",
        "hostname": hostname,
        "ip_address": ip_address,
        "platform": platform.platform(),
        "api_base_url": f"http://{ip_address}:{fastapi_port}/api",
        "services": {
            "device_manager": "running",
            "workflow_engine": "running",
            "scheduler": "running"
        },
        "capabilities": {
            "device_registration": True,
            "workflow_execution": True,
            "test_scheduling": True,
            "event_triggering": True
        }
    }

@app.get("/api/server/wifis")
async def get_server_wifis():
    """Get WiFi networks available to the server and suggest which one autodroid-app should use"""
    import socket
    import platform
    import subprocess
    import re
    
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
                                    wifi["ip_address"] = ip_address
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
                                "ip_address": None
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
                                wifi["ip_address"] = ip_address
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

# Device Management Endpoints
@app.get("/api/devices", response_model=List[Dict[str, Any]])
async def get_devices():
    """Get all registered devices"""
    return [device.__dict__ for device in device_manager.devices.values()]

@app.get("/api/devices/{udid}", response_model=Dict[str, Any])
async def get_device(udid: str):
    """Get specific device information"""
    device = device_manager.devices.get(udid)
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")
    return device.__dict__

@app.post("/api/devices/register")
async def register_device(device_info: Dict[str, Any]):
    """Register a device from app report"""
    try:
        device = device_manager.register_device(device_info)
        return {
            "message": f"Device registered successfully",
            "device": device.__dict__
        }
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

# Authentication Endpoints
@app.post("/api/auth/register", response_model=UserResponse)
async def register_user(user_data: UserCreate):
    """Register a new user"""
    try:
        print(f"DEBUG: Received registration request - email: {user_data.email}, name: {user_data.name}")
        
        # 如果前端只提供了email和password，自动生成name
        if not user_data.name:
            user_data.name = user_data.email.split('@')[0]
            print(f"DEBUG: Auto-generated name: {user_data.name}")
        
        auth_response = auth_service.register_user(
            email=user_data.email,
            name=user_data.name,
            password=user_data.password
        )
        if auth_response:
            print("DEBUG: Registration successful")
            return auth_response["user"]
        else:
            print("DEBUG: Registration failed - auth_response is None")
            raise HTTPException(status_code=400, detail="Registration failed")
    except ValueError as e:
        print(f"DEBUG: ValueError in registration: {e}")
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        print(f"DEBUG: Unexpected error in registration: {e}")
        raise HTTPException(status_code=400, detail="Registration failed")

@app.post("/api/auth/login", response_model=Token)
async def login_user(user_data: UserLogin):
    """Login user and return access token"""
    try:
        token = auth_service.authenticate_user(user_data.email, user_data.password)
        if not token:
            raise HTTPException(status_code=401, detail="Invalid email or password")
        return token
    except ValueError as e:
        raise HTTPException(status_code=401, detail=str(e))

@app.get("/api/auth/me", response_model=UserResponse)
async def get_current_user(authorization: str = Depends(security)):
    """Get current user information"""
    try:
        user = auth_service.get_current_user(authorization.credentials)
        return user
    except ValueError as e:
        raise HTTPException(status_code=401, detail=str(e))

@app.post("/api/auth/logout")
async def logout_user(authorization: str = Depends(security)):
    """Logout user and invalidate session"""
    try:
        auth_service.logout(authorization.credentials)
        return {"message": "Logged out successfully"}
    except ValueError as e:
        raise HTTPException(status_code=401, detail=str(e))

# APK Management Endpoints
@app.get("/api/devices/{udid}/apks")
async def get_device_apks(udid: str):
    """Get all APKs for a device"""
    try:
        apks = device_manager.get_apks(udid)
        return {
            "message": f"Found {len(apks)} APKs for device {udid}",
            "apks": [apk.__dict__ for apk in apks]
        }
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

@app.get("/api/devices/{udid}/apks/{apkid}")
async def get_device_apk(udid: str, apkid: str):
    """Get a specific APK for a device"""
    try:
        apk = device_manager.get_apk(udid, apkid)
        if not apk:
            raise HTTPException(status_code=404, detail=f"APK with ID {apkid} not found for device {udid}")
        return {
            "message": f"Found APK {apkid} for device {udid}",
            "apk": apk.__dict__
        }
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

@app.post("/api/devices/{udid}/apks")
async def add_device_apk(udid: str, apk_info: Dict[str, Any]):
    """Add an APK to a device"""
    try:
        apk = device_manager.add_apk(udid, apk_info)
        return {
            "message": f"APK added successfully to device {udid}",
            "apk": apk.__dict__
        }
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.put("/api/devices/{udid}/apks/{apkid}")
async def update_device_apk(udid: str, apkid: str, apk_info: Dict[str, Any]):
    """Update an APK on a device"""
    try:
        apk = device_manager.update_apk(udid, apkid, apk_info)
        return {
            "message": f"APK updated successfully for device {udid}",
            "apk": apk.__dict__
        }
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

@app.delete("/api/devices/{udid}/apks/{apkid}")
async def delete_device_apk(udid: str, apkid: str):
    """Delete an APK from a device"""
    try:
        success = device_manager.delete_apk(udid, apkid)
        if success:
            return {
                "message": f"APK deleted successfully from device {udid}"
            }
        else:
            raise HTTPException(status_code=404, detail=f"APK with ID {apkid} not found for device {udid}")
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

@app.get("/api/devices/{udid}/apks/{apkid}/workflows")
async def get_apk_workflows(udid: str, apkid: str):
    """Get workflows associated with a specific APK on a device"""
    try:
        # Check if device and APK exist
        apk = device_manager.get_apk(udid, apkid)
        if not apk:
            raise HTTPException(status_code=404, detail=f"APK with ID {apkid} not found for device {udid}")
        
        # TODO: Implement workflow association with APK
        # For now, return an empty list
        return {
            "message": f"Found 0 workflows for APK {apkid} on device {udid}",
            "workflows": []
        }
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

# Workflow Management Endpoints
@app.get("/api/workflows")
async def get_workflows():
    """List all available workflows"""
    workflow_files = workflow_engine.list_workflows(WORKFLOWS_DIR)
    workflows = []
    
    for workflow_file in workflow_files:
        try:
            # Extract workflow name from filename (remove .yml/.yaml extension)
            workflow_name = workflow_file.replace('.yml', '').replace('.yaml', '')
            workflow_path = os.path.join(WORKFLOWS_DIR, workflow_file)
            
            # Parse the workflow file
            workflow_config = workflow_engine.parse_workflow(workflow_path)
            
            # Convert to WorkflowInfo format
            workflow_info = {
                "id": workflow_name,
                "name": workflow_config.name,
                "description": workflow_config.description,
                "metadata": workflow_config.metadata,
                "device_selection": workflow_config.device_selection,
                "steps": [step.__dict__ for step in workflow_config.steps],
                "schedule": workflow_config.schedule
            }
            workflows.append(workflow_info)
        except Exception as e:
            print(f"Error parsing workflow {workflow_file}: {e}")
            continue
    
    return {
        "workflows": workflows
    }

@app.get("/api/workflows/{workflow_name}")
async def get_workflow(workflow_name: str):
    """Get workflow details"""
    workflow_path = os.path.join(WORKFLOWS_DIR, f"{workflow_name}.yml")
    if not os.path.exists(workflow_path):
        raise HTTPException(status_code=404, detail="Workflow not found")
    
    workflow = workflow_engine.parse_workflow(workflow_path)
    return {
        "name": workflow.name,
        "description": workflow.description,
        "metadata": workflow.metadata,
        "device_selection": workflow.device_selection,
        "steps": [step.__dict__ for step in workflow.steps],
        "schedule": workflow.schedule
    }

@app.post("/api/workflows")
async def create_workflow(workflow: WorkflowCreate):
    """Create a new workflow"""
    import yaml
    
    workflow_path = os.path.join(WORKFLOWS_DIR, f"{workflow.name}.yml")
    if os.path.exists(workflow_path):
        raise HTTPException(status_code=400, detail="Workflow with this name already exists")
    
    workflow_data = workflow.model_dump()
    
    with open(workflow_path, 'w') as f:
        yaml.dump(workflow_data, f, default_flow_style=False)
    
    return {
        "message": "Workflow created successfully",
        "workflow_name": workflow.name
    }

# Test Plan Endpoints
@app.get("/api/plans")
async def get_test_plans():
    """List all workflow plans"""
    return {
        "plans": [plan.__dict__ for plan in scheduler.active_plans.values()]
    }

@app.post("/api/plans", response_model=Dict[str, str])
async def create_workflow_plan(plan: WorkflowPlanCreate):
    """Create a new workflow plan"""
    import uuid
    
    workflow_plan = DeviceWorkflowPlan(
        id=str(uuid.uuid4()),
        workflow_id=plan.workflow_id,
        device_udid=plan.device_udid,
        enabled=plan.enabled,
        schedule=plan.schedule,
        priority=plan.priority
    )
    
    scheduler.add_workflow_plan(workflow_plan)
    
    return {
        "message": "Workflow plan created successfully",
        "plan_id": workflow_plan.id
    }

@app.get("/api/plans/{plan_id}")
async def get_test_plan(plan_id: str):
    """Get specific workflow plan"""
    plan = scheduler.active_plans.get(plan_id)
    if not plan:
        raise HTTPException(status_code=404, detail="Workflow plan not found")
    return plan.__dict__

@app.put("/api/plans/{plan_id}")
async def update_workflow_plan(plan_id: str, plan: WorkflowPlanCreate):
    """Update an existing workflow plan"""
    if plan_id not in scheduler.active_plans:
        raise HTTPException(status_code=404, detail="Workflow plan not found")
    
    updated_plan = DeviceWorkflowPlan(
        id=plan_id,
        workflow_id=plan.workflow_id,
        device_udid=plan.device_udid,
        enabled=plan.enabled,
        schedule=plan.schedule,
        priority=plan.priority
    )
    
    scheduler.update_workflow_plan(updated_plan)
    return {"message": "Workflow plan updated successfully"}

@app.delete("/api/plans/{plan_id}")
async def delete_test_plan(plan_id: str):
    """Delete a workflow plan"""
    if plan_id not in scheduler.active_plans:
        raise HTTPException(status_code=404, detail="Workflow plan not found")
    
    scheduler.remove_workflow_plan(plan_id)
    return {"message": "Workflow plan deleted successfully"}



# Event Trigger Endpoint
@app.post("/api/events/trigger")
async def trigger_event(event: EventTrigger):
    """Trigger an event-driven workflow execution"""
    await scheduler.handle_event(event.event_type, event.event_data)
    return {
        "message": "Event processed",
        "event_type": event.event_type
    }

# Test Plan Execution Endpoint
@app.post("/api/plans/{plan_id}/execute")
async def execute_test_plan(plan_id: str, background_tasks: BackgroundTasks):
    """Execute a specific workflow plan"""
    plan = scheduler.active_plans.get(plan_id)
    if not plan:
        raise HTTPException(status_code=404, detail="Workflow plan not found")
    
    # Add to task queue
    scheduler.task_queue.append(plan)
    
    return {
        "message": "Workflow plan added to execution queue",
        "plan_id": plan_id
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
