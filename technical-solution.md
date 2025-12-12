# Technical Solution

## 1. Technology Stack Selection

### Core Frameworks (Updated for WorkScript Integration)
- **Automation Engine**: Pure ADB + UIAutomator (Removed Appium dependency)
- **Device Communication**: ADB + mDNS + FastAPI + WebSocket + WorkScript Engine
- **Container Runtime**: Docker + Podman
- **Programming Language**: Python 3.11+ (Server), Java/Kotlin (Android App)
- **WorkScript System**: Custom workscript engine for task automation
- **WorkPlan System**: Intelligent scheduling and execution framework

### Auxiliary Tools
- **APK Analysis**: aapt, apktool, jadx
- **UI Analysis**: ADB UIAutomator dump, custom element detection
- **Report Generation**: Allure, pytest-html, WorkScript execution reports
- **Configuration Management**: Pydantic, YAML, WorkScript definitions
- **QR Code Generation**: qrcode (Python), ZXing (Android)

### Development Tools
- **API Framework**: FastAPI (WorkScript API endpoints)
- **Testing Framework**: pytest, AndroidJUnitRunner, WorkScript test runner
- **Code Quality**: flake8, black, mypy, Android Lint
- **CI/CD**: GitHub Actions
- **Device Discovery**: mDNS (for lightweight device auto-discovery)
- **Real-time Communication**: FastAPI + WebSocket (WorkScript execution status)
- **Authentication**: Android Biometric API, JWT
- **Android Architecture**: MVVM (Model-View-ViewModel)
- **HTTP Client**: OkHttp
- **JSON Processing**: Gson
- **WorkScript Engine**: Custom Python engine for script execution

## 2. Core Module Design

### 2.1 Device Connection Pool (ADB-Based)
```python
class DeviceConnectionPool:
    def __init__(self, max_connections: int = 5):
        self.max_connections = max_connections
        self.active_connections: Dict[str, ADBDevice] = {}
        self.workscript_engine = WorkScriptEngine()
    
    async def execute_workscript(self, device_udid: str, workscript: WorkScript):
        """Execute WorkScript on specified device using pure ADB"""
        device = await self.get_device(device_udid)
        return await self.workscript_engine.execute(device, workscript)
    
    async def get_device(self, device_udid: str) -> ADBDevice:
        """Get ADB device connection (No Appium required)"""
        if device_udid not in self.active_connections:
            device = ADBDevice(device_udid)
            await device.connect()
            self.active_connections[device_udid] = device
        return self.active_connections[device_udid]
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

### 2.3 WorkScript Engine (New ADB-Based Implementation)
```python
class WorkScriptEngine:
    """
    Pure ADB-based WorkScript execution engine
    Replaces Appium with direct ADB commands and UIAutomator
    """
    
    def __init__(self):
        self.element_detector = ADBElementDetector()
        self.action_executor = ADBActionExecutor()
    
    async def execute(self, device: ADBDevice, workscript: WorkScript) -> ExecutionResult:
        """Execute WorkScript using pure ADB commands"""
        try:
            for step in workscript.steps:
                if step.action == "start_app":
                    await self.action_executor.start_app(device, step.package_name)
                elif step.action == "click":
                    element = await self.element_detector.find_by_id(device, step.element_id)
                    await self.action_executor.click(device, element)
                elif step.action == "send_keys":
                    element = await self.element_detector.find_by_id(device, step.element_id)
                    await self.action_executor.send_keys(device, element, step.text)
                elif step.action == "swipe":
                    await self.action_executor.swipe(device, step.start_coords, step.end_coords)
                
                await asyncio.sleep(step.delay or 0)
            
            return ExecutionResult(success=True, message="WorkScript executed successfully")
            
        except Exception as e:
            return ExecutionResult(success=False, error=str(e))

class ADBElementDetector:
    """Element detection using ADB UIAutomator dump"""
    
    async def find_by_id(self, device: ADBDevice, element_id: str) -> Element:
        """Find element by ID using UIAutomator dump"""
        # Use ADB to dump UI hierarchy
        ui_dump = await device.execute_command("uiautomator dump /sdcard/ui_dump.xml")
        await device.execute_command("pull /sdcard/ui_dump.xml /tmp/ui_dump.xml")
        
        # Parse XML and find element
        element = self.parse_ui_dump("/tmp/ui_dump.xml", element_id)
        return element
    
    def parse_ui_dump(self, xml_file: str, element_id: str) -> Element:
        """Parse UI dump XML to find element coordinates"""
        # Parse XML and extract bounds and center coordinates
        # Return Element object with x, y coordinates for ADB tap

class ADBActionExecutor:
    """Action execution using ADB commands"""
    
    async def start_app(self, device: ADBDevice, package_name: str):
        """Start app using ADB monkey command"""
        await device.execute_command(f"monkey -p {package_name} -c android.intent.category.LAUNCHER 1")
    
    async def click(self, device: ADBDevice, element: Element):
        """Tap element using ADB input tap"""
        await device.execute_command(f"input tap {element.center_x} {element.center_y}")
    
    async def send_keys(self, device: ADBDevice, element: Element, text: str):
        """Send text using ADB input text"""
        # First tap the element to focus
        await self.click(device, element)
        await asyncio.sleep(0.5)
        # Then input text
        await device.execute_command(f"input text '{text}'")
    
    async def swipe(self, device: ADBDevice, start_coords: tuple, end_coords: tuple):
        """Swipe using ADB input swipe"""
        await device.execute_command(f"input swipe {start_coords[0]} {start_coords[1]} {end_coords[0]} {end_coords[1]}")
```

### 2.4 Event-Driven WorkPlan Execution
```python
class WorkPlanEventListener:
    async def handle_new_workplan_trigger(self, trigger_data: dict):
        """Handle new WorkPlan execution triggers"""
        # Find matching WorkPlans
        matching_plans = self.workplan_engine.find_matching_plans(trigger_data)
        
        for plan in matching_plans:
            if await self.can_execute_now(plan):
                # Execute using ADB-based WorkScript engine
                result = await self.workscript_engine.execute_plan(plan, trigger_data)
                await self.report_execution_result(plan, result)
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

### 2.6 Server Connection & Dynamic UI Updates (Android App)

#### Implementation
- **NetworkService**: Background service handling mDNS discovery with real-time status updates
- **DiscoveryStatusManager**: Singleton pattern managing discovery status across the app
- **LiveData Observers**: Reactive UI updates based on discovery state changes
- **Failure Handling**: Comprehensive error handling with user-friendly status messages

#### Key Components
```kotlin
// NetworkService.kt - Background service for mDNS discovery
class NetworkService : Service() {
    private fun notifyDiscoveryFailedListeners() {
        Log.d(TAG, "Notifying discovery failed listeners")
        // Update DiscoveryStatusManager with failure status
        DiscoveryStatusManager.updateDiscoveryFailed(true)
        Log.d(TAG, "Discovery failure notification sent")
    }
}

// DashboardFragment.kt - UI state management
private val discoveryFailedObserver = Observer<Boolean> { failed ->
    if (failed == true) {
        val elapsedTime = (System.currentTimeMillis() - discoveryStartTime) / 1000
        updateConnectionStatus("mDNS Failed after ${elapsedTime}s")
        serverInfoTextView?.text = "Discovery failed"
        serverStatusTextView?.text = "FAILED"
        scanQrButton?.isEnabled = true
        scanQrButton?.text = "Scan QR Code"
        Log.d(TAG, "Discovery failed after ${elapsedTime}s")
    } else {
        // Reset UI when discovery is restarted
        updateConnectionStatus("mDNS Discovering...")
        serverInfoTextView?.text = "Searching..."
        serverStatusTextView?.text = "SEARCHING"
        scanQrButton?.isEnabled = false
        scanQrButton?.text = "Scan QR Code"
    }
}
```

#### Status Flow
1. **Discovery Starting**: "mDNS Discovering..." with disabled QR button
2. **Discovery Failed**: "mDNS Failed after Xs" with enabled QR button
3. **Discovery Restarted**: Reset to initial state for retry
4. **Service Found**: Update with server information and connection options

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

## 3. WorkScript Definition Specification

### 3.1 WorkScript Organization
WorkScripts are organized by **package name**, **application name**, and **version** to support:
- Multiple workscripts per APK (e.g., login, purchase, settings workscripts)
- Version-specific workscripts (different workscripts for different app versions)
- Clear organization and management of test scenarios

### 3.2 WorkScript Definition Structure (ADB-Based)

```yaml
name: "APK_A Login WorkScript (ADB Edition)"
description: "Login flow for target application using pure ADB commands"

metadata:
  app_package: "com.target.app"
  app_name: "Target Application"
  app_activity: ".MainActivity"
  version: "1.0"
  version_constraint: "1.0+"
  execution_engine: "adb"  # Specifies ADB-based execution

workscript_type: "functional"  # functional, performance, regression

priority: 1  # 1-5, 1 being highest

device_selection:
  strategy: "auto"
  criteria:
    min_battery: 20
    android_version: "9.0+"

steps:
  - name: "Launch Application"
    action: "start_app"
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
    
  - name: "Enter Username"
    action: "send_keys"
    locator:
      strategies:
        - type: "id"
          value: "com.target.app:id/username_input"
    text: "test_user"
    timeout: 10
    
  - name: "Enter Password"
    action: "send_keys"
    locator:
      strategies:
        - type: "id"
          value: "com.target.app:id/password_input"
    text: "test_password"
    timeout: 10
    
  - name: "Submit Login"
    action: "click"
    locator:
      strategies:
        - type: "id"
          value: "com.target.app:id/submit_button"
    timeout: 10

schedule:
  type: "daily"
  start_time: "09:00"
  end_time: "18:00"
  interval_minutes: 120
```

### 3.3 Multiple WorkScripts per APK
A single APK can have multiple workscripts for different test scenarios:

#### Example: Multiple WorkScripts for Same APK
```
workscripts/
└── com.target.app/
    └── Target Application/
        ├── 1.0/
        │   ├── login-workscript.yaml
        │   ├── purchase-workscript.yaml
        │   └── settings-workscript.yaml
        └── 2.0/
            ├── login-workscript.yaml  # Updated for v2.0 UI changes
            ├── purchase-workscript.yaml
            └── settings-workscript.yaml
```

### 3.4 Version-Specific WorkScripts
WorkScripts can be version-specific to handle UI/behavior changes between app versions:

#### Example: Version 2.0 Login WorkScript
```yaml
name: "APK_A Login WorkScript (v2.0)"
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

### 3.5 WorkScript Selection Logic (ADB-Enhanced)
The system selects appropriate workscripts based on:
1. Exact package name match
2. App version compatibility check using version_constraint
3. WorkScript type and priority
4. Device compatibility criteria
5. **Execution engine compatibility** (ADB-based vs legacy Appium)

This organization ensures that:
- Each APK can have multiple workscripts for different test scenarios
- Different versions of the same APK can have tailored workscripts
- WorkScripts are easily manageable and maintainable
- The system can automatically select the right workscript for the right app version
- **ADB-based execution provides faster, more reliable automation without Appium overhead**

### 3.6 Migration from Appium to ADB

#### Key Changes:
1. **Removed Appium Dependency**: All automation now uses pure ADB commands
2. **Direct UIAutomator Integration**: Uses `uiautomator dump` for element detection
3. **Faster Execution**: No Appium server overhead, direct device communication
4. **Better Reliability**: Eliminates Appium server connection issues
5. **Simplified Architecture**: Single ADB connection per device

#### Action Mapping (Appium → ADB):
- `driver.find_element_by_id()` → `uiautomator dump` + XML parsing
- `element.click()` → `adb input tap x y`
- `element.send_keys()` → `adb input text 'text'`
- `driver.swipe()` → `adb input swipe x1 y1 x2 y2`
- `driver.start_activity()` → `adb shell am start -n package/activity`

#### Benefits:
- **No Appium Server**: Eliminates server setup and maintenance
- **Faster Element Detection**: Direct UI dump parsing
- **Lower Resource Usage**: No additional Java processes
- **Better Error Handling**: Direct ADB error reporting
- **Simplified Deployment**: Only ADB required on host system

## 5. QR Code Connection Feature

### 5.1 Overview
The QR code connection feature allows the mobile app to quickly connect to the server by scanning a QR code displayed on the server's connection status page. This eliminates the need for manual IP address entry and provides a seamless setup experience.

### 5.2 Server-side QR Code Generation

#### 5.2.1 QR Code Content Structure
The QR code contains a JSON object with the following information:
```json
{
  "server_name": "AutoDroid Server",
  "api_endpoint": "http://192.168.1.59:8003/api",
  "version": "1.0.0",
  "timestamp": "2023-11-15T10:30:00Z"
}
```

#### 5.2.2 Implementation (Python)
```python
# qr_service.py
import qrcode
import json
from datetime import datetime
from fastapi import APIRouter, Depends
from io import BytesIO
import base64

router = APIRouter()

@router.get("/api/qr-code")
async def generate_qr_code():
    """Generate QR code containing server connection information"""
    # Get server information
    server_info = {
        "server_name": "AutoDroid Server",
        "api_endpoint": f"http://{get_server_ip()}:8003/api",
        "version": "1.0.0",
        "timestamp": datetime.utcnow().isoformat()
    }
    
    # Generate QR code
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        box_size=10,
        border=4,
    )
    qr.add_data(json.dumps(server_info))
    qr.make(fit=True)
    
    # Convert to base64 image
    img = qr.make_image(fill_color="black", back_color="white")
    buffer = BytesIO()
    img.save(buffer, format="PNG")
    img_str = base64.b64encode(buffer.getvalue()).decode()
    
    return {
        "qr_code": f"data:image/png;base64,{img_str}",
        "server_info": server_info
    }
```

#### 5.2.3 Frontend Integration
```javascript
// ConnectionStatus.svelte
<script>
  import { onMount } from 'svelte';
  
  let qrCodeData = '';
  let serverInfo = {};
  
  onMount(async () => {
    const response = await fetch('/api/qr-code');
    const data = await response.json();
    qrCodeData = data.qr_code;
    serverInfo = data.server_info;
  });
</script>

<div class="connection-status">
  <h2>Connection Status</h2>
  <div class="server-info">
    <p>Server: {serverInfo.server_name}</p>
    <p>API Endpoint: {serverInfo.api_endpoint}</p>
  </div>
  
  <div class="qr-code-container">
    <h3>Scan to Connect</h3>
    <img src={qrCodeData} alt="QR Code for server connection" />
    <p>Scan this QR code with AutoDroid mobile app to connect</p>
  </div>
</div>
```

### 5.3 Mobile App QR Code Scanning

#### 5.3.1 Implementation (Android)
```java
// QRCodeScannerFragment.java
public class QRCodeScannerFragment extends Fragment {
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private CodeScanner mCodeScanner;
    private QRCodeScanCallback callback;
    
    public interface QRCodeScanCallback {
        void onQRCodeScanned(ServerConnectionInfo serverInfo);
        void onScanError(String error);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_qr_code_scanner, container, false);
        
        // Setup camera and scanner
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            setupScanner(root);
        }
        
        return root;
    }
    
    private void setupScanner(View root) {
        ScannerView scannerView = root.findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(getContext(), scannerView);
        
        mCodeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull Result result) {
                getActivity().runOnUiThread(() -> {
                    try {
                        // Parse QR code content
                        String qrContent = result.getText();
                        ServerConnectionInfo serverInfo = parseQRCodeContent(qrContent);
                        
                        if (callback != null) {
                            callback.onQRCodeScanned(serverInfo);
                        }
                        
                        // Go back to previous screen
                        Navigation.findNavController(getView()).navigateUp();
                    } catch (Exception e) {
                        if (callback != null) {
                            callback.onScanError("Invalid QR code format");
                        }
                    }
                });
            }
        });
        
        scannerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCodeScanner.startPreview();
            }
        });
    }
    
    private ServerConnectionInfo parseQRCodeContent(String qrContent) throws JSONException {
        JSONObject json = new JSONObject(qrContent);
        
        return new ServerConnectionInfo(
            json.getString("server_name"),
            json.getString("api_endpoint"),
            json.getString("version"),
            json.getString("timestamp")
        );
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (mCodeScanner != null) {
            mCodeScanner.startPreview();
        }
    }
    
    @Override
    public void onPause() {
        if (mCodeScanner != null) {
            mCodeScanner.releaseResources();
        }
        super.onPause();
    }
}
```

#### 5.3.2 Data Model
```java
// ServerConnectionInfo.java
public class ServerConnectionInfo {
    private String serverName;
    private String apiEndpoint;
    private String version;
    private String timestamp;
    
    public ServerConnectionInfo(String serverName, String apiEndpoint, 
                              String version, String timestamp) {
        this.serverName = serverName;
        this.apiEndpoint = apiEndpoint;
        this.version = version;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getServerName() { return serverName; }
    public String getApiEndpoint() { return apiEndpoint; }
    public String getVersion() { return version; }
    public String getTimestamp() { return timestamp; }
    
    // Validate if the connection info is still valid (e.g., not too old)
    public boolean isValid() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date timestampDate = sdf.parse(timestamp);
            Date now = new Date();
            
            // Consider valid if less than 24 hours old
            long diffInMs = Math.abs(now.getTime() - timestampDate.getTime());
            long diffInHours = diffInMs / (60 * 60 * 1000);
            
            return diffInHours < 24;
        } catch (Exception e) {
            return false;
        }
    }
}
```

#### 5.3.3 Integration with Connection Flow
```java
// ConnectionFragment.java
public class ConnectionFragment extends Fragment implements QRCodeScannerFragment.QRCodeScanCallback {
    private TextView serverInfoText;
    private Button scanQRButton;
    private Button manualConnectButton;
    private ConnectionViewModel viewModel;
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        scanQRButton.setOnClickListener(v -> {
            Navigation.findNavController(v)
                .navigate(R.id.action_connectionFragment_to_qrCodeScannerFragment);
        });
        
        manualConnectButton.setOnClickListener(v -> {
            showManualInputDialog();
        });
    }
    
    @Override
    public void onQRCodeScanned(ServerConnectionInfo serverInfo) {
        if (serverInfo.isValid()) {
            viewModel.setServerEndpoint(serverInfo.getApiEndpoint());
            serverInfoText.setText("Connected to: " + serverInfo.getServerName());
            
            // Test connection
            viewModel.testConnection();
        } else {
            Toast.makeText(getContext(), "QR code is expired", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onScanError(String error) {
        Toast.makeText(getContext(), "Scan error: " + error, Toast.LENGTH_SHORT).show();
    }
}
```

### 5.4 Security Considerations

1. **QR Code Expiration**: QR codes include timestamps and are considered valid for 24 hours
2. **HTTPS Support**: The system supports HTTPS endpoints for secure connections
3. **Authentication**: After QR code scanning, normal authentication flow is still required
4. **Validation**: The mobile app validates the QR code format before using the connection information

### 5.5 Fallback Mechanism

If QR code scanning fails or is not available, users can:
1. Manually enter the server IP address and port
2. Use mDNS discovery (if enabled)
3. Select from previously connected servers

## 6. App-Server WorkScript Matching

### 4.1 Feature Overview
The Android app can trigger the server to scan the test device's installed APKs and match them with available workscripts. This enables:
- Automatic discovery of compatible workscripts for installed apps
- Easy selection of appropriate workscripts for testing
- Dynamic workscript assignment based on device capabilities

### 4.2 Implementation Details

#### 4.2.1 API Endpoints

**Server API (FastAPI)**
```python
@app.post("/api/devices/{device_id}/scan-apks")
async def scan_device_apks(
    device_id: str,
    current_user: User = Depends(get_current_active_user)
):
    """Scan device for installed APKs and match with workscripts"""
    # 1. Trigger ADB scan on device
    # 2. Extract APK information (package name, app name, version)
    # 3. Match with available workscripts
    # 4. Return matched workscripts to app
    return {
        "device_id": device_id,
        "scanned_at": datetime.utcnow(),
        "matched_workscripts": []
    }

@app.get("/api/devices/{device_id}/matched-workscripts")
async def get_matched_workscripts(
    device_id: str,
    current_user: User = Depends(get_current_active_user)
):
    """Get previously matched workscripts for device"""
    # Return cached matched workscripts
    return {
        "device_id": device_id,
        "matched_workscripts": []
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

#### 4.2.3 WorkScript Matching Algorithm

```python
def match_workscripts(apk_info_list, available_workscripts):
    matched_workscripts = []
    
    for apk_info in apk_info_list:
        # Extract APK details
        package_name = apk_info["package_name"]
        app_version = apk_info["version_name"]
        
        # Find matching workflows
        for workscript in available_workscripts:
            # Check package name match
            if workscript["metadata"]["app_package"] != package_name:
                continue
            
            # Check version compatibility
            version_constraint = workflow["metadata"]["version_constraint"]
            if not is_version_compatible(app_version, version_constraint):
                continue
            
            # Check device compatibility (if workflow has device criteria)
            if not is_device_compatible(device_info, workflow.get("device_selection", {})):
                continue
            
            # Add to matched workflows
            matched_workscripts.append({
                "apk_info": apk_info,
                "workscript": workscript
            })
    
    return matched_workscripts
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
4. **Efficient Testing**: Users can quickly select appropriate workscripts for testing
5. **Scalable Architecture**: Supports multiple devices and workflows simultaneously
6. **Real-time Updates**: Workflows are matched in real-time based on current device state

This feature enhances the usability of the Autodroid system by simplifying the workflow selection process and ensuring that only compatible workflows are presented to users.