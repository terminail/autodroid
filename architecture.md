# System Architecture

## 1. Overall Architecture

```mermaid
flowchart TD
    subgraph ContainerServer[Container Server]
        subgraph CoreComponents[Core Components]
            WorkScriptEngine[WorkScript Engine]
            WorkplanSystem[Workplan System]
            DeviceManagement[Device Management]
        end
        
        RESTfulAPI[RESTful API Service]
        
        CoreComponents --> RESTfulAPI
    end
    
    subgraph CommunicationLayer[Message Communication and Coordination Layer]
        mDNS[mDNS - Device Discovery]
        HTTP[RESTful API - FastAPI]
        WebSocket[WebSocket - Real-time Communication]
        ADB[Android Debug Bridge]
        QRCode[QR Code - Quick Connection]
    end
    
    subgraph TestDevices[Test Devices]
        direction LR
        TestDeviceA[Test Device A] --> TestDeviceB[Test Device B] --> TestDeviceC[Test Device C]
        
        subgraph TestDeviceA
            ProxyAppA[Manager App]
        end
        
        subgraph TestDeviceB
            ProxyAppB[Manager App]
        end
        
        subgraph TestDeviceC
            ProxyAppC[Manager App]
        end
    end
    
    ContainerServer --> mDNS
    ContainerServer --> HTTP
    ContainerServer --> WebSocket
    ContainerServer --> ADB
    ContainerServer --> QRCode

    mDNS --> TestDevices
    HTTP --> TestDevices
    WebSocket --> TestDevices
    ADB --> TestDevices
    QRCode --> TestDevices
    
    %% Styling - Using black text with white background for better clarity
    classDef container stroke:#000,stroke-width:2px,fill:#fff,color:#000;
    classDef component stroke:#000,stroke-width:2px,fill:#fff,color:#000;
    classDef communication stroke:#000,stroke-width:2px,fill:#fff,color:#000;
    classDef device stroke:#000,stroke-width:2px,fill:#fff,color:#000;
    
    class ContainerServer container;
    class WorkScriptEngine,SchedulingSystem,DeviceManagement,RESTfulAPI component;
    class mDNS,HTTP,WebSocket,ADB,QRCode communication;
    class TestDeviceA,TestDeviceB,TestDeviceC,ProxyAppA,ProxyAppB,ProxyAppC device;
```

### Architecture Components

1. **Container Server**
   - Hosts the core automation components
   - Manages containerized services
   - Provides RESTful API for external communication

2. **Core Components**
   - **WorkScript Engine**: Executes automation workflows defined in YAML
   - **Workplan System**: Manages scheduled and event-driven tasks
   - **Device Management**: Handles device discovery, connection, and monitoring

3. **Communication Layer**
   - Uses mDNS for lightweight device auto-discovery in local network
   - Uses FastAPI for RESTful API communication
   - Uses WebSocket for real-time messaging between server and devices
   - Uses QR codes for quick and easy server connection setup
   - Connects the container server with test devices
   - Enables automatic device discovery in local network

4. **Test Devices**
   - Android devices running target applications
   - Equipped with manager app for enhanced communication
   - Supports both USB and network ADB connections

## 2. Container Service Architecture

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

### Container Services

1. **appium-server**
   - Provides Appium automation capabilities
   - Exposes port 4723 for API access
   - Runs in privileged mode for device access

2. **automation-core**
   - Core automation engine
   - Processes workflow definitions
   - Generates reports
   - Mounts volumes for workflows and reports

3. **device-manager**
   - Manages device connections
   - Provides USB and network ADB support
   - Monitors device status

### Service Interactions

- The `automation-core` service communicates with `appium-server` to execute UI automation commands
- The `device-manager` service handles device discovery and management using mDNS
- All services work together to provide a complete automation solution
- Volumes are used to persist workflows and reports outside containers

## 3. QR Code Connection Architecture

### 3.1 Overview

The QR code connection feature provides a user-friendly way for mobile devices to quickly connect to the AutoDroid server by scanning a QR code displayed on the server's connection status page. This eliminates the need for manual IP address entry and provides a seamless setup experience.

### 3.2 QR Code Connection Flow

```mermaid
sequenceDiagram
    participant User
    participant Server as AutoDroid Server
    participant MobileApp as Mobile App
    participant Camera as Phone Camera
    
    User->>Server: Access connection status page
    Server->>Server: Generate QR code with server info
    Server->>User: Display QR code on web interface
    User->>MobileApp: Open connection screen
    User->>MobileApp: Tap "Scan QR Code"
    MobileApp->>Camera: Request camera permission
    Camera-->>MobileApp: Permission granted
    MobileApp->>Camera: Start camera preview
    User->>Camera: Point camera at QR code
    Camera->>MobileApp: Capture QR code image
    MobileApp->>MobileApp: Decode QR code content
    MobileApp->>MobileApp: Validate server information
    MobileApp->>Server: Test connection with extracted endpoint
    Server-->>MobileApp: Connection successful
    MobileApp->>User: Display connected status
```

### 3.3 QR Code Content Structure

The QR code contains a JSON object with the following information:

```json
{
  "server_name": "AutoDroid Server",
  "api_endpoint": "http://192.168.1.59:8003/api",
  "version": "1.0.0",
  "timestamp": "2023-11-15T10:30:00Z"
}
```

### 3.4 Security Considerations

1. **QR Code Expiration**: QR codes include timestamps and are considered valid for 24 hours
2. **HTTPS Support**: The system supports HTTPS endpoints for secure connections
3. **Authentication**: After QR code scanning, normal authentication flow is still required
4. **Validation**: The mobile app validates the QR code format before using the connection information

### 3.5 Fallback Mechanisms

If QR code scanning fails or is not available, users can:
1. Manually enter the server IP address and port
2. Use mDNS discovery (if enabled)
3. Select from previously connected servers

### 3.6 Implementation Components

#### Server-side Components:
- QR Code Generation Service (Python/qrcode)
- Connection Status API Endpoint
- Frontend QR Code Display Component

#### Mobile App Components:
- QR Code Scanner Fragment (Android/ZXing)
- Server Connection Info Data Model
- Connection Validation Logic
- Camera Permission Handling