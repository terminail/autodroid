from typing import Dict, Any, List, Optional
from dataclasses import dataclass, field
from enum import Enum


class TestScenario(Enum):
    COLD_START_APP = "cold_start_app"
    NAVIGATE_TO_GRID = "navigate_to_grid"
    FULL_FLOW = "full_flow"
    ELEMENT_VERIFICATION = "element_verification"


class DeviceType(Enum):
    EMULATOR = "emulator"
    PHYSICAL = "physical"


@dataclass
class DeviceConfig:
    device_id: str
    device_type: DeviceType = DeviceType.PHYSICAL
    platform: str = "Android"
    platform_version: Optional[str] = None
    host: str = "localhost"
    port: int = 4723


@dataclass
class AppConfig:
    package_name: str = "com.tdx.androidCCZQ"
    main_activity: str = ".ui.MainActivity"
    grid_activity: str = ".ui.grid.GridTradingActivity"
    launch_timeout: int = 30
    clear_data_on_start: bool = False


@dataclass
class NavigationStep:
    step_id: str
    page_id: str
    action_type: str
    target_element: Dict[str, str]
    description: str
    expected_page_after: Optional[str] = None
    timeout: int = 10
    retries: int = 2


@dataclass
class TestCase:
    scenario: TestScenario
    description: str
    device_config: DeviceConfig
    app_config: AppConfig
    navigation_steps: List[NavigationStep]
    test_data: Dict[str, Any] = field(default_factory=dict)
    expected_result: Dict[str, Any] = field(default_factory=dict)


COLD_START_TEST_DATA: Dict[str, Any] = {
    "app_name": "明佣宝",
    "package": "com.tdx.androidCCZQ",
    "activity": ".ui.MainActivity",
    "test_mode": True,
    "capture_screenshot": True,
    "capture_xml": True,
    "wait_for_idle": True,
    "idle_timeout": 3.0,
}

NAVIGATION_TEST_DATA: Dict[str, Any] = {
    "source_page": "home",
    "target_page": "entry-xzsg",
    "grid_trading_page": "grid-trading",
    "element_verification": {
        "home": {
            "required_elements": [
                {"resource-id": "entry-xzsg", "description": "条件单入口"}
            ]
        },
        "entry-xzsg": {
            "required_elements": [
                {"text": "6d13594be6254ee3c53cf86bd9783178", "description": "网格交易入口"}
            ]
        }
    },
    "capture_screenshot": True,
    "capture_xml": True,
}


def get_default_device_config() -> DeviceConfig:
    return DeviceConfig(
        device_id="TDCDU17905004388",
        device_type=DeviceType.PHYSICAL,
        platform="Android",
        platform_version=None,
        host="localhost",
        port=4723,
    )


def get_default_app_config() -> AppConfig:
    return AppConfig(
        package_name="com.tdx.androidCCZQ",
        main_activity=".ui.MainActivity",
        grid_activity=".ui.grid.GridTradingActivity",
        launch_timeout=30,
        clear_data_on_start=False,
    )


def get_cold_start_test_case() -> TestCase:
    device_config = get_default_device_config()
    app_config = get_default_app_config()
    
    return TestCase(
        scenario=TestScenario.COLD_START_APP,
        description="Cold start 明佣宝 app and verify home page loads",
        device_config=device_config,
        app_config=app_config,
        navigation_steps=[],
        test_data={
            **COLD_START_TEST_DATA,
            "verify_elements": [
                {"resource-id": "entry-xzsg", "description": "条件单入口按钮"}
            ]
        },
        expected_result={
            "app_started": True,
            "home_page_loaded": True,
            "elements_found": True,
        }
    )


def get_navigation_test_case() -> TestCase:
    device_config = get_default_device_config()
    app_config = get_default_app_config()
    
    return TestCase(
        scenario=TestScenario.NAVIGATE_TO_GRID,
        description="Navigate from home to grid trading page",
        device_config=device_config,
        app_config=app_config,
        navigation_steps=[
            NavigationStep(
                step_id="step_0",
                page_id="home",
                action_type="click",
                target_element={"resource-id": "entry-xzsg"},
                description="Click on 条件单 entry in home page",
                expected_page_after="entry-xzsg",
                timeout=10,
                retries=2,
            ),
            NavigationStep(
                step_id="step_1",
                page_id="entry-xzsg",
                action_type="click",
                target_element={"text": "6d13594be6254ee3c53cf86bd9783178"},
                description="Click on 网格交易 in entry-xzsg page",
                expected_page_after="grid-trading",
                timeout=10,
                retries=2,
            ),
        ],
        test_data=NAVIGATION_TEST_DATA,
        expected_result={
            "navigation_completed": True,
            "final_page": "grid-trading",
            "steps_completed": 2,
        }
    )


def get_full_flow_test_case() -> TestCase:
    cold_start_case = get_cold_start_test_case()
    navigation_case = get_navigation_test_case()
    
    return TestCase(
        scenario=TestScenario.FULL_FLOW,
        description="Full flow: cold start app and navigate to grid trading",
        device_config=cold_start_case.device_config,
        app_config=cold_start_case.app_config,
        navigation_steps=[
            NavigationStep(
                step_id="step_0",
                page_id="home",
                action_type="click",
                target_element={"resource-id": "entry-xzsg"},
                description="Click on 条件单 entry in home page",
                expected_page_after="entry-xzsg",
                timeout=10,
                retries=2,
            ),
            NavigationStep(
                step_id="step_1",
                page_id="entry-xzsg",
                action_type="click",
                target_element={"text": "6d13594be6254ee3c53cf86bd9783178"},
                description="Click on 网格交易 in entry-xzsg page",
                expected_page_after="grid-trading",
                timeout=10,
                retries=2,
            ),
        ],
        test_data={
            **COLD_START_TEST_DATA,
            **NAVIGATION_TEST_DATA,
            "full_flow": True,
        },
        expected_result={
            "app_started": True,
            "navigation_completed": True,
            "final_page": "grid-trading",
            "all_steps_completed": True,
        }
    )


def get_test_case_by_scenario(scenario: TestScenario) -> TestCase:
    scenario_map = {
        TestScenario.COLD_START_APP: get_cold_start_test_case,
        TestScenario.NAVIGATE_TO_GRID: get_navigation_test_case,
        TestScenario.FULL_FLOW: get_full_flow_test_case,
    }
    
    if scenario in scenario_map:
        return scenario_map[scenario]()
    
    raise ValueError(f"Unknown test scenario: {scenario}")


ALL_TEST_CASES: List[TestCase] = [
    get_cold_start_test_case(),
    get_navigation_test_case(),
    get_full_flow_test_case(),
]
