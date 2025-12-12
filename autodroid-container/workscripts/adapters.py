"""
适配器模块
用于适配不同的接口和类
"""

from typing import Dict, Any, Optional
import logging

logger = logging.getLogger(__name__)

class OpenAutoGLMAdapter:
    """Open-AutoGLM适配器"""
    
    def __init__(self, agent):
        """初始化适配器
        
        Args:
            agent: PhoneAgent实例
        """
        self.agent = agent
        self.device_controller = None
    
    def set_device_controller(self, device_controller):
        """设置设备控制器"""
        self.device_controller = device_controller
    
    def test_login_flow(self, app_name: str, package_name: str, username: str, password: str, expected_result: str) -> Dict[str, Any]:
        """测试登录流程"""
        logger.info(f"开始测试登录流程: {app_name} ({package_name})")
        
        result = {
            'app_name': app_name,
            'package_name': package_name,
            'username': username,
            'expected_result': expected_result,
            'start_time': datetime.now().isoformat(),
            'status': 'pending',
            'steps': []
        }
        
        try:
            # 启动应用
            logger.info(f"启动应用: {package_name}")
            launch_result = self.device_controller.start_app(package_name)
            result['steps'].append({
                'action': 'launch_app',
                'result': 'success' if launch_result else 'failed'
            })
            
            time.sleep(3)  # 等待应用加载
            
            # 使用模型进行UI分析
            logger.info("使用模型分析UI元素")
            ui_elements = self.analyze_ui_elements()
            
            # 查找登录相关元素
            login_elements = self.find_login_elements(ui_elements)
            
            if not login_elements:
                logger.warning("未找到登录相关元素，使用默认坐标模式")
                result['status'] = 'failed'
                result['error'] = '未找到登录元素'
                return result
            
            # 执行登录操作
            logger.info("执行登录操作")
            login_result = self.perform_login_actions(login_elements, username, password)
            
            # 验证结果
            logger.info("验证登录结果")
            verification_result = self.verify_login_result()
            
            result['steps'].extend(login_result['steps'])
            result['actual_result'] = verification_result['result']
            result['result_match'] = (expected_result == verification_result['result'])
            result['status'] = 'success'
            result['end_time'] = datetime.now().isoformat()
            
            logger.info(f"登录测试完成: 预期={expected_result}, 实际={verification_result['result']}, 匹配={result['result_match']}")
            
        except Exception as e:
            logger.error(f"登录测试失败: {e}")
            result['status'] = 'failed'
            result['error'] = str(e)
            result['end_time'] = datetime.now().isoformat()
        
        return result
    
    def _analyze_ui_elements(self) -> list:
        """分析UI元素（模拟）"""
        return [
            {
                "type": "input",
                "text": "用户名/邮箱/手机号",
                "coordinates": {"x": 540, "y": 600},
                "confidence": 0.92
            },
            {
                "type": "input",
                "text": "密码",
                "coordinates": {"x": 540, "y": 800},
                "confidence": 0.90
            },
            {
                "type": "button",
                "text": "登录",
                "coordinates": {"x": 540, "y": 1200},
                "confidence": 0.95
            }
        ]
    
    def _execute_login_operations(self, username: str, password: str) -> list:
        """执行登录操作（模拟）"""
        operations = []
        
        # 模拟点击用户名输入框
        operations.append({
            "action": "click",
            "target": "用户名输入框",
            "coordinate": {"x": 540, "y": 600},
            "result": "success"
        })
        
        # 模拟输入用户名
        operations.append({
            "action": "input_text",
            "target": "用户名输入框",
            "text": username,
            "result": "success"
        })
        
        # 模拟点击密码输入框
        operations.append({
            "action": "click",
            "target": "密码输入框",
            "coordinate": {"x": 540, "y": 800},
            "result": "success"
        })
        
        # 模拟输入密码
        operations.append({
            "action": "input_text",
            "target": "密码输入框",
            "text": "*" * len(password),  # 隐藏密码
            "result": "success"
        })
        
        # 模拟点击登录按钮
        operations.append({
            "action": "click",
            "target": "登录按钮",
            "coordinate": {"x": 540, "y": 1200},
            "result": "success"
        })
        
        return operations
    
    def _verify_login_result(self) -> Dict[str, Any]:
        """验证登录结果（模拟）"""
        # 随机返回成功或失败
        import random
        if random.random() > 0.3:  # 70% 成功率
            return {
                "status": "success",
                "message": "登录成功，检测到欢迎页面"
            }
        else:
            return {
                "status": "failure",
                "message": "登录失败，检测到错误提示"
            }

class IntelligentAPKTester:
    """智能APK测试器"""
    
    def __init__(self, adapter: Optional[OpenAutoGLMAdapter] = None):
        """初始化智能测试器
        
        Args:
            adapter: OpenAutoGLM适配器实例
        """
        self.adapter = adapter
        self.test_results = []
    
    def set_adapter(self, adapter: OpenAutoGLMAdapter):
        """设置适配器"""
        self.adapter = adapter
    
    def test_login_flow(self, app_name: str, username: str, password: str, 
                       use_intelligent_mode: bool = True) -> Dict[str, Any]:
        """测试登录流程
        
        Args:
            app_name: 应用名称
            username: 用户名
            password: 密码
            use_intelligent_mode: 是否使用智能模式
            
        Returns:
            测试结果字典
        """
        if self.adapter:
            return self.adapter.test_login_flow(app_name, username, password, use_intelligent_mode)
        else:
            logger.error("未设置适配器")
            return {
                "status": "failed",
                "message": "测试器未初始化适配器"
            }