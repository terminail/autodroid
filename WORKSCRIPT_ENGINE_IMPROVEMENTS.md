# WorkScript Engine 改进方案

基于对 Open-AutoGLM 项目的分析，以下是我们可以借鉴和改进的要点：

## 1. 核心架构改进

### 当前问题
- 我们的引擎主要基于脚本执行，缺乏智能决策能力
- 没有视觉理解和AI驱动的操作决策
- 操作粒度较粗，缺乏精细的坐标系统

### 改进建议

#### 1.1 引入AI决策引擎
```python
# 新增 AI 决策模块
class AIDecisionEngine:
    def __init__(self, model_config):
        self.model_client = ModelClient(model_config)
        self.action_parser = ActionParser()
    
    def decide_next_action(self, screenshot, current_app, task_description):
        """基于屏幕截图和任务描述决定下一步操作"""
        messages = self.build_prompt(screenshot, current_app, task_description)
        response = self.model_client.request(messages)
        return self.action_parser.parse(response)
```

#### 1.2 增强坐标系统
```python
# 新增坐标转换工具
class CoordinateConverter:
    @staticmethod
    def relative_to_absolute(relative_coords, screen_width, screen_height):
        """将相对坐标(0-1000)转换为绝对像素坐标"""
        x = int(relative_coords[0] / 1000 * screen_width)
        y = int(relative_coords[1] / 1000 * screen_height)
        return x, y
    
    @staticmethod
    def absolute_to_relative(absolute_coords, screen_width, screen_height):
        """将绝对像素坐标转换为相对坐标(0-1000)"""
        x = int(absolute_coords[0] / screen_width * 1000)
        y = int(absolute_coords[1] / screen_height * 1000)
        return x, y
```

## 2. 操作能力增强

### 新增操作类型
```python
# 扩展 BaseWorkScript 的操作类型
class BaseWorkScript(ABC):
    # 保留现有方法...
    
    # 新增精细操作
    def tap_at(self, x, y, relative=True):
        """在指定坐标点击"""
        pass
    
    def swipe_from_to(self, start, end, duration=None):
        """从起点滑动到终点"""
        pass
    
    def long_press_at(self, x, y, duration_ms=1000):
        """在指定坐标长按"""
        pass
    
    def double_tap_at(self, x, y):
        """在指定坐标双击"""
        pass
    
    def input_text_with_keyboard(self, text):
        """使用ADB键盘输入文本"""
        pass
```

## 3. 视觉理解集成

### 屏幕截图分析
```python
class ScreenAnalyzer:
    def __init__(self):
        self.ocr_engine = OCREngine()  # 可选：集成OCR
        self.ui_detector = UIDetector()  # 可选：集成UI元素检测
    
    def analyze_screen(self, screenshot_path):
        """分析屏幕内容"""
        return {
            'text_elements': self.ocr_engine.extract_text(screenshot_path),
            'ui_elements': self.ui_detector.detect_elements(screenshot_path),
            'current_app': self.detect_current_app(),
            'screen_size': self.get_screen_dimensions()
        }
```

## 4. 智能任务执行

### 任务规划器
```python
class TaskPlanner:
    def __init__(self, ai_engine):
        self.ai_engine = ai_engine
        self.step_executor = StepExecutor()
    
    def execute_task(self, task_description, max_steps=50):
        """执行复杂任务"""
        for step in range(max_steps):
            # 获取当前屏幕状态
            screenshot = self.take_screenshot()
            screen_info = self.analyze_screen(screenshot)
            
            # AI决策下一步
            action = self.ai_engine.decide_next_action(
                screenshot, screen_info, task_description
            )
            
            # 执行操作
            result = self.step_executor.execute(action)
            
            if result.is_completed:
                return result
        
        return ExecutionResult(success=False, message="Max steps reached")
```

## 5. 错误处理和恢复

### 智能错误恢复
```python
class ErrorRecovery:
    def __init__(self):
        self.recovery_strategies = {
            'app_not_responding': self.handle_app_crash,
            'element_not_found': self.handle_missing_element,
            'network_error': self.handle_network_issue,
            'login_required': self.handle_login_required
        }
    
    def handle_error(self, error_type, context):
        """根据错误类型执行恢复策略"""
        if error_type in self.recovery_strategies:
            return self.recovery_strategies[error_type](context)
        return False  # 无法自动恢复
```

## 6. 配置管理增强

### 应用配置系统
```python
# 类似 Open-AutoGLM 的 APP_PACKAGES
APP_CONFIGURATIONS = {
    '微信': {
        'package': 'com.tencent.mm',
        'main_activity': '.ui.LauncherUI',
        'login_activity': '.plugin.account.ui.LoginPasswordUI',
        'special_handlers': {
            'login': WeChatLoginHandler,
            'payment': WeChatPaymentHandler
        }
    },
    '淘宝': {
        'package': 'com.taobao.taobao',
        'main_activity': 'com.taobao.tao.homepage.MainActivity3',
        'special_handlers': {
            'search': TaobaoSearchHandler,
            'cart': TaobaoCartHandler
        }
    }
}
```

## 7. 多设备支持

### 设备管理器
```python
class DeviceManager:
    def __init__(self):
        self.devices = {}
        self.adb_connection = ADBConnection()
    
    def connect_device(self, device_id, device_type='usb'):
        """连接设备（支持USB和网络）"""
        if device_type == 'network':
            success, message = self.adb_connection.connect(device_id)
        else:
            success = self.adb_connection.get_devices()
        
        if success:
            self.devices[device_id] = DeviceInfo(device_id, device_type)
        return success
    
    def get_device_info(self, device_id):
        """获取设备信息"""
        return self.devices.get(device_id)
```

## 8. 执行流程优化

### 改进的执行引擎
```python
class EnhancedWorkScriptEngine:
    def __init__(self):
        self.ai_engine = AIDecisionEngine()
        self.task_planner = TaskPlanner(self.ai_engine)
        self.screen_analyzer = ScreenAnalyzer()
        self.error_recovery = ErrorRecovery()
        self.device_manager = DeviceManager()
    
    def execute_intelligent_task(self, task_description, **options):
        """执行智能任务"""
        try:
            # 1. 任务解析
            parsed_task = self.parse_task(task_description)
            
            # 2. 环境检查
            device_info = self.check_environment()
            
            # 3. 任务执行
            result = self.task_planner.execute_task(parsed_task)
            
            # 4. 结果验证
            if self.verify_result(result):
                return ExecutionResult(success=True, data=result)
            else:
                # 尝试错误恢复
                recovery_success = self.error_recovery.handle_error(
                    result.error_type, result.context
                )
                if recovery_success:
                    return self.execute_intelligent_task(task_description, **options)
                
        except Exception as e:
            return ExecutionResult(success=False, error=str(e))
```

## 9. 集成方案

### 渐进式集成步骤

1. **第一阶段**：增强基础操作能力
   - 添加坐标系统支持
   - 扩展操作类型（滑动、长按、双击等）
   - 改进错误处理机制

2. **第二阶段**：集成AI决策
   - 添加模型客户端
   - 实现基础的动作解析
   - 支持简单的视觉理解

3. **第三阶段**：智能任务执行
   - 实现任务规划器
   - 添加多步骤任务支持
   - 增强错误恢复能力

4. **第四阶段**：完整系统集成
   - 多设备支持
   - 高级配置管理
   - 性能优化和测试

## 10. 具体实现建议

### 立即可实施的改进

1. **增强坐标系统**：
   - 在现有BaseWorkScript中添加坐标转换方法
   - 支持相对坐标和绝对坐标的转换

2. **扩展操作类型**：
   - 添加滑动、长按、双击等精细操作
   - 改进现有的点击和输入操作

3. **改进错误处理**：
   - 添加更详细的错误类型分类
   - 实现基本的错误恢复机制

4. **添加配置管理**：
   - 为常用应用创建配置映射
   - 支持应用特定的处理逻辑

这些改进将显著提升我们workscript engine的智能化程度和实用性，使其能够处理更复杂的自动化任务。