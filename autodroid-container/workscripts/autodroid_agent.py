"""AutoDroid Agent - Natural language to workplan execution."""

import json
import time
import os
from typing import Dict, List, Any, Optional
from dataclasses import dataclass
from adb_device import ADBDevice, quick_connect
from ui_definitions import UIDefinitionManager, UIElementDefinition, ElementType, IdentifierType
from intelligent_agent import IntelligentAgent, WorkStep, UIElement


@dataclass
class TaskIntent:
    """Parsed task intent from natural language."""
    action: str  # open_app, login, search, click, input, etc.
    target_app: str  # app package name or app name
    parameters: Dict[str, Any]  # additional parameters
    workplan_data: Optional[Dict[str, Any]] = None  # workplan data instead of file path


class AutoDroidAgent:
    """AutoDroid Agent that understands natural language and executes workplans."""
    
    def __init__(self, device: Optional[ADBDevice] = None, ui_manager: Optional[UIDefinitionManager] = None):
        """Initialize AutoDroid Agent.
        
        Args:
            device: ADB device instance
            ui_manager: UI definition manager
        """
        self.device = device or quick_connect()
        self.ui_manager = ui_manager or UIDefinitionManager()
        self.intelligent_agent = IntelligentAgent(device, ui_manager)
        
        # Predefined task templates
        self.task_templates = self._load_task_templates()
        self.app_aliases = self._load_app_aliases()
        
        # Workplans directory
        self.workplans_dir = "workplans"
        os.makedirs(self.workplans_dir, exist_ok=True)
    
    def _load_task_templates(self) -> Dict[str, Dict[str, Any]]:
        """Load predefined task templates."""
        return {
            "login": {
                "description": "Login to an application",
                "required_params": ["username", "password"],
                "optional_params": ["app_package", "login_type"],
                "default_workplan": "login_test.json"
            },
            "search": {
                "description": "Search for something in an app",
                "required_params": ["query"],
                "optional_params": ["app_package", "search_type"],
                "default_workplan": None
            },
            "open_app": {
                "description": "Open a specific application",
                "required_params": ["app_name"],
                "optional_params": [],
                "default_workplan": None
            }
        }
    
    def _load_app_aliases(self) -> Dict[str, str]:
        """Load app name to package name mappings."""
        return {
            "淘宝": "com.taobao.taobao",
            "taobao": "com.taobao.taobao",
            "美团": "com.sankuai.meituan",
            "meituan": "com.sankuai.meituan",
            "微信": "com.tencent.mm",
            "wechat": "com.tencent.mm",
            "支付宝": "com.eg.android.AlipayGphone",
            "alipay": "com.eg.android.AlipayGphone",
            "京东": "com.jingdong.app.mall",
            "jd": "com.jingdong.app.mall",
            "autodroid": "com.autodroid.manager",
            "autodroid manager": "com.autodroid.manager",
            "manager": "com.autodroid.manager"
        }
    
    def parse_natural_language(self, instruction: str) -> TaskIntent:
        """Parse natural language instruction into task intent.
        
        Args:
            instruction: Natural language instruction
            
        Returns:
            Parsed task intent
        """
        instruction_lower = instruction.lower().strip()
        
        # Parse "打开[应用]" pattern
        if "打开" in instruction_lower or "open" in instruction_lower:
            # Extract app name
            app_name = self._extract_app_name(instruction)
            
            return TaskIntent(
                action="open_app",
                target_app=self._resolve_app_package(app_name),
                parameters={"app_name": app_name},
                workplan_data=None
            )
        
        # Parse "登录" pattern
        elif "登录" in instruction_lower or "login" in instruction_lower:
            app_name = self._extract_app_name(instruction)
            
            # 创建登录测试的workplan数据，只包含实际需要的测试数据
            login_workplan = {
                'data': {
                    'email': 'test@example.com',  # 默认测试邮箱
                    'pass': '123456',  # 默认测试密码
                    'timeout': 30,
                    'success_rate': 0.9
                }
            }
            
            return TaskIntent(
                action="login",
                target_app=self._resolve_app_package(app_name) if app_name else "unknown",
                parameters={"app_name": app_name},
                workplan_data=login_workplan
            )
        
        # Parse "搜索" pattern
        elif "搜索" in instruction_lower or "search" in instruction_lower:
            app_name = self._extract_app_name(instruction)
            search_query = self._extract_search_query(instruction)
            
            return TaskIntent(
                action="search",
                target_app=self._resolve_app_package(app_name) if app_name else "unknown",
                parameters={
                    "app_name": app_name,
                    "query": search_query
                },
                workplan_data=None
            )
        
        # Default case - try to find workplan file
        else:
            workplan_data = self._find_workplan_data(instruction)
            
            return TaskIntent(
                action="execute_workplan",
                target_app="unknown",
                parameters={"instruction": instruction},
                workplan_data=workplan_data
            )
    
    def _extract_app_name(self, instruction: str) -> Optional[str]:
        """Extract app name from instruction."""
        # Common patterns for app extraction
        patterns = [
            ("打开", ""),  # 打开淘宝
            ("open", ""),  # open taobao
            ("登录", ""),  # 登录淘宝
            ("login to", ""),  # login to taobao
            ("在", "搜索"),  # 在淘宝搜索
            ("in", "search"),  # search in taobao
        ]
        
        instruction_lower = instruction.lower()
        
        for start_word, end_word in patterns:
            if start_word in instruction_lower:
                try:
                    # Find position of start word
                    start_pos = instruction_lower.find(start_word)
                    remaining_text = instruction[start_pos + len(start_word):].strip()
                    
                    # If there's an end word, extract text between start and end
                    if end_word and end_word in remaining_text:
                        end_pos = remaining_text.find(end_word)
                        app_name = remaining_text[:end_pos].strip()
                    else:
                        # Take first word after start word
                        app_name = remaining_text.split()[0] if remaining_text else ""
                    
                    return app_name.strip() if app_name else None
                except (IndexError, ValueError):
                    continue
        
        return None
    
    def _extract_search_query(self, instruction: str) -> Optional[str]:
        """Extract search query from instruction."""
        # Common patterns for search query extraction
        patterns = [
            ("搜索", ""),  # 搜索无线耳机
            ("search for", ""),  # search for wireless headphones
            ("搜索", ""),  # 在淘宝搜索无线耳机 (after app name)
        ]
        
        instruction_lower = instruction.lower()
        
        for start_word, end_word in patterns:
            if start_word in instruction_lower:
                try:
                    # Find position of start word
                    start_pos = instruction_lower.rfind(start_word)  # Use last occurrence
                    remaining_text = instruction[start_pos + len(start_word):].strip()
                    
                    # Extract the search query (usually the rest of the text)
                    if remaining_text:
                        return remaining_text.strip()
                except (IndexError, ValueError):
                    continue
        
        return None
    
    def _resolve_app_package(self, app_name: str) -> str:
        """Resolve app name to package name."""
        if not app_name:
            return "unknown"
        
        app_name_lower = app_name.lower()
        
        # Check aliases
        if app_name_lower in self.app_aliases:
            return self.app_aliases[app_name_lower]
        
        # Common app packages (add more as needed)
        common_packages = {
            "taobao": "com.taobao.taobao",
            "meituan": "com.sankuai.meituan",
            "wechat": "com.tencent.mm",
            "alipay": "com.eg.android.AlipayGphone",
            "jd": "com.jingdong.app.mall",
            "autodroid": "com.autodroid.manager"
        }
        
        return common_packages.get(app_name_lower, f"unknown.{app_name_lower}")
    
    def _find_workplan_data(self, instruction: str) -> Optional[Dict[str, Any]]:
        """Find workplan data based on instruction."""
        # 检查是否是登录相关的指令
        if any(word in instruction.lower() for word in ['登录', 'login', 'signin']):
            return {
                'data': {  # 只返回实际需要的测试数据
                    'email': 'test@example.com',
                    'pass': '123456',
                    'timeout': 30,
                    'success_rate': 0.9
                }
            }
        
        # 检查是否是搜索相关的指令
        if any(word in instruction.lower() for word in ['搜索', 'search', 'find']):
            return {
                'data': {  # 只返回实际需要的测试数据
                    'query': instruction,
                    'timeout': 30,
                    'success_rate': 0.9
                }
            }
        
        return None
    
    def create_login_workplan(self, username: str, password: str, app_package: str = None) -> Dict[str, Any]:
        """Create a login workplan with provided credentials.
        
        Args:
            username: Login username/email
            password: Login password
            app_package: Target app package
            
        Returns:
            Login workplan
        """
        workplan = {
            "name": f"Login to {app_package or 'app'}",
            "description": f"Automated login with username {username}",
            "app_package": app_package or "unknown.app",
            "credentials": {
                "username": username,
                "password": password
            },
            "steps": [
                {
                    "step_id": "1",
                    "description": "找到并点击用户名/邮箱输入框",
                    "ui_elements": [
                        {
                            "name": "username_field",
                            "type": "input_field",
                            "identifier": {
                                "coordinates": {"x": 200, "y": 300},
                                "text": "用户名",
                                "xpath": "//android.widget.EditText[contains(@text,'用户名')]"
                            },
                            "action": "tap",
                            "wait_time": 0.5
                        },
                        {
                            "name": "input_username",
                            "type": "text_input",
                            "identifier": {"text": "用户名"},
                            "action": "input",
                            "input_data": username,
                            "wait_time": 1.0
                        }
                    ],
                    "expected_result": "用户名输入框已填写",
                    "fallback_action": "尝试通过resource-id查找输入框"
                },
                {
                    "step_id": "2",
                    "description": "找到并点击密码输入框",
                    "ui_elements": [
                        {
                            "name": "password_field",
                            "type": "input_field",
                            "identifier": {
                                "coordinates": {"x": 400, "y": 300},
                                "text": "密码",
                                "xpath": "//android.widget.EditText[contains(@text,'密码')]"
                            },
                            "action": "tap",
                            "wait_time": 0.5
                        },
                        {
                            "name": "input_password",
                            "type": "text_input",
                            "identifier": {"text": "密码"},
                            "action": "input",
                            "input_data": password,
                            "wait_time": 1.0
                        }
                    ],
                    "expected_result": "密码输入框已填写",
                    "fallback_action": "尝试通过resource-id查找密码框"
                },
                {
                    "step_id": "3",
                    "description": "找到并点击登录按钮",
                    "ui_elements": [
                        {
                            "name": "login_button",
                            "type": "button",
                            "identifier": {
                                "coordinates": {"x": 600, "y": 300},
                                "text": "登录",
                                "xpath": "//android.widget.Button[@text='登录']"
                            },
                            "action": "tap",
                            "wait_time": 3.0
                        }
                    ],
                    "expected_result": "登录成功，进入主界面",
                    "fallback_action": "尝试通过resource-id查找登录按钮"
                }
            ]
        }
        
        return workplan
    
    def run(self, instruction: str, workplan_data: Dict[str, Any] = None, verbose: bool = True) -> Dict[str, Any]:
        """Run agent with natural language instruction.
        
        Args:
            instruction: Natural language instruction
            workplan_data: Optional workplan data to use
            verbose: Whether to print thinking process and actions
            
        Returns:
            Execution result
        """
        if verbose:
            print(f"=== AutoDroid Agent ===")
            print(f"Instruction: {instruction}")
            if workplan_data:
                print(f"Workplan data provided: {len(workplan_data)} keys")
        
        # Parse instruction
        task_intent = self.parse_natural_language(instruction)
        
        if verbose:
            print(f"💭 思考过程:")
            print(f"指令解析: '{instruction}' -> 动作: {task_intent.action}, 目标: {task_intent.target_app}")
            if workplan_data:
                print(f"使用提供的workplan数据: {workplan_data.get('name', '未命名')}")
        
        # Handle different actions
        if task_intent.action == "open_app":
            result = self._handle_open_app(task_intent, verbose)
        
        elif task_intent.action == "login":
            result = self._handle_login(task_intent, workplan_data, verbose)
        
        elif task_intent.action == "search":
            result = self._handle_search(task_intent, verbose)
        
        elif task_intent.action == "execute_workplan":
            result = self._handle_execute_workplan(task_intent, workplan_data, verbose)
        
        else:
            result = {
                "success": False,
                "error": f"Unknown action: {task_intent.action}",
                "instruction": instruction
            }
        
        if verbose:
            print(f"🎯 执行结果:")
            print(f"成功: {result.get('success', False)}")
            if result.get('success'):
                print(f"动作: {result.get('action', 'unknown')}")
                if 'workplan_file' in result:
                    print(f"工作文件: {result['workplan_file']}")
            else:
                print(f"错误: {result.get('error', 'unknown error')}")
            print("=" * 50)
        
        return result
    
    def _handle_open_app(self, task_intent: TaskIntent, verbose: bool = True) -> Dict[str, Any]:
        """Handle open app action."""
        app_name = task_intent.parameters.get("app_name", "unknown")
        app_package = task_intent.target_app
        
        if verbose:
            print(f"🎯 执行动作:")
            print(json.dumps({
                "_metadata": "do",
                "action": "Launch",
                "app": app_name,
                "package": app_package
            }, ensure_ascii=False, indent=2))
        
        try:
            # Launch the app
            result = self.device.launch_app(app_package)
            
            if verbose:
                print(f"✅ App launched successfully")
            
            return {
                "success": True,
                "action": "open_app",
                "app_name": app_name,
                "app_package": app_package,
                "result": result
            }
            
        except Exception as e:
            if verbose:
                print(f"❌ Failed to launch app: {e}")
            return {
                "success": False,
                "error": f"Failed to open app: {e}",
                "app_name": app_name,
                "app_package": app_package
            }
    
    def _handle_login(self, task_intent: TaskIntent, workplan_data: Dict[str, Any] = None, verbose: bool = True) -> Dict[str, Any]:
        """Handle login action."""
        app_name = task_intent.parameters.get("app_name", "unknown")
        app_package = task_intent.target_app
        
        if verbose:
            print(f"🎯 执行动作:")
            print(json.dumps({
                "_metadata": "do",
                "action": "Login",
                "app": app_name,
                "package": app_package
            }, ensure_ascii=False, indent=2))
        
        # Use provided workplan data or create from template
        if workplan_data:
            # 直接使用提供的workplan数据中的data字段
            test_data = workplan_data.get('data', workplan_data)
            login_workplan = self.create_login_workplan(
                username=test_data.get('email', 'test@example.com'),
                password=test_data.get('pass', '123456'),
                app_package=app_package
            )
            if verbose:
                print(f"使用提供的测试数据: 邮箱={test_data.get('email', 'test@example.com')}")
        else:
            # 使用默认的测试数据
            login_workplan = self.create_login_workplan(
                username="test@example.com",
                password="123456",
                app_package=app_package
            )
            if verbose:
                print(f"使用默认测试数据")
        
        # Save workplan to file
        workplan_file = f"{self.workplans_dir}/login_{app_package.replace('.', '_')}.json"
        with open(workplan_file, 'w', encoding='utf-8') as f:
            json.dump(login_workplan, f, ensure_ascii=False, indent=2)
        
        if verbose:
            print(f"保存workplan到: {workplan_file}")
        
        # Execute workplan using intelligent agent
        try:
            if verbose:
                print(f"开始执行登录流程...")
            result = self.intelligent_agent.run_workplan(workplan_file)
            
            if verbose:
                print(f"✅ 登录流程执行完成")
            
            return {
                "success": True,
                "action": "login",
                "app_name": app_name,
                "app_package": app_package,
                "workplan_file": workplan_file,
                "execution_result": result
            }
            
        except Exception as e:
            if verbose:
                print(f"❌ 登录执行失败: {e}")
            return {
                "success": False,
                "error": f"Login execution failed: {e}",
                "app_name": app_name,
                "app_package": app_package,
                "workplan_file": workplan_file
            }
    
    def _handle_search(self, task_intent: TaskIntent, verbose: bool = True) -> Dict[str, Any]:
        """Handle search action."""
        app_name = task_intent.parameters.get("app_name", "unknown")
        app_package = task_intent.target_app
        query = task_intent.parameters.get("query", "")
        
        if verbose:
            print(f"🎯 执行动作:")
            print(json.dumps({
                "_metadata": "do",
                "action": "Search",
                "app": app_name,
                "package": app_package,
                "query": query
            }, ensure_ascii=False, indent=2))
        
        # Create search workplan
        search_workplan = {
            "name": f"Search in {app_name}",
            "description": f"Search for '{query}' in {app_name}",
            "app_package": app_package,
            "steps": [
                {
                    "step_id": "1",
                    "description": f"在{app_name}中搜索'{query}'",
                    "ui_elements": [
                        {
                            "name": "search_box",
                            "type": "search_box",
                            "identifier": {
                                "coordinates": {"x": 540, "y": 150},
                                "text": "搜索",
                                "xpath": "//android.widget.EditText[contains(@text,'搜索')]"
                            },
                            "action": "tap",
                            "wait_time": 1.0
                        },
                        {
                            "name": "input_search",
                            "type": "text_input",
                            "identifier": {"text": "搜索"},
                            "action": "input",
                            "input_data": query,
                            "wait_time": 1.0
                        },
                        {
                            "name": "submit_search",
                            "type": "button",
                            "identifier": {
                                "coordinates": {"x": 1000, "y": 150},
                                "text": "搜索",
                                "xpath": "//android.widget.Button[@text='搜索']"
                            },
                            "action": "tap",
                            "wait_time": 3.0
                        }
                    ],
                    "expected_result": f"显示'{query}'的搜索结果",
                    "fallback_action": "使用语音搜索"
                }
            ]
        }
        
        if verbose:
            print(f"创建搜索workplan: {query}")
        
        # Save and execute workplan
        workplan_file = f"{self.workplans_dir}/search_{app_package.replace('.', '_')}_{int(time.time())}.json"
        with open(workplan_file, 'w', encoding='utf-8') as f:
            json.dump(search_workplan, f, ensure_ascii=False, indent=2)
        
        if verbose:
            print(f"保存搜索workplan到: {workplan_file}")
        
        try:
            if verbose:
                print(f"开始执行搜索流程...")
            result = self.intelligent_agent.run_workplan(workplan_file)
            
            if verbose:
                print(f"✅ 搜索流程执行完成")
            
            return {
                "success": True,
                "action": "search",
                "app_name": app_name,
                "query": query,
                "workplan_file": workplan_file,
                "execution_result": result
            }
            
        except Exception as e:
            if verbose:
                print(f"❌ 搜索执行失败: {e}")
            return {
                "success": False,
                "error": f"Search execution failed: {e}",
                "app_name": app_name,
                "query": query,
                "workplan_file": workplan_file
            }
    
    def _handle_execute_workplan(self, task_intent: TaskIntent, workplan_data: Dict[str, Any] = None, verbose: bool = True) -> Dict[str, Any]:
        """Handle execute workplan action."""
        if verbose:
            print(f"🎯 执行动作:")
            print(json.dumps({
                "_metadata": "do",
                "action": "ExecuteWorkplan",
                "instruction": task_intent.parameters.get("instruction", "")
            }, ensure_ascii=False, indent=2))
        
        # 直接使用workplan_data中的测试数据，忽略name、description等元数据字段
        if workplan_data:
            test_data = workplan_data.get('data', workplan_data)
            
            # 根据测试数据创建实际的workplan结构
            if 'email' in test_data and 'pass' in test_data:
                # 登录测试数据
                actual_workplan = self.create_login_workplan(
                    username=test_data.get('email', 'test@example.com'),
                    password=test_data.get('pass', '123456'),
                    app_package="com.autodroid.manager"
                )
                if verbose:
                    print(f"使用登录测试数据: 邮箱={test_data.get('email', 'test@example.com')}")
            else:
                # 其他类型的测试数据，创建通用workplan
                actual_workplan = {
                    "name": "Custom Test",
                    "description": task_intent.parameters.get("instruction", ""),
                    "steps": []
                }
                if verbose:
                    print(f"使用通用测试数据: {list(test_data.keys())}")
            
            # 保存workplan到文件用于执行
            workplan_file = f"{self.workplans_dir}/custom_{int(time.time())}.json"
            with open(workplan_file, 'w', encoding='utf-8') as f:
                json.dump(actual_workplan, f, ensure_ascii=False, indent=2)
            if verbose:
                print(f"生成workplan文件: {workplan_file}")
        else:
            if verbose:
                print(f"❌ 没有提供workplan数据")
            return {
                "success": False,
                "error": "No workplan data provided",
                "instruction": task_intent.parameters.get("instruction", "")
            }
        
        try:
            if verbose:
                print(f"开始执行workplan...")
            result = self.intelligent_agent.run_workplan(workplan_file)
            
            if verbose:
                print(f"✅ workplan执行完成")
            
            return {
                "success": True,
                "action": "execute_workplan",
                "workplan_file": workplan_file,
                "execution_result": result
            }
            
        except Exception as e:
            if verbose:
                print(f"❌ workplan执行失败: {e}")
            return {
                "success": False,
                "error": f"Workplan execution failed: {e}",
                "workplan_file": workplan_file
            }


def main():
    """Example usage of AutoDroid Agent."""
    # Create agent
    agent = AutoDroidAgent()
    
    # Example 1: Open app
    print("\n=== Example 1: Open Autodroid Manager ===")
    result1 = agent.run("打开Autodroid Manager")
    print(f"Result: {result1}")
    
    # Example 2: Login with workplan
    print("\n=== Example 2: Login with workplan ===")
    
    # Create login workplan with specific credentials
    login_workplan = agent.create_login_workplan(
        username="admin@autodroid.com",
        password="admin123",
        app_package="com.autodroid.manager"
    )
    
    result2 = agent.run("登录Autodroid Manager", login_workplan)
    print(f"Result: {result2}")
    
    # Example 3: Search in app
    print("\n=== Example 3: Search in app ===")
    result3 = agent.run("在淘宝搜索无线耳机")
    print(f"Result: {result3}")
    
    # Example 4: Execute specific workplan file
    print("\n=== Example 4: Execute workplan file ===")
    result4 = agent.run("执行登录测试", workplan_data=login_workplan)
    print(f"Result: {result4}")


if __name__ == "__main__":
    main()