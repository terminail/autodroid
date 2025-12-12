#!/usr/bin/env python3
"""
Autodroid Manager 登录测试 - Open-AutoGLM集成版本
使用Open-AutoGLM的智能操作来测试第三方APK
"""

import sys
import os
import time
import json
from datetime import datetime

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), 'Open-AutoGLM'))

# 导入workscript基础类
from core.workscript.base import BaseWorkScript

# 导入Open-AutoGLM组件
from phone_agent.agent import PhoneAgent, AgentConfig
from phone_agent.actions.handler import ActionHandler
from phone_agent.model.client import ModelClient, ModelConfig


class AutoglmLoginTest(BaseWorkScript):
    """使用Open-AutoGLM智能操作的登录测试类"""
    
    def __init__(self, device_udid=None, workplan_id="autoglm_login_test"):
        super().__init__(device_udid=device_udid, workplan_id=workplan_id)
        
        # 配置Open-AutoGLM模型
        self.model_config = ModelConfig(
            base_url="http://localhost:8000/v1",
            api_key="EMPTY",
            model_name="autoglm-phone-9b", 
            max_tokens=3000,
            temperature=0.1,
        )
        
        # 配置Agent
        self.agent_config = AgentConfig(
            max_steps=50,
            device_id=device_udid,
            verbose=True
        )
        
        # 初始化Agent
        self.phone_agent = PhoneAgent(
            model_config=self.model_config,
            agent_config=self.agent_config
        )
        
        self.test_results = []
        
    def setup_test_environment(self):
        """设置测试环境"""
        self.log_info("设置Open-AutoGLM测试环境")
        
        # 确保ADB设备连接
        if not self.device:
            self.log_warning("未找到真实设备，将使用模拟模式")
            return False
            
        self.log_info(f"使用设备: {self.device_udid}")
        return True
        
    def test_login_with_autoglm(self, username, password, app_name="AutoDroid Manager"):
        """使用Open-AutoGLM执行登录测试"""
        self.log_info(f"开始Open-AutoGLM登录测试 - 用户: {username}")
        
        start_time = time.time()
        
        try:
            # 任务描述
            task_description = f"""
            请帮我测试 {app_name} 应用的登录功能。
            测试步骤：
            1. 启动 {app_name} 应用
            2. 找到登录页面
            3. 输入用户名: {username}
            4. 输入密码: {password} 
            5. 点击登录按钮
            6. 验证登录是否成功
            
            如果登录失败，请分析失败原因。
            """
            
            # 使用Open-AutoGLM执行任务
            self.log_info("启动Open-AutoGLM智能操作...")
            result = self.phone_agent.execute_task(task_description)
            
            end_time = time.time()
            duration = end_time - start_time
            
            # 分析结果
            if result and result.get("success"):
                self.log_success(f"Open-AutoGLM登录测试成功 - 耗时: {duration:.2f}秒")
                test_result = {
                    "test_name": "autoglm_login",
                    "status": "passed",
                    "username": username,
                    "duration": duration,
                    "ai_analysis": result.get("analysis", ""),
                    "steps_executed": result.get("steps", [])
                }
            else:
                self.log_error(f"Open-AutoGLM登录测试失败 - 耗时: {duration:.2f}秒")
                test_result = {
                    "test_name": "autoglm_login", 
                    "status": "failed",
                    "username": username,
                    "duration": duration,
                    "error": result.get("error", "未知错误"),
                    "ai_analysis": result.get("analysis", ""),
                    "steps_executed": result.get("steps", [])
                }
                
            self.test_results.append(test_result)
            return test_result["status"] == "passed"
            
        except Exception as e:
            end_time = time.time()
            duration = end_time - start_time
            
            self.log_error(f"Open-AutoGLM执行异常: {str(e)}")
            test_result = {
                "test_name": "autoglm_login",
                "status": "error", 
                "username": username,
                "duration": duration,
                "error": str(e)
            }
            self.test_results.append(test_result)
            return False
            
    def test_multiple_scenarios(self):
        """测试多种登录场景"""
        scenarios = [
            {
                "name": "有效凭据登录",
                "username": "15317227@qq.com",
                "password": "Test@123456",
                "expected": "success"
            },
            {
                "name": "无效密码登录", 
                "username": "15317227@qq.com",
                "password": "wrong_password",
                "expected": "failure"
            },
            {
                "name": "空用户名登录",
                "username": "",
                "password": "Test@123456", 
                "expected": "failure"
            }
        ]
        
        for scenario in scenarios:
            self.log_info(f"\n测试场景: {scenario['name']}")
            success = self.test_login_with_autoglm(
                scenario["username"],
                scenario["password"]
            )
            
            # 验证预期结果
            expected_success = scenario["expected"] == "success"
            if success == expected_success:
                self.log_success(f"场景测试结果符合预期")
            else:
                self.log_error(f"场景测试结果不符合预期 - 期望: {scenario['expected']}, 实际: {'success' if success else 'failure'}")
                
            time.sleep(2)  # 场景间隔
            
    def generate_report(self):
        """生成测试报告"""
        report = {
            "test_id": self.workplan_id,
            "test_type": "Open-AutoGLM集成测试",
            "device_udid": self.device_udid or "模拟模式",
            "timestamp": datetime.now().isoformat(),
            "total_tests": len(self.test_results),
            "passed_tests": sum(1 for r in self.test_results if r["status"] == "passed"),
            "failed_tests": sum(1 for r in self.test_results if r["status"] == "failed"),
            "error_tests": sum(1 for r in self.test_results if r["status"] == "error"),
            "results": self.test_results,
            "summary": {
                "ai_model": "autoglm-phone-9b",
                "integration_type": "智能操作识别",
                "test_framework": "Open-AutoGLM + WorkScript"
            }
        }
        
        # 保存报告
        report_path = self.save_report(report, "autoglm_integration_test")
        self.log_info(f"测试报告已保存: {report_path}")
        
        return report


def main():
    """主函数"""
    print("🤖 Autodroid Manager - Open-AutoGLM集成测试")
    print("=" * 50)
    
    # 获取设备UDID
    device_udid = None
    if len(sys.argv) > 1:
        device_udid = sys.argv[1]
        print(f"使用设备: {device_udid}")
    else:
        print("未指定设备UDID，将使用模拟模式")
    
    # 创建测试实例
    tester = AutoglmLoginTest(device_udid=device_udid)
    
    # 设置测试环境
    if not tester.setup_test_environment():
        print("❌ 测试环境设置失败")
        return 1
    
    # 执行测试
    try:
        print("\n📱 开始Open-AutoGLM智能登录测试...")
        tester.test_multiple_scenarios()
        
        # 生成报告
        report = tester.generate_report()
        
        # 打印摘要
        print(f"\n📊 测试完成:")
        print(f"总测试数: {report['total_tests']}")
        print(f"通过: {report['passed_tests']}")
        print(f"失败: {report['failed_tests']}")
        print(f"错误: {report['error_tests']}")
        
        if report['error_tests'] > 0:
            print("\n⚠️  部分测试出现错误，请查看详细报告")
            return 1
        elif report['failed_tests'] > 0:
            print("\n⚠️  部分测试失败，请查看详细报告")
            return 1
        else:
            print("\n✅ 所有测试通过！")
            return 0
            
    except KeyboardInterrupt:
        print("\n⚠️  测试被用户中断")
        return 1
    except Exception as e:
        print(f"\n❌ 测试执行异常: {str(e)}")
        return 1


if __name__ == "__main__":
    sys.exit(main())