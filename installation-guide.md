# Installation Guide

## 1. Environment Preparation

### 1.1 Dependency Installation

#### Java SDK Installation
```bash
# Ubuntu/Debian
sudo apt install openjdk-11-jdk

# macOS
brew install openjdk@11

# Set environment variables
export JAVA_HOME=/path/to/jdk
export PATH=$JAVA_HOME/bin:$PATH
```

#### Android SDK Installation
```bash
# Install via Android Studio or command line tools
sdkmanager --install "platform-tools" "platforms;android-33"

# Set environment variables
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$PATH
```

#### ADB Tools Installation (Appium-Free)
```bash
# Install Android platform tools (includes ADB)
sdkmanager --install "platform-tools"

# Verify ADB installation
adb version

# No Appium installation required - pure ADB automation!
```

### 1.2 Device Configuration
```bash
# Enable developer options and USB debugging on your Android device first
adb devices

# Enable network ADB (optional, for wireless connection)
adb tcpip 5555
adb connect 192.168.1.100:5555
```

## 2. APK Analysis Process

### 2.1 APK Information Scanning
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

### 2.2 Element Location Analysis

#### Using ADB UIAutomator (Appium-Free)
1. Get UI hierarchy dump:
```bash
adb shell uiautomator dump /sdcard/ui_dump.xml
adb pull /sdcard/ui_dump.xml
```

2. Analyze XML structure to identify elements:
```bash
# Parse XML and find elements by ID, text, or other attributes
python tools/parse_uiautomator.py ui_dump.xml
```

3. Test element interaction:
```bash
# Test click on element by coordinates
adb shell input tap 500 1000

# Test text input
adb shell input text "test input"
```

4. Record element location strategies (ID > Text > Coordinates > XPath)

### 2.3 WorkScript Template Generation
```python
# Automatically generate basic workscript
workscript_file = selector.generate_workscript_template(selected_app)
```

## 3. Containerized Deployment

### 3.1 Build Docker Image
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

### 3.2 Multi-Platform Deployment Configuration

#### Linux/WSL
```yaml
# docker-compose.linux.yml
services:
  autodroid-core:
    devices:
      - /dev/kvm:/dev/kvm
      - /dev/bus/usb:/dev/bus/usb
    privileged: true
```

#### macOS
```yaml
# docker-compose.macos.yml  
services:
  autodroid-core:
    privileged: true
    extra_hosts:
      - "host.docker.internal:host-gateway"
```

#### Windows
```yaml
# docker-compose.windows.yml
services:
  autodroid-core:
    isolation: process
    privileged: true
```

## 4. Verify Installation

1. Check if all dependencies are installed correctly:
   ```bash
   # Verify ADB is working
   adb version
   
   # Test device connection
   adb devices
   ```

2. Verify ADB connection:
   ```bash
   adb devices
   ```

3. **No Appium server required - pure ADB automation!**

4. Build and run the containers to verify the complete setup:
   ```bash
   docker-compose build
   docker-compose up -d
   curl http://localhost:8000/api/health
   ```

## Next Steps

After successful installation, proceed to the [Usage Guide](usage-guide.md) to learn how to create and run your first test plan.