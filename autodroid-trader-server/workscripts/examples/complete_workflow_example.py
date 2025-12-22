#!/usr/bin/env python3
"""
完整的工作脚本引擎使用示例

演示如何使用工作脚本引擎执行登录测试脚本，包括：
1. 初始化引擎
2. 创建工作计划
3. 执行脚本
4. 生成报告
5. 处理错误
"""

import os
import sys
import json
import time
from pathlib import Path

# 将项目根目录添加到Python路径
project_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(project_root))

from core.workscript.engine import WorkScriptEngine


def main():
    """主函数 - 演示完整的工作流程"""
    
    print("🚀 工作脚本引擎使用示例")
    print("=" * 50)
    
    # 1. 初始化引擎
    print("\n📦 初始化工作脚本引擎...")
    try:
        engine = WorkScriptEngine()
        print("✅ 引擎初始化成功")
    except Exception as e:
        print(f"❌ 引擎初始化失败: {e}")
        return
    
    # 2. 列出可用脚本
    print("\n📋 列出可用脚本...")
    available_scripts = engine.list_scripts()
    if available_scripts:
        print(f"找到 {len(available_scripts)} 个脚本:")
        for script in available_scripts:
            print(f"  - {script}")
    else:
        print("⚠️  未找到任何脚本")
    
    # 3. 获取脚本信息
    print("\n🔍 获取登录测试脚本信息...")
    script_info = engine.get_script_info('com.autodroid.manager.login_test')
    if script_info and script_info.get('available'):
        print(f"脚本名称: {script_info['name']}")
        print(f"类名: {script_info['class_name']}")
        print(f"模块路径: {script_info['module_path']}")
        if script_info.get('docstring'):
            print(f"描述: {script_info['docstring']}")
    else:
        print(f"⚠️  未找到登录测试脚本: {script_info.get('error', '未知错误')}")
    
    # 4. 创建测试工作计划
    print("\n📄 创建测试工作计划...")
    workplan = {
        'id': 'login_test_workplan_001',
        'workscript': 'com.autodroid.manager.login_test',
        'data': {
            'username': 'test_user',
            'password': 'test_password',
            'app_package': 'com.autodroid.app',
            'timeout': 30,
            'retry_count': 3
        },
        'serialno': 'emulator-5554',  # 模拟器设备ID
        'priority': 'high',
        'tags': ['login', 'authentication', 'critical'],
        'created_at': time.strftime('%Y-%m-%d %H:%M:%S'),
        'created_by': 'test_engine'
    }
    
    print(f"工作计划ID: {workplan['id']}")
    print(f"目标脚本: {workplan['workscript']}")
    print(f"目标设备: {workplan['serialno']}")
    
    # 5. 执行脚本
    print("\n⚡ 执行登录测试脚本...")
    print("-" * 30)
    
    try:
        result = engine.execute_script(workplan, serialno=workplan['serialno'])
        
        print(f"执行状态: {result['status']}")
        print(f"消息: {result.get('message', '无消息')}")
        
        if 'report_path' in result:
            print(f"报告路径: {result['report_path']}")
        
        if 'data' in result and result['data']:
            print(f"结果数据: {json.dumps(result['data'], indent=2, ensure_ascii=False)}")
        
        if 'error_type' in result:
            print(f"错误类型: {result['error_type']}")
        
        # 6. 验证执行时间
        execution_time = result.get('execution_end_time', '') - result.get('execution_start_time', '')
        if execution_time:
            print(f"执行时间: {execution_time.total_seconds():.2f}秒")
        
    except Exception as e:
        print(f"❌ 脚本执行异常: {e}")
        return
    
    # 7. 保存工作计划到文件
    print("\n💾 保存工作计划到文件...")
    try:
        workplan_file = Path(engine.reports_dir) / f"workplan_{workplan['id']}.json"
        with open(workplan_file, 'w', encoding='utf-8') as f:
            json.dump(workplan, f, indent=2, ensure_ascii=False)
        print(f"✅ 工作计划已保存到: {workplan_file}")
    except Exception as e:
        print(f"⚠️  保存工作计划失败: {e}")
    
    # 8. 保存执行结果到文件
    print("\n📊 保存执行结果到文件...")
    try:
        result_file = Path(engine.reports_dir) / f"result_{workplan['id']}.json"
        with open(result_file, 'w', encoding='utf-8') as f:
            json.dump(result, f, indent=2, ensure_ascii=False, default=str)
        print(f"✅ 执行结果已保存到: {result_file}")
    except Exception as e:
        print(f"⚠️  保存执行结果失败: {e}")
    
    print("\n🎉 示例执行完成！")
    print("=" * 50)
    
    # 9. 显示报告文件位置
    print(f"\n📁 报告文件位置: {engine.reports_dir}")
    try:
        reports_dir = Path(engine.reports_dir)
        if reports_dir.exists():
            print("生成的文件:")
            for file in reports_dir.glob(f"*{workplan['id']}*"):
                print(f"  - {file.name}")
    except Exception as e:
        print(f"⚠️  无法列出报告文件: {e}")


def test_error_handling():
    """测试错误处理"""
    
    print("\n🧪 测试错误处理")
    print("-" * 30)
    
    engine = WorkScriptEngine()
    
    # 测试1: 无效的工作计划
    print("\n1. 测试无效的工作计划...")
    invalid_workplans = [
        {},  # 空工作计划
        {'id': 'test'},  # 缺少workscript字段
        {'id': 'test', 'workscript': 'test'},  # 缺少data字段
        {'id': 'test', 'workscript': 'test', 'data': 'not_dict'}  # data不是字典
    ]
    
    for i, workplan in enumerate(invalid_workplans):
        try:
            result = engine.execute_script(workplan)
            print(f"  测试{i+1}: 应该失败但没有 - {result['status']}")
        except Exception as e:
            print(f"  测试{i+1}: 正确捕获异常 - {type(e).__name__}")
    
    # 测试2: 不存在的脚本
    print("\n2. 测试不存在的脚本...")
    workplan = {
        'id': 'test_nonexistent',
        'workscript': 'nonexistent.script',
        'data': {}
    }
    
    try:
        result = engine.execute_script(workplan)
        print(f"  结果: {result['status']} - {result.get('message', '')}")
    except Exception as e:
        print(f"  异常: {type(e).__name__} - {e}")


def test_multiple_scripts():
    """测试多个脚本执行"""
    
    print("\n🔄 测试多个脚本执行")
    print("-" * 30)
    
    engine = WorkScriptEngine()
    
    # 创建多个工作计划
    workplans = [
        {
            'id': f'multi_test_{i}',
            'workscript': 'com.autodroid.manager.login_test',
            'data': {
                'username': f'test_user_{i}',
                'password': f'password_{i}',
                'timeout': 30
            },
            'serialno': f'emulator-555{i+4}'  # 不同的设备
        }
        for i in range(3)
    ]
    
    results = []
    for workplan in workplans:
        print(f"\n执行工作计划: {workplan['id']}")
        try:
            result = engine.execute_script(workplan)
            results.append(result)
            print(f"  状态: {result['status']}")
        except Exception as e:
            print(f"  异常: {type(e).__name__} - {e}")
            results.append({'status': 'error', 'message': str(e)})
    
    # 统计结果
    success_count = sum(1 for r in results if r['status'] == 'success')
    print(f"\n📊 执行统计:")
    print(f"  总计划数: {len(workplans)}")
    print(f"  成功数: {success_count}")
    print(f"  失败数: {len(workplans) - success_count}")


if __name__ == "__main__":
    # 运行完整示例
    main()
    
    # 运行错误处理测试
    test_error_handling()
    
    # 运行多脚本测试
    test_multiple_scripts()
    
    print("\n🏁 所有测试完成！")