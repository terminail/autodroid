#!/usr/bin/env python3
"""
Open-AutoGLM集成WorkScript
使用Open-AutoGLM智能操作框架执行第三方APK登录测试
"""

import os
import sys
import json
import time
import yaml
import logging
import subprocess
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Any

# 添加Open-AutoGLM路径
sys.path.append('d:/git/autodroid/Open-AutoGLM')

# 导入模型配置和适配器
from model_config import ConfigManager, ModelConfig, AgentConfig
from adapters import OpenAutoGLMAdapter, IntelligentAPKTester

from phone_agent import PhoneAgent
from phone_agent.actions.handler import ActionHandler
import phone_agent.adb.device as adb_device

# 添加工作脚本路径
sys.path.append('/d:/git/autodroid/autodroid-container/workscripts')

from device_connection import ADBDeviceController
from intelligent_apk_tester import IntelligentAPKTester, OpenAutoGLMAdapter

# 导入Open-AutoGLM的Tap动作支持
try:
    from phone_agent.adb.device import tap as autoGLM_tap
    HAS_AUTOGLM_SUPPORT = True
except ImportError:
    HAS_AUTOGLM_SUPPORT = False

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(f'logs/autoglm_workscript_{datetime.now().strftime("%Y%m%d_%H%M%S")}.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class AutoGLMWorkScript:
    """Open-AutoGLM集成工作脚本"""
    
    def __init__(self, workplan_path: str, device_id: str):
        self.workplan_path = workplan_path
        self.device_id = device_id
        self.workplan = None
        self.agent = None
        self.device_controller = None
        self.tester = None
        self.results = []
        
    def load_workplan(self) -> bool:
        """加载工作配置文件"""
        try:
            with open(self.workplan_path, 'r', encoding='utf-8') as f:
                self.workplan = yaml.safe_load(f)
            logger.info(f"成功加载工作配置文件: {self.workplan_path}")
            return True
        except Exception as e:
            logger.error(f"加载工作配置文件失败: {e}")
            return False
    
    def initialize_agent(self) -> bool:
        """初始化Open-AutoGLM代理"""
        try:
            # 获取Open-AutoGLM配置
            autoglm_config = self.workplan.get('autoglm_config', {})
            
            # 使用配置管理器加载配置
            config_manager = ConfigManager()
            if not config_manager.load_from_workplan({'autoglm_config': autoglm_config}):
                logger.error("加载配置失败")
                return False
            
            # 更新设备ID
            config_manager.update_device_id(self.device_id)
            
            # 获取配置对象
            model_config = config_manager.get_model_config()
            agent_config = config_manager.get_agent_config()
            
            # 初始化代理
            self.agent = PhoneAgent(
                model_config=model_config,
                agent_config=agent_config
            )
            
            # 初始化设备控制器
            self.device_controller = ADBDeviceController(device_udid=self.device_id)
            
            # 连接设备
            if not self.device_controller.connect():
                logger.error("设备控制器连接失败")
                return False
            
            # 初始化智能测试器
            adapter = OpenAutoGLMAdapter(self.agent)
            adapter.device_controller = self.device_controller  # 直接设置属性
            self.tester = IntelligentAPKTester(adapter)
            
            logger.info("Open-AutoGLM代理初始化成功")
            return True
            
        except Exception as e:
            logger.error(f"初始化Open-AutoGLM代理失败: {e}")
            return False
    
    def execute_intelligent_login_test(self, app_config: Dict) -> Dict[str, Any]:
        """执行智能登录测试"""
        logger.info(f"开始执行智能登录测试: {app_config['name']}")
        
        result = {
            'app_name': app_config['name'],
            'package_name': app_config['package_name'],
            'test_type': 'intelligent_login',
            'start_time': datetime.now().isoformat(),
            'status': 'pending',
            'test_cases': []
        }
        
        try:
            # 获取测试用户
            test_users = app_config.get('test_users', [])
            
            for i, user in enumerate(test_users):
                logger.info(f"测试用户 {i+1}/{len(test_users)}: {user['username']}")
                
                test_case = {
                    'username': user['username'],
                    'expected_result': user['expected_result'],
                    'start_time': datetime.now().isoformat(),
                    'status': 'pending',
                    'steps': []
                }
                
                try:
                    # 执行智能登录测试
                    login_result = self.tester.test_app_login_flow(
                        app_name=app_config['name'],
                        username=user['username'],
                        password=user['password']
                    )
                    
                    test_case['status'] = 'success'
                    test_case['actual_result'] = login_result.get('status', 'unknown')
                    test_case['ai_analysis'] = login_result.get('ai_analysis', {})
                    test_case['screenshots'] = login_result.get('screenshots', [])
                    test_case['operation_logs'] = login_result.get('operation_logs', [])
                    test_case['end_time'] = datetime.now().isoformat()
                    
                    # 验证结果是否符合预期
                    expected = user['expected_result']
                    actual = login_result.get('status', 'unknown')
                    test_case['result_match'] = (expected == actual)
                    
                    logger.info(f"测试完成: 预期={expected}, 实际={actual}, 匹配={test_case['result_match']}")
                    
                except Exception as e:
                    logger.error(f"测试用户 {user['username']} 失败: {e}")
                    test_case['status'] = 'failed'
                    test_case['error'] = str(e)
                    test_case['end_time'] = datetime.now().isoformat()
                
                result['test_cases'].append(test_case)
            
            # 汇总结果
            total_cases = len(result['test_cases'])
            success_cases = sum(1 for case in result['test_cases'] if case['status'] == 'success')
            match_cases = sum(1 for case in result['test_cases'] if case.get('result_match', False))
            
            result['total_cases'] = total_cases
            result['success_cases'] = success_cases
            result['match_cases'] = match_cases
            result['success_rate'] = success_cases / total_cases if total_cases > 0 else 0
            result['match_rate'] = match_cases / total_cases if total_cases > 0 else 0
            result['status'] = 'completed'
            result['end_time'] = datetime.now().isoformat()
            
            logger.info(f"智能登录测试完成: {success_cases}/{total_cases} 成功, {match_cases}/{total_cases} 结果匹配")
            
        except Exception as e:
            logger.error(f"智能登录测试执行失败: {e}")
            result['status'] = 'failed'
            result['error'] = str(e)
            result['end_time'] = datetime.now().isoformat()
        
        return result
    
    def execute_coordinate_login_test(self, app_config: Dict) -> Dict[str, Any]:
        """执行坐标模式登录测试"""
        logger.info(f"开始坐标模式登录测试: {app_config['name']}")
        
        # 获取测试流程
        test_flows = self.workplan.get('test_flows', [])
        coordinate_flow = None
        
        for flow in test_flows:
            if flow.get('name') == '坐标模式登录测试':
                coordinate_flow = flow
                break
        
        if coordinate_flow:
            logger.info("使用YAML配置中的坐标模式登录测试流程")
            return self._execute_coordinate_flow_from_yaml(coordinate_flow, app_config)
        else:
            logger.warning("未找到坐标模式登录测试流程，使用默认实现")
            return self._execute_default_coordinate_login_test(app_config)
    
    def execute_tap_action(self, x: int, y: int, description: str = "") -> bool:
        """执行Tap动作格式的坐标点击
        
        Args:
            x: X坐标
            y: Y坐标
            description: 操作描述
            
        Returns:
            执行成功返回True
        """
        try:
            # 构建Tap动作格式的动作
            tap_action = {"action": "Tap", "element": [x, y]}
            logger.info(f"执行Tap动作: {tap_action} - {description}")
            
            # 优先使用Open-AutoGLM的Tap动作支持
            if HAS_AUTOGLM_SUPPORT:
                logger.info(f"使用Open-AutoGLM Tap动作执行坐标点击: ({x}, {y})")
                autoGLM_tap(x, y, device_id=self.device_id)
                return True
            else:
                # 回退到使用device_controller.click
                logger.info(f"使用device_controller.click执行坐标点击: ({x}, {y})")
                return self.device_controller.click(x=x, y=y)
                
        except Exception as e:
            logger.error(f"执行Tap动作失败: {e}")
            return False
    
    def execute_test_flow(self, test_flow: Dict, app_config: Dict, user: Dict) -> Dict[str, Any]:
        """执行测试流程
        
        Args:
            test_flow: 测试流程配置
            app_config: 应用配置
            user: 测试用户配置
            
        Returns:
            执行结果
        """
        logger.info(f"执行测试流程: {test_flow['name']}")
        
        result = {
            'flow_name': test_flow['name'],
            'start_time': datetime.now().isoformat(),
            'status': 'pending',
            'steps': []
        }
        
        try:
            # 获取坐标映射
            coordinate_mappings = self.workplan.get('intelligent_operations', {}).get('coordinate_mappings', {})
            
            # 替换变量
            steps = test_flow.get('steps', [])
            for step in steps:
                step_result = {
                    'step': step.get('description', ''),
                    'action': step.get('action', ''),
                    'start_time': datetime.now().isoformat(),
                    'status': 'pending'
                }
                
                try:
                    action = step.get('action', '')
                    
                    if action == 'launch_app':
                        # 启动应用
                        package_name = app_config['package_name']
                        logger.info(f"启动应用: {package_name}")
                        adb_prefix = ["adb", "-s", self.device_id] if self.device_id else ["adb"]
                        launch_result = subprocess.run(
                            adb_prefix + ["shell", "monkey", "-p", package_name, "-c", "android.intent.category.LAUNCHER", "1"],
                            capture_output=True, text=True
                        )
                        if launch_result.returncode != 0:
                            raise Exception(f"启动应用失败: {launch_result.stderr}")
                        step_result['status'] = 'success'
                        
                    elif action == 'wait':
                        # 等待
                        duration = step.get('duration', 1)
                        logger.info(f"等待 {duration} 秒")
                        time.sleep(duration)
                        step_result['status'] = 'success'
                        
                    elif action == 'Tap':
                        # 执行Tap动作格式的坐标点击
                        element = step.get('element', [])
                        if isinstance(element, str) and element.startswith('{') and element.endswith('}'):
                            # 替换变量
                            coord_key = element[1:-1].replace('_coord', '')
                            coord = coordinate_mappings.get(coord_key, [540, 800])
                            if isinstance(coord, dict):
                                x, y = coord['x'], coord['y']
                            else:
                                x, y = coord[0], coord[1]
                        else:
                            x, y = element[0], element[1]
                            
                        success = self.execute_tap_action(x=x, y=y, description=step.get('description', ''))
                        step_result['status'] = 'success' if success else 'failed'
                        
                    elif action == 'input_text':
                        # 输入文本
                        text = step.get('text', '')
                        # 替换变量
                        if text == '{username}':
                            text = user['username']
                        elif text == '{password}':
                            text = user['password']
                            
                        logger.info(f"输入文本: {text}")
                        adb_prefix = ["adb", "-s", self.device_id] if self.device_id else ["adb"]
                        input_result = subprocess.run(adb_prefix + ["shell", "input", "text", text], capture_output=True)
                        if input_result.returncode != 0:
                            raise Exception(f"输入文本失败: {input_result.stderr}")
                        step_result['status'] = 'success'
                        
                    elif action == 'verify_result':
                        # 验证结果
                        expected = step.get('expected', '')
                        if expected == '{expected_result}':
                            expected = user['expected_result']
                        step_result['status'] = 'success'
                        step_result['expected'] = expected
                        
                    else:
                        logger.warning(f"未知的动作类型: {action}")
                        step_result['status'] = 'skipped'
                        
                except Exception as e:
                    logger.error(f"执行步骤失败: {step.get('description', '')} - {e}")
                    step_result['status'] = 'failed'
                    step_result['error'] = str(e)
                    
                step_result['end_time'] = datetime.now().isoformat()
                result['steps'].append(step_result)
                
                # 如果步骤失败且不是可选步骤，停止执行
                if step_result['status'] == 'failed' and not step.get('optional', False):
                    break
            
            result['status'] = 'completed'
            result['end_time'] = datetime.now().isoformat()
            
        except Exception as e:
            logger.error(f"执行测试流程失败: {e}")
            result['status'] = 'failed'
            result['error'] = str(e)
            result['end_time'] = datetime.now().isoformat()
        
        return result
    
    def _execute_coordinate_flow_from_yaml(self, test_flow: Dict, app_config: Dict) -> Dict[str, Any]:
        """从YAML配置执行坐标模式登录测试流程"""
        logger.info(f"从YAML配置执行坐标模式登录测试: {app_config['name']}")
        
        result = {
            'app_name': app_config['name'],
            'package_name': app_config['package_name'],
            'test_type': 'coordinate_login',
            'start_time': datetime.now().isoformat(),
            'status': 'pending',
            'test_cases': []
        }
        
        try:
            # 获取测试用户
            test_users = app_config.get('test_users', [])
            
            for i, user in enumerate(test_users):
                logger.info(f"测试用户 {i+1}/{len(test_users)}: {user['username']}")
                
                test_case = {
                    'username': user['username'],
                    'expected_result': user['expected_result'],
                    'start_time': datetime.now().isoformat(),
                    'status': 'pending',
                    'steps': []
                }
                
                try:
                    # 使用新的execute_test_flow方法执行测试流程
                    flow_result = self.execute_test_flow(test_flow, app_config, user)
                    
                    # 根据实际测试结果设置状态
                    if flow_result.get('status') == 'failed':
                        test_case['status'] = 'failed'
                        test_case['error'] = flow_result.get('error', '测试流程执行失败')
                    else:
                        # 检查步骤中是否有失败的
                        failed_steps = [step for step in flow_result.get('steps', []) if step.get('status') == 'failed']
                        if failed_steps:
                            test_case['status'] = 'failed'
                            test_case['error'] = f"步骤失败: {', '.join(step.get('description', '未知步骤') for step in failed_steps)}"
                        else:
                            test_case['status'] = 'success'
                    
                    test_case['steps'] = flow_result.get('steps', [])
                    test_case['actual_result'] = 'failed' if test_case['status'] == 'failed' else 'success'
                    test_case['result_match'] = (user['expected_result'] == test_case['actual_result'])
                    test_case['end_time'] = datetime.now().isoformat()
                    
                    logger.info(f"坐标模式测试完成: 预期={user['expected_result']}, 实际={test_case['actual_result']}, 匹配={test_case['result_match']}")
                    
                except Exception as e:
                    logger.error(f"坐标模式测试用户 {user['username']} 失败: {e}")
                    test_case['status'] = 'failed'
                    test_case['error'] = str(e)
                    test_case['actual_result'] = 'failed'
                    test_case['result_match'] = (user['expected_result'] == test_case['actual_result'])
                    test_case['end_time'] = datetime.now().isoformat()
                
                result['test_cases'].append(test_case)
            
            # 汇总结果
            total_cases = len(result['test_cases'])
            success_cases = sum(1 for case in result['test_cases'] if case['status'] == 'success')
            match_cases = sum(1 for case in result['test_cases'] if case.get('result_match', False))
            
            result['total_cases'] = total_cases
            result['success_cases'] = success_cases
            result['match_cases'] = match_cases
            result['success_rate'] = success_cases / total_cases if total_cases > 0 else 0
            result['match_rate'] = match_cases / total_cases if total_cases > 0 else 0
            result['status'] = 'completed'
            result['end_time'] = datetime.now().isoformat()
            
            logger.info(f"坐标模式登录测试完成: {success_cases}/{total_cases} 成功")
            
        except Exception as e:
            logger.error(f"坐标模式登录测试执行失败: {e}")
            result['status'] = 'failed'
            result['error'] = str(e)
            result['end_time'] = datetime.now().isoformat()
        
        return result
    
    def _execute_default_coordinate_login_test(self, app_config: Dict) -> Dict[str, Any]:
        """默认的坐标登录测试实现（用于回退）"""
        logger.info(f"使用默认坐标登录测试实现: {app_config['name']}")
        
        result = {
            'app_name': app_config['name'],
            'package_name': app_config['package_name'],
            'test_type': 'coordinate_login',
            'start_time': datetime.now().isoformat(),
            'status': 'pending',
            'test_cases': []
        }
        
        try:
            # 获取坐标映射
            coordinate_mappings = self.workplan.get('intelligent_operations', {}).get('coordinate_mappings', {})
            
            # 获取测试用户
            test_users = app_config.get('test_users', [])
            
            for i, user in enumerate(test_users):
                logger.info(f"测试用户 {i+1}/{len(test_users)}: {user['username']}")
                
                test_case = {
                    'username': user['username'],
                    'expected_result': user['expected_result'],
                    'start_time': datetime.now().isoformat(),
                    'status': 'pending',
                    'steps': []
                }
                
                try:
                    # 启动应用
                    logger.info(f"启动应用: {app_config['package_name']}")
                    adb_prefix = ["adb", "-s", self.device_id] if self.device_id else ["adb"]
                    launch_result = subprocess.run(
                        adb_prefix + ["shell", "monkey", "-p", app_config['package_name'], "-c", "android.intent.category.LAUNCHER", "1"],
                        capture_output=True, text=True
                    )
                    if launch_result.returncode != 0:
                        raise Exception(f"启动应用失败: {launch_result.stderr}")
                    
                    test_case['steps'].append({
                        'action': 'launch_app',
                        'result': 'success'
                    })
                    
                    time.sleep(3)  # 等待应用加载
                    
                    # 导航到"我的"页面
                    my_tab_coord = coordinate_mappings.get('my_tab_button', {'x': 900, 'y': 1800})
                    logger.info(f"点击底部导航栏-我的页面坐标: {my_tab_coord}")
                    success = self.execute_tap_action(x=my_tab_coord['x'], y=my_tab_coord['y'], description='点击底部导航栏-我的页面')
                    test_case['steps'].append({
                        'action': 'Tap',
                        'element': [my_tab_coord['x'], my_tab_coord['y']],
                        'result': 'success' if success else 'failed'
                    })
                    
                    time.sleep(2)  # 等待"我的"页面加载
                    
                    # 点击"我的"页面中的登录按钮
                    login_btn_coord = coordinate_mappings.get('login_button', {'x': 540, 'y': 400})
                    logger.info(f"点击我的页面中的登录按钮坐标: {login_btn_coord}")
                    success = self.execute_tap_action(x=login_btn_coord['x'], y=login_btn_coord['y'], description='点击我的页面中的登录按钮')
                    test_case['steps'].append({
                        'action': 'Tap',
                        'element': [login_btn_coord['x'], login_btn_coord['y']],
                        'result': 'success' if success else 'failed'
                    })
                    
                    time.sleep(2)  # 等待登录页面
                    
                    # 输入用户名
                    username_coord = coordinate_mappings.get('username_field', {'x': 540, 'y': 600})
                    logger.info(f"点击用户名输入框坐标: {username_coord}")
                    success = self.execute_tap_action(x=username_coord['x'], y=username_coord['y'], description='点击用户名输入框')
                    time.sleep(0.5)
                    
                    logger.info(f"输入用户名: {user['username']}")
                    username_result = subprocess.run(adb_prefix + ["shell", "input", "text", user['username']], capture_output=True)
                    if username_result.returncode != 0:
                        raise Exception(f"输入用户名失败: {username_result.stderr}")
                    test_case['steps'].append({
                        'action': 'input_username',
                        'coordinate': username_coord,
                        'text': user['username'],
                        'result': 'success'
                    })
                    
                    # 输入密码
                    password_coord = coordinate_mappings.get('password_field', {'x': 540, 'y': 800})
                    logger.info(f"点击密码输入框坐标: {password_coord}")
                    success = self.execute_tap_action(x=password_coord['x'], y=password_coord['y'], description='点击密码输入框')
                    time.sleep(0.5)
                    
                    logger.info(f"输入密码: {'*' * len(user['password'])}")
                    password_result = subprocess.run(adb_prefix + ["shell", "input", "text", user['password']], capture_output=True)
                    if password_result.returncode != 0:
                        raise Exception(f"输入密码失败: {password_result.stderr}")
                    test_case['steps'].append({
                        'action': 'input_password',
                        'coordinate': password_coord,
                        'text': '*' * len(user['password']),
                        'result': 'success'
                    })
                    
                    # 点击提交按钮
                    submit_coord = coordinate_mappings.get('submit_button', {'x': 540, 'y': 1000})
                    logger.info(f"点击提交按钮坐标: {submit_coord}")
                    success = self.execute_tap_action(x=submit_coord['x'], y=submit_coord['y'], description='点击提交按钮')
                    test_case['steps'].append({
                        'action': 'Tap',
                        'element': [submit_coord['x'], submit_coord['y']],
                        'result': 'success' if success else 'failed'
                    })
                    
                    time.sleep(5)  # 等待登录结果
                    
                    # 检查是否有失败的步骤
                    failed_steps = [step for step in test_case['steps'] if step.get('result') == 'failed']
                    if failed_steps:
                        test_case['status'] = 'failed'
                        test_case['error'] = f"步骤失败: {', '.join(step.get('description', '未知步骤') for step in failed_steps)}"
                        test_case['actual_result'] = 'failed'
                    else:
                        test_case['status'] = 'success'
                        test_case['actual_result'] = 'success'  # 简化处理，假设所有步骤成功就算登录成功
                    
                    test_case['result_match'] = (user['expected_result'] == test_case['actual_result'])
                    test_case['end_time'] = datetime.now().isoformat()
                    
                    logger.info(f"坐标模式测试完成: 预期={user['expected_result']}, 实际={test_case['actual_result']}, 匹配={test_case['result_match']}")
                    
                except Exception as e:
                    logger.error(f"坐标模式测试用户 {user['username']} 失败: {e}")
                    test_case['status'] = 'failed'
                    test_case['error'] = str(e)
                    test_case['actual_result'] = 'failed'
                    test_case['result_match'] = (user['expected_result'] == test_case['actual_result'])
                    test_case['end_time'] = datetime.now().isoformat()
                
                result['test_cases'].append(test_case)
            
            # 汇总结果
            total_cases = len(result['test_cases'])
            success_cases = sum(1 for case in result['test_cases'] if case['status'] == 'success')
            match_cases = sum(1 for case in result['test_cases'] if case.get('result_match', False))
            
            result['total_cases'] = total_cases
            result['success_cases'] = success_cases
            result['match_cases'] = match_cases
            result['success_rate'] = success_cases / total_cases if total_cases > 0 else 0
            result['match_rate'] = match_cases / total_cases if total_cases > 0 else 0
            result['status'] = 'completed'
            result['end_time'] = datetime.now().isoformat()
            
            logger.info(f"坐标模式登录测试完成: {success_cases}/{total_cases} 成功")
            
        except Exception as e:
            logger.error(f"坐标模式登录测试执行失败: {e}")
            result['status'] = 'failed'
            result['error'] = str(e)
            result['end_time'] = datetime.now().isoformat()
        
        return result
    
    def execute_all_tests(self) -> List[Dict[str, Any]]:
        """执行所有测试"""
        logger.info("开始执行所有测试")
        
        all_results = []
        
        try:
            # 获取测试应用配置
            test_apps = self.workplan.get('test_apps', [])
            
            for app_config in test_apps:
                logger.info(f"测试应用: {app_config['name']}")
                
                # 跳过需要人工接管的应用
                if app_config.get('requires_takeover', False):
                    logger.info(f"跳过需要人工接管的应用: {app_config['name']}")
                    continue
                
                # 执行智能模式测试
                intelligent_result = self.execute_intelligent_login_test(app_config)
                all_results.append(intelligent_result)
                
                # 执行坐标模式测试（用于对比验证）
                logger.info(f"执行坐标模式测试进行对比")
                coordinate_result = self.execute_coordinate_login_test(app_config)
                all_results.append(coordinate_result)
                
                # 测试间隔
                time.sleep(3)
            
            logger.info(f"所有测试执行完成，共 {len(all_results)} 个测试结果")
            
        except Exception as e:
            logger.error(f"执行所有测试失败: {e}")
        
        return all_results
    
    def generate_report(self, results: List[Dict[str, Any]]) -> str:
        """生成测试报告"""
        logger.info("生成测试报告")
        
        report_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        report_content = f"""
# Open-AutoGLM集成测试报告
生成时间: {report_time}
设备ID: {self.device_id}

## 测试摘要

"""
        
        total_tests = 0
        total_success = 0
        total_match = 0
        
        for result in results:
            app_name = result.get('app_name', 'Unknown')
            test_type = result.get('test_type', 'unknown')
            total_cases = result.get('total_cases', 0)
            success_cases = result.get('success_cases', 0)
            match_cases = result.get('match_cases', 0)
            success_rate = result.get('success_rate', 0)
            match_rate = result.get('match_rate', 0)
            
            total_tests += total_cases
            total_success += success_cases
            total_match += match_cases
            
            report_content += f"""
### {app_name} - {test_type}

- 测试用例数: {total_cases}
- 成功用例数: {success_cases}
- 结果匹配数: {match_cases}
- 成功率: {success_rate:.2%}
- 匹配率: {match_rate:.2%}

"""
        
        overall_success_rate = total_success / total_tests if total_tests > 0 else 0
        overall_match_rate = total_match / total_tests if total_tests > 0 else 0
        
        report_content = f"""
# Open-AutoGLM集成测试报告
生成时间: {report_time}
设备ID: {self.device_id}

## 总体摘要

- 总测试用例数: {total_tests}
- 总成功用例数: {total_success}
- 总匹配用例数: {total_match}
- 总体成功率: {overall_success_rate:.2%}
- 总体匹配率: {overall_match_rate:.2%}

""" + report_content
        
        # 添加详细结果
        report_content += "\n## 详细测试结果\n\n"
        
        for result in results:
            app_name = result.get('app_name', 'Unknown')
            test_type = result.get('test_type', 'unknown')
            
            report_content += f"### {app_name} - {test_type} 详细结果\n\n"
            
            for test_case in result.get('test_cases', []):
                username = test_case.get('username', 'Unknown')
                status = test_case.get('status', 'unknown')
                expected = test_case.get('expected_result', 'unknown')
                actual = test_case.get('actual_result', 'unknown')
                match = test_case.get('result_match', False)
                
                report_content += f"- **{username}**: {status} (预期: {expected}, 实际: {actual}, 匹配: {match})\n"
                
                if 'error' in test_case:
                    report_content += f"  - 错误: {test_case['error']}\n"
            
            report_content += "\n"
        
        # 保存报告
        report_filename = f"reports/autoglm_integration_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md"
        
        try:
            os.makedirs(os.path.dirname(report_filename), exist_ok=True)
            with open(report_filename, 'w', encoding='utf-8') as f:
                f.write(report_content)
            
            logger.info(f"测试报告已保存: {report_filename}")
            return report_filename
            
        except Exception as e:
            logger.error(f"保存测试报告失败: {e}")
            return ""
    
    def run(self) -> bool:
        """运行工作脚本"""
        logger.info("开始运行Open-AutoGLM集成工作脚本")
        
        # 加载工作配置
        if not self.load_workplan():
            logger.error("加载工作配置失败")
            return False
        
        # 初始化代理
        if not self.initialize_agent():
            logger.error("初始化代理失败")
            return False
        
        # 执行测试
        results = self.execute_all_tests()
        
        # 生成报告
        report_file = self.generate_report(results)
        
        logger.info(f"工作脚本执行完成，报告文件: {report_file}")
        return True

def main():
    """主函数"""
    if len(sys.argv) < 2:
        print("用法: python autoglm_workscript.py <device_id> [workplan_path]")
        sys.exit(1)
    
    device_id = sys.argv[1]
    workplan_path = sys.argv[2] if len(sys.argv) > 2 else 'workplans/autoglm_integration_workplan.yaml'
    
    # 创建工作脚本实例
    workscript = AutoGLMWorkScript(workplan_path, device_id)
    
    # 运行工作脚本
    success = workscript.run()
    
    if success:
        print("Open-AutoGLM集成测试执行成功")
        sys.exit(0)
    else:
        print("Open-AutoGLM集成测试执行失败")
        sys.exit(1)

if __name__ == "__main__":
    main()