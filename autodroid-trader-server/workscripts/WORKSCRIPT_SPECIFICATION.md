# 工作脚本规范 (WorkScript Specification - ADB-Based)

## 概述

工作脚本是用于自动化测试和任务执行的Python脚本，必须遵循本规范才能被工作脚本引擎正确加载和执行。**基于纯ADB命令实现，已移除Appium依赖。**

## 基本要求

### 1. 文件命名
- 脚本文件必须以 `.py` 结尾
- 文件名应该使用小写字母，单词之间用下划线 `_` 分隔
- 示例: `login_test.py`, `app_install_test.py`, `performance_monitor.py`

### 2. 目录结构
- 脚本文件必须放置在 `workscripts` 目录或其子目录中
- 建议使用包结构组织相关脚本，如 `com.autodroid.manager/login_test.py`

### 3. 类定义要求
- 必须定义一个与文件名相同的类（不包含 `.py` 后缀）
- 类必须继承自 `BaseWorkScript`
- 类必须实现 `run()` 方法

## 代码结构模板

```python
#!/usr/bin/env python3
"""
脚本描述

详细描述脚本的功能、用途和特点
"""

import os
import sys
from typing import Dict, Any

# 导入基础类（根据实际路径调整）
from core.workscript.base import BaseWorkScript


class YourScriptName(BaseWorkScript):
    """
    脚本类描述
    
    详细描述脚本类的功能和使用方法
    """
    
    def __init__(self, workplan: Dict[str, Any], device_udid: str = None):
        """
        初始化脚本
        
        Args:
            workplan: 工作计划数据，包含执行所需的所有参数
            device_udid: 设备唯一标识符（可选）
        """
        super().__init__(workplan, device_udid)
        
        # 从workplan获取参数
        self.param1 = self.get_workplan_param('param1', 'default_value')
        self.param2 = self.get_workplan_param('param2', 30)
        
        self.logger.info(f"脚本初始化完成: {self.__class__.__name__}")
    
    def run(self) -> Dict[str, Any]:
        """
        执行脚本的主要逻辑
        
        Returns:
            Dict 包含执行结果，必须包含 'status' 字段
            可能的status值: 'success', 'failed', 'error'
        """
        try:
            self.log_step("步骤1", "描述步骤1的内容")
            # 执行步骤1
            
            self.log_step("步骤2", "描述步骤2的内容")
            # 执行步骤2
            
            # 更多步骤...
            
            # 生成报告
            report_path = self._generate_report(True)
            
            self.log_success("脚本执行成功")
            
            return {
                'status': 'success',
                'message': '脚本执行成功',
                'report_path': report_path,
                'data': {
                    # 额外的执行结果数据
                }
            }
            
        except Exception as e:
            self.log_error(f"脚本执行失败: {str(e)}")
            return {
                'status': 'error',
                'message': f'脚本执行异常: {str(e)}',
                'error_type': type(e).__name__
            }
```

## 返回值规范

### 成功执行
```python
return {
    'status': 'success',
    'message': '执行成功的描述信息',
    'report_path': '/path/to/report.html',  # 可选
    'data': {
        # 额外的结果数据
    }
}
```

### 执行失败
```python
return {
    'status': 'failed',
    'message': '执行失败的原因',
    'report_path': '/path/to/report.html',  # 可选
    'data': {
        # 失败相关的数据
    }
}
```

### 执行错误
```python
return {
    'status': 'error',
    'message': '错误信息',
    'error_type': 'Exception类型名称',
    'data': {
        # 错误相关的数据
    }
}
```

## 可用方法和属性

### 继承的属性
- `self.workplan`: 工作计划数据
- `self.device_udid`: 设备UDID
- `self.logger`: 日志记录器
- `self.report_dir`: 报告输出目录
- `self.start_time`: 开始执行时间
- `self.end_time`: 结束执行时间

### 可用方法
- `self.get_workplan_param(key, default)`: 获取工作计划参数
- `self.log_step(name, message)`: 记录执行步骤
- `self.log_success(message)`: 记录成功信息
- `self.log_error(message)`: 记录错误信息
- `self.log_warning(message)`: 记录警告信息
- `self.save_report(filename, content)`: 保存报告文件

## 最佳实践

### 1. 参数获取
```python
# 获取必需参数
username = self.get_workplan_param('username')
if not username:
    raise ValueError("必须提供用户名参数")

# 获取可选参数，提供默认值
timeout = self.get_workplan_param('timeout', 30)
success_rate = self.get_workplan_param('success_rate', 0.9)
```

### 2. 步骤记录
```python
self.log_step("初始化", "正在初始化测试环境")
# 初始化代码
self.log_step("执行操作", "正在执行主要操作")
# 主要操作代码
self.log_step("验证结果", "正在验证执行结果")
# 验证代码
```

### 3. 错误处理
```python
try:
    # 可能出错的代码
    result = self.perform_risky_operation()
except SpecificException as e:
    self.log_error(f"特定错误: {e}")
    return {
        'status': 'failed',
        'message': f'操作失败: {e}',
        'error_type': type(e).__name__
    }
except Exception as e:
    self.log_error(f"未知错误: {e}")
    return {
        'status': 'error',
        'message': f'执行异常: {e}',
        'error_type': type(e).__name__
    }
```

### 4. 报告生成
```python
def _generate_report(self, success: bool) -> str:
    """生成测试报告"""
    
    # 创建HTML报告
    html_content = self._create_html_report(success)
    html_path = self.save_report('test_report.html', html_content)
    
    # 创建JSON报告
    json_data = {
        'test_name': self.__class__.__name__,
        'timestamp': time.strftime('%Y-%m-%d %H:%M:%S'),
        'result': 'PASS' if success else 'FAIL',
        'device_udid': self.device_udid,
        'workplan_id': self.workplan.get('id')
    }
    json_path = self.save_report('test_result.json', json.dumps(json_data, indent=2))
    
    return html_path
```

## 测试和验证

### 1. 独立测试
```python
if __name__ == "__main__":
    # 创建测试工作计划
    test_workplan = {
        'id': 'test_workplan_001',
        'workscript': 'your_script_name',
        'data': {
            'param1': 'test_value',
            'param2': 123
        }
    }
    
    # 创建脚本实例
    script = YourScriptName(test_workplan, device_udid='test_device')
    
    # 执行脚本
    result = script.execute()
    
    # 打印结果
    print(f"执行结果: {result['status']}")
    print(f"消息: {result.get('message', '')}")
```

### 2. 使用引擎测试
```python
from core.workscript.engine import WorkScriptEngine

# 初始化引擎
engine = WorkScriptEngine()

# 执行脚本
result = engine.execute_script(workplan, device_udid='test_device')

# 验证结果
assert result['status'] in ['success', 'failed', 'error']
```

## 常见错误和解决方案

### 1. 导入错误
```python
# 错误: ModuleNotFoundError
# 解决方案: 确保正确设置Python路径
import sys
sys.path.append('/path/to/autodroid')
from core.workscript.base import BaseWorkScript
```

### 2. 类名不匹配
```python
# 错误: 脚本类名与文件名不匹配
# 解决方案: 确保类名与文件名相同
# 文件名: my_test.py
# 类名: class my_test(BaseWorkScript):
```

### 3. 缺少必需方法
```python
# 错误: 未实现run()方法
# 解决方案: 确保实现run()方法
class MyTest(BaseWorkScript):
    def run(self) -> Dict[str, Any]:
        # 实现逻辑
        return {'status': 'success', 'message': '完成'}
```

## 示例脚本

参考实现:
- `autodroid-trader-server/workscripts/com.autodroid.manager/login_test.py` - 登录功能测试
- `autodroid-trader-server/workscripts/examples/` - 更多示例

## 版本历史

- v1.0.0 (2024-01-15): 初始版本

---

如需帮助或发现问题，请联系开发团队。