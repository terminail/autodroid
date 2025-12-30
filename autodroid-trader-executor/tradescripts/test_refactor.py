#!/usr/bin/env python3
"""
测试重构后的指纹管理系统功能
"""

import sys
import os

# 添加tools目录到路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'tools'))

from flow import FlowManager
from page import PageMatcher
from element import ElementExecutor

def test_page_matcher_fingerprints():
    """测试PageMatcher的指纹管理功能"""
    print("🧪 测试PageMatcher指纹管理功能...")
    
    page_matcher = PageMatcher()
    
    # 测试指纹管理方法
    test_fingerprints = [
        {"resource-id": "com.example:id/button1", "text": "确定"},
        {"resource-id": "com.example:id/button2", "text": "取消"}
    ]
    
    # 添加指纹
    page_matcher.add_page_fingerprints("test_page", test_fingerprints)
    print("✅ 指纹添加成功")
    
    # 获取指纹
    fingerprints = page_matcher.get_page_fingerprints("test_page")
    assert len(fingerprints) == 2, f"期望2个指纹，实际得到{len(fingerprints)}个"
    print("✅ 指纹获取成功")
    
    # 检查是否有指纹
    assert page_matcher.has_fingerprints(), "应该有指纹数据"
    print("✅ 指纹存在检查成功")
    
    # 清空指纹
    page_matcher.clear_fingerprints()
    assert not page_matcher.has_fingerprints(), "指纹应该已被清空"
    print("✅ 指纹清空成功")
    
    print("🎉 PageMatcher指纹管理功能测试通过！")

def test_flow_manager_quick_identify():
    """测试FlowManager的快速页面识别功能"""
    print("\n🧪 测试FlowManager快速页面识别功能...")
    
    # 创建FlowManager实例
    flow_manager = FlowManager("test_apk_dir")
    
    # 测试快速识别方法存在
    assert hasattr(flow_manager, '_quick_identify_page'), "应该存在快速识别方法"
    assert hasattr(flow_manager, '_execute_steps_with_fingerprints'), "应该存在指纹步骤执行方法"
    
    print("✅ FlowManager快速识别功能存在性检查通过")
    
    # 测试指纹步骤执行方法存在
    assert hasattr(flow_manager, '_build_selector_from_element'), "应该存在选择器构建方法"
    assert hasattr(flow_manager, '_execute_selector_action'), "应该存在选择器动作执行方法"
    
    print("✅ FlowManager选择器功能存在性检查通过")
    
    print("🎉 FlowManager快速页面识别功能测试通过！")

def test_element_executor_selectors():
    """测试ElementExecutor的选择器构建功能"""
    print("\n🧪 测试ElementExecutor选择器构建功能...")
    
    # 创建ElementExecutor实例（不需要真实设备）
    element_executor = ElementExecutor(None)
    
    # 测试选择器构建方法
    test_element = {
        "resource-id": "com.example:id/button1",
        "text": "确定",
        "class": "android.widget.Button"
    }
    
    # 测试单个选择器构建
    selector = element_executor.build_selector(test_element)
    assert selector is not None, "应该能构建选择器"
    print(f"✅ 选择器构建成功: {selector}")
    
    # 测试组合选择器构建
    compound_selector = element_executor._build_compound_selector(test_element)
    assert compound_selector is not None, "应该能构建组合选择器"
    print(f"✅ 组合选择器构建成功: {compound_selector}")
    
    # 测试元素标识符生成
    element_id = element_executor._get_element_identifier(test_element)
    assert element_id.startswith("resource-id:"), "应该生成正确的元素标识符"
    print(f"✅ 元素标识符生成成功: {element_id}")
    
    # 测试页面选择器构建
    page_elements = [test_element]
    selectors = element_executor.build_selectors_for_page(page_elements)
    assert len(selectors) == 1, "应该为页面构建一个选择器"
    print(f"✅ 页面选择器构建成功: {selectors}")
    
    print("🎉 ElementExecutor选择器构建功能测试通过！")

def test_architecture_integrity():
    """测试架构完整性"""
    print("\n🧪 测试架构完整性...")
    
    # 检查fingerprint_manager.py是否已删除
    fingerprint_manager_path = os.path.join(os.path.dirname(__file__), 'tools', 'fingerprint_manager.py')
    assert not os.path.exists(fingerprint_manager_path), "fingerprint_manager.py应该已被删除"
    print("✅ fingerprint_manager.py已正确删除")
    
    # 检查各层职责分离
    flow_manager = FlowManager("test_apk_dir")
    
    # Flow层应该包含快速页面识别功能
    assert hasattr(flow_manager, '_quick_identify_page'), "Flow层应该包含快速页面识别"
    
    # Page层应该包含指纹管理功能
    page_matcher = flow_manager.page_matcher
    assert hasattr(page_matcher, 'add_page_fingerprints'), "Page层应该包含指纹管理"
    assert hasattr(page_matcher, 'get_page_fingerprints'), "Page层应该包含指纹获取"
    
    # Element层应该包含选择器构建功能
    element_executor = ElementExecutor(None)
    assert hasattr(element_executor, 'build_selector'), "Element层应该包含选择器构建"
    
    print("✅ 三层架构职责分离正确")
    print("🎉 架构完整性测试通过！")

def main():
    """主测试函数"""
    print("🚀 开始测试重构后的指纹管理系统...\n")
    
    try:
        test_page_matcher_fingerprints()
        test_flow_manager_quick_identify()
        test_element_executor_selectors()
        test_architecture_integrity()
        
        print("\n🎉 所有测试通过！重构成功！")
        print("\n📋 重构总结：")
        print("  ✅ 删除了fingerprint_manager.py文件")
        print("  ✅ 将指纹管理功能移到page.py（page层职责）")
        print("  ✅ 将快速页面识别功能移到flow.py（flow层职责）")
        print("  ✅ 将选择器构建功能移到element.py（element层职责）")
        print("  ✅ 验证了重构后的代码功能")
        print("  ✅ 保持了三层架构的完整性")
        
        return 0
    except Exception as e:
        print(f"\n❌ 测试失败: {e}")
        import traceback
        traceback.print_exc()
        return 1

if __name__ == "__main__":
    sys.exit(main())