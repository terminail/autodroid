#!/usr/bin/env python3
"""
工作脚本引擎使用示例

演示如何使用工作脚本引擎执行工作计划
"""

import os
import sys
import json
import logging
from datetime import datetime

# 添加项目路径
container_path = os.path.join(os.path.dirname(__file__), '../../..')
if container_path not in sys.path:
    sys.path.insert(0, container_path)

from core.workscript.engine import WorkScriptEngine


def main():
    """主函数"""
    
    # 设置日志
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    print("🚀 工作脚本引擎示例")
    print("=" * 50)
    
    # 初始化引擎
    engine = WorkScriptEngine(
        workscripts_dir='./autodroid-container/workscripts',
        reports_dir='./reports'
    )
    
    # 列出可用脚本
    print("\n📋 可用工作脚本:")
    available_scripts = engine.list_available_scripts()
    for i, script in enumerate(available_scripts, 1):
        print(f"  {i}. {script}")
    
    if not available_scripts:
        print("  ❌ 没有找到可用的工作脚本")
        return
    
    # 选择要执行的脚本
    print(f"\n🔧 选择要测试的脚本 (1-{len(available_scripts)}):")
    try:
        choice = int(input("请输入数字: ")) - 1
        if choice < 0 or choice >= len(available_scripts):
            print("❌ 无效的选择")
            return
        
        selected_script = available_scripts[choice]
        print(f"✅ 选择脚本: {selected_script}")
        
    except (ValueError, EOFError):
        print("❌ 无效输入，使用第一个脚本")
        selected_script = available_scripts[0]
    
    # 获取脚本信息
    print(f"\n📊 脚本信息:")
    script_info = engine.get_script_info(selected_script)
    print(f"  名称: {script_info['name']}")
    print(f"  类名: {script_info['class_name']}")
    print(f"  文档: {script_info.get('docstring', '无')}")
    print(f"  可用: {script_info['available']}")
    
    # 创建工作计划
    print(f"\n📝 创建工作计划...")
    workplan = {
        'id': f'example_workplan_{datetime.now().strftime("%Y%m%d_%H%M%S")}',
        'workscript': selected_script,
        'data': {
            # 登录测试脚本的参数
            'username': 'test_user_example',
            'password': 'test_password_example',
            'app_package': 'com.autodroid.manager',
            'app_activity': '.ui.login.LoginActivity',
            'timeout': 30,
            'success_rate': 0.9
        },
        'created_at': datetime.now().isoformat(),
        'status': 'pending'
    }
    
    print(f"  工作计划ID: {workplan['id']}")
    print(f"  脚本: {workplan['workscript']}")
    print(f"  参数: {json.dumps(workplan['data'], indent=2, ensure_ascii=False)}")
    
    # 执行脚本
    print(f"\n⚡ 执行工作脚本...")
    try:
        result = engine.execute_script(workplan, device_udid='example_device_001')
        
        print(f"\n📈 执行结果:")
        print(f"  状态: {result['status']}")
        print(f"  消息: {result.get('message', '无')}")
        print(f"  执行时间: {result.get('execution_time', 0):.2f}秒")
        print(f"  报告目录: {result.get('report_directory', '无')}")
        
        if result['status'] == 'success':
            print("  ✅ 执行成功")
        elif result['status'] == 'failed':
            print("  ⚠️  执行失败")
        else:
            print("  ❌ 执行错误")
        
        # 显示详细结果
        if 'test_steps' in result:
            print(f"\n📝 测试步骤:")
            for i, step in enumerate(result['test_steps'], 1):
                status = "✅" if "成功" in step else "❌"
                print(f"  {i}. {status} {step}")
        
        # 保存结果摘要
        if 'report_directory' in result:
            summary_file = os.path.join(result['report_directory'], 'execution_summary.json')
            with open(summary_file, 'w', encoding='utf-8') as f:
                json.dump({
                    'workplan_id': workplan['id'],
                    'script_name': selected_script,
                    'execution_time': result.get('execution_time', 0),
                    'status': result['status'],
                    'message': result.get('message', ''),
                    'timestamp': datetime.now().isoformat()
                }, f, indent=2, ensure_ascii=False)
            
            print(f"\n💾 执行摘要已保存到: {summary_file}")
        
    except Exception as e:
        print(f"\n❌ 脚本执行失败: {e}")
        import traceback
        traceback.print_exc()
    
    print("\n🎉 示例执行完成")


if __name__ == "__main__":
    main()