#!/usr/bin/env python3
"""
AutoDroid Manager 登录测试验证脚本
用于测试login_test.py的功能
"""

import sys
import os
import json
from datetime import datetime

# 添加autodroid-trader-server路径到Python路径
container_path = os.path.join(os.path.dirname(__file__), '../../../autodroid-trader-server')
if container_path not in sys.path:
    sys.path.insert(0, container_path)

def test_login_script(device_udid=None):
    """测试登录脚本"""
    print("=== AutoDroid Manager 登录测试 ===")
    print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("-" * 50)
    
    try:
        # 导入登录测试类
        from login_test import login_test
        
        print("✓ 成功导入login_test模块")
        
        # 创建登录测试实例
        login_tester = login_test(
            workplan=None,  # 使用默认workplan
            device_udid=device_udid,  # 使用指定的设备UDID
            test_username="15317227@qq.com",
            test_password="123456",
            use_fingerprint=False,  # 不使用指纹登录
            app_package="com.autodroid.manager",
            app_activity="com.autodroid.manager.auth.activity.LoginActivity"
        )
        
        print("✓ 成功创建登录测试实例")
        print(f"  - 测试用户: {login_tester.test_username}")
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

def test_with_fingerprint(device_udid=None):
    """测试指纹登录功能"""
    print("\n=== 指纹登录测试 ===")
    
    try:
        from login_test import login_test
        
        # 创建启用指纹登录的测试实例
        login_tester = login_test(
            workplan=None,
            device_udid=device_udid,
            test_username="15317227@qq.com", 
            test_password="123456",
            use_fingerprint=True,  # 启用指纹登录
            app_package="com.autodroid.manager",
            app_activity="com.autodroid.manager.auth.activity.LoginActivity"
        )
        
        print("✓ 已启用指纹登录测试")
        result = login_tester.run()
        
        print(f"指纹登录结果: {result.get('status', 'unknown')}")
        print(f"使用的登录方式: {result.get('login_method', 'unknown')}")
        
        return result.get('status') == 'success'
        
    except Exception as e:
        print(f"指纹登录测试失败: {e}")
        return False

def main():
    """主函数"""
    # 检查命令行参数
    device_udid = None
    if len(sys.argv) > 1:
        device_udid = sys.argv[1]
        print(f"使用设备: {device_udid}")
    
    print("AutoDroid Manager 登录测试验证程序")
    print("=" * 60)
    
    # 测试基本登录功能
    basic_success = test_login_script(device_udid)
    
    # 测试指纹登录功能
    fingerprint_success = test_with_fingerprint(device_udid)
    
    # 总结结果
    print("\n" + "=" * 60)
    print("测试总结:")
    print(f"基本登录测试: {'✅ 通过' if basic_success else '❌ 失败'}")
    print(f"指纹登录测试: {'✅ 通过' if fingerprint_success else '❌ 失败'}")
    
    if basic_success and fingerprint_success:
        print("\n🎉 所有测试通过!")
        return 0
    else:
        print("\n⚠️  部分测试失败，请检查日志")
        return 1

if __name__ == "__main__":
    sys.exit(main())