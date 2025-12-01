# Technical Solution

## 1. Technology Stack Selection

### Core Frameworks
- **Automation Engine**: Appium + UI Automator 2
- **Device Communication**: ADB + Appium Server + mDNS + FastAPI + WebSocket
- **Container Runtime**: Docker + Podman
- **Programming Language**: Python 3.11+ (Server), Java/Kotlin (Android App)

### Auxiliary Tools
- **APK Analysis**: aapt, apktool, jadx
- **UI Analysis**: Appium Inspector, UI Automator Viewer
- **Report Generation**: Allure, pytest-html
- **Configuration Management**: Pydantic, YAML

### Development Tools
- **API Framework**: FastAPI
- **Testing Framework**: pytest, AndroidJUnitRunner
- **Code Quality**: flake8, black, mypy, Android Lint
- **CI/CD**: GitHub Actions
- **Device Discovery**: mDNS (for lightweight device auto-discovery)
- **Real-time Communication**: FastAPI + WebSocket (for real-time messaging between server and devices)
- **Authentication**: Android Biometric API, JWT
- **Android Architecture**: MVVM (Model-View-ViewModel)
- **HTTP Client**: OkHttp
- **JSON Processing**: Gson

## 2. Core Module Design

### 2.1 Device Connection Pool
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

### 2.2 Intelligent Scheduler
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

### 2.3 Event-Driven Execution
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

### 2.4 User Authentication System (Android App)

#### Architecture
- **MVVM Pattern**: Separation of concerns between UI, business logic, and data
- **AuthViewModel**: Centralized authentication state management
- **AuthService**: API communication layer for authentication
- **Biometric API**: Fingerprint authentication integration

#### Key Components
```java
// AuthViewModel.java - Authentication state management
public class AuthViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isAuthenticated = new MutableLiveData<>(false);
    private final MutableLiveData<String> token = new MutableLiveData<>();
    
    public void login(String email, String password) {
        // Login logic
    }
    
    public void loginWithBiometrics() {
        // Biometric login logic
    }
    
    public boolean isAuthenticated() {
        return isAuthenticated.getValue() != null && isAuthenticated.getValue();
    }
}

// AuthService.java - API communication
public class AuthService {
    private static final String BASE_URL = "http://autodroid-server:8000/api/auth";
    
    public void login(String email, String password, AuthCallback callback) {
        // HTTP login request using OkHttp
    }
    
    public void register(String email, String password, AuthCallback callback) {
        // HTTP register request using OkHttp
    }
}
```

### 2.5 Multi-language Support (Android App)

#### Implementation
- **String Resources**: Separate resource files for each language
- **Locale Detection**: Automatic language selection based on device settings
- **Comprehensive Coverage**: All UI text externalized to string resources

#### Resource Structure
```
res/
├── values/
│   └── strings.xml          # English strings
└── values-zh/
    └── strings.xml          # Chinese strings
```

#### Usage Example
```java
// In Activity/Fragment
loginButton.setText(getString(R.string.login_button));
errorTextView.setText(getString(R.string.email_required));

// In ViewModel (with Application context)
setErrorMessage(getApplication().getString(R.string.passwords_mismatch));
```

## 3. Workflow Definition Specification

### 3.1 Workflow Organization
Workflows are organized by **package name**, **application name**, and **version** to support:
- Multiple workflows per APK (e.g., login, purchase, settings workflows)
- Version-specific workflows (different workflows for different app versions)
- Clear organization and management of test scenarios

### 3.2 Workflow Definition Structure

```yaml
name: "APK_A Login Workflow"
description: "Login flow for target application"

metadata:
  app_package: "com.target.app"
  app_name: "Target Application"
  app_activity: ".MainActivity"
  version: "1.0"
  version_constraint: "1.0+"  # Supports version ranges

workflow_type: "functional"  # functional, performance, regression

priority: 1  # 1-5, 1 being highest

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

### 3.3 Multiple Workflows per APK
A single APK can have multiple workflows for different test scenarios:

#### Example: Multiple Workflows for Same APK
```
workflows/
└── com.target.app/
    └── Target Application/
        ├── 1.0/
        │   ├── login-workflow.yaml
        │   ├── purchase-workflow.yaml
        │   └── settings-workflow.yaml
        └── 2.0/
            ├── login-workflow.yaml  # Updated for v2.0 UI changes
            ├── purchase-workflow.yaml
            └── settings-workflow.yaml
```

### 3.4 Version-Specific Workflows
Workflows can be version-specific to handle UI/behavior changes between app versions:

#### Example: Version 2.0 Login Workflow
```yaml
name: "APK_A Login Workflow (v2.0)"
description: "Login flow for target application v2.0"

metadata:
  app_package: "com.target.app"
  app_name: "Target Application"
  app_activity: ".NewMainActivity"  # Changed in v2.0
  version: "2.0"
  version_constraint: "2.0+"

steps:
  - name: "Launch Application"
    action: "launch_app"
    package: "com.target.app"
    activity: ".NewMainActivity"  # Updated activity
    timeout: 30
    
  - name: "Click Login Button"
    action: "click"
    locator:
      strategies:
        - type: "id"
          value: "com.target.app:id/new_login_button"  # Updated ID in v2.0
        - type: "text"
          value: "Sign In"  # Updated text in v2.0
    timeout: 10
```

### 3.5 Workflow Selection Logic
The system selects appropriate workflows based on:
1. Exact package name match
2. App version compatibility check using version_constraint
3. Workflow type and priority
4. Device compatibility criteria

This organization ensures that:
- Each APK can have multiple workflows for different test scenarios
- Different versions of the same APK can have tailored workflows
- Workflows are easily manageable and maintainable
- The system can automatically select the right workflow for the right app version

## 4. App-Server Workflow Matching

### 4.1 Feature Overview
The Android app can trigger the server to scan the test device's installed APKs and match them with available workflows. This enables:
- Automatic discovery of compatible workflows for installed apps
- Easy selection of appropriate workflows for testing
- Dynamic workflow assignment based on device capabilities

### 4.2 Implementation Details

#### 4.2.1 API Endpoints

**Server API (FastAPI)**
```python
@app.post("/api/devices/{device_id}/scan-apks")
async def scan_device_apks(
    device_id: str,
    current_user: User = Depends(get_current_active_user)
):
    """Scan device for installed APKs and match with workflows"""
    # 1. Trigger ADB scan on device
    # 2. Extract APK information (package name, app name, version)
    # 3. Match with available workflows
    # 4. Return matched workflows to app
    return {
        "device_id": device_id,
        "scanned_at": datetime.utcnow(),
        "matched_workflows": []
    }

@app.get("/api/devices/{device_id}/matched-workflows")
async def get_matched_workflows(
    device_id: str,
    current_user: User = Depends(get_current_active_user)
):
    """Get previously matched workflows for device"""
    # Return cached matched workflows
    return {
        "device_id": device_id,
        "matched_workflows": []
    }
```

**Android App API Call**
```java
// In NetworkService.java
public void scanInstalledApks() {
    // Send scan request to server via HTTP
    String scanRequest = String.format("{\"device_id\": \"%s\"}", deviceId);
    
    // Using FastAPI endpoint
    authService.scanApks(deviceId, new AuthCallback() { /* ... */ });
}
```

#### 4.2.2 APK Scanning Process

**App-Based Scanning Approach** (Recommended)
1. **App Trigger**: User taps "Scan Installed APKs" button in app
2. **App PackageManager**: App uses `PackageManager` to get installed APKs:
   ```java
   PackageManager pm = getPackageManager();
   List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
   ```
3. **APK Information Extraction**: App extracts directly from device:
   - Package name
   - App name (using `pm.getApplicationLabel()`)
   - Version code and name (using `PackageInfo`)
4. **Send to Server**: App sends extracted APK info to server via HTTP/WebSocket
5. **Workflow Matching**: Server matches APK info with available workflows using:
   - Exact package name match
   - Version compatibility check
   - Device capability validation
6. **Result Return**: Server returns matched workflows to app via WebSocket or HTTP response
7. **App Display**: App displays matched workflows for user selection

**Benefits of App-Based Scanning**:
- More efficient: No need for server to execute ADB commands
- Faster response: Direct access to APK information
- Reduced server load: App handles resource-intensive scanning
- Better reliability: No dependency on ADB connection
- More secure: Less exposure of device information

#### 4.2.3 Workflow Matching Algorithm

```python
def match_workflows(apk_info_list, available_workflows):
    matched_workflows = []
    
    for apk_info in apk_info_list:
        # Extract APK details
        package_name = apk_info["package_name"]
        app_version = apk_info["version_name"]
        
        # Find matching workflows
        for workflow in available_workflows:
            # Check package name match
            if workflow["metadata"]["app_package"] != package_name:
                continue
            
            # Check version compatibility
            version_constraint = workflow["metadata"]["version_constraint"]
            if not is_version_compatible(app_version, version_constraint):
                continue
            
            # Check device compatibility (if workflow has device criteria)
            if not is_device_compatible(device_info, workflow.get("device_selection", {})):
                continue
            
            # Add to matched workflows
            matched_workflows.append({
                "apk_info": apk_info,
                "workflow": workflow
            })
    
    return matched_workflows
```

### 4.3 App-Server Interaction Flow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Android App    │     │   Autodroid     │     │  Test Device    │
│  (Test Device)  │────▶│     Server      │────▶│  (ADB Interface)│
└─────────────────┘     └─────────────────┘     └─────────────────┘
         │                         │                         │
         │ Request Workflow Scan   │                         │
         │────────────────────────▶│                         │
         │                         │ Execute ADB Scan        │
         │                         │────────────────────────▶│
         │                         │                         │
         │                         │ Extract APK Info        │
         │                         │◀────────────────────────│
         │                         │                         │
         │                         │ Match with Workflows    │
         │                         │                         │
         │ Return Matched Workflows│                         │
         │◀────────────────────────│                         │
         │                         │                         │
         │ Display Workflows       │                         │
         │ to User                 │                         │
         ▼                         ▼                         ▼
```

### 4.4 Benefits of This Approach

1. **Automatic Workflow Discovery**: Users don't need to manually find compatible workflows
2. **Dynamic Matching**: Workflows are matched based on actual device capabilities
3. **Version Awareness**: Only compatible workflows for the installed app version are returned
4. **Efficient Testing**: Users can quickly select appropriate workflows for testing
5. **Scalable Architecture**: Supports multiple devices and workflows simultaneously
6. **Real-time Updates**: Workflows are matched in real-time based on current device state

This feature enhances the usability of the Autodroid system by simplifying the workflow selection process and ensuring that only compatible workflows are presented to users.