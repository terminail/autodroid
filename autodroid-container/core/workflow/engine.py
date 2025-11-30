import yaml
import asyncio
from typing import Dict, List, Any, Optional
from dataclasses import dataclass

@dataclass
class WorkflowStep:
    name: str
    action: str
    locator: Optional[Dict[str, Any]] = None
    timeout: int = 30
    retries: int = 0
    text: Optional[str] = None
    package: Optional[str] = None
    activity: Optional[str] = None

@dataclass
class WorkflowConfig:
    name: str
    description: str
    metadata: Dict[str, str]
    device_selection: Dict[str, Any]
    steps: List[WorkflowStep]
    schedule: Optional[Dict[str, Any]] = None

class WorkflowEngine:
    def __init__(self):
        self.active_workflows: Dict[str, asyncio.Task] = {}
    
    def parse_workflow(self, workflow_path: str) -> WorkflowConfig:
        """Parse a YAML workflow file into a WorkflowConfig object"""
        with open(workflow_path, 'r') as f:
            workflow_data = yaml.safe_load(f)
        
        steps = []
        for step_data in workflow_data.get('steps', []):
            step = WorkflowStep(
                name=step_data['name'],
                action=step_data['action'],
                locator=step_data.get('locator'),
                timeout=step_data.get('timeout', 30),
                retries=step_data.get('retries', 0),
                text=step_data.get('text'),
                package=step_data.get('package'),
                activity=step_data.get('activity')
            )
            steps.append(step)
        
        return WorkflowConfig(
            name=workflow_data['name'],
            description=workflow_data['description'],
            metadata=workflow_data['metadata'],
            device_selection=workflow_data['device_selection'],
            steps=steps,
            schedule=workflow_data.get('schedule')
        )
    
    async def execute_step(self, driver: Any, step: WorkflowStep) -> bool:
        """Execute a single workflow step"""
        try:
            if step.action == 'launch_app':
                driver.start_activity(app_package=step.package, app_activity=step.activity)
                await asyncio.sleep(2)  # Wait for app to launch
            
            elif step.action == 'click':
                if step.locator:
                    for strategy in step.locator.get('strategies', []):
                        try:
                            if strategy['type'] == 'id':
                                element = driver.find_element_by_id(strategy['value'])
                            elif strategy['type'] == 'text':
                                element = driver.find_element_by_android_uiautomator(
                                    f"new UiSelector().text('{strategy['value']}')"
                                )
                            elif strategy['type'] == 'xpath':
                                element = driver.find_element_by_xpath(strategy['value'])
                            else:
                                continue
                            
                            element.click()
                            await asyncio.sleep(1)  # Wait after click
                            return True
                        except Exception:
                            continue
            
            elif step.action == 'input_text':
                if step.locator and step.text:
                    for strategy in step.locator.get('strategies', []):
                        try:
                            if strategy['type'] == 'id':
                                element = driver.find_element_by_id(strategy['value'])
                            elif strategy['type'] == 'text':
                                element = driver.find_element_by_android_uiautomator(
                                    f"new UiSelector().text('{strategy['value']}')"
                                )
                            else:
                                continue
                            
                            element.clear()
                            element.send_keys(step.text)
                            await asyncio.sleep(1)  # Wait after input
                            return True
                        except Exception:
                            continue
            
            elif step.action == 'wait_for_element':
                if step.locator:
                    for strategy in step.locator.get('strategies', []):
                        try:
                            if strategy['type'] == 'id':
                                driver.find_element_by_id(strategy['value'])
                            elif strategy['type'] == 'text':
                                driver.find_element_by_android_uiautomator(
                                    f"new UiSelector().text('{strategy['value']}')"
                                )
                            else:
                                continue
                            return True
                        except Exception:
                            await asyncio.sleep(1)  # Wait and retry
            
            return False
        except Exception as e:
            print(f"Error executing step {step.name}: {e}")
            return False
    
    async def execute_workflow(self, workflow_config: WorkflowConfig, device_udid: str, appium_url: str) -> Dict[str, Any]:
        """Execute an entire workflow on a specific device"""
        from appium import webdriver
        
        results = {
            'workflow_name': workflow_config.name,
            'device_udid': device_udid,
            'start_time': asyncio.get_event_loop().time(),
            'steps': [],
            'success': True
        }
        
        try:
            # Initialize Appium driver
            desired_caps = {
                'platformName': 'Android',
                'deviceName': device_udid,
                'appPackage': workflow_config.metadata['app_package'],
                'appActivity': workflow_config.metadata['app_activity'],
                'automationName': 'UiAutomator2',
                'udid': device_udid
            }
            
            driver = webdriver.Remote(appium_url, desired_caps)
            
            # Execute each step
            for step in workflow_config.steps:
                step_result = {
                    'name': step.name,
                    'action': step.action,
                    'start_time': asyncio.get_event_loop().time(),
                    'success': False
                }
                
                # Try to execute step with retries
                for attempt in range(step.retries + 1):
                    if await self.execute_step(driver, step):
                        step_result['success'] = True
                        break
                    await asyncio.sleep(1)  # Wait before retry
                
                step_result['end_time'] = asyncio.get_event_loop().time()
                step_result['duration'] = step_result['end_time'] - step_result['start_time']
                results['steps'].append(step_result)
                
                if not step_result['success']:
                    results['success'] = False
                    break
            
            driver.quit()
        except Exception as e:
            results['success'] = False
            results['error'] = str(e)
        
        results['end_time'] = asyncio.get_event_loop().time()
        results['duration'] = results['end_time'] - results['start_time']
        
        return results
    
    def list_workflows(self, workflows_dir: str) -> List[str]:
        """List all workflow files in the specified directory"""
        import os
        workflow_files = []
        for file in os.listdir(workflows_dir):
            if file.endswith('.yml') or file.endswith('.yaml'):
                workflow_files.append(file)
        return workflow_files
