# Autodroid Trader Server

Autodroid Trader Server 是 Autodroid Android 自动化系统的服务端组件，提供设备管理、工作脚本执行、测试调度等功能。

## 环境要求

- Python 3.13+
- pip 22.0+
- Node.js 18+ (Appium 需要)
- Java 11+ (Android SDK 需要)

## 安装

### 1. 安装 Node.js (Appium 依赖)

#### Windows:
```bash
# 下载并安装 Node.js 18.x LTS
# 访问 https://nodejs.org/ 下载安装包或使用以下命令
winget install OpenJS.NodeJS.LTS
```

#### macOS/Linux:
```bash
# 使用包管理器安装
# Ubuntu/Debian:
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# macOS (使用 Homebrew):
brew install node@18
```

### 2. 安装 Appium

```bash
# 全局安装 Appium
npm install -g appium

# 安装 Android 驱动
appium driver install uiautomator2

# 验证安装
appium driver list
```

### 3. 使用 Conda 创建并激活虚拟环境：
```bash
# 创建虚拟环境
cd autodroid-trader-server
conda create -n autodroid python=3.13.5

# 激活虚拟环境
conda activate autodroid
```

### 4. 安装 Python 依赖（使用中国镜像提高速度）：
```bash
# 使用阿里云镜像
pip install -e . -i https://mirrors.aliyun.com/pypi/simple/

# 或使用腾讯云镜像
# pip install -e . -i https://mirrors.cloud.tencent.com/pypi/simple/

# 或使用华为云镜像
# pip install -e . -i https://mirrors.huaweicloud.com/repository/pypi/simple/


cd /d/git/autodroid/autodroid-trader-server && conda activate autodroid && pip install -e . -v --index-url https://pypi.tuna.tsinghua.edu.cn/simple

```

## 启动服务器

### 非 Docker 方式

1. 确保虚拟环境已激活

2. 启动 Appium 服务器（新终端窗口）：
   ```bash
   appium --port 4723 --base-path /wd/hub
   ```

3. 启动 Autodroid 服务器（推荐方式）：
   ```bash
   cd 'd:/git/autodroid/autodroid-trader-server'; conda activate autodroid; python run_server.py
   ```

4. **Windows用户**：
   ```bash
   cd 'd:/git/autodroid/autodroid-trader-server'; conda activate autodroid; ./start_server.bat
   ```

### Docker 方式

1. 构建 Docker 镜像：
   ```bash
   cd workscripts
   docker build -t autodroid-test .
   ```

2. 运行 Docker 容器：
   ```bash
   docker run -it --rm \
     -e PHONE_IP=192.168.1.100 \
     -e APP_PACKAGE=com.your.app \
     -e APPIUM_SERVER=http://host.docker.internal:4723 \
     -p 5555:5555 \
     -p 4723:4723 \
     -v $(pwd)/screenshots:/app/screenshots \
     autodroid-test
   ```

## API 访问示例

### 1. 健康检查

```bash
curl http://localhost:8004/api/health
```

响应示例：
```json
{
  "status": "healthy",
  "timestamp": 1733123456.789,
  "services": {
    "device_manager": "running",
    "scheduler": "running"
  }
}
```

### 2. 获取服务器信息

```bash
curl http://localhost:8004/api/server
```

响应示例：
```json
{
  "name": "Autodroid Server",
  "version": "1.0.0",
  "hostname": "your-hostname",
  "ip_address": "192.168.1.100",
  "platform": "Windows-10-10.0.19045-SP0",
  "python_version": "3.13.5",
  "services": {
    "device_manager": "running",
    "workscript_engine": "running",
    "scheduler": "running"
  },
  "capabilities": {
    "device_registration": true,
    "test_scheduling": true,
    "event_triggering": true
  },
  "api_endpoints": {
    "health": "/api/health",
    "devices": "/api/devices",
    "device_register": "/api/devices/register",
    "test_plans": "/api/plans"
  }
}
```

### 3. 注册设备

```bash
curl -X POST -H "Content-Type: application/json" -d '{"udid": "emulator-5554", "device_name": "Pixel 6 Pro", "android_version": "13", "battery_level": 80, "connection_type": "network"}' http://localhost:8004/api/devices/register
```

响应示例：
```json
{
  "message": "Device registered successfully",
  "device": {
    "udid": "emulator-5554",
    "device_name": "Pixel 6 Pro",
    "android_version": "13",
    "battery_level": 80,
    "is_online": true,
    "connection_type": "network"
  }
}
```

### 4. 获取所有注册设备

```bash
curl http://localhost:8004/api/devices
```

响应示例：
```json
[
  {
    "udid": "emulator-5554",
    "device_name": "Pixel 6 Pro",
    "android_version": "13",
    "battery_level": 80,
    "is_online": true,
    "connection_type": "network"
  }
]
```

## 主要 API 端点

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/health` | GET | 健康检查 |
| `/api/server` | GET | 获取服务器信息 |
| `/api/devices` | GET | 获取所有注册设备 |
| `/api/devices/{udid}` | GET | 获取特定设备信息 |
| `/api/devices/register` | POST | 注册设备 |
| `/api/plans` | GET | 获取所有测试计划 |
| `/api/plans` | POST | 创建新测试计划 |
| `/api/plans/{plan_id}` | GET | 获取特定测试计划 |
| `/api/plans/{plan_id}` | PUT | 更新测试计划 |
| `/api/plans/{plan_id}` | DELETE | 删除测试计划 |
| `/api/plans/{plan_id}/execute` | POST | 执行测试计划 |
| `/api/events/trigger` | POST | 触发事件 |

## 开发说明

### 代码结构

```
autodroid-trader-server/
├── api/                 # API 层
│   └── main.py          # FastAPI 应用入口
├── core/                # 核心业务逻辑
│   ├── device/          # 设备管理
│   │   └── device_manager.py  # 设备管理器
│   ├── workflow/        # 工作脚本引擎
│   └── scheduling/      # 调度器
├── workscripts/         # Appium 测试脚本
│   ├── Dockerfile       # Docker 容器配置
│   └── com.example.app/ # 示例测试脚本
├── pyproject.toml       # 项目配置和依赖
└── README.md            # 项目说明文档
```

### Appium 测试脚本开发

Appium 测试脚本位于 `workscripts/` 目录下，使用 Python 编写。主要特点：

1. **基于 Appium 框架**：使用 Appium Python Client 进行设备操作
2. **多种定位策略**：支持 ID、XPath、Android UIAutomator 等多种元素定位方式
3. **图像识别支持**：集成 Airtest 图像识别功能，适配加固应用
4. **异常处理**：完善的错误捕获和截图保存机制

#### 示例测试脚本结构：

```python
from appium import webdriver
from appium.webdriver.common.appiumby import AppiumBy
import pytest

# 设备能力配置
DESIRED_CAPS = {
    'platformName': 'Android',
    'platformVersion': '10',
    'deviceName': 'Android Device',
    'appPackage': 'com.example.app',
    'appActivity': '.MainActivity',
    'automationName': 'UiAutomator2',
    'noReset': True
}

# 连接设备
driver = webdriver.Remote('http://localhost:4723/wd/hub', DESIRED_CAPS)

# 元素定位和操作
element = driver.find_element(AppiumBy.ID, 'com.example.app:id/button')
element.click()
```

### 测试

运行测试：
```bash
# 运行所有测试
pytest

# 运行特定测试文件
pytest workscripts/com.example.app/test_script.py

# 运行测试并生成报告
pytest --html=report.html workscripts/com.example.app/test_script.py
```

## 注意事项

1. **Appium 服务器**：确保在启动 Autodroid 服务器前已启动 Appium 服务器
2. **设备连接**：确保测试设备已通过 ADB 连接并授权
3. **端口配置**：Appium 默认使用 4723 端口，Autodroid 服务器默认使用 8004 端口
4. **Android SDK**：确保已安装 Android SDK 并配置了环境变量
5. **Java 环境**：Appium 需要 Java 11 或更高版本

## 故障排除

### Appium 相关问题

1. **Appium 服务器启动失败**：
   ```bash
   # 检查 Node.js 版本
   node --version
   
   # 重新安装 Appium
   npm uninstall -g appium
   npm install -g appium
   ```

2. **驱动安装失败**：
   ```bash
   # 重新安装 Android 驱动
   appium driver uninstall uiautomator2
   appium driver install uiautomator2
   ```

3. **设备连接问题**：
   ```bash
   # 检查设备连接状态
   adb devices
   
   # 重启 ADB 服务
   adb kill-server
   adb start-server
   ```

### 端口被占用

如果端口 8004 或 4723 已被占用，可以修改配置：

1. 修改 Appium 端口：
   ```bash
   appium --port 4724 --base-path /wd/hub
   ```

2. 修改 Autodroid 服务器端口（编辑配置文件）：
   ```bash
   uvicorn api.main:app --host 0.0.0.0 --port 8001 --reload
   ```

### 依赖安装失败

尝试更新 pip 后重新安装：

```bash
pip install --upgrade pip
pip install -e .
```

## 许可证

MIT License