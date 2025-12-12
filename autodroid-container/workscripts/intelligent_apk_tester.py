#!/usr/bin/env python3
"""
Open-AutoGLM与WorkScript集成适配器
将Open-AutoGLM的操作集成到我们的设备连接框架中
"""

import sys
import os
import time
import subprocess
from typing import Optional, Dict, Any

# 添加路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'Open-AutoGLM'))

# 导入Open-AutoGLM的ADB操作
from phone_agent.adb.device import (
    tap, swipe, back, home, launch_app, 
    get_current_app
)
from phone_agent.adb.input import detect_and_set_adb_keyboard, restore_keyboard

# 导入我们的设备连接
from device_connection import ADBDeviceController, DeviceConnectionPool


class OpenAutoGLMAdapter:
    """Open-AutoGLM操作适配器"""
    
    def __init__(self, device_udid: Optional[str] = None):
        self.device_udid = device_udid
        self.adb_device = None
        self.original_ime = None
        
    def connect(self) -> bool:
        """连接到设备"""
        if not self.device_udid:
            return False
            
        # 使用我们的设备连接池
        pool = DeviceConnectionPool()
        self.adb_device = pool.get_device(self.device_udid)
        
        if not self.adb_device:
            return False
            
        return self.adb_device.connect()
        
    def launch_app(self, app_name: str) -> bool:
        """启动应用"""
        try:
            success = launch_app(app_name, self.device_udid)
            time.sleep(2)  # 等待应用启动
            return success
        except Exception as e:
            print(f"启动应用失败: {e}")
            return False
            
    def tap(self, x: int, y: int) -> bool:
        """点击坐标"""
        try:
            tap(x, y, self.device_udid)
            return True
        except Exception as e:
            print(f"点击失败: {e}")
            return False
            
    def input_text(self, text: str) -> bool:
        """输入文本"""
        try:
            # 切换到ADB键盘
            self.original_ime = detect_and_set_adb_keyboard(self.device_udid)
            time.sleep(0.5)
            
            # 输入文本
            autoglm_type_text(text, self.device_udid)
            time.sleep(0.5)
            
            # 恢复原始键盘
            if self.original_ime:
                restore_keyboard(self.original_ime, self.device_udid)
                
            return True
        except Exception as e:
            print(f"输入文本失败: {e}")
            return False
            
    def back(self) -> bool:
        """返回"""
        try:
            back(self.device_udid)
            return True
        except Exception as e:
            print(f"返回失败: {e}")
            return False
            
    def get_current_app(self) -> str:
        """获取当前应用"""
        try:
            return get_current_app(self.device_udid)
        except Exception as e:
            print(f"获取当前应用失败: {e}")
            return "Unknown"


class IntelligentAPKTester:
    """智能APK测试器 - 使用Open-AutoGLM操作"""
    
    def __init__(self, device_udid: Optional[str] = None):
        self.device_udid = device_udid
        self.adapter = OpenAutoGLMAdapter(device_udid)
        self.test_results = []
        
    def setup(self) -> bool:
        """设置测试环境"""
        print("设置智能APK测试环境...")
        
        if not self.device_udid:
            print("❌ 未提供设备UDID")
            return False
            
        if not self.adapter.connect():
            print(f"❌ 无法连接到设备: {self.device_udid}")
            return False
            
        print(f"✓ 成功连接到设备: {self.device_udid}")
        return True
        
    def test_app_login_flow(self, app_name: str, username: str, password: str) -> Dict[str, Any]:
        """测试应用登录流程"""
        print(f"\n🧪 测试 {app_name} 登录流程...")
        
        start_time = time.time()
        steps = []
        
        try:
            # 步骤1: 启动应用
            print("步骤1: 启动应用")
            if not self.adapter.launch_app(app_name):
                return self._create_result("failed", "应用启动失败", steps, start_time)
            steps.append("✓ 应用启动成功")
            time.sleep(3)
            
            # 步骤2: 查找登录按钮并点击
            print("步骤2: 查找登录入口")
            # 这里可以使用坐标映射或AI识别
            # 暂时使用通用坐标
            login_button_coords = self._find_login_button()
            if not login_button_coords:
                return self._create_result("failed", "未找到登录按钮", steps, start_time)
                
            if not self.adapter.tap(login_button_coords[0], login_button_coords[1]):
                return self._create_result("failed", "点击登录按钮失败", steps, start_time)
            steps.append("✓ 点击登录按钮")
            time.sleep(2)
            
            # 步骤3: 输入用户名
            print("步骤3: 输入用户名")
            username_field_coords = self._find_username_field()
            if not username_field_coords:
                return self._create_result("failed", "未找到用户名输入框", steps, start_time)
                
            if not self.adapter.tap(username_field_coords[0], username_field_coords[1]):
                return self._create_result("failed", "点击用户名输入框失败", steps, start_time)
            time.sleep(1)
            
            if not self.adapter.input_text(username):
                return self._create_result("failed", "输入用户名失败", steps, start_time)
            steps.append(f"✓ 输入用户名: {username}")
            time.sleep(1)
            
            # 步骤4: 输入密码
            print("步骤4: 输入密码")
            password_field_coords = self._find_password_field()
            if not password_field_coords:
                return self._create_result("failed", "未找到密码输入框", steps, start_time)
                
            if not self.adapter.tap(password_field_coords[0], password_field_coords[1]):
                return self._create_result("failed", "点击密码输入框失败", steps, start_time)
            time.sleep(1)
            
            if not self.adapter.input_text(password):
                return self._create_result("failed", "输入密码失败", steps, start_time)
            steps.append("✓ 输入密码")
            time.sleep(1)
            
            # 步骤5: 点击登录按钮
            print("步骤5: 点击登录按钮")
            submit_button_coords = self._find_submit_button()
            if not submit_button_coords:
                return self._create_result("failed", "未找到提交按钮", steps, start_time)
                
            if not self.adapter.tap(submit_button_coords[0], submit_button_coords[1]):
                return self._create_result("failed", "点击提交按钮失败", steps, start_time)
            steps.append("✓ 点击登录按钮")
            time.sleep(3)
            
            # 步骤6: 验证登录结果
            print("步骤6: 验证登录结果")
            current_app = self.adapter.get_current_app()
            if self._is_login_successful(current_app):
                steps.append("✓ 登录验证成功")
                return self._create_result("passed", "登录测试成功", steps, start_time)
            else:
                steps.append("✗ 登录验证失败")
                return self._create_result("failed", "登录验证失败", steps, start_time)
                
        except Exception as e:
            return self._create_result("error", f"测试异常: {str(e)}", steps, start_time)
            
    def _find_login_button(self):
        """查找登录按钮坐标 - 可以根据实际应用调整"""
        # 通用登录按钮位置 (屏幕中央偏下)
        return (540, 1200)
        
    def _find_username_field(self):
        """查找用户名输入框坐标"""
        # 通用用户名输入框位置 (屏幕中上部)
        return (540, 600)
        
    def _find_password_field(self):
        """查找密码输入框坐标"""
        # 通用密码输入框位置 (屏幕中部)
        return (540, 800)
        
    def _find_submit_button(self):
        """查找提交按钮坐标"""
        # 通用提交按钮位置 (屏幕中下部)
        return (540, 1000)
        
    def _is_login_successful(self, current_app: str) -> bool:
        """判断登录是否成功"""
        # 检查当前应用状态，这里简化处理
        # 实际应该检查UI变化、错误提示等
        try:
            # 获取设备截图进行AI分析
            screenshot_path = f"/sdcard/login_test_{int(time.time())}.png"
            subprocess.run(["adb", "-s", self.device_udid, "shell", "screencap", "-p", screenshot_path], check=True)
            
            # 这里可以添加AI分析逻辑来判断登录状态
            # 暂时简化处理，实际应该基于UI状态判断
            return False  # 默认认为登录失败，需要更复杂的验证逻辑
        except:
            return False
        
    def _create_result(self, status: str, message: str, steps: list, start_time: float) -> Dict[str, Any]:
        """创建测试结果"""
        duration = time.time() - start_time
        return {
            "status": status,
            "message": message,
            "steps": steps,
            "duration": duration,
            "timestamp": time.time()
        }
        
    def test_multiple_apps(self, test_configs: list):
        """测试多个应用"""
        for config in test_configs:
            print(f"\n{'='*50}")
            print(f"测试应用: {config['app_name']}")
            print(f"用户: {config['username']}")
            print('='*50)
            
            result = self.test_app_login_flow(
                config['app_name'],
                config['username'], 
                config['password']
            )
            
            result['app_name'] = config['app_name']
            result['username'] = config['username']
            self.test_results.append(result)
            
            # 间隔
            time.sleep(2)
            
    def generate_report(self) -> Dict[str, Any]:
        """生成测试报告"""
        total_tests = len(self.test_results)
        passed_tests = sum(1 for r in self.test_results if r['status'] == 'passed')
        failed_tests = sum(1 for r in self.test_results if r['status'] == 'failed')
        error_tests = sum(1 for r in self.test_results if r['status'] == 'error')
        
        report = {
            "test_summary": {
                "total_tests": total_tests,
                "passed_tests": passed_tests,
                "failed_tests": failed_tests,
                "error_tests": error_tests,
                "success_rate": (passed_tests / total_tests * 100) if total_tests > 0 else 0
            },
            "test_results": self.test_results,
            "integration_info": {
                "framework": "Open-AutoGLM + WorkScript",
                "operation_type": "智能坐标操作",
                "device_udid": self.device_udid
            }
        }
        
        return report
        
    def print_summary(self):
        """打印测试摘要"""
        report = self.generate_report()
        summary = report['test_summary']
        
        print(f"\n{'='*60}")
        print("📊 智能APK测试摘要")
        print('='*60)
        print(f"总测试数: {summary['total_tests']}")
        print(f"通过: {summary['passed_tests']} ✅")
        print(f"失败: {summary['failed_tests']} ❌")
        print(f"错误: {summary['error_tests']} ⚠️")
        print(f"成功率: {summary['success_rate']:.1f}%")
        print('='*60)
        
        for result in self.test_results:
            status_icon = "✅" if result['status'] == 'passed' else "❌" if result['status'] == 'failed' else "⚠️"
            print(f"{status_icon} {result['app_name']} - {result['username']}: {result['message']}")


def main():
    """主函数"""
    print("🤖 智能APK测试器 - Open-AutoGLM集成")
    print("使用智能坐标操作测试第三方APK登录功能")
    print('='*60)
    
    # 获取设备UDID
    device_udid = None
    if len(sys.argv) > 1:
        device_udid = sys.argv[1]
        print(f"使用设备: {device_udid}")
    else:
        print("请提供设备UDID作为参数")
        return 1
    
    # 创建测试器
    tester = IntelligentAPKTester(device_udid)
    
    # 设置环境
    if not tester.setup():
        return 1
    
    # 测试配置
    test_configs = [
        {
            "app_name": "AutoDroid Manager",
            "username": "15317227@qq.com",
            "password": "Test@123456"
        },
        {
            "app_name": "AutoDroid Manager", 
            "username": "15317227@qq.com",
            "password": "wrong_password"  # 测试失败场景
        }
    ]
    
    # 执行测试
    try:
        tester.test_multiple_apps(test_configs)
        tester.print_summary()
        
        # 保存详细报告
        report = tester.generate_report()
        import json
        from datetime import datetime
        
        report_file = f"autoglm_integration_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
            
        print(f"\n📄 详细报告已保存: {report_file}")
        
        return 0 if report['test_summary']['error_tests'] == 0 else 1
        
    except KeyboardInterrupt:
        print("\n测试被中断")
        return 1
    except Exception as e:
        print(f"测试异常: {str(e)}")
        return 1


if __name__ == "__main__":
    sys.exit(main())