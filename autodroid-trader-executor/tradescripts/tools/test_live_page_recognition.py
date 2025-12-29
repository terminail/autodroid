#!/usr/bin/env python3
"""
测试页面识别功能：
1. 获取设备当前页面的 live XML
2. 执行页面识别
3. 打印识别结果（匹配到的离线XML文件名和分数）
"""

from pathlib import Path
import sys
import time

# 添加tools目录到路径
sys.path.insert(0, str(Path(__file__).parent))

from adb_operator import ADBAutoOpTool
from page import PageFingerprint, parse_xml


def print_fingerprint_info(name: str, fp: PageFingerprint):
    print(f"\n  📄 {name}:")
    print(f"     bounds数量: {len(fp.bounds_set)}")
    print(f"     resource_ids数量: {len(fp.resource_ids)}")
    print(f"     texts数量: {len(fp.texts)}")
    print(f"     content_descs数量: {len(fp.content_descs)}")
    print(f"     clickable_count: {fp.clickable_count}")
    print(f"     webview_texts: {fp.webview_texts}")


def main():
    print("=" * 60)
    print("📱 页面识别测试工具")
    print("=" * 60)
    
    # 初始化ADB操作器
    print("\n🔗 正在连接设备...")
    try:
        operator = ADBAutoOpTool()
        print("✅ 设备连接成功")
    except Exception as e:
        print(f"❌ 设备连接失败: {e}")
        return
    
    # 加载页面指纹
    print("\n📦 正在加载页面指纹...")
    operator._load_flow_pages()
    print("✅ 页面指纹加载完成")
    
    # 获取当前页面的live XML
    print("\n📥 正在获取当前页面XML...")
    live_xml = operator.dump_hierarchy()
    print(f"✅ 获取到XML，大小: {len(live_xml)} 字符")
    
    # 保存live XML用于调试
    timestamp = int(time.time())
    debug_dir = Path(__file__).parent / "debug_xmls"
    debug_dir.mkdir(exist_ok=True)
    live_path = debug_dir / f"live_current_{timestamp}.xml"
    live_path.write_text(live_xml, encoding="utf-8")
    print(f"💾 已保存live XML: {live_path}")
    
    # 获取所有已加载的页面指纹
    fingerprints = operator.flow_manager._page_matcher._page_fingerprints
    print(f"\n📦 已加载 {len(fingerprints)} 个页面指纹")
    
    # 解析live XML
    live_root = parse_xml(live_xml)
    
    # 打印当前页面的结构统计
    live_bounds = []
    live_resource_ids = set()
    live_texts = set()
    live_webview_texts = set()
    for elem in live_root.iter():
        if elem.tag == "hierarchy":
            continue
        bounds = elem.get("bounds", "").strip()
        rid = elem.get("resource-id", "").strip()
        text = elem.get("text", "").strip()
        class_name = elem.get("class", "").strip()
        
        if bounds:
            live_bounds.append(bounds)
        if rid:
            live_resource_ids.add(rid)
        if text:
            live_texts.add(text)
        if class_name == "android.webkit.WebView" and text:
            live_webview_texts.add(text)
    
    print(f"\n📊 当前页面结构:")
    print(f"   bounds数量: {len(live_bounds)}")
    print(f"   resource_ids数量: {len(live_resource_ids)}")
    print(f"   texts数量: {len(live_texts)}")
    print(f"   webview_texts: {live_webview_texts}")
    
    # 执行页面识别
    print("\n🔍 正在识别页面...")
    page_id, score, all_scores = operator.identify_page(live_xml)

    # 如果有页面ID，显示对比信息
    if page_id and page_id in fingerprints:
        fp = fingerprints[page_id]
        print(f"\n📄 匹配页面 ({page_id}) 的特征:")
        print(f"   bounds数量: {len(fp.bounds_set)}")
        print(f"   resource_ids数量: {len(fp.resource_ids)}")
        print(f"   texts数量: {len(fp.texts)}")
        print(f"   webview_texts: {fp.webview_texts}")
        
        # 计算共有特征
        common_ids = set(fp.resource_ids) & live_resource_ids
        common_texts = set(fp.texts) & live_texts
        print(f"\n🔗 共有特征:")
        print(f"   共有resource_ids: {len(common_ids)}/{len(fp.resource_ids)}")
        if common_ids:
            print(f"     {list(common_ids)[:5]}")
        print(f"   共有texts: {len(common_texts)}/{len(fp.texts)}")
        if common_texts:
            print(f"     {list(common_texts)[:5]}")
        
        # 保存对比文件
        compare_path = debug_dir / f"compare_{page_id}_{timestamp}.xml"
        compare_path.write_text(live_xml, encoding="utf-8")
        print(f"\n💾 已保存对比文件:")
        print(f"   live: {live_path.name}")
        print(f"   offline: {compare_path.name}")
    
    # 打印所有页面的匹配结果表格（最后）
    print("\n" + "=" * 70)
    print("📊 页面匹配结果")
    print("=" * 70)
    print(f"{'离线xml':<22} {'匹配分':>10} {'选中':<8}")
    print("-" * 70)

    for pid, s, details in all_scores:
        is_selected = "☑️" if pid == page_id else ""
        print(f"{pid:<22} {s:>10.2f} {is_selected:<8}")

    print("=" * 70)
    print(f"\n🎯 当前页面: {page_id} (匹配分数: {score:.2f})")
    
    print("=" * 60)
    print("\n✨ 测试完成")


if __name__ == "__main__":
    main()
