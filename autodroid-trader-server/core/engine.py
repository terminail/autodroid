"""
增强版工作脚本引擎 - 集成AI决策和高级功能
增强版工作脚本引擎
"""

import json
import time
import logging
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, List, Optional, Callable, Union
from datetime import datetime

# 导入现有模块
# 导入现有基础模块
from workscript.engine import WorkScriptEngine
from workscript import (
    EnhancedBaseWorkScript, Action, ScreenInfo, CoordinateConverter,
    APP_CONFIGURATIONS, AppNavigator
)


@dataclass
class ModelConfig:
    """AI模型配置"""
    base_url: str = "http://localhost:8000/v1"
    api_key: str = "EMPTY"
    model_name: str = "default-model"
    max_tokens: int = 3000
    temperature: float = 0.0
    top_p: float = 0.85
    frequency_penalty: float = 0.2
    extra_body: Dict[str, Any] = field(default_factory=lambda: {"skip_special_tokens": False})


@dataclass
class AgentConfig:
    """智能代理配置"""
    max_steps: int = 100
    device_id: Optional[str] = None
    verbose: bool = True
    enable_ai: bool = False  # 是否启用AI决策
    enable_vision: bool = False  # 是否启用视觉理解
    confirmation_required: bool = True  # 是否需要敏感操作确认


@dataclass
class ExecutionResult:
    """执行结果"""
    success: bool
    message: str
    data: Optional[Dict[str, Any]] = None
    actions: List[Dict[str, Any]] = field(default_factory=list)
    execution_time: float = 0.0
    error: Optional[str] = None


@dataclass
class StepResult:
    """单步执行结果"""
    success: bool
    finished: bool
    action: Optional[Action] = None
    thinking: str = ""
    message: Optional[str] = None
    screenshot: Optional[str] = None


class AIDecisionEngine:
    """AI决策引擎"""
    
    def __init__(self, model_config: Optional[ModelConfig] = None):
        self.model_config = model_config or ModelConfig()
        self.conversation_history = []
        
    def decide_next_action(self, task_description: str, screen_info: ScreenInfo, 
                          previous_actions: List[Dict[str, Any]]) -> Action:
        """基于AI模型决定下一步操作"""
        # 这里应该集成实际的AI模型调用
        # 暂时返回模拟的决策结果
        
        prompt = self.build_decision_prompt(task_description, screen_info, previous_actions)
        
        # 模拟AI响应
        if "登录" in task_description:
            return Action(
                action_type="tap_at",
                parameters={"x": 500, "y": 800, "relative": True},
                description="AI决策：点击登录按钮",
                requires_confirmation=False
            )
        elif "搜索" in task_description:
            return Action(
                action_type="tap_at",
                parameters={"x": 500, "y": 200, "relative": True},
                description="AI决策：点击搜索框",
                requires_confirmation=False
            )
        else:
            return Action(
                action_type="finish",
                parameters={"message": "任务完成"},
                description="AI决策：完成任务",
                requires_confirmation=False
            )
    
    def build_decision_prompt(self, task_description: str, screen_info: ScreenInfo,
                            previous_actions: List[Dict[str, Any]]) -> str:
        """构建决策提示"""
        prompt = f"""
        任务描述: {task_description}
        当前应用: {screen_info.current_app}
        屏幕尺寸: {screen_info.width}x{screen_info.height}
        历史操作: {len(previous_actions)} 个
        
        请基于当前状态决定下一步操作。
        可用操作类型: tap_at, swipe, input_text, long_press, double_tap, back, home, finish
        
        返回格式:
        ACTION: <操作类型>
        PARAMETERS: <参数>
        DESCRIPTION: <操作描述>
        """
        return prompt.strip()


class ScreenCapture:
    """屏幕截图管理器"""
    
    def __init__(self, device_id: Optional[str] = None):
        self.device_id = device_id
        self.screenshot_counter = 0
        self.screenshot_dir = Path("reports/screenshots")
        self.screenshot_dir.mkdir(parents=True, exist_ok=True)
    
    def capture_screen(self, filename: Optional[str] = None) -> str:
        """截取屏幕并保存"""
        if not filename:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"screenshot_{timestamp}_{self.screenshot_counter}.png"
            self.screenshot_counter += 1
        
        screenshot_path = self.screenshot_dir / filename
        
        # 这里应该集成实际的屏幕截图逻辑
        # 暂时创建空文件作为占位符
        screenshot_path.touch()
        
        return str(screenshot_path)
    
    def get_screen_info(self) -> ScreenInfo:
        """获取屏幕信息"""
        # 这里应该集成实际的设备信息获取
        return ScreenInfo(
            width=1080,
            height=1920,
            current_app="Unknown",
            screenshot_path=self.capture_screen(),
            text_elements=[],
            ui_elements=[]
        )


class ErrorRecovery:
    """错误恢复处理器"""
    
    def __init__(self):
        self.recovery_strategies = {
            'app_not_responding': self.handle_app_crash,
            'element_not_found': self.handle_missing_element,
            'network_error': self.handle_network_issue,
            'login_required': self.handle_login_required,
            'timeout': self.handle_timeout
        }
        self.max_retries = 3
    
    def handle_error(self, error_type: str, context: Dict[str, Any]) -> bool:
        """处理错误并尝试恢复"""
        if error_type in self.recovery_strategies:
            return self.recovery_strategies[error_type](context)
        return False
    
    def handle_app_crash(self, context: Dict[str, Any]) -> bool:
        """处理应用崩溃"""
        logging.info("尝试恢复应用崩溃")
        # 重新启动应用
        app_name = context.get('current_app', '')
        if app_name in APP_CONFIGURATIONS:
            # 这里应该集成应用重启逻辑
            return True
        return False
    
    def handle_missing_element(self, context: Dict[str, Any]) -> bool:
        """处理元素未找到"""
        logging.info("尝试处理元素未找到")
        # 等待并重试，或者尝试替代方案
        time.sleep(2)  # 等待页面加载
        return True
    
    def handle_network_issue(self, context: Dict[str, Any]) -> bool:
        """处理网络问题"""
        logging.info("尝试处理网络问题")
        # 等待网络恢复
        time.sleep(5)
        return True
    
    def handle_login_required(self, context: Dict[str, Any]) -> bool:
        """处理需要登录的情况"""
        logging.info("需要登录，尝试自动登录")
        # 这里可以实现自动登录逻辑
        return False  # 暂时无法自动处理
    
    def handle_timeout(self, context: Dict[str, Any]) -> bool:
        """处理超时"""
        logging.info("处理超时问题")
        # 增加等待时间
        time.sleep(3)
        return True


class TaskExecutor:
    """任务执行器"""
    
    def __init__(self, agent_config: AgentConfig):
        self.config = agent_config
        self.screen_capture = ScreenCapture(agent_config.device_id)
        self.error_recovery = ErrorRecovery()
        self.ai_engine = None
        
        if agent_config.enable_ai:
            self.ai_engine = AIDecisionEngine()
    
    def execute_task(self, task_description: str, work_script: EnhancedBaseWorkScript) -> ExecutionResult:
        """执行任务"""
        start_time = time.time()
        actions = []
        
        try:
            work_script.initialize_enhanced_features(self.config.device_id)
            
            # 初始化任务
            self.log_task_start(task_description)
            
            # 检查是否有run方法，直接调用脚本运行
            if hasattr(work_script, 'run'):
                logging.info(f"直接调用脚本的run方法执行任务: {task_description}")
                result = work_script.run(task_description=task_description)
                execution_time = time.time() - start_time
                
                # 转换结果为ExecutionResult格式
                return ExecutionResult(
                    success=result.get('success', False),
                    message=result.get('message', '任务完成'),
                    actions=result.get('actions', []),
                    execution_time=execution_time
                )
            
            # 执行任务步骤（传统模式）
            for step in range(self.config.max_steps):
                step_result = self.execute_step(task_description, work_script, actions)
                
                if step_result.action:
                    actions.append({
                        "step": step + 1,
                        "action": step_result.action.action_type,
                        "description": step_result.action.description,
                        "success": step_result.success
                    })
                
                if step_result.finished:
                    execution_time = time.time() - start_time
                    return ExecutionResult(
                        success=True,
                        message=step_result.message or "任务完成",
                        actions=actions,
                        execution_time=execution_time
                    )
                
                if not step_result.success:
                    # 尝试错误恢复
                    recovery_success = self.error_recovery.handle_error(
                        "execution_error", {"context": step_result.message}
                    )
                    
                    if not recovery_success:
                        execution_time = time.time() - start_time
                        return ExecutionResult(
                            success=False,
                            message=f"步骤 {step + 1} 执行失败: {step_result.message}",
                            actions=actions,
                            execution_time=execution_time,
                            error=step_result.message
                        )
            
            # 达到最大步骤限制
            execution_time = time.time() - start_time
            return ExecutionResult(
                success=False,
                message="达到最大步骤限制",
                actions=actions,
                execution_time=execution_time
            )
            
        except Exception as e:
            execution_time = time.time() - start_time
            return ExecutionResult(
                success=False,
                message=f"任务执行异常: {str(e)}",
                actions=actions,
                execution_time=execution_time,
                error=str(e)
            )
    
    def execute_step(self, task_description: str, work_script: EnhancedBaseWorkScript, 
                    previous_actions: List[Dict[str, Any]]) -> StepResult:
        """执行单步操作"""
        try:
            # 获取当前屏幕状态
            screen_info = self.screen_capture.get_screen_info()
            work_script.screen_info = screen_info
            
            if self.config.enable_ai and self.ai_engine:
                # AI决策模式
                action = self.ai_engine.decide_next_action(
                    task_description, screen_info, previous_actions
                )
                
                if action.action_type == "finish":
                    return StepResult(
                        success=True,
                        finished=True,
                        action=action,
                        thinking="AI决策完成任务",
                        message=action.parameters.get("message", "任务完成")
                    )
                
                # 执行AI决策的动作
                result = work_script.execute_action(action)
                
                return StepResult(
                    success=result["success"],
                    finished=False,
                    action=action,
                    thinking=action.description,
                    message=result.get("message")
                )
            
            else:
                # 传统脚本模式
                # 这里可以根据任务描述调用相应的脚本方法
                if hasattr(work_script, 'execute_intelligent_step'):
                    return work_script.execute_intelligent_step(task_description)
                else:
                    # 如果没有智能步骤方法，则完成任务
                    return StepResult(
                        success=True,
                        finished=True,
                        message="传统模式：任务步骤完成"
                    )
                    
        except Exception as e:
            return StepResult(
                success=False,
                finished=False,
                thinking="步骤执行失败",
                message=f"步骤执行异常: {str(e)}"
            )
    
    def log_task_start(self, task_description: str):
        """记录任务开始"""
        logging.info(f"开始执行任务: {task_description}")
        if self.config.verbose:
            print(f"🚀 开始执行任务: {task_description}")


class EnhancedWorkScriptEngine:
    """增强版工作脚本引擎"""
    
    def __init__(self, reports_dir: str = "reports", agent_config: Optional[AgentConfig] = None):
        self.reports_dir = Path(reports_dir)
        self.reports_dir.mkdir(parents=True, exist_ok=True)
        
        self.agent_config = agent_config or AgentConfig()
        self.task_executor = TaskExecutor(self.agent_config)
        self.script_registry = {}
        
        self._setup_logging()
        self._register_enhanced_scripts()
    
    def _setup_logging(self):
        """设置日志记录"""
        log_file = self.reports_dir / "enhanced_engine.log"
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(log_file),
                logging.StreamHandler()
            ]
        )
        self.logger = logging.getLogger(__name__)
    
    def _register_enhanced_scripts(self):
        """注册增强版脚本"""
        # 导入增强版脚本
        from enhanced_workscript import EnhancedLoginTestScript
        
        self.script_registry["enhanced_login"] = EnhancedLoginTestScript()
        
        self.logger.info(f"注册了 {len(self.script_registry)} 个增强版脚本")
    
    def execute_intelligent_task(self, task_description: str, script_name: str = None, 
                               work_script: EnhancedBaseWorkScript = None, **kwargs) -> ExecutionResult:
        """执行智能任务"""
        
        if not work_script:
            if script_name and script_name in self.script_registry:
                work_script = self.script_registry[script_name]
            else:
                # 创建默认的智能脚本
                work_script = self.create_intelligent_script(task_description)
        
        # 执行任务
        result = self.task_executor.execute_task(task_description, work_script)
        
        # 保存结果
        self.save_execution_result(result, task_description)
        
        return result
    
    def create_intelligent_script(self, task_description: str) -> EnhancedBaseWorkScript:
        """基于任务描述创建智能脚本"""
        
        class IntelligentScript(EnhancedBaseWorkScript):
            def __init__(self, task_desc):
                super().__init__()
                self.name = f"智能脚本_{task_desc[:20]}"
                self.description = f"基于AI的智能脚本: {task_desc}"
                self.task_description = task_desc
            
            def run(self, **kwargs):
                # 这里可以实现基于任务描述的动态行为
                return {
                    "success": True,
                    "message": f"智能执行任务: {self.task_description}",
                    "actions": self.get_action_summary()
                }
        
        return IntelligentScript(task_description)
    
    def save_execution_result(self, result: ExecutionResult, task_description: str):
        """保存执行结果"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        result_file = self.reports_dir / f"enhanced_execution_{timestamp}.json"
        
        result_data = {
            "task_description": task_description,
            "timestamp": timestamp,
            "success": result.success,
            "message": result.message,
            "execution_time": result.execution_time,
            "actions": result.actions,
            "error": result.error,
            "data": result.data
        }
        
        with open(result_file, 'w', encoding='utf-8') as f:
            json.dump(result_data, f, ensure_ascii=False, indent=2)
        
        self.logger.info(f"执行结果已保存到: {result_file}")
    
    def get_available_apps(self) -> List[str]:
        """获取可用的应用列表"""
        return AppNavigator.get_common_apps()
    
    def get_app_config(self, app_name: str) -> Dict[str, Any]:
        """获取应用配置"""
        return AppNavigator.get_app_config(app_name)
    
    def set_ai_config(self, model_config: ModelConfig, agent_config: AgentConfig):
        """设置AI配置"""
        self.agent_config = agent_config
        self.task_executor = TaskExecutor(agent_config)
        if agent_config.enable_ai:
            self.task_executor.ai_engine = AIDecisionEngine(model_config)
        
        self.logger.info("AI配置已更新")


# 使用示例和测试
if __name__ == "__main__":
    # 创建增强版引擎
    agent_config = AgentConfig(
        max_steps=50,
        verbose=True,
        enable_ai=True,  # 启用AI决策
        enable_vision=False,  # 暂时不启用视觉理解
        confirmation_required=False
    )
    
    enhanced_engine = EnhancedWorkScriptEngine(
        reports_dir="reports/enhanced",
        agent_config=agent_config
    )
    
    # 执行智能任务
    print("🚀 开始执行智能任务测试...")
    
    # 测试1: 智能登录任务
    result1 = enhanced_engine.execute_intelligent_task(
        task_description="执行智能登录测试",
        script_name="enhanced_login",
        username="test_user",
        password="test_password"
    )
    
    print(f"\n📊 任务执行结果:")
    print(f"成功: {result1.success}")
    print(f"消息: {result1.message}")
    print(f"执行时间: {result1.execution_time:.2f}秒")
    print(f"操作数量: {len(result1.actions)}")
    
    # 测试2: 自定义智能任务
    result2 = enhanced_engine.execute_intelligent_task(
        task_description="在淘宝中搜索无线耳机并添加到购物车"
    )
    
    print(f"\n📊 智能购物任务结果:")
    print(f"成功: {result2.success}")
    print(f"消息: {result2.message}")
    print(f"执行时间: {result2.execution_time:.2f}秒")
    
    # 显示可用应用
    print(f"\n📱 可用应用列表:")
    for app in enhanced_engine.get_available_apps():
        config = enhanced_engine.get_app_config(app)
        print(f"  - {app}: {config.get('package', '未知包名')}")
    
    print(f"\n✅ 增强版引擎测试完成！")