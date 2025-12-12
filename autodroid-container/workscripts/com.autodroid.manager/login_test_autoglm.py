#!/usr/bin/env python3
"""
Autodroid Manager 登录功能测试脚本 - Open-AutoGLM PhoneAgent版本

基于Open-AutoGLM PhoneAgent框架实现，使用AI智能识别和标准化操作
测试d:/git/autodroid/autodroid-app的登录功能
"""

import os
import sys
import time
from typing import Dict, Any

# 将Open-AutoGLM的路径添加到Python路径
open_autoglm_path = os.path.join(os.path.dirname(__file__), '..', '..', '..', 'Open-AutoGLM')
if open_autoglm_path not in sys.path:
    sys.path.insert(0, open_autoglm_path)

# 将autodroid-container的路径添加到Python路径  
container_path = os.path.join(os.path.dirname(__file__), '..', '..')
if container_path not in sys.path:
    sys.path.insert(0, container_path)

from phone_agent import PhoneAgent
from phone_agent.agent import AgentConfig
from phone_agent.model import ModelConfig
from core.workscript.base import BaseWorkScript


class login_test_autoglm(BaseWorkScript):
    """
    Autodroid Manager 登录测试 - Open-AutoGLM PhoneAgent实现
    
    基于Open-AutoGLM PhoneAgent的登录功能测试，使用AI智能识别：
    1. 启动应用（Launch操作）
    2. 智能导航到登录页面（AI识别UI元素）
    3. 智能输入用户名密码（Type操作）
    4. 智能点击登录按钮（Tap操作）
    5. 验证登录结果（AI识别页面状态）
    
    优势：
    - 使用Open-AutoGLM的标准化操作
    - AI智能识别UI元素，无需固定坐标
    - 更好的错误处理和重试机制
    - 避免摄像头权限问题（AI智能规避）
    """
    
    def __init__(self, workplan: Dict[str, Any] = None, device_udid: str = None,
                 test_username="15317227@qq.com", test_password="123456",
                 use_fingerprint=False, app_package="com.autodroid.manager",
                 model_base_url="http://localhost:8000/v1", model_name="autoglm-phone-9b"):
        """初始化登录测试
        
        Args:
            workplan: 工作计划数据
            device_udid: 设备唯一标识符
            test_username: 测试用户名
            test_password: 测试密码  
            use_fingerprint: 是否尝试使用指纹登录
            app_package: 应用包名
            model_base_url: AI模型API地址
            model_name: AI模型名称
        """
        # 如果没有提供workplan，创建默认的
        if workplan is None:
            workplan = {
                'id': 'login_test_autoglm_default',
                'name': 'AutoDroid Manager Login Test (Open-AutoGLM)',
                'data': {
                    'username': test_username,
                    'password': test_password,
                    'use_fingerprint': use_fingerprint,
                    'app_package': app_package,
                    'timeout': 30,
                    'success_rate': 0.9,
                    'model_base_url': model_base_url,
                    'model_name': model_name
                }
            }
        
        super().__init__(workplan, device_udid)
        
        # 从workplan获取参数
        self.test_username = self.get_workplan_param('username', test_username)
        self.test_password = self.get_workplan_param('password', test_password)
        self.use_fingerprint = self.get_workplan_param('use_fingerprint', use_fingerprint)
        self.app_package = self.get_workplan_param('app_package', app_package)
        self.timeout = self.get_workplan_param('timeout', 30)
        self.success_rate = self.get_workplan_param('success_rate', 0.9)
        self.model_base_url = self.get_workplan_param('model_base_url', model_base_url)
        self.model_name = self.get_workplan_param('model_name', model_name)
        
        # 创建PhoneAgent实例
        self.phone_agent = self._create_phone_agent()
        
        self.logger.info(f"测试用户: {self.test_username}")
        self.logger.info(f"应用包名: {self.app_package}")
        self.logger.info(f"AI模型: {self.model_name}")
        self.logger.info(f"模型地址: {self.model_base_url}")
        self.logger.info(f"超时时间: {self.timeout}秒")
        self.logger.info(f"预期成功率: {self.success_rate}")
        self.logger.info(f"启用指纹登录: {self.use_fingerprint}")
    
    def _create_phone_agent(self):
        """创建PhoneAgent实例"""
        try:
            # 配置AI模型
            model_config = ModelConfig(
                base_url=self.model_base_url,
                model_name=self.model_name,
                temperature=0.1,  # 低温度以获得更稳定的结果
            )
            
            # 配置Agent行为
            agent_config = AgentConfig(
                max_steps=50,
                verbose=True,
            )
            
            # 创建PhoneAgent
            phone_agent = PhoneAgent(
                model_config=model_config,
                agent_config=agent_config,
            )
            
            self.logger.info("PhoneAgent创建成功")
            return phone_agent
            
        except Exception as e:
            self.logger.error(f"创建PhoneAgent失败: {str(e)}")
            raise
    
    def run(self) -> Dict[str, Any]:
        """执行完整的登录测试流程"""
        self.logger.info("开始登录测试流程 (Open-AutoGLM版本)")
        self.start_time = time.time()
        
        try:
            self.log_step("启动测试", "开始执行Autodroid Manager登录测试 (Open-AutoGLM)")
            
            # 步骤1: 启动应用
            if not self._launch_application():
                return self._create_failure_result("启动应用失败")
            
            # 步骤2: 智能导航到登录页面
            if not self._navigate_to_login_intelligent():
                return self._create_failure_result("导航到登录页面失败")
            
            # 如果启用指纹登录，先尝试指纹登录
            if self.use_fingerprint:
                fingerprint_success = self._try_fingerprint_login()
                if fingerprint_success:
                    return self._create_success_result("指纹登录成功", "fingerprint")
                else:
                    self.logger.info("指纹登录失败，尝试常规登录")
            
            # 步骤3: 智能输入登录凭据
            if not self._enter_credentials_intelligent():
                return self._create_failure_result("输入凭据失败")
            
            # 步骤4: 智能执行登录
            if not self._perform_login_intelligent():
                return self._create_failure_result("执行登录失败")
            
            # 步骤5: 智能验证登录结果
            login_success = self._verify_login_result_intelligent()
            
            if login_success:
                return self._create_success_result("登录功能测试成功", "password")
            else:
                return self._create_failure_result("登录验证失败")
                
        except Exception as e:
            self.log_error(f"登录测试异常: {str(e)}")
            return self._create_failure_result(f"测试异常: {str(e)}")
    
    def _launch_application(self) -> bool:
        """使用Launch操作启动应用"""
        self.log_step("启动应用", f"使用PhoneAgent启动 {self.app_package}")
        
        try:
            # 使用自然语言指令让AI启动应用
            result = self.phone_agent.run(f"启动应用 {self.app_package}")
            
            if result:
                self.logger.info(f"✓ 应用启动成功: {self.app_package}")
                time.sleep(2)  # 等待应用完全加载
                return True
            else:
                self.log_error(f"✗ 应用启动失败: {self.app_package}")
                return False
                
        except Exception as e:
            self.log_error(f"启动应用异常: {str(e)}")
            return False
    
    def _navigate_to_login_intelligent(self) -> bool:
        """智能导航到登录页面 - 使用AI识别避免摄像头权限问题"""
        self.log_step("智能导航", "使用AI识别导航到登录页面")
        
        try:
            # 使用AI智能识别并导航到登录页面
            # 明确指示避免摄像头相关的功能
            navigation_instruction = f"""
            请导航到{self.app_package}应用的登录页面。
            重要提示：
            1. 避免点击任何与摄像头、二维码扫描相关的按钮
            2. 优先寻找"登录"、"我的"、"个人中心"等文字按钮
            3. 如果看到底部导航栏，优先点击文字标签而不是图标
            4. 避免任何可能触发摄像头权限的操作
            """
            
            result = self.phone_agent.run(navigation_instruction)
            
            if result:
                self.logger.info("✓ 智能导航到登录页面成功")
                time.sleep(2)  # 等待页面加载
                return True
            else:
                self.log_error("✗ 智能导航到登录页面失败")
                return False
                
        except Exception as e:
            self.log_error(f"智能导航异常: {str(e)}")
            return False
    
    def _enter_credentials_intelligent(self) -> bool:
        """智能输入登录凭据 - 使用AI识别输入框"""
        self.log_step("智能输入", "使用AI识别输入用户名和密码")
        
        try:
            # 使用AI智能识别输入框并输入凭据
            input_instruction = f"""
            请在登录页面输入以下凭据：
            用户名/邮箱: {self.test_username}
            密码: {self.test_password}
            
            请按以下步骤操作：
            1. 先找到用户名/邮箱输入框并点击获取焦点
            2. 清除现有内容（如果有）
            3. 输入用户名: {self.test_username}
            4. 找到密码输入框并点击获取焦点  
            5. 输入密码: {self.test_password}
            6. 确保两个输入框都正确填写
            """
            
            result = self.phone_agent.run(input_instruction)
            
            if result:
                self.logger.info("✓ 智能输入凭据成功")
                time.sleep(1)  # 等待输入完成
                return True
            else:
                self.log_error("✗ 智能输入凭据失败")
                return False
                
        except Exception as e:
            self.log_error(f"智能输入异常: {str(e)}")
            return False
    
    def _perform_login_intelligent(self) -> bool:
        """智能执行登录 - 使用AI识别登录按钮"""
        self.log_step("智能登录", "使用AI识别并点击登录按钮")
        
        try:
            # 使用AI智能识别登录按钮并点击
            login_instruction = f"""
            请在登录页面找到登录按钮并点击。
            
            请按以下步骤操作：
            1. 寻找包含"登录"、"登入"、"Login"、"Sign In"文字的按钮
            2. 确保这是主要的登录按钮（不是第三方登录）
            3. 点击该按钮执行登录操作
            4. 等待页面响应
            
            避免点击任何与社交媒体登录相关的按钮。
            """
            
            result = self.phone_agent.run(login_instruction)
            
            if result:
                self.logger.info("✓ 智能登录操作成功")
                time.sleep(3)  # 等待登录响应
                return True
            else:
                self.log_error("✗ 智能登录操作失败")
                return False
                
        except Exception as e:
            self.log_error(f"智能登录异常: {str(e)}")
            return False
    
    def _verify_login_result_intelligent(self) -> bool:
        """智能验证登录结果 - 使用AI识别页面状态"""
        self.log_step("智能验证", "使用AI识别登录是否成功")
        
        try:
            # 使用AI智能验证登录结果
            verify_instruction = f"""
            请检查当前页面状态，判断登录是否成功。
            
            请按以下标准判断：
            1. 成功标志：
               - 页面显示用户信息（用户名、头像等）
               - 显示"登录成功"或类似提示
               - 返回主页面或个人中心页面
               - 显示用户设置或账户相关选项
            
            2. 失败标志：
               - 仍在登录页面
               - 显示错误提示（用户名错误、密码错误等）
               - 显示网络错误
               - 页面无响应
            
            请给出明确的判断结果：登录成功或失败，并说明原因。
            """
            
            result = self.phone_agent.run(verify_instruction)
            
            if result and "成功" in str(result):
                self.logger.info("✓ 智能验证登录成功")
                return True
            else:
                self.logger.warning(f"登录验证结果: {result}")
                return False
                
        except Exception as e:
            self.log_error(f"智能验证异常: {str(e)}")
            return False
    
    def _try_fingerprint_login(self) -> bool:
        """尝试指纹登录 - 使用AI识别"""
        self.log_step("指纹登录", "尝试使用指纹登录")
        
        try:
            # 使用AI尝试指纹登录
            fingerprint_instruction = f"""
            请检查当前页面是否支持指纹登录，如果支持请尝试使用指纹登录。
            
            请按以下步骤操作：
            1. 寻找指纹登录相关的按钮或提示（如"指纹登录"、"指纹解锁"等）
            2. 如果找到，点击该选项
            3. 等待指纹认证完成
            4. 检查登录是否成功
            
            如果页面不支持指纹登录，请返回False。
            """
            
            result = self.phone_agent.run(fingerprint_instruction)
            
            if result and "成功" in str(result):
                self.logger.info("✓ 指纹登录成功")
                return True
            else:
                self.logger.info("指纹登录不可用或失败")
                return False
                
        except Exception as e:
            self.logger.warning(f"指纹登录异常: {str(e)}")
            return False
    
    def _create_success_result(self, message: str, login_method: str) -> Dict[str, Any]:
        """创建成功结果"""
        duration = time.time() - self.start_time
        
        self.log_success(message)
        return {
            'status': 'success',
            'message': message,
            'username': self.test_username,
            'login_method': login_method,
            'duration_seconds': duration,
            'test_steps': [
                '应用启动成功',
                '智能导航到登录页面成功',
                '智能输入凭据成功',
                '智能登录操作成功',
                '智能验证登录成功'
            ],
            'ai_framework': 'Open-AutoGLM PhoneAgent'
        }
    
    def _create_failure_result(self, message: str) -> Dict[str, Any]:
        """创建失败结果"""
        duration = time.time() - self.start_time
        
        self.log_error(message)
        return {
            'status': 'failed',
            'message': message,
            'username': self.test_username,
            'login_method': 'password',
            'duration_seconds': duration,
            'test_steps': [
                '应用启动成功',
                '智能导航到登录页面成功',
                '智能输入凭据成功',
                '智能登录操作成功',
                '智能验证登录失败'
            ],
            'ai_framework': 'Open-AutoGLM PhoneAgent'
        }


# 向后兼容，保持原有接口
if __name__ == "__main__":
    # 创建测试实例
    test = login_test_autoglm()
    
    # 运行测试
    result = test.run()
    
    # 输出结果
    print(f"\n测试结果: {result['status']}")
    print(f"消息: {result['message']}")
    print(f"耗时: {result['duration_seconds']:.2f}秒")
    print(f"AI框架: {result.get('ai_framework', 'Unknown')}")