#!/usr/bin/env python3
"""
AutoDroid Manager 真实设备登录测试
使用adb连接的设备进行实际测试
"""

import sys
import os
import json
from datetime import datetime

# 添加autodroid-container路径到Python路径
container_path = os.path.join(os.path.dirname(__file__), '../../../autodroid-container')
if container_path not in sys.path:
    sys.path.insert(0, container_path)

def test_with_real_device():
    """使用真实设备测试登录功能"""
    print("=== AutoDroid Manager 真实设备登录测试 ===")
    print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("-" * 50)
    
    try:
        # 导入登录测试类
        from login_test import login_test
        
        print("✓ 成功导入login_test模块")
        
        # 检查设备连接
        import subprocess
        result = subprocess.run(['adb', 'devices'], capture_output=True, text=True)
        if 'emulator-5554' not in result.stdout:
            print("❌ 未找到连接的设备")
            return False
        
        print("✓ 检测到设备: emulator-5554")
        
        # 检查应用是否安装
        result = subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'pm', 'list', 'packages'], 
                                capture_output=True, text=True)
        if 'com.autodroid.manager' not in result.stdout:
            print("❌ 设备上未安装AutoDroid Manager应用")
            return False
        
        print("✓ 检测到AutoDroid Manager应用已安装")
        
        # 创建登录测试实例（使用真实设备UDID）
        login_tester = login_test(
            workplan={
                'id': 'real_device_test',
                'name': 'AutoDroid Manager Real Device Login Test',
                'data': {
                    'username': '15317227@qq.com',
                    'password': '123456',
                    'use_fingerprint': False,
                    'app_package': 'com.autodroid.manager',
                    'app_activity': 'com.autodroid.manager.auth.activity.LoginActivity',
                    'timeout': 30,
                    'success_rate': 0.9,
                    'device_udid': 'emulator-5554'
                }
            },
            device_udid='emulator-5554',
            test_username='15317227@qq.com',
            test_password='123456',
            use_fingerprint=False,
            app_package='com.autodroid.manager',
            app_activity='com.autodroid.manager.auth.activity.LoginActivity'
        )
        
        print("✓ 成功创建登录测试实例")
        print(f"  - 测试用户: {login_tester.test_username}")
        print(f"  - 设备UDID: {login_tester.device_udid}")
        print(f"  - 应用包名: {login_tester.app_package}")
        print(f"  - 登录活动: {login_tester.app_activity}")
        print(f"  - 指纹登录: {login_tester.use_fingerprint}")
        
        # 执行登录测试
        print("\n开始执行登录测试...")
        result = login_tester.run()
        
        print("\n=== 测试结果 ===")
        print(f"状态: {result.get('status', 'unknown')}")
        print(f"消息: {result.get('message', '无消息')}")
        print(f"用户名: {result.get('username', 'unknown')}")
        print(f"登录方式: {result.get('login_method', 'unknown')}")
        print(f"耗时: {result.get('duration_seconds', 0):.2f}秒")
        
        if 'test_steps' in result:
            print("测试步骤:")
            for i, step in enumerate(result['test_steps'], 1):
                print(f"  {i}. {step}")
        
        if 'report_path' in result:
            print(f"报告路径: {result['report_path']}")
        
        if 'error' in result:
            print(f"错误信息: {result['error']}")
        
        # 验证结果
        if result.get('status') == 'success':
            print("\n✅ 登录测试通过!")
            return True
        else:
            print(f"\n❌ 登录测试失败: {result.get('message', '未知错误')}")
            return False
            
    except ImportError as e:
        print(f"❌ 导入模块失败: {e}")
        return False
    except Exception as e:
        print(f"❌ 测试执行失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def check_device_status():
    """检查设备状态"""
    print("=== 设备状态检查 ===")
    
    try:
        import subprocess
        
        # 检查设备连接
        result = subprocess.run(['adb', 'devices'], capture_output=True, text=True)
        print("连接的设备:")
        print(result.stdout)
        
        if 'emulator-5554' in result.stdout:
            # 获取设备信息
            print("\n设备详细信息:")
            
            # 设备型号
            result = subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'getprop', 'ro.product.model'], 
                                    capture_output=True, text=True)
            print(f"设备型号: {result.stdout.strip()}")
            
            # Android版本
            result = subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'getprop', 'ro.build.version.release'], 
                                    capture_output=True, text=True)
            print(f"Android版本: {result.stdout.strip()}")
            
            # 检查应用是否安装
            result = subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'pm', 'list', 'packages'], 
                                    capture_output=True, text=True)
            if 'com.autodroid.manager' in result.stdout:
                print("✓ AutoDroid Manager应用已安装")
            else:
                print("❌ AutoDroid Manager应用未安装")
                
            return True
        else:
            print("❌ 未检测到设备")
            return False
            
    except Exception as e:
        print(f"设备检查失败: {e}")
        return False

def main():
    """主函数"""
    print("AutoDroid Manager 真实设备登录测试程序")
    print("=" * 60)
    
    # 检查设备状态
    device_ok = check_device_status()
    
    if not device_ok:
        print("\n❌ 设备检查失败，无法继续测试")
        return 1
    
    print("\n" + "=" * 60)
    
    # 使用真实设备测试登录功能
    login_success = test_with_real_device()
    
    # 总结结果
    print("\n" + "=" * 60)
    print("测试总结:")
    print(f"设备状态检查: {'✅ 通过' if device_ok else '❌ 失败'}")
    print(f"登录功能测试: {'✅ 通过' if login_success else '❌ 失败'}")
    
    if device_ok and login_success:
        print("\n🎉 所有测试通过!")
        return 0
    else:
        print("\n⚠️  部分测试失败，请检查日志")
        return 1

if __name__ == "__main__":
    sys.exit(main())