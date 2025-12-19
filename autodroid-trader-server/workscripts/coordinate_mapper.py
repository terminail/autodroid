"""Coordinate mapping tool for Android UI elements."""

import json
import time
import os
from typing import Dict, List, Tuple, Optional, Any
from dataclasses import dataclass
from adb_device import ADBDevice, quick_connect
from ui_definitions import UIDefinitionManager, UIElementDefinition, ElementType, IdentifierType


@dataclass
class CoordinatePoint:
    """Represents a coordinate point with metadata."""
    x: int
    y: int
    element_name: str = ""
    element_type: str = ""
    description: str = ""
    timestamp: float = 0.0
    
    def __post_init__(self):
        if self.timestamp == 0.0:
            self.timestamp = time.time()


class CoordinateMapper:
    """Interactive coordinate mapping tool for Android UI elements."""
    
    def __init__(self, device: Optional[ADBDevice] = None):
        """Initialize coordinate mapper.
        
        Args:
            device: ADB device instance
        """
        self.device = device or quick_connect()
        self.definition_manager = UIDefinitionManager()
        self.current_screen = None
        self.mapped_elements: List[CoordinatePoint] = []
        self.screenshots_dir = "coordinate_mapping_screenshots"
        
        # Create screenshots directory
        os.makedirs(self.screenshots_dir, exist_ok=True)
    
    def start_interactive_mapping(self, app_package: str = None, screen_name: str = "unknown_screen"):
        """Start interactive coordinate mapping session.
        
        Args:
            app_package: Target app package name
            screen_name: Name for this screen
        """
        print("=== Android UI Coordinate Mapper ===")
        print("This tool helps you map UI element coordinates interactively.")
        print("\nInstructions:")
        print("1. Make sure your Android device is connected and USB debugging is enabled")
        print("2. Navigate to the screen you want to map")
        print("3. For each element, you'll be asked to tap on it")
        print("4. The tool will capture coordinates and take screenshots")
        print("\nTo get accurate coordinates on your device:")
        print("- Enable Developer Options")
        print("- Enable 'Pointer Location' in Developer Options")
        print("- This will show real-time coordinates when you touch the screen")
        print("\n" + "="*50 + "\n")
        
        # Get current app info if not provided
        if not app_package:
            current_app = self.device.get_current_app()
            app_package = current_app.get("package", "unknown")
            print(f"Detected current app: {app_package}")
        
        self.current_screen = screen_name
        self.mapped_elements = []
        
        print(f"Mapping screen: {screen_name}")
        print(f"App package: {app_package}")
        print("\nAvailable element types:")
        
        element_types = [
            "button", "input_field", "text_view", "icon", "image",
            "checkbox", "radio_button", "dropdown", "link",
            "app_icon", "search_box", "login_button", "email_field",
            "password_field", "submit_button"
        ]
        
        for i, elem_type in enumerate(element_types, 1):
            print(f"{i:2d}. {elem_type}")
        
        print("\nCommands:")
        print("  'add' - Add a new element")
        print("  'list' - List mapped elements")
        print("  'save' - Save mapping to file")
        print("  'test' - Test mapped elements")
        print("  'help' - Show this help")
        print("  'quit' - Exit mapping session")
        print()
        
        while True:
            command = input("Enter command (add/list/save/test/help/quit): ").strip().lower()
            
            if command == "add":
                self._add_element_interactive()
            elif command == "list":
                self._list_mapped_elements()
            elif command == "save":
                self._save_mapping(app_package)
            elif command == "test":
                self._test_mapped_elements()
            elif command == "help":
                self._show_help()
            elif command == "quit":
                print("Exiting coordinate mapper...")
                break
            else:
                print(f"Unknown command: {command}")
    
    def _add_element_interactive(self):
        """Add a new element interactively."""
        print("\n--- Add New Element ---")
        
        # Get element name
        element_name = input("Element name (e.g., 'login_button'): ").strip()
        if not element_name:
            print("Element name cannot be empty")
            return
        
        # Get element display name
        display_name = input("Display name (e.g., '登录按钮'): ").strip()
        if not display_name:
            display_name = element_name.replace("_", " ").title()
        
        # Get element type
        print("Select element type:")
        element_types = list(ElementType)
        for i, elem_type in enumerate(element_types, 1):
            print(f"{i:2d}. {elem_type.value}")
        
        try:
            type_choice = int(input("Enter element type number: ")) - 1
            if 0 <= type_choice < len(element_types):
                element_type = element_types[type_choice]
            else:
                print("Invalid choice, using 'button'")
                element_type = ElementType.BUTTON
        except ValueError:
            print("Invalid input, using 'button'")
            element_type = ElementType.BUTTON
        
        # Get coordinates
        print("\nGetting coordinates...")
        print("Please tap on the element on your device (you have 10 seconds)")
        print("Make sure 'Pointer Location' is enabled in Developer Options")
        
        # Take before screenshot
        before_screenshot = f"{self.screenshots_dir}/before_{element_name}.png"
        self.device.get_screenshot(before_screenshot)
        print(f"Captured before screenshot: {before_screenshot}")
        
        # Wait for user to tap
        input("Press Enter when ready to capture coordinates (after you tap on the element)...")
        
        # Take after screenshot
        after_screenshot = f"{self.screenshots_dir}/after_{element_name}.png"
        self.device.get_screenshot(after_screenshot)
        print(f"Captured after screenshot: {after_screenshot}")
        
        # Get coordinates from user (since we can't capture tap events directly)
        print("\nEnter the coordinates where you tapped:")
        try:
            x = int(input("X coordinate: "))
            y = int(input("Y coordinate: "))
        except ValueError:
            print("Invalid coordinates, using center of screen")
            x, y = 540, 960  # Center of 1080x1920 screen
        
        # Get element size (optional)
        print("Enter element size (optional, press Enter to skip):")
        try:
            width = int(input("Width: ") or "0")
            height = int(input("Height: ") or "0")
        except ValueError:
            width, height = 0, 0
        
        # Get action type
        print("Select default action:")
        actions = ["tap", "input", "long_press", "swipe", "double_tap"]
        for i, action in enumerate(actions, 1):
            print(f"{i}. {action}")
        
        try:
            action_choice = int(input("Enter action number: ")) - 1
            if 0 <= action_choice < len(actions):
                action = actions[action_choice]
            else:
                action = "tap"
        except ValueError:
            action = "tap"
        
        # Get input data if action is 'input'
        input_data = None
        if action == "input":
            input_data = input("Default input text: ").strip()
        
        # Get wait time
        try:
            wait_time = float(input("Wait time after action (seconds): ") or "1.0")
        except ValueError:
            wait_time = 1.0
        
        # Create coordinate point
        coord_point = CoordinatePoint(
            x=x,
            y=y,
            element_name=element_name,
            element_type=element_type.value,
            description=display_name,
            timestamp=time.time()
        )
        
        # Add additional identifiers
        identifiers = {
            IdentifierType.COORDINATES: {
                "x": x,
                "y": y,
                "width": width,
                "height": height
            }
        }
        
        # Ask for text identifier
        text_identifier = input("Text identifier (optional): ").strip()
        if text_identifier:
            identifiers[IdentifierType.TEXT] = {"text": text_identifier, "partial": True}
        
        # Ask for resource ID
        resource_id = input("Resource ID (optional): ").strip()
        if resource_id:
            identifiers[IdentifierType.RESOURCE_ID] = {"id": resource_id}
        
        # Create UI element definition
        element_def = UIElementDefinition(
            name=display_name,
            element_type=element_type,
            identifiers=identifiers,
            action=action,
            input_data=input_data,
            wait_after_action=wait_time
        )
        
        self.mapped_elements.append(coord_point)
        
        print(f"\n✓ Added element: {element_name}")
        print(f"  Position: ({x}, {y})")
        print(f"  Type: {element_type.value}")
        print(f"  Action: {action}")
        if input_data:
            print(f"  Input: {input_data}")
        print()
    
    def _list_mapped_elements(self):
        """List all mapped elements."""
        if not self.mapped_elements:
            print("No elements mapped yet")
            return
        
        print(f"\n--- Mapped Elements for '{self.current_screen}' ---")
        print(f"Total: {len(self.mapped_elements)} elements")
        print()
        
        for i, element in enumerate(self.mapped_elements, 1):
            print(f"{i:2d}. {element.element_name}")
            print(f"    Name: {element.description}")
            print(f"    Position: ({element.x}, {element.y})")
            print(f"    Type: {element.element_type}")
            print(f"    Mapped: {time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(element.timestamp))}")
            print()
    
    def _save_mapping(self, app_package: str):
        """Save mapping to file."""
        if not self.mapped_elements:
            print("No elements to save")
            return
        
        print("\n--- Save Mapping ---")
        
        # Get save filename
        filename = input(f"Save filename (default: {self.current_screen}_mapping.json): ").strip()
        if not filename:
            filename = f"{self.current_screen}_mapping.json"
        
        if not filename.endswith('.json'):
            filename += '.json'
        
        # Create screen definition
        elements = {}
        for element in self.mapped_elements:
            # Create element definition
            element_def = UIElementDefinition(
                name=element.description,
                element_type=ElementType(element.element_type),
                identifiers={
                    IdentifierType.COORDINATES: {"x": element.x, "y": element.y}
                },
                app_package=app_package,
                screen=self.current_screen
            )
            elements[element.element_name] = element_def
        
        # Save coordinate mapping data
        mapping_data = {
            "screen_name": self.current_screen,
            "app_package": app_package,
            "mapping_timestamp": time.time(),
            "total_elements": len(self.mapped_elements),
            "elements": []
        }
        
        for element in self.mapped_elements:
            element_data = {
                "name": element.element_name,
                "display_name": element.description,
                "position": {"x": element.x, "y": element.y},
                "type": element.element_type,
                "timestamp": element.timestamp
            }
            mapping_data["elements"].append(element_data)
        
        # Save to file
        filepath = os.path.join(self.definition_manager.definitions_dir, filename)
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(mapping_data, f, ensure_ascii=False, indent=2)
        
        print(f"✓ Saved mapping to: {filepath}")
        print(f"  Total elements: {len(self.mapped_elements)}")
        print()
    
    def _test_mapped_elements(self):
        """Test mapped elements by tapping on them."""
        if not self.mapped_elements:
            print("No elements to test")
            return
        
        print("\n--- Testing Mapped Elements ---")
        print("This will tap on each mapped element to verify coordinates")
        print("Make sure you're on the correct screen!")
        
        confirm = input("Proceed with testing? (y/N): ").strip().lower()
        if confirm != 'y':
            return
        
        for i, element in enumerate(self.mapped_elements, 1):
            print(f"\nTesting element {i}/{len(self.mapped_elements)}: {element.element_name}")
            print(f"  Tapping at ({element.x}, {element.y})...")
            
            try:
                self.device.tap(element.x, element.y)
                print("  ✓ Tap successful")
                
                # Wait a bit between tests
                time.sleep(1)
                
                # Take screenshot after tap
                test_screenshot = f"{self.screenshots_dir}/test_{element.element_name}.png"
                self.device.get_screenshot(test_screenshot)
                print(f"  Screenshot saved: {test_screenshot}")
                
            except Exception as e:
                print(f"  ✗ Tap failed: {e}")
            
            # Small delay between tests
            time.sleep(0.5)
        
        print("\n✓ Testing complete!")
    
    def _show_help(self):
        """Show help information."""
        print("\n=== Coordinate Mapper Help ===")
        print("\nThis tool helps you map UI element coordinates by:")
        print("1. Taking screenshots before and after you tap on elements")
        print("2. Recording the coordinates where you tapped")
        print("3. Creating element definitions with multiple identification methods")
        print("\nBest practices:")
        print("- Enable 'Pointer Location' in Developer Options to see coordinates")
        print("- Take multiple screenshots to verify consistency")
        print("- Test mapped coordinates before saving")
        print("- Use both coordinate and text-based identifiers when possible")
        print("- Document what each element does")
        print("\nAlternative methods to get coordinates:")
        print("- Android Studio Layout Inspector")
        print("- uiautomatorviewer (in Android SDK)")
        print("- Appium Inspector")
        print("- Manual measurement from screenshots")
        print()


def create_predefined_login_mappings():
    """Create predefined login screen mappings for common apps."""
    mapper = CoordinateMapper()
    
    # Common login screen elements with typical coordinates
    # These would need to be adjusted based on actual app layout
    
    common_login_elements = [
        CoordinatePoint(200, 300, "email_field", "email_field", "邮箱地址输入框"),
        CoordinatePoint(400, 300, "password_field", "password_field", "密码输入框"),
        CoordinatePoint(600, 300, "login_button", "login_button", "登录按钮"),
        CoordinatePoint(300, 400, "forgot_password", "link", "忘记密码链接"),
        CoordinatePoint(500, 400, "register_link", "link", "注册账号链接"),
        CoordinatePoint(100, 500, "google_login", "button", "Google登录按钮"),
        CoordinatePoint(300, 500, "facebook_login", "button", "Facebook登录按钮"),
        CoordinatePoint(500, 500, "wechat_login", "button", "微信登录按钮"),
    ]
    
    # Save as template
    mapping_data = {
        "screen_name": "common_login_screen",
        "app_package": "generic.app",
        "description": "Common login screen element positions (adjust based on your app)",
        "elements": []
    }
    
    for element in common_login_elements:
        element_data = {
            "name": element.element_name,
            "display_name": element.description,
            "position": {"x": element.x, "y": element.y},
            "type": element.element_type,
            "recommended_identifiers": {
                "coordinates": {"x": element.x, "y": element.y},
                "text": element.description,
                "xpath": f"//android.widget.EditText[@hint='{element.description}']" if "field" in element.element_type else f"//android.widget.Button[@text='{element.description}']"
            }
        }
        mapping_data["elements"].append(element_data)
    
    # Save template
    filepath = os.path.join("ui_definitions", "common_login_template.json")
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(mapping_data, f, ensure_ascii=False, indent=2)
    
    print(f"Created common login screen template: {filepath}")
    print("Adjust these coordinates based on your specific app's login screen!")


def main():
    """Main function."""
    import sys
    
    if len(sys.argv) > 1:
        if sys.argv[1] == "create_template":
            create_predefined_login_mappings()
            return
        elif sys.argv[1] == "interactive":
            mapper = CoordinateMapper()
            screen_name = sys.argv[2] if len(sys.argv) > 2 else "custom_screen"
            app_package = sys.argv[3] if len(sys.argv) > 3 else None
            mapper.start_interactive_mapping(app_package, screen_name)
            return
    
    print("Coordinate Mapper Tool")
    print("Usage:")
    print("  python coordinate_mapper.py interactive [screen_name] [app_package]")
    print("  python coordinate_mapper.py create_template")
    print()
    print("Example:")
    print("  python coordinate_mapper.py interactive meituan_home com.sankuai.meituan")


if __name__ == "__main__":
    main()