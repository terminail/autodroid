"""UI element definitions and recognition system for Android automation."""

import json
import time
from typing import Dict, List, Optional, Tuple, Any
from dataclasses import dataclass, asdict
from enum import Enum
import os


class ElementType(Enum):
    """UI element types."""
    BUTTON = "button"
    INPUT_FIELD = "input_field"
    TEXT_VIEW = "text_view"
    ICON = "icon"
    IMAGE = "image"
    CHECKBOX = "checkbox"
    RADIO_BUTTON = "radio_button"
    DROPDOWN = "dropdown"
    LINK = "link"
    APP_ICON = "app_icon"
    SEARCH_BOX = "search_box"
    LOGIN_BUTTON = "login_button"
    EMAIL_FIELD = "email_field"
    PASSWORD_FIELD = "password_field"
    SUBMIT_BUTTON = "submit_button"


class IdentifierType(Enum):
    """Element identification methods."""
    COORDINATES = "coordinates"
    TEXT = "text"
    XPATH = "xpath"
    RESOURCE_ID = "resource_id"
    CLASS_NAME = "class_name"
    DESCRIPTION = "description"
    IMAGE_TEMPLATE = "image_template"
    COLOR_PATTERN = "color_pattern"


@dataclass
class UIElementDefinition:
    """Definition of a UI element with multiple identification methods."""
    name: str
    element_type: ElementType
    identifiers: Dict[IdentifierType, Dict[str, Any]]  # Multiple ways to identify
    app_package: Optional[str] = None  # Specific app this element belongs to
    screen: Optional[str] = None  # Specific screen/page name
    action: str = "tap"  # Default action
    input_data: Optional[str] = None  # For input fields
    wait_after_action: float = 1.0  # Wait time after action
    fallback_identifiers: List[Dict[IdentifierType, Dict[str, Any]]] = None
    validation_text: Optional[str] = None  # Text to verify action succeeded
    
    def __post_init__(self):
        if self.fallback_identifiers is None:
            self.fallback_identifiers = []


@dataclass
class ScreenDefinition:
    """Definition of a complete app screen with all its elements."""
    name: str
    app_package: str
    elements: Dict[str, UIElementDefinition]
    screen_identifiers: List[Dict[IdentifierType, Dict[str, Any]]]  # How to recognize this screen
    expected_elements: List[str] = None  # Elements that should be present
    
    def __post_init__(self):
        if self.expected_elements is None:
            self.expected_elements = []


class UIDefinitionManager:
    """Manager for UI element definitions."""
    
    def __init__(self, definitions_dir: str = "ui_definitions"):
        """Initialize UI definition manager.
        
        Args:
            definitions_dir: Directory to store UI definitions
        """
        self.definitions_dir = definitions_dir
        self.screens: Dict[str, ScreenDefinition] = {}
        self.elements: Dict[str, UIElementDefinition] = {}
        self.app_configs: Dict[str, Dict[str, Any]] = {}
        
        # Create definitions directory if it doesn't exist
        os.makedirs(definitions_dir, exist_ok=True)
        
        # Load existing definitions
        self.load_all_definitions()
    
    def create_login_screen_example(self) -> ScreenDefinition:
        """Create example login screen definition."""
        login_elements = {
            "email_field": UIElementDefinition(
                name="邮箱地址输入框",
                element_type=ElementType.EMAIL_FIELD,
                identifiers={
                    IdentifierType.COORDINATES: {"x": 200, "y": 300, "width": 400, "height": 60},
                    IdentifierType.TEXT: {"text": "邮箱地址", "partial": True},
                    IdentifierType.RESOURCE_ID: {"id": "com.example.app:id/email_input"},
                    IdentifierType.XPATH: {"xpath": "//android.widget.EditText[@hint='邮箱地址']"}
                },
                action="input",
                input_data="user@example.com",
                wait_after_action=0.5,
                validation_text="邮箱格式正确"
            ),
            
            "password_field": UIElementDefinition(
                name="密码输入框",
                element_type=ElementType.PASSWORD_FIELD,
                identifiers={
                    IdentifierType.COORDINATES: {"x": 400, "y": 300, "width": 400, "height": 60},
                    IdentifierType.TEXT: {"text": "密码", "partial": True},
                    IdentifierType.RESOURCE_ID: {"id": "com.example.app:id/password_input"},
                    IdentifierType.XPATH: {"xpath": "//android.widget.EditText[@hint='密码']"}
                },
                action="input",
                input_data="password123",
                wait_after_action=0.5,
                validation_text="密码已输入"
            ),
            
            "login_button": UIElementDefinition(
                name="登录按钮",
                element_type=ElementType.LOGIN_BUTTON,
                identifiers={
                    IdentifierType.COORDINATES: {"x": 600, "y": 300, "width": 200, "height": 80},
                    IdentifierType.TEXT: {"text": "登录", "exact": True},
                    IdentifierType.RESOURCE_ID: {"id": "com.example.app:id/login_btn"},
                    IdentifierType.XPATH: {"xpath": "//android.widget.Button[@text='登录']"}
                },
                action="tap",
                wait_after_action=2.0,
                validation_text="登录成功"
            ),
            
            "forgot_password_link": UIElementDefinition(
                name="忘记密码链接",
                element_type=ElementType.LINK,
                identifiers={
                    IdentifierType.COORDINATES: {"x": 700, "y": 400, "width": 150, "height": 40},
                    IdentifierType.TEXT: {"text": "忘记密码", "partial": True},
                    IdentifierType.XPATH: {"xpath": "//android.widget.TextView[contains(@text,'忘记')]"}
                },
                action="tap",
                wait_after_action=1.0
            )
        }
        
        login_screen = ScreenDefinition(
            name="login_screen",
            app_package="com.example.app",
            elements=login_elements,
            screen_identifiers=[
                {IdentifierType.TEXT: {"text": "登录", "partial": True}},
                {IdentifierType.RESOURCE_ID: {"id": "com.example.app:id/login_container"}},
                {IdentifierType.XPATH: {"xpath": "//android.widget.LinearLayout[@id='login_form']"}}
            ],
            expected_elements=["email_field", "password_field", "login_button"]
        )
        
        return login_screen
    
    def create_meituan_home_screen_example(self) -> ScreenDefinition:
        """Create example Meituan home screen definition."""
        meituan_elements = {
            "search_box": UIElementDefinition(
                name="搜索框",
                element_type=ElementType.SEARCH_BOX,
                identifiers={
                    IdentifierType.COORDINATES: {"x": 100, "y": 150, "width": 880, "height": 60},
                    IdentifierType.TEXT: {"text": "搜索商家、商品", "partial": True},
                    IdentifierType.RESOURCE_ID: {"id": "com.sankuai.meituan:id/search_edit"},
                    IdentifierType.XPATH: {"xpath": "//android.widget.EditText[contains(@text,'搜索')]"}
                },
                action="input",
                input_data="附近美食",
                wait_after_action=1.0
            ),
            
            "food_category": UIElementDefinition(
                name="美食分类",
                element_type=ElementType.ICON,
                identifiers={
                    IdentifierType.COORDINATES: {"x": 135, "y": 350, "width": 90, "height": 90},
                    IdentifierType.TEXT: {"text": "美食", "exact": True},
                    IdentifierType.IMAGE_TEMPLATE: {"template": "meituan_food_icon.png", "threshold": 0.8}
                },
                action="tap",
                wait_after_action=2.0
            ),
            
            "nearby_restaurants": UIElementDefinition(
                name="附近商家",
                element_type=ElementType.TEXT_VIEW,
                identifiers={
                    IdentifierType.COORDINATES: {"x": 50, "y": 600, "width": 200, "height": 50},
                    IdentifierType.TEXT: {"text": "附近商家", "partial": True},
                    IdentifierType.XPATH: {"xpath": "//android.widget.TextView[contains(@text,'附近')]"}
                },
                action="tap",
                wait_after_action=1.5
            )
        }
        
        meituan_screen = ScreenDefinition(
            name="meituan_home",
            app_package="com.sankuai.meituan",
            elements=meituan_elements,
            screen_identifiers=[
                {IdentifierType.TEXT: {"text": "美团", "partial": True}},
                {IdentifierType.RESOURCE_ID: {"id": "com.sankuai.meituan:id/main_tab_home"}},
                {IdentifierType.XPATH: {"xpath": "//android.widget.TextView[@text='首页']"}}
            ],
            expected_elements=["search_box", "food_category"]
        )
        
        return meituan_screen
    
    def save_screen_definition(self, screen: ScreenDefinition, filename: str = None):
        """Save screen definition to JSON file.
        
        Args:
            screen: Screen definition to save
            filename: Optional custom filename
        """
        if filename is None:
            filename = f"{screen.name}.json"
        
        filepath = os.path.join(self.definitions_dir, filename)
        
        # Convert to serializable format
        screen_data = {
            "name": screen.name,
            "app_package": screen.app_package,
            "elements": {},
            "screen_identifiers": [],
            "expected_elements": screen.expected_elements
        }
        
        # Convert elements
        for elem_name, elem_def in screen.elements.items():
            elem_data = {
                "name": elem_def.name,
                "element_type": elem_def.element_type.value,
                "identifiers": {},
                "app_package": elem_def.app_package,
                "screen": elem_def.screen,
                "action": elem_def.action,
                "input_data": elem_def.input_data,
                "wait_after_action": elem_def.wait_after_action,
                "fallback_identifiers": elem_def.fallback_identifiers or [],
                "validation_text": elem_def.validation_text
            }
            
            # Convert identifiers
            for id_type, id_data in elem_def.identifiers.items():
                elem_data["identifiers"][id_type.value] = id_data
            
            screen_data["elements"][elem_name] = elem_data
        
        # Convert screen identifiers
        for identifier in screen.screen_identifiers:
            id_data = {}
            for id_type, id_value in identifier.items():
                id_data[id_type.value] = id_value
            screen_data["screen_identifiers"].append(id_data)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(screen_data, f, ensure_ascii=False, indent=2)
        
        print(f"Saved screen definition: {filepath}")
    
    def load_screen_definition(self, filename: str) -> ScreenDefinition:
        """Load screen definition from JSON file.
        
        Args:
            filename: JSON filename
            
        Returns:
            Screen definition
        """
        filepath = os.path.join(self.definitions_dir, filename)
        
        with open(filepath, 'r', encoding='utf-8') as f:
            screen_data = json.load(f)
        
        # Reconstruct elements
        elements = {}
        for elem_name, elem_data in screen_data["elements"].items():
            identifiers = {}
            for id_type_str, id_value in elem_data["identifiers"].items():
                id_type = IdentifierType(id_type_str)
                identifiers[id_type] = id_value
            
            element_def = UIElementDefinition(
                name=elem_data["name"],
                element_type=ElementType(elem_data["element_type"]),
                identifiers=identifiers,
                app_package=elem_data.get("app_package"),
                screen=elem_data.get("screen"),
                action=elem_data.get("action", "tap"),
                input_data=elem_data.get("input_data"),
                wait_after_action=elem_data.get("wait_after_action", 1.0),
                fallback_identifiers=elem_data.get("fallback_identifiers", []),
                validation_text=elem_data.get("validation_text")
            )
            elements[elem_name] = element_def
        
        # Reconstruct screen identifiers
        screen_identifiers = []
        for identifier_data in screen_data["screen_identifiers"]:
            identifier = {}
            for id_type_str, id_value in identifier_data.items():
                id_type = IdentifierType(id_type_str)
                identifier[id_type] = id_value
            screen_identifiers.append(identifier)
        
        screen_def = ScreenDefinition(
            name=screen_data["name"],
            app_package=screen_data["app_package"],
            elements=elements,
            screen_identifiers=screen_identifiers,
            expected_elements=screen_data.get("expected_elements", [])
        )
        
        return screen_def
    
    def load_all_definitions(self):
        """Load all screen definitions from the definitions directory."""
        if not os.path.exists(self.definitions_dir):
            return
        
        for filename in os.listdir(self.definitions_dir):
            if filename.endswith('.json'):
                try:
                    screen_def = self.load_screen_definition(filename)
                    self.screens[screen_def.name] = screen_def
                    
                    # Also add individual elements to global registry
                    for elem_name, elem_def in screen_def.elements.items():
                        global_name = f"{screen_def.name}.{elem_name}"
                        self.elements[global_name] = elem_def
                    
                    print(f"Loaded screen definition: {screen_def.name}")
                except Exception as e:
                    print(f"Failed to load {filename}: {e}")
    
    def get_element_definition(self, screen_name: str, element_name: str) -> Optional[UIElementDefinition]:
        """Get element definition by screen and element name.
        
        Args:
            screen_name: Screen name
            element_name: Element name
            
        Returns:
            Element definition or None
        """
        global_name = f"{screen_name}.{element_name}"
        return self.elements.get(global_name)
    
    def get_screen_definition(self, screen_name: str) -> Optional[ScreenDefinition]:
        """Get screen definition by name.
        
        Args:
            screen_name: Screen name
            
        Returns:
            Screen definition or None
        """
        return self.screens.get(screen_name)
    
    def create_workplan_from_screen(self, screen_name: str, task_description: str) -> Dict[str, Any]:
        """Create a workplan based on screen definition.
        
        Args:
            screen_name: Screen name
            task_description: Task description
            
        Returns:
            Workplan data
        """
        screen_def = self.get_screen_definition(screen_name)
        if not screen_def:
            raise ValueError(f"Screen definition not found: {screen_name}")
        
        workplan = {
            "name": f"{screen_name}_{task_description}",
            "description": f"Automated task on {screen_name}: {task_description}",
            "app_package": screen_def.app_package,
            "steps": []
        }
        
        # Create steps based on expected elements
        for i, elem_name in enumerate(screen_def.expected_elements):
            element_def = screen_def.elements[elem_name]
            
            step = {
                "step_id": str(i + 1),
                "description": f"Interact with {element_def.name}",
                "ui_elements": [
                    {
                        "name": element_def.name,
                        "type": element_def.element_type.value,
                        "identifier": self._convert_identifiers_for_workplan(element_def.identifiers),
                        "action": element_def.action,
                        "input_data": element_def.input_data,
                        "wait_time": element_def.wait_after_action
                    }
                ],
                "expected_result": element_def.validation_text or f"{element_def.name} interacted successfully",
                "fallback_action": None
            }
            
            workplan["steps"].append(step)
        
        return workplan
    
    def _convert_identifiers_for_workplan(self, identifiers: Dict[IdentifierType, Dict[str, Any]]) -> Dict[str, Any]:
        """Convert identifiers for workplan format."""
        workplan_identifiers = {}
        
        for id_type, id_data in identifiers.items():
            if id_type == IdentifierType.COORDINATES:
                workplan_identifiers["coordinates"] = {"x": id_data["x"], "y": id_data["y"]}
            elif id_type == IdentifierType.TEXT:
                workplan_identifiers["text"] = id_data["text"]
            elif id_type == IdentifierType.RESOURCE_ID:
                workplan_identifiers["id"] = id_data["id"]
            elif id_type == IdentifierType.XPATH:
                workplan_identifiers["xpath"] = id_data["xpath"]
        
        return workplan_identifiers
    
    def generate_coordinate_mapping_guide(self, screen_name: str) -> str:
        """Generate a guide for coordinate mapping of a screen.
        
        Args:
            screen_name: Screen name
            
        Returns:
            Coordinate mapping guide
        """
        screen_def = self.get_screen_definition(screen_name)
        if not screen_def:
            return f"Screen definition not found: {screen_name}"
        
        guide = []
        guide.append(f"=== Coordinate Mapping Guide for {screen_name} ===")
        guide.append(f"App Package: {screen_def.app_package}")
        guide.append("")
        guide.append("Element Coordinates:")
        guide.append("-" * 50)
        
        for elem_name, element_def in screen_def.elements.items():
            if IdentifierType.COORDINATES in element_def.identifiers:
                coords = element_def.identifiers[IdentifierType.COORDINATES]
                guide.append(f"{elem_name}:")
                guide.append(f"  Name: {element_def.name}")
                guide.append(f"  Position: ({coords.get('x', 'N/A')}, {coords.get('y', 'N/A')})")
                guide.append(f"  Size: {coords.get('width', 'N/A')} x {coords.get('height', 'N/A')}")
                guide.append(f"  Type: {element_def.element_type.value}")
                guide.append(f"  Action: {element_def.action}")
                if element_def.input_data:
                    guide.append(f"  Default Input: {element_def.input_data}")
                guide.append("")
        
        guide.append("To get accurate coordinates:")
        guide.append("1. Enable Developer Options on Android device")
        guide.append("2. Enable 'Pointer Location' in Developer Options")
        guide.append("3. Touch the screen to see real-time coordinates")
        guide.append("4. Use Android Studio Layout Inspector")
        guide.append("5. Use uiautomatorviewer tool")
        guide.append("6. Take screenshot and measure coordinates")
        
        return "\n".join(guide)


def main():
    """Example usage of UI definition manager."""
    # Create manager
    manager = UIDefinitionManager()
    
    # Create example screen definitions
    login_screen = manager.create_login_screen_example()
    meituan_screen = manager.create_meituan_home_screen_example()
    
    # Save definitions
    manager.save_screen_definition(login_screen)
    manager.save_screen_definition(meituan_screen)
    
    # Generate coordinate mapping guide
    print(manager.generate_coordinate_mapping_guide("login_screen"))
    print("\n" + "="*60 + "\n")
    print(manager.generate_coordinate_mapping_guide("meituan_home"))
    
    # Create workplan from screen
    workplan = manager.create_workplan_from_screen("login_screen", "自动登录")
    print("\nGenerated Workplan:")
    print(json.dumps(workplan, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()