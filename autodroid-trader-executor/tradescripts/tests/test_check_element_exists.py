#!/usr/bin/env python3
"""
测试 check_element_exists 方法是否能找到首页的指纹元素
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

from u2device import U2Device
from flow import FlowManager
from config import get_apks_path

def test_check_element_exists():
    """测试 check_element_exists 方法"""
    
    print("🔍 开始测试 check_element_exists 方法...")
    
    try:
        # 初始化设备
        device = U2Device()
        print("✅ 设备初始化成功")
        
        # 测试首页的指纹元素
        test_selectors = [
            # 首页的指纹元素
            'text("川财APP首页")',
            'resourceId("outbox")',
            'resourceId("state_box")',
            'resourceId("cczq_kf")',
            'resourceId("hot-zxg")',
            'resourceId("hot-schq")',
            'resourceId("hot-wdcc")',
            
            # 其他可能的元素
            'text("自选股")',
            'text("市场行情")',
            'text("我的持仓")',
            'text("输入股票代码/首字母")',
        ]
        
        print("\n📋 测试选择器列表:")
        for i, selector in enumerate(test_selectors, 1):
            print(f"  {i}. {selector}")
        
        print("\n🔍 开始检查元素是否存在...")
        
        results = []
        for selector in test_selectors:
            exists = device.check_element_exists(selector, timeout=2.0)
            status = "✅ 存在" if exists else "❌ 不存在"
            results.append((selector, exists))
            print(f"  {status}: {selector}")
        
        # 统计结果
        total_tests = len(results)
        passed_tests = sum(1 for _, exists in results if exists)
        
        print(f"\n📊 测试结果统计:")
        print(f"  总测试数: {total_tests}")
        print(f"  通过数: {passed_tests}")
        print(f"  失败数: {total_tests - passed_tests}")
        print(f"  成功率: {passed_tests/total_tests*100:.1f}%")
        
        # 检查首页指纹元素是否匹配
        home_fingerprint_exists = any(exists for selector, exists in results 
                                     if '川财APP首页' in selector)
        
        if home_fingerprint_exists:
            print("\n🎉 首页指纹元素匹配成功！")
            print("✅ check_element_exists 方法可以正确识别首页")
        else:
            print("\n⚠️ 首页指纹元素未匹配")
            print("💡 建议检查设备当前页面是否为主页")
        
        return results
        
    except Exception as e:
        print(f"❌ 测试失败: {e}")
        return []

def test_lightweight_page_recognition():
    """测试轻量级页面识别功能"""
    
    print("\n🔍 开始测试轻量级页面识别...")
    
    try:
        # 初始化FlowManager
        apk_dir = get_apks_path()
        print(f"📂 APK目录路径: {apk_dir}")
        
        device = U2Device()
        flow_manager = FlowManager(apk_dir, device)
        
        # 加载页面指纹
        flow_manager.load_and_build_fingerprints("com.tdx.androidCCZQ", "wang-ge-jiao-yi")
        
        # 测试轻量级页面识别
        result = flow_manager._quick_identify_page()
        
        if result:
            page_id, score, method = result
            print(f"✅ 轻量级页面识别成功:")
            print(f"   页面ID: {page_id}")
            print(f"   匹配分数: {score:.2f}")
            print(f"   识别方法: {method}")
        else:
            print("❌ 轻量级页面识别失败")
            
        return result
        
    except Exception as e:
        print(f"❌ 轻量级页面识别测试失败: {e}")
        return None

if __name__ == "__main__":
    print("=" * 60)
    print("🔬 check_element_exists 方法测试")
    print("=" * 60)
    
    # 运行基本测试
    test_results = test_check_element_exists()
    
    # 运行轻量级页面识别测试
    recognition_result = test_lightweight_page_recognition()
    
    print("\n" + "=" * 60)
    print("🏁 测试完成")
    print("=" * 60)