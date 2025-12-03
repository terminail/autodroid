"""
Workflow management API endpoints for Autodroid system.
Handles workflow creation, execution, scheduling, and test plan management.
"""

from fastapi import APIRouter, HTTPException, BackgroundTasks
from typing import Dict, Any
import os
import yaml
import uuid

from .models import WorkflowCreate, WorkflowPlanCreate, EventTrigger
from core.workflow.engine import WorkflowEngine
from core.scheduling.scheduler import SmartScheduler, DeviceWorkflowPlan

# Initialize router
router = APIRouter(prefix="/api", tags=["workflows"])

# Initialize core components
workflow_engine = WorkflowEngine()
scheduler = SmartScheduler()

# Configuration
WORKFLOWS_DIR = os.getenv("WORKFLOWS_DIR", os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "workflows"))

# Workflow Management Endpoints
@router.get("/workflows")
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

@router.get("/workflows/{workflow_name}")
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

@router.post("/workflows")
async def create_workflow(workflow: WorkflowCreate):
    """Create a new workflow"""
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
@router.get("/plans")
async def get_test_plans():
    """List all workflow plans"""
    return {
        "plans": [plan.__dict__ for plan in scheduler.active_plans.values()]
    }

@router.post("/plans", response_model=Dict[str, str])
async def create_workflow_plan(plan: WorkflowPlanCreate):
    """Create a new workflow plan"""
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

@router.get("/plans/{plan_id}")
async def get_test_plan(plan_id: str):
    """Get specific workflow plan"""
    plan = scheduler.active_plans.get(plan_id)
    if not plan:
        raise HTTPException(status_code=404, detail="Workflow plan not found")
    return plan.__dict__

@router.put("/plans/{plan_id}")
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

@router.delete("/plans/{plan_id}")
async def delete_test_plan(plan_id: str):
    """Delete a workflow plan"""
    if plan_id not in scheduler.active_plans:
        raise HTTPException(status_code=404, detail="Workflow plan not found")
    
    scheduler.remove_workflow_plan(plan_id)
    return {"message": "Workflow plan deleted successfully"}

# Event Trigger Endpoint
@router.post("/events/trigger")
async def trigger_event(event: EventTrigger):
    """Trigger an event-driven workflow execution"""
    await scheduler.handle_event(event.event_type, event.event_data)
    return {
        "message": "Event processed",
        "event_type": event.event_type
    }

# Test Plan Execution Endpoint
@router.post("/plans/{plan_id}/execute")
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