# Autodroid 工作脚本引擎

## 项目概述

本项目实现了Autodroid容器系统的工作脚本引擎，提供了标准化的Python脚本执行框架。引擎支持动态脚本加载、生命周期管理、错误处理、报告生成等功能。

## 主要特性

✅ **ABC抽象类设计**: 基于`BaseWorkScript`抽象基类规范所有工作脚本  
✅ **动态装载执行**: 工作脚本引擎能够动态加载和执行符合规范的Python脚本  
✅ **标准化接口**: 统一的工作计划格式和执行结果格式  
✅ **完整生命周期管理**: 脚本初始化、执行、报告生成、错误处理  
✅ **日志和报告**: 详细的执行日志和HTML/JSON报告生成  
✅ **错误处理**: 完善的异常捕获和错误分类  
✅ **测试验证**: 完整的测试用例和示例代码  

## 快速开始

### 1. 运行测试
```bash
cd d:/git/autodroid
python autodroid-trader-server/core/workscript/test_engine.py
```

### 2. 运行完整示例
```bash
cd d:/git/autodroid
python autodroid-trader-server/workscripts/examples/complete_workflow_example.py
```

### 3. 查看登录测试脚本
```bash
cd d:/git/autodroid
python autodroid-trader-server/workscripts/examples/run_workplan.py
```

## 项目结构

```
autodroid-trader-server/
├── core/
│   └── workscript/
│       ├── __init__.py              # 模块初始化
│       ├── base.py                  # BaseWorkScript抽象类
│       ├── engine.py                # WorkScriptEngine引擎
│       ├── test_engine.py           # 引擎测试
│       └── README.md                # 引擎文档
├── workscripts/
│   ├── WORKSCRIPTS_ENGINE_DESIGN.md    # 原始设计文档
│   ├── WORKSCRIPT_SPECIFICATION.md     # 脚本规范文档
│   ├── com.autodroid.trader/
│   │   └── login_test.py              # 登录功能测试脚本 ✅
│   └── examples/
│       ├── sample_workplan.json       # 工作计划示例
│       ├── run_workplan.py           # 简单使用示例
│       └── complete_workflow_example.py  # 完整工作流示例
└── reports/                          # 报告输出目录
```

## 核心组件

### BaseWorkScript (ABC抽象类)
- 定义了所有工作脚本必须实现的接口
- 提供标准化的参数获取、日志记录、报告生成方法
- 规范脚本的生命周期和返回值格式

### WorkScriptEngine (动态装载执行)
- 动态加载符合规范的Python脚本
- 执行工作脚本并管理执行过程
- 处理错误和生成执行报告

### login_test.py (实际测试例子)
- 基于`BaseWorkScript`实现的登录功能测试脚本
- 演示了如何测试`d:/git/autodroid/autodroid-trader-app`的登录功能
- 包含完整的测试步骤和报告生成

## 使用示例

### 创建工作脚本
```python
from core.workscript.base import BaseWorkScript

class my_test(BaseWorkScript):
    def run(self):
        self.log_step("测试步骤", "执行测试")
        return {'status': 'success', 'message': '测试通过'}
```

### 使用引擎执行
```python
from core.workscript.engine import WorkScriptEngine

engine = WorkScriptEngine()
workplan = {
    'id': 'test_001',
    'workscript': 'my_test',
    'data': {}
}
result = engine.execute_script(workplan)
```

## 技术亮点

1. **抽象类设计模式**: 使用ABC确保所有脚本遵循统一规范
2. **动态导入**: 使用`importlib`实现运行时脚本加载
3. **异常处理**: 完善的错误分类和处理机制
4. **日志系统**: 结构化的日志记录和报告生成
5. **类型注解**: 完整的类型提示提高代码可维护性

## 文档

- [工作脚本引擎详细文档](autodroid-trader-server/core/workscript/README.md)
- [工作脚本编写规范](autodroid-trader-server/workscripts/WORKSCRIPT_SPECIFICATION.md)
- [引擎设计原始文档](autodroid-trader-server/workscripts/WORKSCRIPTS_ENGINE_DESIGN.md)

## 测试结果

✅ 引擎初始化测试通过  
✅ 脚本加载测试通过  
✅ 工作计划验证测试通过  
✅ 登录测试脚本执行测试通过  
✅ 错误处理测试通过  
✅ 多脚本执行测试通过  
✅ 报告生成测试通过  

## 开发状态

- **状态**: 完成 ✅
- **版本**: v1.0.0
- **测试**: 全面测试通过
- **文档**: 完整文档和示例

## 贡献

欢迎提交Issue和Pull Request来改进工作脚本引擎。

## 许可证

MIT License - 详见LICENSE文件

---

**完成时间**: 2024年1月15日  
**作者**: Autodroid开发团队