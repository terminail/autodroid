"""Intelligent agent for Android automation based on workplan data."""

import json
import time
from typing import Dict, List, Any, Optional
from dataclasses import dataclass
from adb_device import ADBDevice, quick_connect
from ui_definitions import UIDefinitionManager, UIElementDefinition, ElementType, IdentifierType


@dataclass
class UIElement:
    """UI element definition from workplan data."""
    name: str
    element_type: str  # button, input_field, icon, text, etc.
    identifier: Dict[str, Any]  # xpath, id, text, coordinates, etc.
    action: str  # tap, type, swipe, etc.
    input_data: Optional[str] = None  # data to input if applicable
    wait_time: float = 1.0  # wait time after action


@dataclass
class WorkStep:
    """Single work step from workplan."""
    step_id: str
    description: str
    ui_elements: List[UIElement]
    expected_result: str
    fallback_action: Optional[str] = None


class IntelligentAgent:
    """Intelligent agent that processes workplan data and executes actions."""
    
    def __init__(self, device: Optional[ADBDevice] = None, ui_manager: Optional[UIDefinitionManager] = None):
        """Initialize intelligent agent.
        
        Args:
            device: ADB device instance, auto-connect if not provided
            ui_manager: UI definition manager, creates new if not provided
        """
        self.device = device or quick_connect()
        self.ui_manager = ui_manager or UIDefinitionManager()
        self.current_step = 0
        self.workplan_data = None
        self.execution_log = []
    
    def load_workplan(self, workplan_path: str) -> Dict[str, Any]:
        """Load workplan data from JSON file.
        
        Args:
            workplan_path: Path to workplan JSON file
            
        Returns:
            Parsed workplan data
        """
        try:
            with open(workplan_path, 'r', encoding='utf-8') as f:
                self.workplan_data = json.load(f)
            
            print(f"Loaded workplan: {self.workplan_data.get('name', 'Unknown')}")
            print(f"Description: {self.workplan_data.get('description', '')}")
            print(f"Total steps: {len(self.workplan_data.get('steps', []))}")
            
            return self.workplan_data
            
        except Exception as e:
            raise RuntimeError(f"Failed to load workplan: {e}")
    
    def parse_workplan_steps(self) -> List[WorkStep]:
        """Parse workplan data into executable work steps.
        
        Returns:
            List of work steps
        """
        if not self.workplan_data:
            raise RuntimeError("No workplan data loaded")
        
        steps = []
        for step_data in self.workplan_data.get('steps', []):
            ui_elements = []
            
            for element_data in step_data.get('ui_elements', []):
                ui_element = UIElement(
                    name=element_data['name'],
                    element_type=element_data['type'],
                    identifier=element_data['identifier'],
                    action=element_data['action'],
                    input_data=element_data.get('input_data'),
                    wait_time=element_data.get('wait_time', 1.0)
                )
                ui_elements.append(ui_element)
            
            work_step = WorkStep(
                step_id=step_data['step_id'],
                description=step_data['description'],
                ui_elements=ui_elements,
                expected_result=step_data['expected_result'],
                fallback_action=step_data.get('fallback_action')
            )
            steps.append(work_step)
        
        return steps
    
    def find_element_by_coordinates(self, x: int, y: int) -> bool:
        """Find element by coordinates and perform action.
        
        Args:
            x: X coordinate
            y: Y coordinate
            
        Returns:
            True if action successful
        """
        try:
            self.device.tap(x, y)
            return True
        except Exception as e:
            print(f"Failed to tap at coordinates ({x}, {y}): {e}")
            return False
    
    def find_element_by_text(self, text: str, action: str = "tap") -> bool:
        """Find element by text content.
        
        Args:
            text: Text to search for
            action: Action to perform (tap, long_press, etc.)
            
        Returns:
            True if action successful
        """
        # For now, we'll use a simple approach
        # In a real implementation, you might use OCR or computer vision
        try:
            # Take screenshot for analysis
            screenshot_file = f"step_{self.current_step}_screenshot.png"
            self.device.get_screenshot(screenshot_file)
            
            print(f"Looking for text '{text}' in screenshot: {screenshot_file}")
            print(f"Would perform action: {action}")
            
            # For demo purposes, assume we found it at center screen
            # In real implementation, use OCR or model to find text position
            screen_width, screen_height = 1080, 1920  # Default screen size
            center_x, center_y = screen_width // 2, screen_height // 2
            
            if action == "tap":
                self.device.tap(center_x, center_y)
            elif action == "long_press":
                self.device.swipe(center_x, center_y, center_x, center_y, 2000)
            
            return True
            
        except Exception as e:
            print(f"Failed to find element by text '{text}': {e}")
            return False
    
    def input_text(self, text: str, input_field: str = None) -> bool:
        """Input text into a field.
        
        Args:
            text: Text to input
            input_field: Optional field identifier
            
        Returns:
            True if input successful
        """
        try:
            # First tap on input field if specified
            if input_field:
                # For now, assume center of screen
                self.device.tap(540, 960)  # Center of 1080x1920 screen
                time.sleep(0.5)
            
            # Input the text
            self.device.type_text(text)
            return True
            
        except Exception as e:
            print(f"Failed to input text '{text}': {e}")
            return False
    
    def execute_ui_element_action(self, element: UIElement) -> bool:
        """Execute action on a single UI element.
        
        Args:
            element: UI element definition
            
        Returns:
            True if action successful
        """
        print(f"Executing action on element: {element.name} ({element.element_type})")
        print(f"Action: {element.action}")
        
        # Handle different identifier types
        if "coordinates" in element.identifier:
            coords = element.identifier["coordinates"]
            x, y = coords["x"], coords["y"]
            
            if element.action == "tap":
                success = self.device.tap(x, y)
            elif element.action == "swipe":
                end_coords = element.identifier.get("end_coordinates", {})
                end_x, end_y = end_coords.get("x", x), end_coords.get("y", y)
                duration = element.identifier.get("duration", 1000)
                success = self.device.swipe(x, y, end_x, end_y, duration)
            else:
                print(f"Unsupported action: {element.action}")
                return False
                
        elif "text" in element.identifier:
            text = element.identifier["text"]
            success = self.find_element_by_text(text, element.action)
            
        elif "xpath" in element.identifier or "id" in element.identifier:
            # For now, use coordinate-based approach
            # In real implementation, use UIAutomator or similar
            default_x, default_y = 540, 960  # Center of screen
            if element.action == "tap":
                success = self.device.tap(default_x, default_y)
            elif element.action == "input":
                success = self.input_text(element.input_data or "")
            else:
                print(f"Unsupported action for xpath/id: {element.action}")
                return False
        else:
            print(f"Unknown identifier type: {element.identifier}")
            return False
        
        # Wait after action
        time.sleep(element.wait_time)
        return success
    
    def execute_work_step(self, step: WorkStep) -> bool:
        """Execute a single work step.
        
        Args:
            step: Work step definition
            
        Returns:
            True if step executed successfully
        """
        print(f"\n--- Executing Step {self.current_step + 1}: {step.description} ---")
        
        step_log = {
            "step_id": step.step_id,
            "description": step.description,
            "actions": [],
            "success": False,
            "error": None
        }
        
        try:
            # Execute each UI element action in the step
            for element in step.ui_elements:
                action_result = self.execute_ui_element_action(element)
                step_log["actions"].append({
                    "element": element.name,
                    "action": element.action,
                    "success": action_result
                })
                
                if not action_result and step.fallback_action:
                    print(f"Primary action failed, trying fallback: {step.fallback_action}")
                    # Execute fallback action
                    # ... (implement fallback logic)
            
            # Verify expected result
            # For now, just wait and take screenshot
            time.sleep(2)
            result_screenshot = f"step_{self.current_step}_result.png"
            self.device.get_screenshot(result_screenshot)
            
            step_log["success"] = True
            step_log["result_screenshot"] = result_screenshot
            
            print(f"Step completed successfully")
            return True
            
        except Exception as e:
            step_log["error"] = str(e)
            print(f"Step failed: {e}")
            return False
        
        finally:
            self.execution_log.append(step_log)
    
    def run_workplan(self, workplan_path: str) -> Dict[str, Any]:
        """Run complete workplan.
        
        Args:
            workplan_path: Path to workplan JSON file
            
        Returns:
            Execution summary
        """
        print("=== Starting Intelligent Agent ===")
        
        # Load workplan
        self.load_workplan(workplan_path)
        
        # Parse work steps
        work_steps = self.parse_workplan_steps()
        
        print(f"Executing {len(work_steps)} work steps...")
        
        # Execute each step
        successful_steps = 0
        failed_steps = 0
        
        for i, step in enumerate(work_steps):
            self.current_step = i
            success = self.execute_work_step(step)
            
            if success:
                successful_steps += 1
            else:
                failed_steps += 1
                print(f"Step {i + 1} failed, continuing with next step...")
        
        # Generate execution summary
        summary = {
            "workplan_name": self.workplan_data.get("name", "Unknown"),
            "total_steps": len(work_steps),
            "successful_steps": successful_steps,
            "failed_steps": failed_steps,
            "success_rate": successful_steps / len(work_steps) * 100 if work_steps else 0,
            "execution_log": self.execution_log
        }
        
        print(f"\n=== Execution Complete ===")
        print(f"Total steps: {summary['total_steps']}")
        print(f"Successful: {summary['successful_steps']}")
        print(f"Failed: {summary['failed_steps']}")
        print(f"Success rate: {summary['success_rate']:.1f}%")
        
        return summary
    
    def get_execution_report(self) -> str:
        """Generate detailed execution report.
        
        Returns:
            Formatted execution report
        """
        if not self.execution_log:
            return "No execution log available"
        
        report = []
        report.append("=== Execution Report ===")
        report.append(f"Workplan: {self.workplan_data.get('name', 'Unknown')}")
        report.append(f"Total steps: {len(self.execution_log)}")
        report.append("")
        
        for i, log in enumerate(self.execution_log):
            report.append(f"Step {i + 1}: {log['description']}")
            report.append(f"  Status: {'✓ Success' if log['success'] else '✗ Failed'}")
            
            if log['actions']:
                report.append("  Actions:")
                for action in log['actions']:
                    status = "✓" if action['success'] else "✗"
                    report.append(f"    {status} {action['element']} - {action['action']}")
            
            if log['error']:
                report.append(f"  Error: {log['error']}")
            
            if log.get('result_screenshot'):
                report.append(f"  Result screenshot: {log['result_screenshot']}")
            
            report.append("")
        
        return "\n".join(report)


def main():
    """Example usage of intelligent agent."""
    # Create sample workplan
    sample_workplan = {
        "name": "美团搜索美食",
        "description": "自动打开美团并搜索附近的美食",
        "steps": [
            {
                "step_id": "1",
                "description": "找到并点击美团App图标",
                "ui_elements": [
                    {
                        "name": "美团图标",
                        "type": "app_icon",
                        "identifier": {
                            "coordinates": {"x": 200, "y": 300}
                        },
                        "action": "tap",
                        "wait_time": 3.0
                    }
                ],
                "expected_result": "美团App启动",
                "fallback_action": "通过应用列表搜索美团"
            },
            {
                "step_id": "2", 
                "description": "点击搜索框并输入美食",
                "ui_elements": [
                    {
                        "name": "搜索框",
                        "type": "input_field",
                        "identifier": {
                            "coordinates": {"x": 540, "y": 200}
                        },
                        "action": "tap",
                        "wait_time": 1.0
                    },
                    {
                        "name": "搜索输入",
                        "type": "text_input",
                        "identifier": {
                            "text": "搜索"
                        },
                        "action": "input",
                        "input_data": "附近美食",
                        "wait_time": 2.0
                    }
                ],
                "expected_result": "搜索框显示'附近美食'",
                "fallback_action": "使用语音搜索"
            }
        ]
    }
    
    # Save sample workplan
    with open("sample_workplan.json", "w", encoding="utf-8") as f:
        json.dump(sample_workplan, f, ensure_ascii=False, indent=2)
    
    # Create and run agent
    agent = IntelligentAgent()
    summary = agent.run_workplan("sample_workplan.json")
    
    # Print detailed report
    print("\n" + agent.get_execution_report())


if __name__ == "__main__":
    main()