# 工作脚本引擎 (WorkScript Engine)

## 概述

工作脚本引擎是Autodroid容器系统的核心组件，负责动态加载和执行Python工作脚本。它提供了标准化的脚本执行框架，支持脚本生命周期管理、错误处理、报告生成等功能。

## 架构设计

### 核心组件

1. **BaseWorkScript** - 抽象基类，定义所有工作脚本的接口
2. **WorkScriptEngine** - 引擎类，负责脚本加载和执行
3. **脚本规范** - 定义脚本编写标准

### 文件结构

```
autodroid-container/
├── core/
│   └── workscript/
│       ├── __init__.py          # 模块初始化
│       ├── base.py              # BaseWorkScript抽象类
│       ├── engine.py            # WorkScriptEngine引擎
│       └── test_engine.py       # 引擎测试
├── workscripts/
│   ├── WORKSCRIPTS_ENGINE_DESIGN.md    # 原始设计文档
│   ├── WORKSCRIPT_SPECIFICATION.md     # 脚本规范文档
│   ├── com.autodroid.manager/
│   │   └── login_test.py              # 登录测试脚本示例
│   └── examples/
│       ├── sample_workplan.json       # 工作计划示例
│       ├── run_workplan.py           # 引擎使用示例
│       └── complete_workflow_example.py  # 完整工作流示例
└── reports/                    # 报告输出目录
```

## 快速开始

### 1. 创建工作脚本

```python
#!/usr/bin/env python3
"""我的工作脚本"""

from core.workscript.base import BaseWorkScript
from typing import Dict, Any

class my_work_script(BaseWorkScript):
    """我的工作脚本类"""
    
    def __init__(self, workplan: Dict[str, Any], device_udid: str = None):
        super().__init__(workplan, device_udid)
        self.param1 = self.get_workplan_param('param1', 'default')
    
    def run(self) -> Dict[str, Any]:
        """执行脚本逻辑"""
        try:
            self.log_step("步骤1", "执行主要操作")
            # 你的业务逻辑
            
            return {
                'status': 'success',
                'message': '执行成功',
                'data': {'result': 'ok'}
            }
        except Exception as e:
            return {
                'status': 'error',
                'message': str(e),
                'error_type': type(e).__name__
            }
```

### 2. 使用引擎执行脚本

```python
from core.workscript.engine import WorkScriptEngine

# 初始化引擎
engine = WorkScriptEngine()

# 创建工作计划
workplan = {
    'id': 'my_workplan_001',
    'workscript': 'my_work_script',
    'data': {
        'param1': 'value1',
        'param2': 'value2'
    },
    'device_udid': 'emulator-5554'
}

# 执行脚本
result = engine.execute_script(workplan)
print(f"执行结果: {result['status']}")
```

## API参考

### BaseWorkScript

#### 属性
- `workplan`: 工作计划数据
- `device_udid`: 设备唯一标识符
- `logger`: 日志记录器
- `report_dir`: 报告输出目录

#### 方法
- `get_workplan_param(key, default)`: 获取工作计划参数
- `log_step(name, message)`: 记录执行步骤
- `log_success(message)`: 记录成功信息
- `log_error(message)`: 记录错误信息
- `save_report(filename, content)`: 保存报告文件
- `execute()`: 执行脚本（包装run方法）
- `run()`: 抽象方法，子类必须实现

### WorkScriptEngine

#### 方法
- `execute_script(workplan, device_udid)`: 执行工作脚本
- `load_script(script_name)`: 动态加载脚本
- `list_available_scripts()`: 列出可用脚本
- `get_script_info(script_name)`: 获取脚本信息
- `list_scripts()`: 兼容方法，同list_available_scripts()

## 工作计划格式

```json
{
    "id": "workplan_unique_id",
    "workscript": "script_name",
    "data": {
        "param1": "value1",
        "param2": "value2"
    },
    "device_udid": "device_identifier",
    "priority": "high|medium|low",
    "tags": ["tag1", "tag2"]
}
```

## 执行结果格式

### 成功
```json
{
    "status": "success",
    "message": "执行成功的描述",
    "report_path": "/path/to/report.html",
    "data": {
        "result": "additional data"
    },
    "execution_start_time": "2024-01-15T10:00:00",
    "execution_end_time": "2024-01-15T10:01:30",
    "engine_version": "1.0.0"
}
```

### 失败
```json
{
    "status": "failed",
    "message": "执行失败的原因",
    "report_path": "/path/to/report.html",
    "data": {},
    "execution_start_time": "2024-01-15T10:00:00",
    "execution_end_time": "2024-01-15T10:01:30",
    "engine_version": "1.0.0"
}
```

### 错误
```json
{
    "status": "error",
    "message": "错误信息",
    "error_type": "Exception类型",
    "execution_start_time": "2024-01-15T10:00:00",
    "execution_end_time": "2024-01-15T10:01:30",
    "engine_version": "1.0.0"
}
```

## 测试和验证

### 运行引擎测试
```bash
cd d:/git/autodroid
python autodroid-container/core/workscript/test_engine.py
```

### 运行完整示例
```bash
cd d:/git/autodroid
python autodroid-container/workscripts/examples/complete_workflow_example.py
```

### 运行简单示例
```bash
cd d:/git/autodroid
python autodroid-container/workscripts/examples/run_workplan.py
```

## 最佳实践

### 1. 错误处理
```python
def run(self) -> Dict[str, Any]:
    try:
        # 主要逻辑
        pass
    except SpecificException as e:
        return {
            'status': 'failed',
            'message': f'特定错误: {e}',
            'error_type': type(e).__name__
        }
    except Exception as e:
        return {
            'status': 'error',
            'message': f'未知错误: {e}',
            'error_type': type(e).__name__
        }
```

### 2. 参数验证
```python
def __init__(self, workplan: Dict[str, Any], device_udid: str = None):
    super().__init__(workplan, device_udid)
    
    # 必需参数
    self.username = self.get_workplan_param('username')
    if not self.username:
        raise ValueError("必须提供用户名参数")
    
    # 可选参数
    self.timeout = self.get_workplan_param('timeout', 30)
```

### 3. 步骤记录
```python
def run(self) -> Dict[str, Any]:
    self.log_step("初始化", "正在初始化测试环境")
    # 初始化代码
    
    self.log_step("执行操作", "正在执行主要操作")
    # 主要操作
    
    self.log_step("验证结果", "正在验证执行结果")
    # 验证代码
    
    return {'status': 'success', 'message': '执行完成'}
```

### 4. 报告生成
```python
def _generate_report(self, success: bool) -> str:
    html_content = f"""
    <html>
    <head><title>测试报告</title></head>
    <body>
        <h1>测试执行报告</h1>
        <p>状态: {'成功' if success else '失败'}</p>
        <p>时间: {time.strftime('%Y-%m-%d %H:%M:%S')}</p>
    </body>
    </html>
    """
    return self.save_report('test_report.html', html_content)
```

## 扩展功能

### 1. 自定义报告格式
可以通过重写`save_report`方法或使用其他报告生成库来创建更复杂的报告。

### 2. 脚本依赖管理
可以在`__init__`方法中初始化外部依赖，如数据库连接、API客户端等。

### 3. 并行执行
可以通过创建多个引擎实例来实现脚本的并行执行。

### 4. 插件系统
可以通过扩展`BaseWorkScript`来创建特定领域的脚本基类。

## 故障排除

### 常见问题

1. **ModuleNotFoundError**: 确保正确设置Python路径
2. **脚本加载失败**: 检查脚本是否继承`BaseWorkScript`
3. **类名不匹配**: 确保类名与文件名相同
4. **缺少必需方法**: 确保实现`run()`方法

### 调试技巧

1. 启用详细日志记录
2. 使用`self.logger`记录调试信息
3. 检查报告目录中的日志文件
4. 使用示例代码进行测试

## 版本历史

- **v1.0.0** (2024-01-15): 初始版本
  - 基础引擎实现
  - BaseWorkScript抽象类
  - 动态脚本加载
  - 报告生成功能
  - 错误处理机制

## 贡献指南

1. 遵循PEP 8编码规范
2. 添加适当的文档字符串
3. 编写单元测试
4. 更新相关文档

## 许可证

本项目采用MIT许可证 - 详见项目根目录的LICENSE文件。

---

如需更多帮助，请查看项目文档或联系开发团队。