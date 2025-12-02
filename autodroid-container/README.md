# Autodroid Container

Autodroid Container 是 Autodroid Android 自动化系统的服务端组件，提供设备管理、工作流执行、测试调度等功能。

## 环境要求

- Python 3.10+
- pip 22.0+

## 安装

1. 使用 Conda 创建并激活虚拟环境：
   ```bash
    # 创建虚拟环境
      
    cd autodroid-container

    conda create -n autodroid python=3.10
   
   # 激活虚拟环境
    
    conda activate autodroid
   ```

2. 安装依赖（使用中国镜像提高速度）：
   ```bash
   # 使用阿里云镜像
   pip install -e . -i https://mirrors.aliyun.com/pypi/simple/
   
   # 或使用腾讯云镜像
   # pip install -e . -i https://mirrors.cloud.tencent.com/pypi/simple/
   
   # 或使用华为云镜像
   # pip install -e . -i https://mirrors.huaweicloud.com/repository/pypi/simple/
   ```

## 启动服务器

### 非 Docker 方式

1. 确保虚拟环境已激活

2. 直接运行 FastAPI 应用：
   ```bash
   uvicorn api.main:app --host 0.0.0.0 --port 8003 --reload
   ```

3. 或者使用项目脚本：
   ```bash
   python -m api.main

   cd /d/git/autodroid/autodroid-container && conda activate liugejiao && python run_server.py
   ```

### 服务启动后

- 服务器将在 `http://0.0.0.0:8003` 上运行
- API 文档将在 `http://localhost:8003/docs` 上可用
- 可通过 `http://localhost:8003/redoc` 查看另一种格式的 API 文档

## API 访问示例

### 1. 健康检查

```bash
curl http://localhost:8003/api/health
```

响应示例：
```json
{
  "status": "healthy",
  "timestamp": 1733123456.789,
  "services": {
    "device_manager": "running",
    "workflow_engine": "running",
    "scheduler": "running"
  }
}
```

### 2. 获取服务器信息

```bash
curl http://localhost:8003/api/server
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
    "workflow_engine": "running",
    "scheduler": "running"
  },
  "capabilities": {
    "device_registration": true,
    "workflow_execution": true,
    "test_scheduling": true,
    "event_triggering": true
  },
  "api_endpoints": {
    "health": "/api/health",
    "devices": "/api/devices",
    "device_register": "/api/devices/register",
    "workflows": "/api/workflows",
    "test_plans": "/api/plans"
  }
}
```

### 3. 注册设备

```bash
curl -X POST -H "Content-Type: application/json" -d '{"udid": "emulator-5554", "device_name": "Pixel 6 Pro", "android_version": "13", "battery_level": 80, "connection_type": "network"}' http://localhost:8000/api/devices/register
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
curl http://localhost:8000/api/devices
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
| `/api/workflows` | GET | 获取所有工作流 |
| `/api/workflows/{workflow_name}` | GET | 获取特定工作流 |
| `/api/workflows` | POST | 创建新工作流 |
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
autodroid-container/
├── api/                 # API 层
│   └── main.py          # FastAPI 应用入口
├── core/                # 核心业务逻辑
│   ├── device/          # 设备管理
│   │   └── device_manager.py  # 设备管理器
│   ├── workflow/        # 工作流引擎
│   └── scheduling/      # 调度器
├── pyproject.toml       # 项目配置和依赖
└── README.md            # 项目说明文档
```

### 测试

运行测试：
```bash
pytest
```

## 注意事项

1. 确保在启动服务器前已安装所有依赖
2. 服务器默认监听所有网络接口（0.0.0.0），在生产环境中请根据需要调整
3. 开发环境下使用 `--reload` 参数可以实现代码热重载
4. 生产环境建议使用 Gunicorn 等 WSGI 服务器配合 Uvicorn

## 故障排除

### 端口被占用

如果端口 8000 已被占用，可以使用其他端口：

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
