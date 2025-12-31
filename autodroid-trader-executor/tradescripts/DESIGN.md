# Autodroid Trader - Tradescript Engine Design

## Overview

The Tradescript Engine is a data-driven, page-aware automation framework designed to execute trading plans on Android trading applications. The engine operates by **using ADB to dump page XML**, then parsing offline XML "scripts" containing custom `autodroid:*` attributes that describe trading operations, enabling complete separation of business logic, element location, and trading data.

## Core Architecture

### 1. Component Structure

```
tradescripts/                 # Main project root
│
├── config.yaml               # Central configuration file
│
├── tools/                    # Core framework library (flow → page → element architecture)
│   ├── flow.py               # Flow layer: Orchestrates execution across multiple pages
│   ├── page.py               # Page layer: Handles page recognition and execution
│   ├── element.py            # Element layer: Handles element matching and action execution
│   ├── u2device.py           # Device operations: ADB and uiautomator2 integration
│   ├── adb_operator.py       # ADB operation utilities (main entry point)
│   ├── interactive_cli.py    # Interactive CLI for manual testing
│   ├── adb_debug_tool.py     # ADB debugging utilities
│   ├── adb_dumper.py         # Page XML dumper
│   ├── check_namespaces.py   # Namespace validation
│   ├── verify_tradescript.py # Script verification
│   └── xml_analyzer.py       # XML analysis tools
│
├── apks/                     # Page definition library (offline XML with autodroid:* attributes)
│   ├── config.yaml           # Central APK configuration
│   ├── cn.com.gjzq.yjb2/     # apk_package directory
│   │   ├── config.yaml       # Package-level config
│   │   └── testflowa/        # flow_name directory
│   │       ├── config.yaml
│   │       └── home.xml      # ADB dump + manually edited with autodroid:*
│   └── com.tdx.androidCCZQ/  # Another apk_package
│       ├── config.yaml
│       ├── general/          # flow_name
│       │   ├── config.yaml
│       │   ├── home.xml
│       │   └── ...
│       └── netgrid-trading/  # flow_name
│           ├── config.yaml
│           └── ...
│
├── tests/                    # Test suite
│   └── test_api.py           # API tests
│
└── DESIGN.md                 # Architecture documentation
```

### 1.1 Three-Layer Architecture (flow → page → element)

The framework follows a hierarchical architecture that reflects the natural structure of Android UI automation:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Flow Layer (flow.py)                                │
│                         FlowManager                                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │  Responsibilities:                                                     │ │
│  │  - Orchestrates execution across multiple pages                       │ │
│  │  - Manages flow configuration and end pages                            │ │
│  │  - Coordinates page recognition and execution                          │ │
│  │  - Integrates ElementExecutor for action execution                     │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                         Page Layer (page.py)                           │ │
│  │  ┌──────────────────────┐  ┌──────────────────────┐                    │ │
│  │  │   PageMatcher        │  │   PageExecutor       │                    │ │
│  │  │  - Page recognition  │  │  - Page execution    │                    │ │
│  │  │  - Fingerprinting    │  │  - Step orchestration│                    │ │
│  │  └──────────────────────┘  └──────────────────────┘                    │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                        Element Layer (element.py)                       │ │
│  │  ┌──────────────────────┐  ┌──────────────────────┐                    │ │
│  │  │   ElementMatcher     │  │   ElementExecutor    │                    │ │
│  │  │  - Element matching  │  │  - Action execution  │                    │ │
│  │  │  - Bounds matching   │  │  - ADB operations    │                    │ │
│  │  └──────────────────────┘  └──────────────────────┘                    │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Architecture Principles:**

1. **Flow Layer (flow.py)**: 
   - `FlowManager` is the top-level orchestrator
   - Manages the entire execution flow across multiple pages
   - Coordinates page recognition and execution
   - Integrates `ElementExecutor` directly for action execution
   - No intermediate ActionExecutor needed - direct flow → element integration

2. **Page Layer (page.py)**:
   - `PageMatcher`: Handles page recognition and fingerprinting
   - `PageExecutor`: Handles page step execution
   - Separates recognition concerns from execution concerns
   - Works with both FlowManager and ElementMatcher

3. **Element Layer (element.py)**:
   - `ElementMatcher`: Handles element matching across devices
   - `ElementExecutor`: Handles action execution on elements
   - Provides the lowest-level interaction with device

**Key Design Decision:**
The architecture eliminates the intermediate `ActionExecutor` class that was previously used as a bridge between `FlowManager` and `ElementExecutor`. Instead, `FlowManager` now directly integrates `ElementExecutor`, reflecting the flow → page → element hierarchy more cleanly.

## Core Workflow: ADB-Based Approach

### 2.1 Design Architecture (Three-Layer: flow → page → element)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         APK Directory Structure                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  apk_dir (from config.yaml: "d:/git/.../apks")                               │
│    │                                                                         │
│    ├── config.yaml                                                           │
│    │                                                                         │
│    └── com.tdx.androidCCZQ (apk_package)                                     │
│          │                                                                   │
│          ├── config.yaml                                                     │
│          │                                                                   │
│          ├── general (flow_name)                                             │
│          │    ├── config.yaml                                                │
│          │    ├── home.xml                                                   │
│          │    ├── gong-gao.xml ← page_id = "gong-gao"                        │
│          │    ├── hang-qing.xml                                              │
│          │    └── ...                                                        │
│          │                                                                   │
│          └── netgrid-trading (flow_name)                                     │
│               ├── config.yaml                                                │
│               ├── home.xml                                                   │
│               └── ...                                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                    Flow Layer: Page Library Loading                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  FlowManager.load_and_build_fingerprints(apk_package, flow_name)            │
│    │                                                                         │
│    ├── apk_dir = self.apk_dir                                               │
│    ├── flow_dir = apk_dir / apk_package / flow_name                          │
│    │                                                                         │
│    └── For each XML file in flow_dir:                                        │
│          │                                                                   │
│          ├── page_id = xml_file.stem (filename without extension)            │
│          │                                                                   │
│          ├── Parse offline XML, collect elements with autodroid:action       │
│          │   └── action_elements: [{resource-id, text, content-desc,         │
│          │                       class, children}, ...]                      │
│          │       ⚠️ NO bounds stored (live page has real bounds)             │
│          │                                                                   │
│          └── Store: {page_id: PageFingerprint}                               │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                    Page Layer: Page Matching                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  FlowManager.identify_page(live_xml)                                         │
│    │                                                                         │
│    └── PageMatcher.identify_page(live_root)                                  │
│          │                                                                   │
│          └── For each offline page:                                          │
│                │                                                             │
│                ├── For each action_element in offline page:                  │
│                │   └── ElementMatcher.find_element(live_root, offline_elem)  │
│                │       → bool                                                │
│                │                                                             │
│                └── Calculate match_rate = matched_count / total_elements     │
│                                                                              │
│  ElementMatcher.find_element(live_root, offline_elem)                        │
│    │                                                                         │
│    ├── Priority 1: resource-id match (most reliable)                        │
│    │   └── match_by_resource_id(live_root, offline_elem["resource-id"])     │
│    │                                                                         │
│    ├── Priority 2: text match (second reliable)                             │
│    │   └── match_by_text(live_root, offline_elem["text"])                   │
│    │                                                                         │
│    ├── Priority 3: content-desc match                                       │
│    │   └── match_by_content_desc(live_root, offline_elem["content-desc"])   │
│    │                                                                         │
│    └── Priority 4: child features match (fallback)                          │
│        └── match_by_children(live_root, offline_elem["children"])         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                    Flow → Page → Element: Step Execution                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  FlowManager.execute_page_steps(page_id, live_xml)                           │
│    │                                                                         │
│    └── PageExecutor.execute_steps(page_id, live_root,                        │
│                                    FlowManager._execute_action_callback,    │
│                                    end_pages, refresh_page_callback)        │
│          │                                                                   │
│          └── For each pre-parsed step in page_fingerprints[page_id]:        │
│                │                                                             │
│                ├── Extract offline element attributes (no bounds)           │
│                │   └── {resource-id, text, content-desc, class, children}   │
│                │                                                             │
│                ├── ElementMatcher.find_element(live_root, offline_elem)     │
│                │       → live_elem                                          │
│                │       ⚠️ live_elem HAS bounds (from live XML)              │
│                │                                                             │
│                └── FlowManager._execute_action_callback(step, action,       │
│                                                       elem_info, live_elem)  │
│                      │                                                       │
│                      └── ElementExecutor.execute_action(step_info, live_elem)│
│                          │                                                   │
│                          └── Based on action type:                           │
│                              ├── click → Parse bounds, calculate center,    │
│                              │          ADB tap                              │
│                              ├── input → Clear field, input text via ADB    │
│                              ├── get_text → Read text, save to context      │
│                              └── ...                                         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Architecture Flow:**

1. **Flow Layer (FlowManager)**:
   - Orchestrates the entire execution flow
   - Manages page library loading and fingerprinting
   - Coordinates page recognition and execution
   - Directly integrates ElementExecutor for action execution

2. **Page Layer (PageMatcher + PageExecutor)**:
   - PageMatcher: Handles page recognition using fingerprinting
   - PageExecutor: Handles step execution on a page
   - Works with ElementMatcher to find elements

3. **Element Layer (ElementMatcher + ElementExecutor)**:
   - ElementMatcher: Finds elements in live XML using offline element info
   - ElementExecutor: Executes actions on matched elements
   - Provides the lowest-level interaction with device via ADB
```

### Key Differences from Appium Approach

| Aspect | Appium Approach (Old) | ADB Approach (New) |
| :--- | :--- | :--- |
| **Page Source** | `driver.page_source` via Appium RPC | `adb shell uiautomator dump` via ADB |
| **XML Format** | Appium-specific format | Standard UI Automator XML |
| **Dependency** | Appium server must be running | ADB only (lighter weight) |
| **Speed** | Slower (HTTP RPC overhead) | Faster (direct ADB call) |
| **Element Location** | Appium's find_element | ADB shell input or Appium fallback |

## Core Components and Workflows

### 2.1 ADB Driver Manager (`ADBManager`)

The ADB operations component of the framework.

- **Input**: Device ID, optional package name filter
- **Processing**: Execute ADB commands via subprocess to perform device operations
- **Output**: Return execution results from device operations
- **Advantages**: Lightweight, fast, no Appium server dependency
- **ADB Commands**: Supports tap, swipe, input text, dump page, app management, and more

```python
class ADBManager:
    def __init__(self, adb_path: Optional[str] = None):
        self.adb_path = adb_path or self._find_adb()

    def tap(self, device_id: str, x: int, y: int) -> bool:
        """Execute ADB tap at specified coordinates"""
        result = self.run_command([
            "-s", device_id, "shell", "input", "tap", str(x), str(y)
        ])
        return result.returncode == 0

    def dump_page_xml(self, device_id: str) -> str:
        """Capture current page UI hierarchy via ADB and return XML string"""
        self.run_command(["-s", device_id, "shell", "uiautomator", "dump"])
        result = self.run_command([
            "-s", device_id, "shell", "cat", "/sdcard/window_dump.xml"
        ])
        self.run_command(["-s", device_id, "shell", "rm", "/sdcard/window_dump.xml"])
        return result.stdout.strip() if result.returncode == 0 else ""
```

**Key Methods:**
| Method | Description |
|--------|-------------|
| `tap(device_id, x, y)` | Click at coordinates |
| `swipe(device_id, x1, y1, x2, y2)` | Swipe from one point to another |
| `input_text(device_id, text)` | Input text via ADB |
| `dump_page_xml(device_id)` | Get page UI hierarchy XML |
| `start_app(device_id, package, activity)` | Launch application |
| `cold_start_app(device_id, package, activity)` | Force stop and restart app |

### 2.2 Smart Page Matching Engine (`PageMatcher`)

The "eyes" and "brain" of the framework.

- **Input**:
  - Live page XML from ADB dump
  - Offline XML files with `autodroid:*` attributes from `apks/` directory
- **Processing**:
  1. Load all page fingerprints from APK directory on initialization
  2. For each offline XML, extract features (resource-ids, texts, classes, content-descs, clickables)
  3. Parse live XML from ADB dump and extract features
  4. Calculate weighted similarity score between live features and offline fingerprints
- **Fingerprint Algorithm**:
  - Extract features from offline XML elements
  - Use stable native attributes as page fingerprint features
  - **Weighted Matching**: Different features have different importance weights
  - **Selector-Based Matching** (Recommended):
    - Use `autodroid:fingerprint="true"` to mark unique page elements
    - Only marked elements are used for page matching (selector scheme)
    - Improves accuracy and reduces false positives
    - Example: Mark elements with unique resource-id, text, or combination of attributes
- **Output**: Return MatchResult with page_id, score, and matched elements
- **Advantages**: Supports multi-entry points, insensitive to non-critical UI changes

**Feature Weights:**
| Feature | Weight | Rationale |
|---------|--------|-----------|
| `resource-id` | 3.0 | Most reliable identifier |
| `content-desc` | 2.5 | Accessibility description |
| `text` | 2.0 | Visible text content |
| `class` | 1.0 | Element type |
| `clickable` | 0.5 | Interaction state |

```python
class PageMatcher:
    def __init__(self, apks_dir: str, match_threshold: float = 0.6):
        self.apks_dir = Path(apks_dir)
        self.match_threshold = match_threshold
        self.page_fingerprints: Dict[str, PageFingerprint] = {}
        self._load_all_pages()

    def identify_current_page(self, live_xml_source: str) -> MatchResult:
        live_root = ET.fromstring(live_xml_source.encode("utf-8"))
        live_features = self._extract_features(live_root)

        best_match_id = None
        best_score = 0.0
        best_matched_elements = []

        for page_id, fingerprint in self.page_fingerprints.items():
            score, matched_elements = self._calculate_similarity(
                live_features, fingerprint.features
            )
            if score > best_score:
                best_score = score
                best_match_id = page_id
                best_matched_elements = matched_elements

        if best_match_id and best_score >= self.match_threshold:
            return MatchResult(page_id=best_match_id, score=best_score, ...)
        else:
            return MatchResult(page_id=None, score=best_score, ...)
```

### 2.3 Data-Driven Executor (`ActionHandler` + `DataDrivenExecutor`)

The "hands" of the framework.

**ActionHandler**: Handles individual element operations.

**DataDrivenExecutor**: Orchestrates step-by-step execution on a page.

- **Input**:
  - Current live page XML from ADB dump
  - Matched offline XML with `autodroid:*` attributes
  - External injected test data (`Dict`)
  - Runtime context (`Context`)
- **Processing**:
  1. Parse offline XML, extract elements with `autodroid:*` attributes
  2. Collect steps sorted by `autodroid:step`
  3. For each step element:
     - Extract native attributes from offline XML
     - Resolve value from `name` (test_data lookup) or `value` (hardcoded)
     - Execute action via ActionHandler
     - Optionally save result to context via `save_to`
  4. Execute operations on real device via ADB
- **Output**: Drive the device to complete all steps, update runtime context

```python
class ActionHandler:
    # Supported Action Types
    ACTION_CLICK = "click"
    ACTION_INPUT = "input"
    ACTION_SELECT = "select"
    ACTION_GET_TEXT = "get_text"
    ACTION_WAIT = "wait"
    ACTION_SWIPE = "swipe"
    ACTION_VERIFY = "verify"
    ACTION_PRESS_KEY = "press_key"

    def execute_action(
        self,
        action: str,
        element: ET.Element,
        value: Optional[str] = None,
        save_to: Optional[str] = None,
        description: str = ""
    ) -> Tuple[bool, Optional[str]]:
        if action == self.ACTION_CLICK:
            bounds = self.get_bounds_from_xml(element)
            x, y = self.calculate_center(bounds)
            return self.adb_click(x, y), None
        elif action == self.ACTION_INPUT:
            if value:
                self.adb_clear_input(x, y)
                return self.adb_input_text(value), None
        elif action == self.ACTION_GET_TEXT:
            text = element.get("text", "")
            return text is not None, text
        elif action == self.ACTION_SWIPE:
            return self.execute_swipe(element, value), None
        elif action == self.ACTION_PRESS_KEY:
            return self.execute_press_key(value), None
        # ... other actions
```

### 2.4 Tradeflow Coordinator (`TradeflowCoordinator`)

The "commander" of the framework.

- **Workflow**:
  1. According to configuration
  2. Call `ADBDumpManager` to capture current page XML via ADB
  3. Call `PageMatcher` to confirm current page
  4. Load corresponding offline XML, call `DataDrivenExecutor` to execute
  5. After steps are executed, call `ADBDumpManager` + `PageMatcher` again to perceive new page
  6. Loop steps 2-5 until the page perceived by the framework has no unexecuted steps, or reaches preset endpoint
- **Features**: Implements self-driven page flow, completely driven by the actual jump logic of the application, without hard-coding "page A is followed by page B" in the script

```python
class TradeflowCoordinator:
    def __init__(
        self,
        adb_manager: ADBManager,
        device_id: str,
        apks_dir: str,
        page_matcher: PageMatcher,
        data_executor: DataDrivenExecutor,
        wait_timeout: int = 30
    ):
        self.adb_manager = adb_manager
        self.device_id = device_id
        self.apks_dir = Path(apks_dir)
        self.page_matcher = page_matcher
        self.data_executor = data_executor
        self.wait_timeout = wait_timeout

    def execute_tradeflow(self, tradeflow: List[Dict[str, Any]]) -> List[ExecutionResult]:
        results = []
        for testcase in tradeflow:
            page_id = testcase["page_id"]
            test_data = testcase.get("data", {})
            result = self.data_executor.execute_page_flow(page_id, test_data)
            results.append(result)
        return results
```

### 2.5 Test Runner Framework (`TradeScriptTestRunner`)

The main entry point for executing test scenarios.

- **Function**: Coordinate the execution of test scenarios, providing test lifecycle management
- **Process**:
  1. Initialize ADBManager, PageMatcher, DataDrivenExecutor, and TradeflowCoordinator
  2. Execute test cases based on TestScenario enum
  3. Generate test reports in JSON format

```python
class TradeScriptTestRunner:
    def __init__(
        self,
        apks_dir: str = "app/src/main/assets/apks",
        output_dir: Optional[str] = None,
    ):
        self.apks_dir = Path(apks_dir)
        self.output_dir = Path(output_dir) if output_dir else Path("test_output")
        self.adb_manager = ADBManager()
        self.page_matcher: Optional[PageMatcher] = None
        self.data_executor: Optional[DataDrivenExecutor] = None
        self.flow_coordinator: Optional[TradeflowCoordinator] = None
        self.test_results: List[TestResult] = []

    def initialize(self, device_id: str) -> bool:
        # Initialize all components
        self.page_matcher = PageMatcher(apks_dir=str(self.apks_dir))
        self.data_executor = DataDrivenExecutor(adb_manager, device_id, str(self.apks_dir))
        self.flow_coordinator = TradeflowCoordinator(page_matcher, data_executor)
        return True

    def run_test_case(self, test_case: TestCase) -> TestResult:
        # Execute a single test case
        pass

    def run_all_tests(self) -> Dict[str, TestResult]:
        # Execute all registered test cases
        pass

    def generate_report(self, output_path: str = "test_report.json") -> None:
        # Generate JSON test report
        pass
```

**Supported Test Scenarios:**
| Scenario | Description |
|----------|-------------|
| `COLD_START_APP` | Cold start application and verify home page |
| `NAVIGATE_TO_GRID` | Navigate from home to grid trading page |
| `FULL_FLOW` | Complete flow: cold start + navigation |
| `ELEMENT_VERIFICATION` | Verify element presence on page |

## Core: `autodroid:*` Custom Attribute System Design

### 3.1 Design Philosophy

The `autodroid:*` attribute system is a **"declarative" test description language**. It allows test designers to declare an element's **behavioral intent**, **data requirements**, and **verification logic** in offline XML in the form of tags (attributes), thereby completely separating test logic from code.

### 3.2 Attribute Classification and Overview

All `autodroid:*` attributes can be divided into four major categories according to their core functions:

| Category | Core Purpose | Key Attributes |
| :--- | :--- | :--- |
| **2. Process and Steps** | Orchestrate operation order and process control | `autodroid:step`, `autodroid:action` |
| **3. Data and Variables** | Implement data-driven and state transfer | `autodroid:name`, `autodroid:value`, `autodroid:save_to` |
| **4. Elements and Location** | Provide location assistance and verification information | `autodroid:desc` (implicit: depends on native attributes like `resource-id`, `text`) |

### 3.3 Detailed Attribute Definition Table

The following table is the complete definition of the `autodroid:*` attribute set, which is the **"grammar manual"** of the framework.

| Attribute Name | Scope | Value Type | Required | Description and Purpose | Example |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`autodroid:step`** | Operable elements | Integer | **No** (auto-generated if only `action` present) | **Step sequence number**. Defines the **execution order** of operations within the same page. If not specified and only `autodroid:action` is present, steps are auto-numbered sequentially (1, 2, 3...). | `autodroid:step="1"` |
| **`autodroid:action`** | Operable elements | String | **Yes** (required for actionable elements) | **Action to execute**. The "verb" of the framework, determines the specific operation on the element. See [Action Type Table]. | `autodroid:action="click"` |
| **`autodroid:fingerprint`** | Any element | String | No | **Page fingerprint marker**. When set to `"true"`, marks this element as a unique identifier for the page. Only elements marked with `autodroid:fingerprint="true"` are used for page matching (selector scheme), significantly improving recognition accuracy. Recommended for elements with unique `resource-id`, `text`, or combination of attributes. | `autodroid:fingerprint="true"` |
| **`autodroid:name`** | Elements requiring data | String | No | **Data field key name**. Provides key for `input`, `select` etc. actions, used to dynamically find and fill values from external test data (`test_data`). **Mutually exclusive with `value`, higher priority.** | `autodroid:name="username"` |
| **`autodroid:value`** | Elements requiring data | String | No | **Hardcoded default value**. When `name` is not set, or `name` is not matched in external data, this value will be used. Implements **flexible fallback mechanism**. | `autodroid:value="test@example.com"` |
| **`autodroid:save_to`** | Elements that can output data | String | No | **Runtime variable storage key**. Mainly used for `action="get_text"`, saves captured text to **runtime context** (`context`) for reference by subsequent steps or final assertion. | `autodroid:save_to="product_price"` |
| **`autodroid:desc`** | Any element | String | No | **Human description**. Only used to improve readability and maintainability of XML files, **does not affect framework execution logic**. | `autodroid:desc="Click login button"` |

### 3.4 Core Action Types (`autodroid:action`) Detailed

| Action Value | Applicable Elements | Required Data Attributes | Behavior Description |
| :--- | :--- | :--- | :--- |
| **`click`** | `Button`, `TextView` etc. | None | Perform click operation on element. |
| **`input`** | `EditText` etc. input fields | `name` or `value` | 1. Clear input field; 2. Input text determined by `name` or `value`. |
| **`select`** | `Spinner` etc. dropdowns | `name` or `value` or `option` | 1. Click to expand dropdown; 2. Select option matching specified value. |
| **`get_text`** | Any text elements | `save_to` (recommended) | Get the `text` attribute value of element, and store in `context`. |
| **`wait`** | Any (or virtual element) | `value` (as wait seconds) | Force wait for specified time. Commonly used for step-to-step waiting. |
| **`swipe`** | `ScrollView` etc. | `value` (e.g. `up:0.5`) | Perform swipe operation within element. |
| **`verify`** | Any | `value` (expected value) | Verify element's property (e.g. text) matches expected value, used for assertion. |
| **`press_key`** | Any (or virtual element) | `value` (e.g. `BACK`, `ENTER`) | Press hardware/soft key using ADB. Used for navigation actions. |
| **`wait_for_user`** | Any (or virtual element) |  | Force wait for User Intervention. Commonly used for security verification or manual operation. |
| **`long_click`** | `Button`, `TextView` etc. | None | Perform long click operation on element. |
| **`scroll`** | `ScrollView`, `RecyclerView` etc. | `value` (e.g. `down`, `up`) | Scroll the container element in specified direction. |

### 3.5 Data Parsing Priority Logic

This is the core logic of the framework's data-driven capability. When executing `input` or `select` actions, the value to use is determined in the following order:

```
Start parsing step data
├─ Does element have autodroid:name?
│  ├─ Yes → Look up corresponding key-value in external test_data
│  │         ├─ Found? → ✅ Use external data
│  │         └─ Not found? → Does element have autodroid:value?
│  └─ No → Does element have autodroid:value?
│           ├─ Yes → ✅ Use hardcoded default value
│           └─ No → ❌ Cannot get data, record error
```

**Formula expression**:
`Final value = test_data.get(autodroid:name) or autodroid:value`

## Complete Configuration and Code Implementation

### 4.1 Config Format (apks/config.yaml)

```yaml
# APK 页面库根目录配置
apk_dir: "d:/git/autodroid/autodroid-trader-executor/app/src/main/assets/apks"
```

### 4.2 Page XML Definition Example (`apks/com.tdx.androidCCZQ/general/home.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<hierarchy>
    
    <!-- Page fingerprint: Unique element that identifies this page -->
    <!-- Mark with autodroid:fingerprint="true" for accurate page recognition -->
    <node
        index="0"
        text="川财APP首页"
        resource-id=""
        class="android.webkit.WebView"
        package="com.tdx.androidCCZQ"
        autodroid:fingerprint="true" />
    
    <!-- 步骤1: 输入股票代码. 使用 'name' 从外部数据获取值，fallback 到 'value' -->
    <android.widget.EditText
        index="0"
        text=""
        resource-id="com.tdx.androidCCZQ:id/stock_code"
        class="android.widget.EditText"
        autodroid:step="1"
        autodroid:action="input"
        autodroid:name="stock_code"
        autodroid:value="000001"
        autodroid:desc="Input stock code" />
    
    <!-- 步骤2: 输入交易金额. 通常从外部数据读取，无 fallback 值 -->
    <android.widget.EditText
        resource-id="com.tdx.androidCCZQ:id/trade_amount"
        autodroid:step="2"
        autodroid:action="input"
        autodroid:name="trade_amount"
        autodroid:desc="Input trade amount" />
    
    <!-- 步骤3: 点击买入按钮. 点击后应用会跳转到确认页面 -->
    <android.widget.Button
        resource-id="com.tdx.androidCCZQ:id/buy_btn"
        text="Buy"
        autodroid:step="3"
        autodroid:action="click"
        autodroid:desc="Click buy button" />
        
</hierarchy>
```

### 4.3 Simple Page with Single Action (autodroid:step optional)

When a page has only one action, `autodroid:step` can be omitted - it will be auto-numbered as 1.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<hierarchy>
    
    <!-- Single click action, step will be auto-numbered as 1 -->
    <android.widget.TextView
        text="公告"
        class="android.widget.TextView"
        autodroid:action="click"
        autodroid:desc="点击公告" />
        
</hierarchy>
```

### 4.2 External Trading Data (`data/trade_plans.json`)

```json
[
  {
    "plan_id": "plan_001",
    "data": {
      "stock_code": "000001",
      "trade_amount": "1000",
      "trade_type": "buy"
    }
  },
  {
    "plan_id": "plan_002",
    "data": {
      "stock_code": "600036",
      "trade_amount": "500",
      "trade_type": "sell"
    }
  }
]
```

### 4.3 Trading Flow Configuration: Define by Target Rather Than Starting Point (`flows/flow_buy_stock.json`)

Each trading flow defines its **target starting page** and required **trading data**. No longer assumes app is in a certain state.

```json
{
  "flow_id": "flow_buy_stock",
  "name": "Buy specific stock flow",
  "target_start_page": "netgrid_trading_page", // This flow wants to start from trading page
  "required_data": { // External data required by this flow
    "stock_code": "000001",
    "trade_amount": "1000"
  },
  "description": "Buy a specific stock from trading page"
}
```

### 4.4 General Navigation Instruction Library: "Subway Map" in Application (`navigation/global_nav_actions.xml`)

This file defines how to navigate between major pages in the app, without relying on logout. It consists of a series of general, low-risk navigation actions.

```xml
<hierarchy>
  <!-- Rule 1: If currently on any page, click physical back button until back to home page -->
  <android.view.View
    autodroid:rule="current_page != 'home_page'"
    autodroid:action="press_key"
    autodroid:value="BACK"
    autodroid:max_retry="10" />
    
  <!-- Rule 2: If on profile page, use bottom navigation to switch to home page -->
  <android.widget.Button
    resource-id="com.tdx.androidCCZQ:id/tab_home"
    autodroid:rule="current_page == 'profile_page'"
    autodroid:action="click"
    autodroid:desc="Click bottom navigation 'Home'" />
    
  <!-- Rule 3: If on trade result page, click "Back to Home" dedicated button -->
  <android.widget.Button
    text="Return to Home"
    autodroid:rule="current_page == 'trade_result_page'"
    autodroid:action="click" />
    
  <!-- More general navigation rules... -->
</hierarchy>
```

### 4.5 Main Orchestrator Configuration (`orchestrator_config.json`)

This file connects everything, defining the execution order and strategy of the test suite.

```json
{
  "trading_suite_name": "Daily Trading Operations",
  "global_navigation_file": "navigation/global_nav_actions.xml",
  "flow_execution_order": [
    "flow_buy_stock",
    "flow_sell_stock"
  ],
  "flow_retry_policy": {
    "max_navigation_attempts": 3,
    "fail_test_on_navigation_failure": false
  }
}
```

### 4.6 Framework Core Library (`lib/tradescript_engine.py`)

```python
import xml.etree.ElementTree as ET
import time
import json
import os
import subprocess
from appium import webdriver
from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import NoSuchElementException, TimeoutException
from pathlib import Path
from typing import Optional, List, Dict, Tuple
from core.tradescript.adb_driver import ADBManager
from core.tradescript.page_matcher import PageMatcher
from core.tradescript.data_executor import DataDrivenExecutor
from core.tradescript.flow_coordinator import TradeflowCoordinator


class ADBManager:
    """Comprehensive ADB operations manager for device control"""
    
    def __init__(self, device_id: Optional[str] = None):
        self.device_id = device_id
        self.adb_path = "adb"
        
    def set_device_id(self, device_id: str) -> None:
        """Set target device ID"""
        self.device_id = device_id
        
    def dump_page(self, output_path: str = "temp_dump.xml") -> str:
        """Execute ADB dump and return XML content"""
        subprocess.run(
            [self.adb_path, "-s", self.device_id, "shell", "uiautomator", "dump"],
            check=True
        )
        subprocess.run(
            [self.adb_path, "-s", self.device_id, "pull", "/sdcard/window_dump.xml", output_path],
            check=True
        )
        with open(output_path, "r", encoding="utf-8") as f:
            return f.read()
        
    def tap(self, x: int, y: int) -> bool:
        """Tap at coordinates"""
        subprocess.run([
            self.adb_path, "-s", self.device_id, "shell", "input", "tap", str(x), str(y)
        ])
        return True
        
    def input_text(self, text: str) -> bool:
        """Input text via ADB"""
        subprocess.run([
            self.adb_path, "-s", self.device_id, "shell", "input", "text", text
        ])
        return True
        
    def press_key(self, key: str) -> bool:
        """Press key (BACK, HOME, etc.)"""
        subprocess.run([
            self.adb_path, "-s", self.device_id, "shell", "input", "keyevent", key
        ])
        return True
        
    def swipe(self, x1: int, y1: int, x2: int, y2: int, duration: int = 300) -> bool:
        """Swipe from (x1, y1) to (x2, y2)"""
        subprocess.run([
            self.adb_path, "-s", self.device_id, "shell", "input", "swipe",
            str(x1), str(y1), str(x2), str(y2), str(duration)
        ])
        return True
        
    def get_current_package(self) -> str:
        """Get current foreground package"""
        result = subprocess.run([
            self.adb_path, "-s", self.device_id, "shell", "dumpsys", "window"
        ], capture_output=True, text=True)
        for line in result.stdout.split('\n'):
            if 'mCurrentFocus' in line:
                focus = line.split('=')[-1].strip()
                return focus.split()[0] if focus != 'null' else ''
        return ''
        
    def cleanup(self, local_path: str = "temp_dump.xml"):
        """Clean up temporary file"""
        if os.path.exists(local_path):
            os.remove(local_path)


class PageMatcher:
    """Smart page matching using ADB XML with weighted feature matching"""
    
    def __init__(self, apks_dir: str):
        self.apks_dir = Path(apks_dir)
        self.page_fingerprints: Dict[str, Dict] = {}
        self.match_threshold = 0.6
        self._load_page_fingerprints()
        
    def _load_page_fingerprints(self):
        """Load all page XML, build fingerprint library"""
        self.page_fingerprints = {}
        apk_dirs = [d for d in self.apks_dir.iterdir() if d.is_dir()]
        
        for apk_dir in apk_dirs:
            flow_dirs = [f for f in apk_dir.iterdir() if f.is_dir()]
            for flow_dir in flow_dirs:
                xml_files = list(flow_dir.glob("*.xml"))
                for xml_file in xml_files:
                    page_id = xml_file.stem
                    self.page_fingerprints[page_id] = self._build_fingerprint(xml_file)
                    
    def _build_fingerprint(self, xml_path: Path) -> Dict:
        """Build fingerprint from XML file"""
        tree = ET.parse(xml_path)
        root = tree.getroot()
        elements = []
        for elem in root.iter():
            if elem.tag == "hierarchy":
                continue
            autodroid_attrs = {
                k: v for k, v in elem.attrib.items() if k.startswith('autodroid:')
            }
            if autodroid_attrs:
                elements.append({
                    'tag': elem.tag,
                    'resource-id': elem.get('resource-id', ''),
                    'text': elem.get('text', ''),
                    'class': elem.get('class', ''),
                    'bounds': elem.get('bounds', ''),
                    'autodroid': autodroid_attrs
                })
        return {'elements': elements, 'xml_path': str(xml_path)}
        
    def identify_current_page(self, live_xml: str) -> Tuple[Optional[str], float, List[str]]:
        """Identify current page: match live XML against offline fingerprints"""
        best_match_id, best_score = None, 0
        matched_elements = []
        live_root = ET.fromstring(live_xml.encode('utf-8'))
        live_features = self._extract_features(live_root)
        
        for page_id, offline_fingerprint in self.page_fingerprints.items():
            score, matched = self._calculate_similarity(
                offline_fingerprint['elements'], 
                live_features
            )
            if score > best_score:
                best_score, best_match_id, matched_elements = score, page_id, matched
                
        if best_score >= self.match_threshold:
            return best_match_id, best_score, matched_elements
        return None, best_score, []
        
    def _extract_features(self, root: ET.Element) -> List[Dict]:
        """Extract features from live XML"""
        features = []
        for elem in root.iter():
            if elem.tag == "hierarchy":
                continue
            features.append({
                'tag': elem.tag,
                'resource-id': elem.get('resource-id', ''),
                'text': elem.get('text', ''),
                'class': elem.get('class', ''),
                'bounds': elem.get('bounds', '')
            })
        return features
        
    def _calculate_similarity(self, offline_elements: List[Dict], live_elements: List[Dict]) -> Tuple[float, List[str]]:
        """Calculate similarity with weighted matching"""
        if not offline_elements:
            return 0.0, []
            
        total_weight = 0.0
        matched_weight = 0.0
        matched_elements = []
        
        for offline_elem in offline_elements:
            weight = 1.0
            if offline_elem.get('autodroid', {}).get('autodroid:step'):
                weight = 2.0
            total_weight += weight
            
            if self._element_match(offline_elem, live_elements):
                matched_weight += weight
                matched_elements.append(offline_elem.get('text', offline_elem.get('resource-id', '')))
                
        return matched_weight / total_weight if total_weight > 0 else 0.0, matched_elements
        
    def _element_match(self, offline_elem: Dict, live_elements: List[Dict]) -> bool:
        """Check if offline element matches any live element"""
        for live_elem in live_elements:
            if (offline_elem['resource-id'] == live_elem['resource-id'] or
                (offline_elem['text'] and offline_elem['text'] == live_elem['text']) or
                (offline_elem['class'] == live_elem['class'])):
                return True
        return False


class ActionHandler:
    """Handles execution of individual actions"""
    
    def __init__(self, adb_manager: ADBManager):
        self.adb = adb_manager
        
    def execute(self, action: str, element_info: Dict, data: Optional[Dict] = None) -> bool:
        """Execute action on element"""
        native_attrs = element_info.get('native', {})
        bounds = native_attrs.get('bounds', '')
        
        if bounds:
            coords = self._parse_bounds(bounds)
            x, y = coords[0] + (coords[2] - coords[0]) // 2, coords[1] + (coords[3] - coords[1]) // 2
        else:
            return False
            
        action_handlers = {
            'click': lambda: self.adb.tap(x, y),
            'long_click': lambda: self._adb_long_click(x, y),
            'input': lambda: self.adb.input_text(self._get_value(element_info, data)),
            'swipe': lambda: self._adb_swipe(x, y, element_info, data),
            'press_key': lambda: self.adb.press_key(element_info.get('value', 'BACK')),
        }
        
        handler = action_handlers.get(action)
        if handler:
            return handler()
        return False
        
    def _parse_bounds(self, bounds: str) -> List[int]:
        """Parse bounds string to coordinates"""
        import re
        match = re.search(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds)
        if match:
            return list(map(int, match.groups()))
        return [0, 0, 0, 0]
        
    def _get_value(self, element_info: Dict, data: Optional[Dict] = None) -> str:
        """Get value for input action"""
        name = element_info.get('name')
        value = element_info.get('value', '')
        if name and data and name in data:
            return str(data[name])
        return value
        
    def _adb_long_click(self, x: int, y: int, duration: int = 1000) -> bool:
        """Perform long click using swipe trick"""
        subprocess.run([
            "adb", "-s", self.adb.device_id, "shell", "input", "swipe",
            str(x), str(y), str(x), str(y), str(duration)
        ])
        return True
        
    def _adb_swipe(self, x: int, y: int, element_info: Dict, data: Optional[Dict] = None) -> bool:
        """Perform swipe operation"""
        direction = element_info.get('value', 'up:0.5')
        parts = direction.split(':')
        if len(parts) == 2:
            direction_type, ratio = parts[0], float(parts[1])
            offset = int(300 * ratio)
            if direction_type == 'up':
                self.adb.swipe(x, y, x, y - offset)
            elif direction_type == 'down':
                self.adb.swipe(x, y, x, y + offset)
            return True
        return False


class DataDrivenExecutor:
    """Execute operations based on ADB XML matching with data-driven approach"""
    
    def __init__(self, adb_manager: ADBManager, device_id: str, apks_dir: str):
        self.adb = adb_manager
        self.device_id = device_id
        self.apks_dir = Path(apks_dir)
        self.page_matcher = PageMatcher(apks_dir)
        self.action_handler = ActionHandler(adb_manager)
        self.trade_data: Dict = {}
        self.runtime_context: Dict = {}
        self.current_page_id: Optional[str] = None
        
    def execute_page(self, page_file_name: str, live_xml: str) -> bool:
        """Execute all steps for specified page using live XML"""
        offline_xml_path = self.apks_dir / f"{page_file_name}.xml"
        if not offline_xml_path.exists():
            print(f"Page XML not found: {offline_xml_path}")
            return False
            
        tree = ET.parse(offline_xml_path)
        offline_root = tree.getroot()
        live_root = ET.fromstring(live_xml.encode('utf-8'))
        
        steps = self._collect_steps(offline_root)
        for step in steps:
            self._execute_step(step, live_root)
        return True
        
    def _collect_steps(self, root: ET.Element) -> List[Dict]:
        """Collect and sort steps by autodroid:step"""
        steps = []
        for elem in root.iter():
            if elem.tag == "hierarchy":
                continue
            action = elem.get('autodroid:action')
            if action:
                step_info = {
                    'step': int(elem.get('autodroid:step', len(steps) + 1)),
                    'action': action,
                    'element': {
                        'resource-id': elem.get('resource-id', ''),
                        'text': elem.get('text', ''),
                        'class': elem.get('class', ''),
                        'bounds': elem.get('bounds', ''),
                        'name': elem.get('autodroid:name'),
                        'value': elem.get('autodroid:value'),
                        'save_to': elem.get('autodroid:save_to')
                    }
                }
                steps.append(step_info)
        return sorted(steps, key=lambda s: s['step'])
        
    def _execute_step(self, step_info: Dict, live_root: ET.Element):
        """Execute single step using offline element's native attributes"""
        native = step_info['element']
        live_elem = self._find_live_element(live_root, native)
        
        if live_elem:
            success = self.action_handler.execute(
                step_info['action'],
                {**native, 'native': live_elem.attrib},
                self.trade_data
            )
            if success:
                if native.get('save_to') and live_elem.get('text'):
                    self.runtime_context[native['save_to']] = live_elem.get('text')
                    
    def _find_live_element(self, live_root: ET.Element, native: Dict) -> Optional[ET.Element]:
        """Find matching element in live XML by native attributes"""
        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
                
            match = True
            if native.get('resource-id'):
                offline_rid = native['resource-id'].split('/')[-1]
                live_rid = elem.get('resource-id', '').split('/')[-1]
                if offline_rid != live_rid:
                    match = False
                    
            if match and native.get('text'):
                if native['text'] != elem.get('text', ''):
                    match = False
                    
            if match:
                return elem
        return None


### 4.7 Test Runner Framework (`test_runner.py`)

实际代码中的测试运行器实现 (`TradeScriptTestRunner`):

```python
class TradeScriptTestRunner:
    """Test execution framework supporting multiple test scenarios"""
    
    def __init__(
        self,
        apks_dir: str = "app/src/main/assets/apks",
        output_dir: Optional[str] = None,
    ):
        self.apks_dir = Path(apks_dir)
        self.output_dir = Path(output_dir) if output_dir else Path("test_output")
        self.adb_manager = ADBManager()
        self.page_matcher: Optional[PageMatcher] = None
        self.data_executor: Optional[DataDrivenExecutor] = None
        self.flow_coordinator: Optional[TradeflowCoordinator] = None
        self.test_results: List[TestResult] = []

    def initialize(self, device_id: str) -> bool:
        # Initialize all components
        self.page_matcher = PageMatcher(apks_dir=str(self.apks_dir))
        self.data_executor = DataDrivenExecutor(self.adb_manager, device_id, str(self.apks_dir))
        self.flow_coordinator = TradeflowCoordinator(self.page_matcher, self.data_executor)
        return True

    def run_test_case(self, test_case: TestCase) -> TestResult:
        # Execute a single test case
        pass

    def run_all_tests(self) -> Dict[str, TestResult]:
        # Execute all registered test cases
        pass

    def generate_report(self, output_path: str = "test_report.json") -> None:
        # Generate JSON test report
        pass
```

**Supported Test Scenarios:**
| Scenario | Description |
|----------|-------------|
| `COLD_START_APP` | Cold start application and verify home page |
| `NAVIGATE_TO_GRID` | Navigate from home to grid trading page |
| `FULL_FLOW` | Complete flow: cold start + navigation |
| `ELEMENT_VERIFICATION` | Verify element presence on page |


### 4.8 Service Layer and Execution Entry (`service.py`)

实际代码中的服务层实现:

```python
from core.tradescript.adb_driver import ADBManager
from core.tradescript.page_matcher import PageMatcher
from core.tradescript.data_executor import DataDrivenExecutor
from core.tradescript.flow_coordinator import TradeflowCoordinator
from core.tradescript.test_runner import TradeScriptTestRunner
from core.config import Config


class TradeScriptService:
    """TradeScript service layer for managing test execution"""
    
    def __init__(self, config: Config):
        self.config = config
        self.adb_manager = ADBManager()
        self.page_matcher: Optional[PageMatcher] = None
        self.data_executor: Optional[DataDrivenExecutor] = None
        self.flow_coordinator: Optional[TradeflowCoordinator] = None
        self.test_runner: Optional[TradeScriptTestRunner] = None
        
    def initialize(self, device_id: str) -> bool:
        """Initialize all components with device"""
        self.adb_manager.set_device_id(device_id)
        apks_dir = self.config.get('apks_dir', 'app/src/main/assets/apks')
        
        self.page_matcher = PageMatcher(apks_dir=apks_dir)
        self.data_executor = DataDrivenExecutor(
            adb_manager=self.adb_manager,
            device_id=device_id,
            apks_dir=apks_dir
        )
        self.flow_coordinator = TradeflowCoordinator(
            page_matcher=self.page_matcher,
            data_executor=self.data_executor
        )
        self.test_runner = TradeScriptTestRunner(
            apks_dir=apks_dir,
            output_dir='test_output'
        )
        return True
        
    def execute_trade_plan(self, plan_id: str, data: Dict) -> Dict:
        """Execute a single trade plan"""
        if not self.flow_coordinator:
            return {'success': False, 'error': 'Service not initialized'}
            
        self.data_executor.trade_data = data
        current_page, score, _ = self.flow_coordinator.identify_current_page()
        
        if current_page:
            success = self.flow_coordinator.execute_page(current_page)
            return {
                'success': success,
                'plan_id': plan_id,
                'page': current_page,
                'confidence': score
            }
        return {'success': False, 'error': 'Page not identified', 'confidence': score}
        
    def run_full_suite(self, suite_config: Dict) -> List[Dict]:
        """Execute full test suite"""
        if not self.test_runner:
            return []
            
        results = []
        for flow_id in suite_config.get('flow_execution_order', []):
            flow_result = self.test_runner.run_test_case(
                TestCase(
                    test_id=flow_id,
                    scenario=TestScenario.FULL_FLOW,
                    data=suite_config.get('required_data', {})
                )
            )
            results.append(flow_result)
        return results
```

**使用示例:**

```python
from core.config import Config
from core.tradescript.service import TradeScriptService


def main():
    # Load configuration
    config = Config('config.yaml')
    
    # Initialize service
    service = TradeScriptService(config)
    device_id = config.get('device_id')
    service.initialize(device_id)
    
    # Execute single trade plan
    trade_data = {
        'stock_code': '000001',
        'trade_amount': '1000'
    }
    result = service.execute_trade_plan('plan_001', trade_data)
    print(f"Result: {result}")


if __name__ == '__main__':
    main()
```

## Dynamic Workflow Routing and Lightweight State Navigation

The core idea is to change the definition of "trading flow": **A trading flow should not be bound to a unique `start page`, but should be defined as "the ability to navigate from current page to target page and execute a series of operations".** The framework needs two new capabilities:
1. **Dynamic routing**: Based on current page, intelligently select and execute corresponding trading flow.
2. **State navigation**: Provide a set of lightweight instructions to navigate between different pages without resetting the app's core state (like login state).

### Solution Workflow Diagram

```
Start trading suite
├─ Read flow configuration list and general navigation config
├─ Traverse each business flow
│  ├─ Identify current page
│  ├─ Is current page the target starting page of business flow?
│  │  ├─ Yes → Execute the business flow directly
│  │  └─ No → Enable "state navigation", execute general navigation steps (like clicking back/home)
│  │         ├─ Is target starting page reached successfully?
│  │         │  ├─ Yes → Execute the business flow
│  │         │  └─ No → Record failure and try next flow
│  │         └─ Execute business flow
│  └─ Business flow execution completed
├─ All flows executed
└─ Trading suite completed, generate report
```

This solution addresses multi-flow navigation by allowing any starting point execution. Each trading flow is defined as "performing a series of operations on the `profile_page` page". Regardless of which page the previous flow ended on (`pageX` or `pageY`), the orchestrator will first try to navigate to `profile_page` through **general navigation** before executing operations. 

The general navigation instruction library (`global_nav_actions.xml`) uses navigation paths accessible within the app while maintaining the logged-in state. This is like having an assistant who knows how to navigate from bedroom to living room in your house (logged-in app) without kicking you out of the house (logging out).

## Page Fingerprinting with `autodroid:fingerprint="true"`

### Overview

The framework supports two page matching schemes:

1. **Weighted Feature Matching (Legacy)**: Uses all elements with `autodroid:action` attributes for page matching with weighted scoring
2. **Selector-Based Matching (Recommended)**: Uses only elements marked with `autodroid:fingerprint="true"` for page matching

### Why Use `autodroid:fingerprint="true"`?

**Problems with Weighted Feature Matching:**
- Uses all action elements, which may include many common UI elements (buttons, inputs)
- Common elements across pages can cause false positives
- Requires careful weight tuning to avoid misidentification
- More computationally expensive as it processes many elements

**Benefits of Selector-Based Matching:**
- **Higher Accuracy**: Only uses carefully selected unique elements for page identification
- **Reduced False Positives**: Unique elements are less likely to appear on multiple pages
- **Simpler Logic**: No need for complex weight calculations
- **Better Performance**: Processes fewer elements during matching
- **Explicit Control**: You explicitly choose which elements identify each page

### How to Use `autodroid:fingerprint="true"`

#### Step 1: Identify Unique Elements for Each Page

For each page in your application, identify 1-3 elements that uniquely identify that page. Good candidates include:

- Elements with unique `resource-id` values
- Elements with unique `text` content (e.g., page titles)
- Combinations of attributes that together form a unique signature
- Elements that are stable and unlikely to change between app versions

**Example Unique Elements:**
```xml
<!-- Page: Home -->
<node text="川财APP首页" class="android.webkit.WebView" autodroid:fingerprint="true" />

<!-- Page: Trading -->
<node resource-id="com.tdx.androidCCZQ:id/trading_panel" autodroid:fingerprint="true" />

<!-- Page: Profile -->
<node text="我的账户" resource-id="com.tdx.androidCCZQ:id/profile_title" autodroid:fingerprint="true" />
```

#### Step 2: Add `autodroid:fingerprint="true"` to Offline XML

Add the attribute to the identified elements in your offline XML files:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<hierarchy>
    
    <!-- Page fingerprint: Unique element that identifies this page -->
    <node
        index="0"
        text="川财APP首页"
        resource-id=""
        class="android.webkit.WebView"
        package="com.tdx.androidCCZQ"
        content-desc=""
        checkable="false"
        checked="false"
        clickable="false"
        enabled="true"
        focusable="true"
        focused="true"
        scrollable="true"
        long-clickable="false"
        password="false"
        selected="false"
        bounds="[0,0][1080,1676]"
        autodroid:fingerprint="true" />
    
    <!-- Action elements for execution -->
    <node
        resource-id="com.tdx.androidCCZQ:id/stock_code"
        autodroid:action="input"
        autodroid:name="stock_code"
        autodroid:desc="Input stock code" />
        
</hierarchy>
```

#### Step 3: Framework Automatically Uses Fingerprint Elements

When the framework loads page XML files, it automatically:
1. Extracts all elements with `autodroid:fingerprint="true"` into a fingerprint list
2. Uses only these fingerprint elements for page matching
3. Matches live XML elements against fingerprint elements using the same matching logic (resource-id, text, content-desc, etc.)

### Matching Algorithm for Fingerprint Elements

The framework uses the following priority order to match fingerprint elements:

```python
def _match_fingerprint_element(offline_elem, live_elem):
    # Priority 1: resource-id match (most reliable)
    if offline_elem.resource_id and offline_elem.resource_id == live_elem.resource_id:
        return True
    
    # Priority 2: text match (second reliable)
    if offline_elem.text and offline_elem.text == live_elem.text:
        return True
    
    # Priority 3: content-desc match
    if offline_elem.content_desc and offline_elem.content_desc == live_elem.content_desc:
        return True
    
    # Priority 4: class + bounds combination (fallback)
    if (offline_elem.class_name == live_elem.class_name and 
        offline_elem.bounds == live_elem.bounds):
        return True
    
    return False
```

### Page Matching with Fingerprint Elements

When identifying the current page:

```python
def identify_page(live_root, offline_pages):
    best_match = None
    best_score = 0.0
    
    for page_id, page_info in offline_pages.items():
        fingerprint_elements = page_info.fingerprint_elements
        
        if not fingerprint_elements:
            # Fallback to weighted matching if no fingerprint elements
            continue
        
        matched_count = 0
        for offline_fp in fingerprint_elements:
            live_elem = find_matching_element(live_root, offline_fp)
            if live_elem:
                matched_count += 1
        
        # Calculate match rate: matched / total fingerprint elements
        match_rate = matched_count / len(fingerprint_elements)
        
        if match_rate > best_score:
            best_score = match_rate
            best_match = page_id
    
    # Return best match if score >= threshold (e.g., 0.5)
    if best_score >= 0.5:
        return best_match, best_score
    return None, best_score
```

### Best Practices

1. **Use 1-3 Fingerprint Elements per Page**: More elements increase accuracy but also increase complexity
2. **Choose Stable Elements**: Prefer elements that don't change frequently between app versions
3. **Prioritize resource-id**: resource-id is the most reliable identifier
4. **Use Unique Text**: If resource-id is not available, use unique text content
5. **Test Matching**: Verify that fingerprint elements correctly identify pages during testing
6. **Avoid Common Elements**: Don't use elements like "Back" button that appear on multiple pages

### Example: Complete Page with Fingerprint

```xml
<?xml version="1.0" encoding="UTF-8"?>
<hierarchy>
    
    <!-- Fingerprint Element 1: Unique page title -->
    <node
        index="0"
        text="网格交易"
        resource-id="com.tdx.androidCCZQ:id/page_title"
        class="android.widget.TextView"
        package="com.tdx.androidCCZQ"
        autodroid:fingerprint="true" />
    
    <!-- Fingerprint Element 2: Unique panel -->
    <node
        index="1"
        resource-id="com.tdx.androidCCZQ:id/grid_trading_panel"
        class="android.widget.LinearLayout"
        package="com.tdx.androidCCZQ"
        autodroid:fingerprint="true" />
    
    <!-- Action Step 1 -->
    <node
        resource-id="com.tdx.androidCCZQ:id/stock_code_input"
        autodroid:step="1"
        autodroid:action="input"
        autodroid:name="stock_code"
        autodroid:desc="Input stock code" />
    
    <!-- Action Step 2 -->
    <node
        resource-id="com.tdx.androidCCZQ:id/confirm_btn"
        autodroid:step="2"
        autodroid:action="click"
        autodroid:desc="Click confirm button" />
        
</hierarchy>
```

### Verification

After adding fingerprint elements, verify page matching accuracy:

```python
from page import PageMatcher

pm = PageMatcher()

# Test page identification
live_xml = dump_page_xml()
page_id, score = pm.identify_page(live_xml)

print(f"Identified page: {page_id} (confidence: {score:.2f})")
```

## Implementation Steps Summary

1. **Environment Setup**: Ensure Python, ADB, tested APK, and device connection are ready.
2. **Dump Pages**: Use `adb shell uiautomator dump` to get XML of various pages of target app, save to `apks/` directory.
3. **Edit "Scripts"**: In offline XML of each page, add `autodroid:*` series attributes to elements that need operation according to business order.
4. **Prepare Data**: Create JSON files in `data/` directory to define multiple sets of trading data.
5. **Configure Device**: Fill in device ID in the execution script.
6. **Execute Trading**: Run `python run_tradescript.py`, the framework will automatically capture page via ADB, identify pages using native attributes matching, locate elements in live XML using offline element's resource-id/text/bounds, execute steps via ADB commands, and complete the entire business process.