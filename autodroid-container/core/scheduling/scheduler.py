import asyncio
import datetime
from typing import Dict, List, Any, Optional
from dataclasses import dataclass

@dataclass
class DeviceTestPlan:
    id: str
    workflow_name: str
    device_udid: str
    enabled: bool
    schedule: Dict[str, Any]
    priority: int = 0

class SmartScheduler:
    def __init__(self):
        self.active_plans: Dict[str, DeviceTestPlan] = {}
        self.task_queue: List[DeviceTestPlan] = []
    
    def add_test_plan(self, plan: DeviceTestPlan):
        """Add a new test plan to the scheduler"""
        self.active_plans[plan.id] = plan
    
    def remove_test_plan(self, plan_id: str):
        """Remove a test plan from the scheduler"""
        if plan_id in self.active_plans:
            del self.active_plans[plan_id]
    
    def update_test_plan(self, plan: DeviceTestPlan):
        """Update an existing test plan"""
        if plan.id in self.active_plans:
            self.active_plans[plan.id] = plan
    
    def is_within_schedule(self, schedule: Dict[str, Any], current_time: datetime.datetime) -> bool:
        """Check if current time is within the schedule"""
        schedule_type = schedule.get('type', 'daily')
        
        if schedule_type == 'daily':
            # Check if current time is between start and end time
            start_time = datetime.datetime.strptime(schedule['start_time'], '%H:%M').time()
            end_time = datetime.datetime.strptime(schedule['end_time'], '%H:%M').time()
            return start_time <= current_time.time() <= end_time
        
        elif schedule_type == 'interval':
            # Check if enough time has passed since last execution
            interval_minutes = schedule.get('interval_minutes', 60)
            # This would require tracking last execution time, simplified for now
            return True
        
        elif schedule_type == 'cron':
            # Basic cron-like functionality
            cron_expr = schedule.get('expression', '* * * * *')
            # Simplified implementation - would need proper cron parsing
            return True
        
        return False
    
    async def get_due_test_plans(self) -> List[DeviceTestPlan]:
        """Get all test plans that are due for execution"""
        current_time = datetime.datetime.now()
        active_plans = []
        
        for device_id, plan in self.active_plans.items():
            if plan.enabled and self.is_within_schedule(plan.schedule, current_time):
                active_plans.append(plan)
        
        # Sort by priority
        active_plans.sort(key=lambda x: x.priority, reverse=True)
        
        return active_plans
    
    async def schedule_tasks(self):
        """Main scheduling loop"""
        while True:
            due_plans = await self.get_due_test_plans()
            
            for plan in due_plans:
                if plan.id not in [task.id for task in self.task_queue]:
                    self.task_queue.append(plan)
            
            await asyncio.sleep(60)  # Check every minute
    
    def get_next_task(self) -> Optional[DeviceTestPlan]:
        """Get the next task from the queue"""
        if self.task_queue:
            return self.task_queue.pop(0)
        return None
    
    async def handle_event(self, event_type: str, event_data: Dict[str, Any]):
        """Handle event-driven test plan execution"""
        # Find test plans that match the event
        for plan_id, plan in self.active_plans.items():
            if plan.enabled and 'event_trigger' in plan.schedule:
                trigger = plan.schedule['event_trigger']
                if trigger['type'] == event_type:
                    # Check if event data matches trigger conditions
                    match = True
                    for key, value in trigger.get('conditions', {}).items():
                        if event_data.get(key) != value:
                            match = False
                            break
                    
                    if match:
                        self.task_queue.append(plan)
