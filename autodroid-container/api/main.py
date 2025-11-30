from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from typing import List, Dict, Any, Optional
from pydantic import BaseModel
import os
import asyncio

from core.device.manager import DeviceManager, DeviceInfo
from core.workflow.engine import WorkflowEngine, WorkflowConfig
from core.scheduling.scheduler import SmartScheduler, DeviceTestPlan

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

# Initialize core components
device_manager = DeviceManager()
workflow_engine = WorkflowEngine()
scheduler = SmartScheduler()

# Configuration
APPIUM_URL = os.getenv("APPIUM_URL", "http://appium-server:4723")
WORKFLOWS_DIR = os.getenv("WORKFLOWS_DIR", "/app/workflows")
MAX_CONCURRENT_TASKS = int(os.getenv("MAX_CONCURRENT_TASKS", "5"))

# Pydantic models
class WorkflowCreate(BaseModel):
    name: str
    description: str
    metadata: Dict[str, str]
    device_selection: Dict[str, Any]
    steps: List[Dict[str, Any]]
    schedule: Optional[Dict[str, Any]] = None

class TestPlanCreate(BaseModel):
    workflow_name: str
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

async def initialize_zenoh():
    """Initialize Zenoh for device discovery"""
    await device_manager.initialize_zenoh()

# API Endpoints
@app.on_event("startup")
async def startup_event():
    """Initialize services on startup"""
    await start_scheduler()
    await initialize_zenoh()

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

# Device Management Endpoints
@app.get("/api/devices", response_model=List[Dict[str, Any]])
async def get_devices():
    """Get all connected devices"""
    devices = device_manager.scan_devices()
    return [device.__dict__ for device in devices]

@app.get("/api/devices/{udid}", response_model=Dict[str, Any])
async def get_device(udid: str):
    """Get specific device information"""
    device = device_manager.devices.get(udid)
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")
    return device.__dict__

@app.post("/api/devices/scan")
async def scan_devices():
    """Manually scan for connected devices using ADB and Zenoh"""
    devices = await device_manager.scan_devices()
    return {
        "message": f"Found {len(devices)} devices",
        "devices": [device.__dict__ for device in devices]
    }

@app.post("/api/devices/scan/zenoh")
async def scan_devices_zenoh(timeout: int = 5):
    """Manually scan for devices using Zenoh"""
    device_ids = await device_manager.discover_devices_zenoh(timeout)
    await device_manager.update_devices_from_zenoh()
    
    # Get updated device list
    zenoh_devices = [device for device in device_manager.devices.values() if device.zenoh_connected]
    
    return {
        "message": f"Found {len(device_ids)} devices via Zenoh",
        "device_ids": device_ids,
        "zenoh_devices": [device.__dict__ for device in zenoh_devices]
    }

# Workflow Management Endpoints
@app.get("/api/workflows")
async def get_workflows():
    """List all available workflows"""
    workflows = workflow_engine.list_workflows(WORKFLOWS_DIR)
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
    """List all test plans"""
    return {
        "plans": [plan.__dict__ for plan in scheduler.active_plans.values()]
    }

@app.post("/api/plans", response_model=Dict[str, str])
async def create_test_plan(plan: TestPlanCreate):
    """Create a new test plan"""
    import uuid
    
    test_plan = DeviceTestPlan(
        id=str(uuid.uuid4()),
        workflow_name=plan.workflow_name,
        device_udid=plan.device_udid,
        enabled=plan.enabled,
        schedule=plan.schedule,
        priority=plan.priority
    )
    
    scheduler.add_test_plan(test_plan)
    
    return {
        "message": "Test plan created successfully",
        "plan_id": test_plan.id
    }

@app.get("/api/plans/{plan_id}")
async def get_test_plan(plan_id: str):
    """Get specific test plan"""
    plan = scheduler.active_plans.get(plan_id)
    if not plan:
        raise HTTPException(status_code=404, detail="Test plan not found")
    return plan.__dict__

@app.put("/api/plans/{plan_id}")
async def update_test_plan(plan_id: str, plan: TestPlanCreate):
    """Update an existing test plan"""
    if plan_id not in scheduler.active_plans:
        raise HTTPException(status_code=404, detail="Test plan not found")
    
    updated_plan = DeviceTestPlan(
        id=plan_id,
        workflow_name=plan.workflow_name,
        device_udid=plan.device_udid,
        enabled=plan.enabled,
        schedule=plan.schedule,
        priority=plan.priority
    )
    
    scheduler.update_test_plan(updated_plan)
    return {"message": "Test plan updated successfully"}

@app.delete("/api/plans/{plan_id}")
async def delete_test_plan(plan_id: str):
    """Delete a test plan"""
    if plan_id not in scheduler.active_plans:
        raise HTTPException(status_code=404, detail="Test plan not found")
    
    scheduler.remove_test_plan(plan_id)
    return {"message": "Test plan deleted successfully"}

# Workflow Execution Endpoints
@app.post("/api/workflows/{workflow_name}/execute")
async def execute_workflow(workflow_name: str, device_udid: str, background_tasks: BackgroundTasks):
    """Execute a workflow on a specific device"""
    # Check if device exists
    device = device_manager.devices.get(device_udid)
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")
    
    # Check if workflow exists
    workflow_path = os.path.join(WORKFLOWS_DIR, f"{workflow_name}.yml")
    if not os.path.exists(workflow_path):
        raise HTTPException(status_code=404, detail="Workflow not found")
    
    # Parse workflow
    workflow = workflow_engine.parse_workflow(workflow_path)
    
    # Execute workflow in background
    async def run_workflow():
        result = await workflow_engine.execute_workflow(workflow, device_udid, APPIUM_URL)
        # Save result to reports directory
        import json
        import datetime
        
        report_path = os.path.join(
            "reports",
            f"{workflow_name}_{device_udid}_{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        )
        
        with open(report_path, 'w') as f:
            json.dump(result, f, indent=2)
    
    background_tasks.add_task(run_workflow)
    
    return {
        "message": "Workflow execution started",
        "workflow_name": workflow_name,
        "device_udid": device_udid
    }

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
    """Execute a specific test plan"""
    plan = scheduler.active_plans.get(plan_id)
    if not plan:
        raise HTTPException(status_code=404, detail="Test plan not found")
    
    # Add to task queue
    scheduler.task_queue.append(plan)
    
    return {
        "message": "Test plan added to execution queue",
        "plan_id": plan_id
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
