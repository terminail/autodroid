# Cross-Platform Android App Automation Container Solution

## Q: Based on the above discussion, I plan to adopt a container approach to automate the manipulation of third-party APKs. The container can be podman or docker according to user selection, supporting deployment on Macos, WSL/Linux, and Windows. Please write a complete requirement document including requirements, technical solution, tool installation, APK analysis, etc., to guide developers in implementing it step by step. [DeepSeek: Cross-Platform Android App Automation Container Solution](https://chat.deepseek.com/a/chat/s/519ce808-5602-45c6-bebb-96e2eb47add8)

## A: Sure, I'll organize a complete document based on the above discussion, including requirements document, technical solution, tool installation, APK analysis, etc. Please wait.

## Project Overview

### 1.1 Project Background
With the growth of mobile application testing needs, there is a demand for an automated testing solution that can be deployed cross-platform, support multi-device concurrency, and is easy to manage. Traditional solutions have problems such as complex environment configuration, poor platform compatibility, and high maintenance costs.

### 1.2 Solution
Develop a cross-platform Android app automation framework based on container technology, implementing standard UI automation through Appium, combined with a workplan system, to provide stable and reliable third-party APK automation testing capabilities.

### 1.3 Core Values
- 🐳 **Containerized Deployment**: Supports Docker and Podman, eliminating environment dependencies
- 🖥️ **Full Platform Support**: macOS, Windows, WSL/Linux
- 📱 **Non-intrusive**: No modification to target APKs, based on UI hierarchy analysis
- 🔧 **Workplan System**: Supports scheduled tasks and event-driven testing
- 📊 **Centralized Management**: Unified configuration management and result monitoring

---

## I. Requirements Document

### 1. Functional Requirements

#### 1.1 Core Function Modules

**F001 - Containerized Deployment Management**
- Supports Docker and Podman container runtimes
- Automatic detection and adaptation of host platforms
- One-click service start and stop
- Container health status monitoring

**F002 - Device Auto Discovery**
- Network ADB device auto-discovery and connection
- USB device direct connection support (Linux/macOS)
- Real-time device status monitoring (battery, storage, network)
- Multi-device parallel management (up to 5 concurrent devices)

**F003 - APK Intelligent Analysis**
- Automatic scanning of installed APKs on mobile phones
- Extract package name, app name, version information
- Identify main Activity and interface structure
- Generate basic workscript templates

**F004 - WorkScript Engine**
- YAML configuration-driven workscript definition
- Supports data-driven test cases
- Conditional branching and loop control
- Step-level timeout and retry mechanism

**F005 - Workplan System**
- Time-based scheduled tasks
- Event-driven automatic triggering
- Resource-aware workplan system
- Task queue and priority management

**F006 - Real-time Monitoring Reports**
- Real-time execution log recording
- Automated report generation (HTML/JSON)
- Automatic failure screenshot saving
- Performance indicator statistics and analysis

### 2. Non-functional Requirements

#### 2.1 Performance Requirements
- Container startup time < 30 seconds
- Element positioning response time < 3 seconds
- Support for concurrent execution of 5 automation tasks
- Memory usage < 1GB

#### 2.2 Reliability Requirements
- Automation task success rate > 98%
- Automatic network interruption recovery
- Device disconnection reconnection mechanism
- Complete exception handling

#### 2.3 Compatibility Requirements
- Android 8.0+ device support
- Support for x86_64 and ARM64 architectures
- Adaptation to multiple screen resolutions
- Support for light/dark themes

---

## II. System Architecture

### 1. Overall Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Container Server                          │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ Workflow     │  │ Scheduling   │  │ Device Management│   │
│  │ Engine       │  │ System       │  │                  │   │
│  └───────┬──────┘  └───────┬──────┘  └──────────┬───────┘   │
│          │                 │                    │           │
│  ┌───────┴─────────────────┴────────────────────┴───────┐   │
│  │                 RESTful API Service                     │   │
│  └───────────────────────────┬──────────────────────────┘   │
└──────────────────────────────┼──────────────────────────────┘
                               │
                      ┌────────┴────────┐
                      │  Message        │
                      │ Communication   │
                      │ Layer           │
                      │ (WebSocket/HTTP)│
                      └────────┬────────┘
                               │
       ┌───────────────────────┼───────────────────────┐
       │                       │                       │
┌──────┴──────┐         ┌──────┴──────┐         ┌──────┴──────┐
│  Test Device A │         │  Test Device B │         │  Test Device C │
│  ┌────────┐ │         │  ┌────────┐ │         │  ┌────────┐ │
│  │ Manager│ │         │  │ Manager│ │         │  │ Manager│ │
│  │ App    │ │         │  │ App    │ │         │  │ App    │ │
└─────────────┘         └─────────────┘         └─────────────┘
```

### 2. Container Service Architecture

```yaml
# docker-compose.yml
version: '3.8'

services:
  appium-server:
    image: appium/appium:latest
    ports: ["4723:4723"]
    privileged: true
    environment:
      - RELAXED_SECURITY=true
      - ALLOW_INSECURE=adb_shell

  automation-core:
    image: autodroid/core:latest
    depends_on: [appium-server]
    environment:
      - APPIUM_URL=http://appium-server:4723
      - MAX_CONCURRENT_TASKS=5
    volumes:
      - ./workflows:/app/workflows
      - ./reports:/app/reports

  device-manager:
    image: autodroid/device-manager:latest
    privileged: true
    devices:
      - /dev/bus/usb:/dev/bus/usb
    volumes:
      - /var/run/usbmuxd:/var/run/usbmuxd
```

---

## III. Technical Solution

### 1. Technology Stack Selection

#### Core Frameworks
- **Automation Engine**: Appium + UI Automator 2
- **Device Communication**: ADB + Appium Server
- **Container Runtime**: Docker + Podman
- **Programming Language**: Python 3.11+

#### Auxiliary Tools
- **APK Analysis**: aapt, apktool, jadx
- **UI Analysis**: Appium Inspector, UI Automator Viewer
- **Report Generation**: Allure, pytest-html
- **Configuration Management**: Pydantic, YAML

#### Development Tools
- **API Framework**: FastAPI
- **Testing Framework**: pytest
- **Code Quality**: flake8, black, mypy
- **CI/CD**: GitHub Actions

### 2. Core Module Design

#### 2.1 Device Connection Pool
```python
class DeviceConnectionPool:
    def __init__(self, max_connections: int = 5):
        self.max_connections = max_connections
        self.active_connections: Dict[str, webdriver.Remote] = {}
    
    async def execute_task(self, device_udid: str, workflow: WorkflowConfig):
        """Execute task on specified device"""
        driver = await self.get_driver(device_udid)
        return await asyncio.create_task(
            self._execute_workflow(driver, workflow)
        )
```

#### 2.2 Intelligent Scheduler
```python
class SmartScheduler:
    async def get_due_test_plans(self) -> List[DeviceTestPlan]:
        """Get due test plans"""
        current_time = datetime.now()
        active_plans = []
        
        for device_id, plan in self.active_plans.items():
            if plan.enabled and self.is_within_schedule(plan.schedule, current_time):
                active_plans.append(plan)
        
        return active_plans
```

#### 2.3 Event-Driven Execution
```python
class TestDataEventListener:
    async def handle_new_test_data(self, test_data: dict):
        """Handle new test data"""
        # Find matching test plans
        matching_plans = self.condition_engine.find_matching_plans(test_data)
        
        for plan in matching_plans:
            if await self.can_execute_now(plan):
                await self.trigger_test_execution(plan, test_data)
```

### 3. Workflow Definition Specification

```yaml
name: "APK_A Standard Test Flow"
description: "Complete business flow including login"

metadata:
  app_package: "com.target.app"
  app_activity: ".MainActivity"
  version: "1.0"

device_selection:
  strategy: "auto"
  criteria:
    min_battery: 20
    android_version: "9.0+"

steps:
  - name: "Launch Application"
    action: "launch_app"
    package: "com.target.app"
    activity: ".MainActivity"
    timeout: 30
    
  - name: "Click Login Button"
    action: "click"
    locator:
      strategies:
        - type: "id"
          value: "com.target.app:id/login_button"
        - type: "text"
          value: "Login"
    timeout: 10

schedule:
  type: "daily"
  start_time: "09:00"
  end_time: "18:00"
  interval_minutes: 120
```

---

## IV. Implementation Guide

### 1. Environment Preparation

#### 1.1 Dependency Installation

**Java SDK Installation:**
```bash
# Ubuntu/Debian
sudo apt install openjdk-11-jdk

# macOS
brew install openjdk@11

# Set environment variables
export JAVA_HOME=/path/to/jdk
export PATH=$JAVA_HOME/bin:$PATH
```

**Android SDK Installation:**
```bash
# Install via Android Studio or command line tools
sdkmanager --install "platform-tools" "platforms;android-33"

# Set environment variables
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$PATH
```

**Appium Installation:**
```bash
npm install -g appium
npm install -g appium-doctor

# Install Python client
pip install appium-python-client
```

#### 1.2 Device Configuration
```bash
# Enable developer options and USB debugging
adb devices

# Enable network ADB
adb tcpip 5555
adb connect 192.168.1.100:5555
```

### 2. APK Analysis Process

#### 2.1 APK Information Scanning
```python
# Use APK scanning tool
from apk_scanner import APKScanner

scanner = APKScanner()
apps = scanner.scan_all_apps()

# Interactive selection of target APK
from interactive_apk_selector import APKSelector
selector = APKSelector()
selected_app = selector.interactive_select()
```

#### 2.2 Element Location Analysis

**Using Appium Inspector:**
1. Configure Desired Capabilities:
```json
{
  "platformName": "Android",
  "appPackage": "com.target.app",
  "appActivity": ".MainActivity",
  "automationName": "UiAutomator2"
}
```

2. Start session and analyze interface elements
3. Record element location strategies (ID > Text > Accessibility ID > XPath)

#### 2.3 WorkScript Template Generation
```python
# Automatically generate basic workscript
workscript_file = selector.generate_workscript_template(selected_app)
```

### 3. Containerized Deployment

#### 3.1 Build Docker Image
```dockerfile
FROM python:3.11-slim

# Install dependencies
RUN apt-get update && apt-get install -y \
    wget unzip android-tools-adb

# Install Android platform tools
RUN wget -q https://dl.google.com/android/repository/platform-tools-latest-linux.zip \
    && unzip platform-tools-latest-linux.zip -d /opt/

# Install Python dependencies
COPY requirements.txt .
RUN pip install -r requirements.txt

WORKDIR /app
COPY . .

CMD ["python", "main.py"]
```

#### 3.2 Multi-Platform Deployment Configuration

**Linux/WSL:**
```yaml
# docker-compose.linux.yml
services:
  autodroid-core:
    devices:
      - /dev/kvm:/dev/kvm
      - /dev/bus/usb:/dev/bus/usb
    privileged: true
```

**macOS:**
```yaml
# docker-compose.macos.yml  
services:
  autodroid-core:
    privileged: true
    extra_hosts:
      - "host.docker.internal:host-gateway"
```

**Windows:**
```yaml
# docker-compose.windows.yml
services:
  autodroid-core:
    isolation: process
    privileged: true
```

### 4. Development Implementation Plan

#### Phase 1: Basic Framework (3 weeks)
- Container base image construction
- Device connection management
- Appium service integration
- Basic API framework

**Deliverables:**
- Runnable Docker image
- Device connection verification tool
- Basic REST API

#### Phase 2: Automation Engine (3 weeks)
- WorkScript parser
- Element positioning engine
- Step executor
- Error handling mechanism

**Deliverables:**
- WorkScript execution engine
- Element positioning library
- Error recovery mechanism

#### Phase 3: Workplan System (2 weeks)
- Scheduled task scheduling
- Event-driven triggering
- Resource management
- Task queue

**Deliverables:**
- Intelligent scheduler
- Event listening system
- Resource monitoring

#### Phase 4: APK Analysis Tools (2 weeks)
- APK scanning tool
- Element discovery assistance
- Template generator
- Interactive selection

**Deliverables:**
- APK analysis toolset
- WorkScript template generator
- User interaction interface

#### Phase 5: Testing Optimization (2 weeks)
- Multi-platform testing
- Performance optimization
- Documentation writing
- User acceptance testing

**Deliverables:**
- Complete test report
- User documentation
- Deployment guide

---

## V. Usage Guide

### 1. Quick Start

#### 1.1 Environment Preparation
```bash
# 1. Install dependencies
./scripts/install_dependencies.sh

# 2. Connect test devices
adb devices

# 3. Scan APK applications
python tools/apk_scanner.py
```

#### 1.2 Container Deployment
```bash
# 1. Build images
docker-compose build

# 2. Start services
docker-compose up -d

# 3. Verify services
curl http://localhost:8000/api/health
```

#### 1.3 Create Test Plan
```bash
# 1. Select target APK
python tools/interactive_apk_selector.py

# 2. Use Appium Inspector to analyze elements

# 3. Edit workflow file
# 4. Submit test plan
```

### 2. Daily Operations

#### 2.1 Monitor Service Status
```bash
# Check container status
docker-compose ps

# View logs
docker-compose logs -f

# Monitor device connections
curl http://localhost:8000/api/devices
```

#### 2.2 Manage Test Plans
```bash
# List all plans
curl http://localhost:8000/api/plans

# Create new plan
curl -X POST http://localhost:8000/api/plans \
  -H "Content-Type: application/json" \
  -d '{"template_id": "apk_a", "schedule": {...}}'
```

---

## VI. Appendix

### 1. Troubleshooting

#### 1.1 Common Issues
- **ADB connection failure**: Check USB debugging and network connection
- **Element positioning failure**: Use multiple positioning strategy combinations
- **Container permission issues**: Use privileged mode
- **Insufficient device resources**: Adjust the number of concurrent tasks

#### 1.2 Debugging Tools
```bash
# Check device connection
adb devices
adb shell dumpsys window

# Check Appium service
appium --log-level debug

# View container logs
docker-compose logs autodroid-core
```

### 2. Performance Optimization Suggestions

#### 2.1 Container Configuration
- Allocate sufficient memory to containers (recommended 2GB+)
- Use SSD storage to improve IO performance
- Optimize network configuration to reduce latency

#### 2.2 Test Optimization
- Use Resource ID priority positioning strategy
- Reasonably set timeout and retry parameters
- Avoid unnecessary screenshot operations
- Use data-driven approach to reduce duplicate code

### 3. Extension Development Guide

#### 3.1 Adding New Positioning Strategies
```python
class CustomLocatorStrategy:
    def locate_element(self, strategy: str, value: str):
        if strategy == "custom":
            return self.find_by_custom_method(value)
```

#### 3.2 Integrating New Test Data Sources
```python
class CustomDataProvider:
    async def get_test_data(self, source_config: dict):
        # Implement custom data retrieval logic
        pass
```

---

## Summary

This document provides a complete cross-platform Android app automation container solution, covering all aspects of project implementation from requirement analysis to technical implementation, from environment preparation to daily use. The development team can follow the phased implementation plan to gradually build a complete automated testing platform.

Key success factors:
1. **Standardization**: Unified workflow definition and configuration management
2. **Automation**: Minimize manual intervention to the greatest extent
3. **Extensibility**: Modular design supports function expansion
4. **Usability**: Complete toolchain and documentation support

Through this solution, teams can efficiently conduct automated testing of third-party APKs, improving testing efficiency and quality assurance levels.