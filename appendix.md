# Appendix

## 1. Troubleshooting

### 1.1 Common Issues

| Issue | Solution |
|-------|----------|
| **ADB connection failure** | Check USB debugging is enabled on the device, verify USB cable connection, or check network connection for wireless ADB |
| **Element positioning failure** | Use multiple positioning strategy combinations, ensure the element is visible, increase timeout values |
| **Container permission issues** | Run containers in privileged mode, ensure proper device mapping in docker-compose |
| **Insufficient device resources** | Adjust concurrent task count, close other apps on test devices, use devices with higher specifications |
| **Appium server errors** | Check Appium logs for detailed error messages, verify Java and Android SDK installations |
| **Container startup failure** | Check container logs with `docker-compose logs`, verify all dependencies are properly installed |

### 1.2 Debugging Tools
```bash
# Check device connection
adb devices
adb shell dumpsys window

# Check device status
adb shell dumpsys battery
adb shell df -h
adb shell netstat

# Check Appium service
appium --log-level debug

# View container logs
docker-compose logs autodroid-core
docker-compose logs appium-server
docker-compose logs device-manager

# Check API status
curl http://localhost:8000/api/health
curl http://localhost:8000/api/devices
```

## 2. Performance Optimization Suggestions

### 2.1 Container Configuration
- Allocate sufficient memory to containers (recommended 2GB+)
- Use SSD storage to improve IO performance
- Optimize network configuration to reduce latency
- Limit container resource usage based on host capabilities

### 2.2 Test Optimization
- Use Resource ID priority positioning strategy for faster element locating
- Reasonably set timeout and retry parameters to avoid unnecessary delays
- Avoid unnecessary screenshot operations during test execution
- Use data-driven approach to reduce duplicate code
- Implement parallel test execution for faster results
- Clean up test data after each test run

## 3. Extension Development Guide

### 3.1 Adding New Positioning Strategies

To add a custom element positioning strategy:

```python
class CustomLocatorStrategy:
    def locate_element(self, strategy: str, value: str):
        if strategy == "custom":
            return self.find_by_custom_method(value)
    
    def find_by_custom_method(self, value: str):
        # Implement your custom positioning logic here
        # Example: Find element by custom attribute
        return self.driver.find_element_by_xpath(f"//*[@custom-attr='{value}']")
```

### 3.2 Integrating New Test Data Sources

To integrate a custom test data source:

```python
class CustomDataProvider:
    async def get_test_data(self, source_config: dict):
        """Implement custom data retrieval logic"""
        # Example: Get data from a REST API
        import aiohttp
        
        url = source_config.get("url")
        headers = source_config.get("headers", {})
        
        async with aiohttp.ClientSession() as session:
            async with session.get(url, headers=headers) as response:
                if response.status == 200:
                    return await response.json()
                else:
                    raise Exception(f"Failed to fetch test data: {response.status}")
```

### 3.3 Extending Workflow Actions

To add a new action type to the workflow engine:

```python
class CustomActionExecutor:
    def __init__(self, driver):
        self.driver = driver
    
    def execute(self, action: dict):
        action_type = action.get("type")
        
        if action_type == "custom_action":
            return self._execute_custom_action(action)
        # Handle other action types...
    
    def _execute_custom_action(self, action: dict):
        # Implement your custom action logic here
        # Example: Perform a complex gesture
        from appium.webdriver.common.touch_action import TouchAction
        
        start_x = action.get("start_x", 100)
        start_y = action.get("start_y", 100)
        end_x = action.get("end_x", 200)
        end_y = action.get("end_y", 200)
        
        touch_action = TouchAction(self.driver)
        touch_action.press(x=start_x, y=start_y).move_to(x=end_x, y=end_y).release().perform()
        
        return {"status": "success", "message": "Custom action executed"}
```

## 4. Glossary

| Term | Definition |
|------|------------|
| **ADB** | Android Debug Bridge, a command-line tool for communicating with Android devices |
| **Appium** | An open-source tool for automating native, mobile web, and hybrid applications |
| **UI Automator 2** | An automation framework for Android devices, used by Appium for UI interactions |
| **Container** | A lightweight, standalone, executable package that includes everything needed to run an application |
| **Docker** | A platform for developing, shipping, and running applications in containers |
| **Podman** | An open-source container runtime that provides a Docker-compatible command line interface |
| **Workflow** | A sequence of automated steps defined in YAML format to test an application |
| **Element Locator** | A method to identify UI elements in an application, such as ID, text, or XPath |