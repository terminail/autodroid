# Autodroid Trader - Tradescript Engine Design

## Overview

The Tradescript Engine is a data-driven, page-aware automation framework designed to execute trading plans on Android trading applications. The engine operates by parsing offline XML "scripts" containing custom `autodroid:*` attributes that describe trading operations, enabling complete separation of business logic, element location, and trading data.

## Core Architecture

### 1. Component Structure

```
tradescript-engine/
│
├── config/
│   └── capabilities.json      # Appium device connection configuration
│
├── data/                      # External trading data
│   ├── market_data.json
│   └── trade_plans.json
│
├── apks/                      # Core: Page definition library (offline XML)
│   ├── cn.com.gjzq.yjb2/
│   │   ├── testflowa/
│   │   │   ├── config.yaml
│   │   │   └── home.xml
│   │   ├── testflowb/
│   │   │   ├── config.yaml
│   │   │   └── home.xml
│   │   └── config.yaml
│   └── com.tdx.androidCCZQ/
│       ├── netgrid-trading/
│       │   ├── config.yaml
│       │   ├── entry-xzsg.xml
│       │   ├── home.xml
│       │   ├── my-condition-orders.xml
│       │   ├── netgrid-trading-confirm.xml
│       │   ├── netgrid-trading-success.xml
│       │   ├── netgrid-trading-with-stock.xml
│       │   ├── netgrid-trading.xml
│       │   ├── stock-search-result.xml
│       │   └── stock-search.xml
│       └── config.yaml
│
├── flows/                     # Trading flow configuration (defines what to do)
│   ├── flow_buy_stock.json
│   └── flow_sell_stock.json
│
├── navigation/                # General navigation instruction library
│   └── global_nav_actions.xml
│
├── lib/                       # Framework core library
│   └── tradescript_engine.py
│
├── orchestrator_config.json   # Main orchestrator configuration
└── run_tradescript.py         # Main execution script entry point
```

## Core Components and Workflows

### 2.1 Smart Page Matching Engine (`PageMatcher`)

The "eyes" and "brain" of the framework.

- **Input**: Current interface XML source obtained from Appium in real-time
- **Processing**: Compare real-time XML with all offline XML files in the `apks/` directory for "fingerprint" matching
- **Fingerprint Algorithm**: Extract elements with `autodroid:*` attributes from offline XML, using their stable native attributes (such as `resource-id`, `text`, `class` combinations) as the page's characteristic fingerprint
- **Output**: Calculate match degree, return the `page_id` with the highest match degree. Successful matching means the framework "knows" which page it is currently on
- **Advantages**: Natively supports multi-entry points (can enter `product_detail_page` from different pages) and is insensitive to non-critical UI layout changes

### 2.2 Data-Driven Executor (`DataDrivenExecutor`)

The "hands" of the framework.

- **Input**: Offline XML of current page, external injection test data (`Dict`), runtime context (`Context`)
- **Processing**:
  1. Parse XML, sort all elements to be operated according to `autodroid:step`
  2. For each element, generate a **locator priority list** (strategy: `resource-id` > `accessibility-id` > `text` > `XPath`)
  3. Decide action according to `autodroid:action`, and decide action value according to `name` or `value`
     * **Value Logic**: `if name exists in test_data: use test_data[name] else: use value`
  4. Execute operations on real devices
- **Output**: Drive the device to complete all steps, and may update runtime context (such as saving captured text)

### 2.3 Tradeflow Coordinator (`TradeflowCoordinator`)

The "commander" of the framework.

- **Workflow**:
  1. According to configuration or manually specify `start_page_id`
  2. Call `PageMatcher` to confirm current page
  3. Load corresponding offline XML, call `DataDrivenExecutor` to execute
  4. After steps are executed, call `PageMatcher` again to perceive new page
  5. Loop steps 2-4 until the page perceived by the framework has no unexecuted steps, or reaches preset endpoint
- **Features**: Implements self-driven page flow, completely driven by the actual jump logic of the application, without hard-coding "page A is followed by page B" in the script

### 2.4 Enhanced Smart Orchestrator (`EnhancedOrchestrator`)

The "commander-in-chief" of the framework, used to manage multiple test flows.

- **Function**: Coordinate the execution of multiple independent test processes, providing smart navigation capabilities
- **Process**:
  1. Read the process configuration list in `orchestrator_config.json`
  2. Traverse each business flow, identify current page
  3. If current page is not the target starting page, try to navigate to target page through general navigation rules
  4. Execute corresponding business process on target page
  5. Loop to execute all processes until completion

## Core: `autodroid:*` Custom Attribute System Design

### 3.1 Design Philosophy

The `autodroid:*` attribute system is a **"declarative" test description language**. It allows test designers to declare an element's **behavioral intent**, **data requirements**, and **verification logic** in offline XML in the form of tags (attributes), thereby completely separating test logic from code.

### 3.2 Attribute Classification and Overview

All `autodroid:*` attributes can be divided into four major categories according to their core functions:

| Category | Core Purpose | Key Attributes |
| :--- | :--- | :--- |
| **1. Page and Identification** | Define page identity, establish page library | `autodroid:page_id` |
| **2. Process and Steps** | Orchestrate operation order and process control | `autodroid:step`, `autodroid:action`, `autodroid:wait_after` |
| **3. Data and Variables** | Implement data-driven and state transfer | `autodroid:name`, `autodroid:value`, `autodroid:save_to` |
| **4. Elements and Location** | Provide location assistance and verification information | `autodroid:desc` (implicit: depends on native attributes like `resource-id`, `text`) |

### 3.3 Detailed Attribute Definition Table

The following table is the complete definition of the `autodroid:*` attribute set, which is the **"grammar manual"** of the framework.

| Attribute Name | Scope | Value Type | Required | Description and Purpose | Example |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`autodroid:page_id`** | XML root node | String | **Yes** | **Page unique identifier**. The framework indexes and matches pages in the page library based on this. | `<hierarchy autodroid:page_id="login_page">` |
| **`autodroid:step`** | Operable elements | Integer | **Yes** (if element needs to be operated) | **Step sequence number**. Defines the **execution order** of operations within the same page, the framework executes according to this order. | `autodroid:step="1"` |
| **`autodroid:action`** | Operable elements | String | **Yes** (if element needs to be operated) | **Action to execute**. The "verb" of the framework, determines the specific operation on the element. See [Action Type Table]. | `autodroid:action="click"` |
| **`autodroid:name`** | Elements requiring data | String | No | **Data field key name**. Provides key for `input`, `select` etc. actions, used to dynamically find and fill values from external test data (`test_data`). **Mutually exclusive with `value`, higher priority.** | `autodroid:name="username"` |
| **`autodroid:value`** | Elements requiring data | String | No | **Hardcoded default value**. When `name` is not set, or `name` is not matched in external data, this value will be used. Implements **flexible fallback mechanism**. | `autodroid:value="test@example.com"` |
| **`autodroid:save_to`** | Elements that can output data | String | No | **Runtime variable storage key**. Mainly used for `action="get_text"`, saves captured text to **runtime context** (`context`) for reference by subsequent steps or final assertion. | `autodroid:save_to="product_price"` |
| **`autodroid:wait_after`** | Any step element | Float | No | **Wait time after step (seconds)**. After current step succeeds, pause for specified time to wait for interface stability or loading. | `autodroid:wait_after="2.5"` |
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

### 4.1 Page XML Definition Example (`apks/com.tdx.androidCCZQ/netgrid-trading.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- Root node must declare page_id -->
<hierarchy autodroid:page_id="netgrid_trading_page">
    
    <!-- Step 1: Input stock code. Use 'name' to get value from external data, fallback to 'value' -->
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
    
    <!-- Step 2: Input trade amount. Amount usually read from external data, no fallback value -->
    <android.widget.EditText
        resource-id="com.tdx.androidCCZQ:id/trade_amount"
        autodroid:step="2"
        autodroid:action="input"
        autodroid:name="trade_amount"
        autodroid:desc="Input trade amount" />
    
    <!-- Step 3: Click buy button. After clicking, app will navigate to confirmation page -->
    <android.widget.Button
        resource-id="com.tdx.androidCCZQ:id/buy_btn"
        text="Buy"
        autodroid:step="3"
        autodroid:action="click"
        autodroid:wait_after="1.5"
        autodroid:desc="Click buy button" />
        
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
<hierarchy autodroid:page_id="global_nav_actions">
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
from appium import webdriver
from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import NoSuchElementException, TimeoutException

class TradescriptEngine:
    def __init__(self, driver, apks_dir='apks'):
        self.driver = driver
        self.apks_dir = apks_dir
        self.page_fingerprints = self._load_page_fingerprints()
        self.trade_data = {}  # Injected by main script
        self.runtime_context = {}  # Store runtime data
        self.wait = WebDriverWait(self.driver, 10)
        
    def _load_page_fingerprints(self):
        """Load all page XML, build page fingerprint library"""
        fingerprints = {}
        # ... (traverse apks_dir, parse XML, extract elements with autodroid attributes features)
        return fingerprints
        
    def identify_current_page(self, live_xml_source):
        """Identify current page: core matching algorithm"""
        best_match_id, best_score = None, 0
        live_root = ET.fromstring(live_xml_source.encode('utf-8'))
        live_features = self._extract_features(live_root)
        
        for page_id, offline_features in self.page_fingerprints.items():
            score = self._calculate_similarity(offline_features, live_features)
            if score > best_score:
                best_score, best_match_id = score, page_id
        return best_match_id if best_score > 0.6 else None  # Adjustable threshold
        
    def execute_page_flow(self, page_id):
        """Execute all steps for specified page"""
        xml_path = f"{self.apks_dir}/{page_id}.xml"
        tree = ET.parse(xml_path)
        root = tree.getroot()
        steps = self._collect_steps(root)
        
        for step_info in steps:
            self._execute_single_step(step_info)
            # Wait after execution
            wait_time = float(step_info['element'].get('autodroid:wait_after', 0))
            if wait_time > 0:
                time.sleep(wait_time)
                
    def _execute_single_step(self, step_info):
        """Execute single step: smart location + data-driven execution"""
        elem = step_info['element']
        action = step_info['action']
        
        # 1. Smart location
        locators = self._generate_locators(elem)
        live_element = self._find_element(locators)
        if not live_element:
            print(f"Location failed: {elem.attrib}")
            return
            
        # 2. Data parsing: decide value to use
        field_name = elem.get('autodroid:name')
        hardcoded_value = elem.get('autodroid:value')
        target_value = self.trade_data.get(field_name, hardcoded_value)
        
        # 3. Execute action
        if action == 'input' and target_value:
            live_element.clear()
            live_element.send_keys(target_value)
            print(f"Input: [{target_value}]")
        elif action == 'select' and target_value:
            self._handle_dropdown(live_element, target_value)
        elif action == 'click':
            live_element.click()
            print("Click")
        elif action == 'get_text':
            captured = live_element.text
            save_key = elem.get('autodroid:save_to', field_name)
            if save_key:
                self.runtime_context[save_key] = captured
                print(f"Save text [{save_key}]: {captured}")
                
    def _generate_locators(self, offline_elem):
        """Generate locator priority list"""
        locators = []
        attrs = offline_elem.attrib
        
        if 'resource-id' in attrs:
            rid = attrs['resource-id'].split('/')[-1]
            locators.append((AppiumBy.ID, rid))
        if 'content-desc' in attrs:
            locators.append((AppiumBy.ACCESSIBILITY_ID, attrs['content-desc']))
        if 'text' in attrs:
            uia = f'new UiSelector().text("{attrs["text"]}")'
            locators.append((AppiumBy.ANDROID_UIAUTOMATOR, uia))
        # ... Can add more fallback location strategies
        return locators
```

### 4.7 Enhanced Smart Orchestrator Core Logic

The following pseudo-code shows how the orchestrator works:

```
class EnhancedOrchestrator:
    def run_trading_suite(self, suite_config):
        """Execute entire trading suite"""
        for flow_id in suite_config['flow_execution_order']:
            print(f"\nPreparing to execute flow: {flow_id}")
            
            # 1. Load flow definition
            flow = self.load_flow_definition(flow_id)
            target_page = flow['target_start_page']
            
            # 2. Identify current page
            current_page = self.page_matcher.identify(driver.page_source)
            print(f"Current page: {current_page}, Target start page: {target_page}")
            
            # 3. If not on target page, try general navigation
            if current_page != target_page:
                success = self.apply_global_navigation(
                    from_page=current_page,
                    to_page=target_page,
                    nav_rules=suite_config['global_navigation_file']
                )
                if not success:
                    print(f"Warning: Cannot navigate from {current_page} to {target_page}, skip this flow.")
                    continue  # Skip this flow, continue to next one
            
            # 4. Confirm on target page, inject data and execute business steps for that page
            self.trade_data = flow['required_data']
            self.execute_page_flow(target_page)  # Execute specific operations defined in your apks/ directory
            
            print(f"Flow {flow_id} execution completed.")
        print("\nAll trading flows execution completed!")
    
    def apply_global_navigation(self, from_page, to_page, nav_rules):
        """Apply general navigation rules, try to navigate from from_page to to_page"""
        # Load navigation rules XML
        # Find navigation rules applicable to current page (from_page)
        # Execute these rules (e.g. click back button, click home button)
        # After each execution, re-identify current page, check if reached to_page or expected intermediate pages
        # If successfully reached to_page, return True; if failed after multiple attempts, return False
        pass
```

### 4.8 Main Execution Script (`run_tradescript.py`)

```
#!/usr/bin/env python3
import json
from appium import webdriver
from lib.tradescript_engine import TradescriptEngine

def main():
    # 1. Load external trading data
    with open('data/trade_plans.json', 'r') as f:
        trade_plans = json.load(f)
        
    # 2. Load device configuration
    with open('config/capabilities.json', 'r') as f:
        desired_caps = json.load(f)
    
    for plan in trade_plans:
        print(f"\nStarting trade plan: {plan['plan_id']}")
        
        # 3. Start Appium session
        driver = webdriver.Remote('http://localhost:4723', desired_caps)
        time.sleep(3)  # Wait for app initial page
        
        try:
            # 4. Initialize framework and inject test data for current case
            tradescript = TradescriptEngine(driver, apks_dir='apks')
            tradescript.trade_data = plan['data']
            
            # 5. Start self-driven workflow (assume starting from trading page)
            current_page = tradescript.identify_current_page(driver.page_source)
            if not current_page:
                current_page = 'netgrid_trading_page'  # Or specify start page via config
            
            while current_page:
                print(f"Current page: {current_page}")
                tradescript.execute_page_flow(current_page)
                # After execution, re-identify page
                new_page = tradescript.identify_current_page(driver.page_source)
                if new_page == current_page:
                    print("Flow ends on this page.")
                    break
                current_page = new_page
                
            # 6. (Optional) Perform assertions using runtime_context
            # if tradescript.runtime_context.get('trade_status') != 'success':
            #    raise AssertionError("Trade failed")
                
        finally:
            driver.quit()

if __name__ == '__main__':
    main()
```

## Dynamic Workflow Routing and Lightweight State Navigation

The core idea is to change the definition of "trading flow": **A trading flow should not be bound to a unique `start_page_id`, but should be defined as "the ability to navigate from current page to target page and execute a series of operations".** The framework needs two new capabilities:
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

This solution addresses multi-flow navigation by allowing any starting point execution. Each trading flow is defined as "performing a series of operations on the `profile_page` page". Regardless of which page the previous flow ended on (`pageX` or `pageY`), the orchestrator will first try to navigate to `profile_page` through **general navigation** before executing operations. The flow is decoupled from fixed `start_page_id`.

The general navigation instruction library (`global_nav_actions.xml`) uses navigation paths accessible within the app while maintaining the logged-in state. This is like having an assistant who knows how to navigate from bedroom to living room in your house (logged-in app) without kicking you out of the house (logging out).

## Implementation Steps Summary

1. **Environment Setup**: Ensure Python, Appium Server, tested APK, and device connection are ready.
2. **Analyze Pages**: Use Appium Inspector or `adb shell uiautomator dump` to get XML of various pages of target app, save to `apks/` directory.
3. **Edit "Scripts"**: In offline XML of each page, add `autodroid:*` series attributes to elements that need operation according to business order.
4. **Prepare Data**: Create JSON files in `data/` directory to define multiple sets of trading data.
5. **Configure Connection**: Fill in device information and app information in `config/capabilities.json`.
6. **Execute Trading**: Run `python run_tradescript.py`, the framework will automatically load data, identify pages, execute steps, and complete the entire business process of trading.