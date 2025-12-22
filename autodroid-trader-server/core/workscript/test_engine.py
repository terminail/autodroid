#!/usr/bin/env python3
"""
工作脚本引擎测试
"""

import os
import sys
import json
import logging
from datetime import datetime

# 添加项目路径
container_path = os.path.join(os.path.dirname(__file__), '../..')
if container_path not in sys.path:
    sys.path.insert(0, container_path)

from core.workscript.engine import WorkScriptEngine


def test_engine():
    """测试工作脚本引擎"""
    
    print("🚀 开始测试工作脚本引擎...")
    
    # 初始化引擎
    engine = WorkScriptEngine()
    
    # 列出可用脚本
    print("\n📋 可用脚本:")
    available_scripts = engine.list_available_scripts()
    for script in available_scripts:
        print(f"  - {script}")
    
    # 测试加载脚本
    if 'login_test' in available_scripts:
        print("\n🔧 测试加载 login_test 脚本...")
        try:
            script_class = engine.load_script('login_test')
            print(f"✅ 脚本加载成功: {script_class.__name__}")
            
            # 获取脚本信息
            script_info = engine.get_script_info('login_test')
            print(f"📊 脚本信息:")
            print(f"  名称: {script_info['name']}")
            print(f"  类名: {script_info['class_name']}")
            print(f"  可用: {script_info['available']}")
            
        except Exception as e:
            print(f"❌ 脚本加载失败: {e}")
    
    # 创建测试工作计划
    test_workplan = {
        'id': 'test_workplan_001',
        'workscript': 'login_test',
        'data': {
            'username': 'test_user_123',
            'password': 'test_password_456',
            'app_package': 'com.autodroid.manager',
            'app_activity': '.ui.login.LoginActivity',
            'timeout': 30,
            'success_rate': 0.8
        },
        'created_at': datetime.now().isoformat(),
        'status': 'pending'
    }
    
    print(f"\n📝 测试工作计划:")
    print(f"  ID: {test_workplan['id']}")
    print(f"  脚本: {test_workplan['workscript']}")
    print(f"  参数: {json.dumps(test_workplan['data'], indent=2, ensure_ascii=False)}")
    
    # 执行脚本
    print(f"\n⚡ 执行工作脚本...")
    try:
        result = engine.execute_script(test_workplan, device_serialno='test_device_001')
        
        print(f"\n📊 执行结果:")
        print(f"  状态: {result['status']}")
        print(f"  消息: {result.get('message', '无')}")
        print(f"  执行时间: {result.get('execution_time', 0):.2f}秒")
        print(f"  报告路径: {result.get('report_directory', '无')}")
        
        if 'test_steps' in result:
            print(f"  测试步骤:")
            for step in result['test_steps']:
                print(f"    - {step}")
        
        # 保存结果到文件
        result_file = os.path.join(result['report_directory'], 'engine_test_result.json')
        with open(result_file, 'w', encoding='utf-8') as f:
            json.dump(result, f, indent=2, ensure_ascii=False)
        
        print(f"\n💾 结果已保存到: {result_file}")
        
    except Exception as e:
        print(f"❌ 脚本执行失败: {e}")
        import traceback
        traceback.print_exc()
    
    print("\n✅ 引擎测试完成")


def test_script_validation():
    """测试脚本验证"""
    
    print("\n🔍 测试脚本验证...")
    
    engine = WorkScriptEngine()
    
    # 测试无效的工作计划
    invalid_workplans = [
        {},  # 空工作计划
        {'workscript': 'test'},  # 缺少data字段
        {'data': {}},  # 缺少workscript字段
        {'workscript': 'test', 'data': 'not_dict'}  # data不是字典
    ]
    
    for i, workplan in enumerate(invalid_workplans):
        print(f"\n  测试无效工作计划 {i+1}:")
        try:
            result = engine.execute_script(workplan)
            print(f"    ❌ 应该失败但没有: {result}")
        except Exception as e:
            print(f"    ✅ 正确失败: {e}")


if __name__ == "__main__":
    # 设置日志
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # 运行测试
    test_engine()
    test_script_validation()
    
    print("\n🎉 所有测试完成")