# Usage Guide

## 1. Quick Start

### 1.1 Environment Preparation
```bash
# 1. Install dependencies
./scripts/install_dependencies.sh

# 2. Connect test devices
adb devices

# 3. Scan APK applications
python tools/apk_scanner.py
```

### 1.2 Container Deployment
```bash
# 1. Build images
docker-compose build

# 2. Start services
docker-compose up -d

# 3. Verify services
curl http://localhost:8000/api/health
```

### 1.3 Create Test Plan
```bash
# 1. Select target APK
python tools/interactive_apk_selector.py

# 3. Use ADB UIAutomator to analyze elements
#    - Use `adb shell uiautomator dump` to get UI hierarchy
#    - Parse XML output to identify element properties
#    - Use ADB commands for element interaction testing

# 3. Edit workflow file
#    - Create a new YAML file in the workflows directory
#    - Follow the workflow specification from technical-solution.md

# 4. Submit test plan
#    - Use the API or UI to submit your workscript file
```

## 2. Daily Operations

### 2.1 Monitor Service Status
```bash
# Check container status
docker-compose ps

# View logs
docker-compose logs -f

# Monitor device connections
curl http://localhost:8000/api/devices
```

### 2.2 Manage Test Plans
```bash
# List all plans
curl http://localhost:8000/api/plans

# Create new plan
curl -X POST http://localhost:8000/api/plans \
  -H "Content-Type: application/json" \
  -d '{"template_id": "apk_a", "schedule": {...}}'

# Get plan details
curl http://localhost:8000/api/plans/{plan_id}

# Update plan
curl -X PUT http://localhost:8000/api/plans/{plan_id} \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'

# Delete plan
curl -X DELETE http://localhost:8000/api/plans/{plan_id}
```

## 3. Workflow Management

### 3.1 Create Custom Workflows

1. **Create a new workflow file** in the `workflows/` directory with `.yml` extension
2. **Follow the workflow specification** from [technical-solution.md](technical-solution.md#3-workflow-definition-specification)
3. **Test the workflow** using the API or UI
4. **Schedule the workflow** using the workplan system

### 3.2 Example Workflow Structure

```yaml
name: "Custom Test Flow"
description: "My custom test workflow"

metadata:
  app_package: "com.example.app"
  app_activity: ".MainActivity"
  version: "1.0"

device_selection:
  strategy: "auto"
  criteria:
    min_battery: 30
    android_version: "8.0+"

steps:
  - name: "Launch App"
    action: "launch_app"
    package: "com.example.app"
    activity: ".MainActivity"
    timeout: 30
    
  - name: "Click Button"
    action: "click"
    locator:
      strategies:
        - type: "id"
          value: "com.example.app:id/button_id"
    timeout: 10
    
  - name: "Enter Text"
    action: "input_text"
    locator:
      strategies:
        - type: "text"
          value: "Input Field"
    text: "Test input"
    timeout: 10
```

## 4. Report Management

### 4.1 Access Reports

Reports are generated automatically after each test run and stored in the `reports/` directory.

```bash
# List available reports
ls -la reports/

# View HTML report in browser
open reports/{report_id}/index.html
```

### 4.2 Report Formats

- **HTML**: Interactive reports with screenshots and detailed logs
- **JSON**: Machine-readable format for integration with other systems
- **Allure**: Enhanced reporting with charts and statistics (if Allure is enabled)

## 5. Troubleshooting

For common issues and debugging tips, refer to the [Appendix](appendix.md#1-troubleshooting).