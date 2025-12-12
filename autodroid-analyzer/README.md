# Autodroid Analyzer

## 概述

Autodroid Analyzer 是一个基于 Python 的 Android 应用分析工具，专门用于自动化分析和理解 Android 应用的用户界面和交互流程。

## 架构说明

前后端分离架构：
- **后端API**: Python FastAPI (端口8001)
- **前端应用**: SvelteKit + Vite (端口3000)
- **数据库**: SQLite

## 快速开始

### 环境要求

- Python 3.10+
- Node.js 16+
- npm 8+

### 安装依赖

```bash
# 安装项目包（自动安装依赖）
pip install -e .
```
# 安装前端依赖
cd frontend
npm install
```

### 启动方式

**Windows用户**：双击运行批处理文件：
```bash
start_server.bat
```

**命令行启动**：
```bash
cd 'd:/git/autodroid/autodroid-analyzer'; conda activate liugejiao; python run_server.py
```

**注意**: 启动脚本只会启动API服务器。前端需要单独启动：
```bash
cd 'd:/git/autodroid/autodroid-analyzer/frontend' && npm run dev
```

### 服务启动后

- **API服务器**: `http://localhost:8001`
- **前端应用**: `http://localhost:3000`
- **API文档**: `http://localhost:8001/docs`

启动成功后，控制台会显示访问地址。

## 核心功能

- 🔍 **多模态页面分析**：结合DOM树和截图分析应用界面
- 📊 **用户操作监控**：自动记录用户操作序列
- 🌳 **操作图构建**：生成应用交互流程的可视化图表
- 📈 **分析报告生成**：输出详细的HTML和Markdown报告

## API 接口

### 基础接口

| 端点 | 方法 | 描述 |
|------|------|------|
| `/` | GET | 获取API基本信息 |
| `/docs` | GET | 查看API文档 |

### 模块化API结构

Autodroid Analyzer API采用模块化设计，包含以下功能模块：

- **analysis** - 分析功能模块 (`/api/analysis`)
- **apks** - APK管理模块 (`/api/apks`)
- **devices** - 设备管理模块 (`/api/devices`)
- **server** - 服务器管理模块 (`/api/server`)

所有API端点都位于 `/api` 前缀下，例如：`http://localhost:8001/api/analysis`

### API访问示例

```bash
# 获取API基本信息
curl http://localhost:8001/

# 响应示例
{
  "message": "Autodroid Analyzer API",
  "version": "1.0.0",
  "modules": ["analysis", "apks", "devices", "server"]
}
```

## 项目结构

autodroid-analyzer/
├── config.py                           # 配置管理器（统一配置管理）
├── analysis/                     # 分析管理模块
│   ├── app_analyzer.py           # 应用分析器主类
│   ├── interactive_analyzer.py   # 交互式分析器
│   ├── navigation_system.py      # 导航系统
│   ├── analysis_utils.py         # 分析工具函数
│   ├── database_manager.py       # 数据库管理器
│   └── human_assistant.py        # 人工协助模块
├── device/                       # 设备连接模块
│   └── device_manager.py         # 设备管理器
├── useroperation/                # 用户操作监控模块
│   ├── monitoring_system.py      # 监控系统
│   ├── user_operation.py         # 用户操作类
│   └── user_operation_manager.py # 用户操作管理器
├── screenshot/                   # 交互式截屏模块
│   ├── screenshot_manager.py     # 截屏管理器
│   ├── page_analyzer.py          # 页面分析器
│   └── page_recognizer.py        # 页面识别器
├── api/                          # API服务模块
│   ├── main.py                   # FastAPI主服务
│   ├── __init__.py               # 模块初始化
│   └── analyzer.db               # 分析器数据库
├── frontend/                     # 前端界面模块
│   ├── package.json              # 项目配置
│   ├── svelte.config.js          # Svelte配置
│   ├── vite.config.js            # Vite配置
│   ├── src/                      # 源代码目录
│   └── static/                   # 静态资源
├── tests/                        # 测试模块
│   ├── test_basic.py
│   ├── test_database.py
│   ├── test_operations.py
│   ├── test_interactive_analyzer.py
│   ├── test_autodroid_manager.py
│   └── test_refactored_analyzer.py
├── config.yaml                   # 配置文件
├── requirements.txt              # 依赖包列表
├── setup.py                      # 包安装配置
├── run_analysis.py               # 分析入口脚本
├── apk/                          # APK相关工具目录
│   ├── list_apks.py              # APK列表工具
│   └── apk_packer_detector.py    # APK加固检测工具
├── DESIGN.md                     # 设计文档
└── README.md                     # 项目说明文档
```

## 配置文件

`config.yaml` 设置：
- **分析设置**: 监控开关、最大深度等
- **数据库**: SQLite路径
- **输出目录**: 报告、截图保存路径
- **服务器**: API端口(8001)和前端配置
- **日志**: 日志文件和级别

## 故障排除

### 连接问题

- **API无法访问**: 确认服务已启动，检查端口8001
- **前端连接失败**: 确认前后端都已启动，检查CORS错误
- **健康检查**: 访问 `http://localhost:8001/api/health`

### 设备问题

- **设备连接失败**: 检查ADB调试是否启用
- **应用启动失败**: 确认包名正确且应用已安装

## 许可证

本项目基于MIT许可证开源。

## 详细功能说明

### 多模态页面分析

```python
# 获取当前页面
current_page = analyzer.get_current_page()

# 执行多模态分析
multimodal_results = analyzer.analyze_page_multimodal(current_page)

# 显示分析结果
analyzer._display_multimodal_analysis(current_page)
```

**输出示例：**
```
🔍 多模态分析结果:
----------------------------------------
🌳 DOM树分析:
   页面标题: 主页面
   Activity: com.example.MainActivity
   元素数量: 25
   关键元素:
     1. 登录
     2. 注册
     3. 设置

📸 截图分析:
   布局复杂度: 0.75

📊 详细元素分析:
   可点击元素: 8
   文本元素: 12
   推荐交互点:
     1. 登录 (重要性: 0.95)
     2. 注册 (重要性: 0.85)
```

### 用户操作监控

```python
# 开始监控
analyzer.start_user_operation_monitoring()

# 用户操作应用...
# 分析器会自动检测页面变化并记录操作

# 停止监控
analyzer.stop_user_operation_monitoring()

# 查看操作记录
for action in analyzer.user_actions:
    print(f"{action.timestamp}: {action.action_type} -> {action.result_page}")
```

### 操作图分析

```python
# 生成操作图分析报告
report = analyzer.get_operation_analysis_report()

# 获取路径覆盖率
coverage = analyzer.operation_graph.calculate_path_coverage()
print(f"路径覆盖率: {coverage:.2%}")

# 生成可视化图表
mermaid_graph = analyzer.operation_graph.generate_mermaid_graph()
print(mermaid_graph)
```

## 高级功能

### 自定义分析模式

```python
# 设置特定的分析模式
analyzer.multimodal_recognizer.set_analysis_modes({
    "uiautomator2": True,    # DOM树分析
    "screenshot": True,      # 截图分析
    "user_monitoring": True  # 用户操作监控
})
```

### 详细元素分析

```python
# 获取详细的元素分析
detailed_analysis = analyzer.get_detailed_element_analysis(current_page)

if "error" not in detailed_analysis:
    clickable_elements = detailed_analysis.get("clickable_elements", 0)
    text_elements = detailed_analysis.get("text_elements", 0)
    interaction_points = detailed_analysis.get("interaction_points", [])
    
    print(f"可点击元素: {clickable_elements}")
    print(f"文本元素: {text_elements}")
    print(f"推荐交互点: {len(interaction_points)}个")
```

### 报告生成

```python
# 生成完整的分析报告
analyzer.generate_analysis_report()

# 报告包含：
# - 用户操作记录
# - 操作图分析
# - 页面分析结果
# - 操作图可视化
# - 性能统计信息
```

## 项目结构

```
autodroid-analyzer/
├── config.py                           # 配置管理器（统一配置管理）
├── src/
│   └── autodroid_analyzer/
│       ├── __init__.py
│       └── analysis/
│           ├── __init__.py
│           ├── app_analyzer.py          # 应用分析器主类
│           ├── interactive_analyzer.py  # 交互式分析器
│           ├── device_manager.py        # 设备管理器
│           ├── database_manager.py      # 数据库管理器
│           ├── human_assistant.py       # 人工协助模块
│           ├── analysis_utils.py        # 分析工具函数
│           └── config.yaml              # 配置文件
├── requirements.txt
├── setup.py
├── test_basic.py
└── example_usage.py
```

## 命令行使用

### 运行测试

```bash
# 运行基本功能测试
cd d:\git\autodroid\autodroid-analyzer
python test_basic.py

# 运行示例使用脚本
python example_usage.py
```

### 直接分析应用

```python
# 直接分析指定应用
from autodroid_analyzer.analysis.interactive_analyzer import InteractiveAppAnalyzer

analyzer = InteractiveAppAnalyzer("emulator-5554", "com.autodroid.manager")
analyzer.analyze_with_user_interaction(max_depth=3)
```

## 输出文件

分析器会在指定的输出目录生成以下文件：

- `analysis_report.md` - 完整的分析报告
- `operation_graph.png` - 操作图可视化
- `screenshot_*.png` - 页面截图
- `ui_hierarchy_*.xml` - UI层次结构文件
- `user_actions.json` - 用户操作记录

## 故障排除

### 常见问题

1. **设备连接失败**
   - 确保设备已连接且ADB调试已启用
   - 检查设备ID是否正确

2. **应用启动失败**
   - 确认应用包名正确
   - 检查应用是否已安装

3. **多模态分析失败**
   - 确保已安装opencv-python和uiautomator2
   - 检查设备屏幕是否可访问

### 调试模式

```python
# 启用详细日志
import logging
logging.basicConfig(level=logging.DEBUG)

# 创建分析器时设置详细输出
analyzer = InteractiveAppAnalyzer(device_id, app_package, verbose=True)
```

## 改进建议

基于用户反馈，分析程序可以进一步优化：

1. **避免自动录屏**：由用户选择菜单录屏，避免硬盘空间快速耗尽
2. **改进程序流程**：
   - 程序启动后开启独立进程，用于自动监控用户操作，记录用户操作序列
   - 提供交互菜单：
     - 用户输入1：截屏并分析DOM保存，记录最后一次用户操作与本次截屏的关联
     - 用户输入q：开始分析，生成操作图分析报告，分析完毕退出程序

## API参考

### 主要方法

- `launch_app()` - 启动目标应用
- `get_current_page()` - 获取当前页面信息
- `analyze_with_user_interaction()` - 交互式分析主方法
- `analyze_page_multimodal()` - 多模态页面分析
- `start_user_operation_monitoring()` - 开始用户操作监控
- `generate_analysis_report()` - 生成分析报告

### 数据类

- `UserAction` - 用户操作记录
- `PageNode` - 页面节点信息
- `OperationEdge` - 操作边信息

## 许可证

本项目基于MIT许可证开源。

## 贡献指南

欢迎提交Issue和Pull Request来改进这个工具！

## 联系方式

如有问题或建议，请通过以下方式联系：
- 项目Issue: [GitHub Issues]
- 邮箱: team@autodroid.com