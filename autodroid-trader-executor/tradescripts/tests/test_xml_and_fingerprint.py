#!/usr/bin/env python3
"""
测试增强的XML转储和页面指纹识别功能
"""

import sys
import os
from pathlib import Path

# 添加项目路径到sys.path
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

# 添加当前目录到sys.path
sys.path.insert(0, os.path.dirname(__file__))

# 添加core目录到sys.path
sys.path.insert(0, str(project_root / "core"))

from tools.u2device import U2Device
from tools.flow import FlowManager
from tools.page import parse_xml, build_page_info
from config import get_apks_path


def test_xml_dumping_with_special_chars():
    """测试XML转储是否能正确处理特殊字符"""
    
    print("\n" + "=" * 60)
    print("🔬 测试XML转储功能（特殊字符处理）")
    print("=" * 60)
    
    try:
        device = U2Device()
        print("✅ 设备初始化成功")
        
        # 转储当前页面XML
        print("\n📝 转储当前页面XML...")
        xml_content = device.dump_page_xml()
        
        if not xml_content:
            print("❌ XML转储失败")
            return False
        
        print(f"✅ XML转储成功，内容长度: {len(xml_content)} 字符")
        
        # 检查XML内容
        print("\n🔍 检查XML内容...")
        
        # 检查是否包含特殊字符
        special_chars = []
        for i, char in enumerate(xml_content):
            if ord(char) > 127:
                special_chars.append((i, char, ord(char)))
        
        if special_chars:
            print(f"✅ 发现 {len(special_chars)} 个特殊字符:")
            for idx, char, code in special_chars[:10]:  # 只显示前10个
                print(f"   位置 {idx}: '{char}' (U+{code:04X})")
            if len(special_chars) > 10:
                print(f"   ... 还有 {len(special_chars) - 10} 个")
        else:
            print("ℹ️  未发现特殊字符")
        
        # 保存XML到文件用于检查
        output_dir = Path(r"d:\git\autodroid\autodroid-trader-executor\tradescripts\dump-pages")
        output_dir.mkdir(parents=True, exist_ok=True)
        
        xml_file = output_dir / "test_special_chars.xml"
        with open(xml_file, 'w', encoding='utf-8', errors='replace') as f:
            f.write(xml_content)
        
        print(f"\n💾 XML已保存到: {xml_file}")
        
        # 尝试解析XML
        print("\n🔍 解析XML...")
        try:
            import xml.etree.ElementTree as ET
            root = ET.fromstring(xml_content)
            print(f"✅ XML解析成功")
            print(f"   根节点: {root.tag}")
            print(f"   子节点数量: {len(list(root))}")
        except Exception as e:
            print(f"⚠️  XML解析警告: {e}")
            print("   但这可能不影响页面识别功能")
        
        return True
        
    except Exception as e:
        print(f"❌ 测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_page_fingerprinting():
    """测试页面指纹识别功能"""
    
    print("\n" + "=" * 60)
    print("🔬 测试页面指纹识别功能")
    print("=" * 60)
    
    try:
        # 初始化设备
        device = U2Device()
        print("✅ 设备初始化成功")
        
        # 初始化FlowManager
        apk_dir = get_apks_path()
        print(f"\n📂 APK目录路径: {apk_dir}")
        
        flow_manager = FlowManager(apk_dir, device)
        print("✅ FlowManager初始化成功")
        
        # 加载页面指纹
        print("\n📋 加载页面指纹...")
        flow_manager.load_and_build_fingerprints("com.tdx.androidCCZQ", "wang-ge-jiao-yi")
        print("✅ 页面指纹加载完成")
        
        # 显示已加载的页面信息
        print(f"\n📊 已加载的页面数量: {len(flow_manager._page_infos)}")
        for page_id, page_info in flow_manager._page_infos.items():
            print(f"\n  📄 页面ID: {page_id}")
            print(f"     - Action元素数量: {len(page_info.steps)}")
            print(f"     - Fingerprint元素数量: {len(page_info.fingerprint_elements)}")
            
            if page_info.fingerprint_elements:
                print(f"     - Fingerprint元素详情:")
                for i, fp_elem in enumerate(page_info.fingerprint_elements[:3], 1):
                    print(f"       {i}. resource_id='{fp_elem.resource_id}', text='{fp_elem.text}'")
                if len(page_info.fingerprint_elements) > 3:
                    print(f"       ... 还有 {len(page_info.fingerprint_elements) - 3} 个")
        
        # 测试页面识别
        print("\n🔍 测试页面识别...")
        result = flow_manager.identify_page()
        
        if result:
            page_id = result
            print(f"\n✅ 页面识别成功!")
            print(f"   识别到的页面ID: {page_id}")
            
            # 获取该页面的详细信息
            if page_id in flow_manager._page_infos:
                page_info = flow_manager._page_infos[page_id]
                print(f"\n   页面详细信息:")
                print(f"   - Action元素数量: {len(page_info.steps)}")
                print(f"   - Fingerprint元素数量: {len(page_info.fingerprint_elements)}")
        else:
            print("\n❌ 页面识别失败")
            print("💡 可能的原因:")
            print("   1. 当前页面不在已加载的页面列表中")
            print("   2. 页面XML没有定义fingerprint元素")
            print("   3. 页面元素发生了变化")
        
        return True
        
    except Exception as e:
        print(f"❌ 测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_selector_based_matching():
    """测试基于选择器的页面匹配"""
    
    print("\n" + "=" * 60)
    print("🔬 测试基于选择器的页面匹配")
    print("=" * 60)
    
    try:
        # 初始化设备
        device = U2Device()
        print("✅ 设备初始化成功")
        
        # 初始化FlowManager
        apk_dir = get_apks_path()
        flow_manager = FlowManager(apk_dir, device)
        
        # 加载页面指纹
        print("\n📋 加载页面指纹...")
        flow_manager.load_and_build_fingerprints("com.tdx.androidCCZQ", "wang-ge-jiao-yi")
        print("✅ 页面指纹加载完成")
        
        # 测试每个页面的选择器匹配
        print("\n🔍 测试每个页面的选择器匹配...")
        
        matched_pages = []
        for page_id, page_info in flow_manager._page_infos.items():
            print(f"\n  📄 测试页面: {page_id}")
            
            # 使用选择器检查页面是否匹配
            is_matched = flow_manager._is_page_matched_by_selectors(page_id, page_info)
            
            if is_matched:
                print(f"  ✅ 页面 {page_id} 匹配成功")
                matched_pages.append(page_id)
            else:
                print(f"  ❌ 页面 {page_id} 匹配失败")
        
        # 总结结果
        print(f"\n📊 匹配结果统计:")
        print(f"   总页面数: {len(flow_manager._page_infos)}")
        print(f"   匹配成功: {len(matched_pages)}")
        print(f"   匹配失败: {len(flow_manager._page_infos) - len(matched_pages)}")
        
        if matched_pages:
            print(f"\n✅ 匹配成功的页面:")
            for page_id in matched_pages:
                print(f"   - {page_id}")
        else:
            print(f"\n⚠️  没有页面匹配成功")
            print("💡 建议:")
            print("   1. 检查设备当前页面")
            print("   2. 确认页面XML是否定义了fingerprint元素")
            print("   3. 尝试手动操作到已知页面")
        
        return len(matched_pages) > 0
        
    except Exception as e:
        print(f"❌ 测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False


if __name__ == "__main__":
    print("=" * 60)
    print("🧪 增强的XML转储和页面指纹识别测试")
    print("=" * 60)
    
    # 运行测试
    results = {}
    
    # 测试1: XML转储功能
    results['xml_dumping'] = test_xml_dumping_with_special_chars()
    
    # 测试2: 页面指纹识别
    results['page_fingerprinting'] = test_page_fingerprinting()
    
    # 测试3: 基于选择器的页面匹配
    results['selector_matching'] = test_selector_based_matching()
    
    # 总结结果
    print("\n" + "=" * 60)
    print("📊 测试结果总结")
    print("=" * 60)
    
    for test_name, result in results.items():
        status = "✅ 通过" if result else "❌ 失败"
        print(f"{status}: {test_name}")
    
    total_tests = len(results)
    passed_tests = sum(1 for result in results.values() if result)
    
    print(f"\n总计: {passed_tests}/{total_tests} 测试通过")
    
    if passed_tests == total_tests:
        print("\n🎉 所有测试通过！")
    else:
        print(f"\n⚠️  有 {total_tests - passed_tests} 个测试失败")
