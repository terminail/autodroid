#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
XML分析工具 - 页面XML对比和元素分析工具
合并了以下脚本的功能:
- compare_xml.py
- compare_both_xmls.py
- check_downloaded_xml.py
- debug_home_element.py
- debug_entry_xzsg.py
"""

import re
from pathlib import Path
from typing import Dict, List, Tuple


class XMLAnalyzer:
    """XML页面分析工具类"""

    def __init__(self):
        self.entries_cache = {}

    def parse_xml_entries(self, xml_path: str, label: str = "") -> Dict:
        """解析XML中的所有entry元素"""
        with open(xml_path, 'r', encoding='utf-8') as f:
            content = f.read()

        entries = {}
        pattern = r'resource-id="(entry-[^"]*)"[^>]*>'
        for m in re.finditer(pattern, content):
            resource_id = m.group(1)
            if resource_id not in entries:
                start = m.start()
                element_str = content[start:start+250]

                index_match = re.search(r'index="(\d+)"', element_str)
                clickable_match = re.search(r'clickable="([^"]*)"', element_str)
                bounds_match = re.search(r'bounds="(\[[0-9,]+)"', element_str)
                text_match = re.search(r'text="([^"]*)"', element_str)
                content_desc_match = re.search(r'content-desc="([^"]*)"', element_str)

                entries[resource_id] = {
                    'index': index_match.group(1) if index_match else '?',
                    'clickable': clickable_match.group(1) if clickable_match else '?',
                    'bounds': bounds_match.group(1) if bounds_match else '',
                    'text': text_match.group(1) if text_match else '',
                    'content-desc': content_desc_match.group(1) if content_desc_match else ''
                }

        if label:
            print(f"\n{'='*60}")
            print(f"  {label}")
            print(f"{'='*60}")
            print(f"Total entry elements: {len(entries)}")
            for rid in sorted(entries.keys()):
                info = entries[rid]
                print(f"  [{info['index']:>2}] {rid:<12} clickable={info['clickable']:<5} text='{info['text']}'")

        self.entries_cache[label] = entries
        return entries

    def compare_entries(self, entries1: Dict, entries2: Dict,
                        label1: str, label2: str) -> Tuple[List, List, List]:
        """对比两组entry元素"""
        all_ids = set(entries1.keys()) | set(entries2.keys())

        matches, missing, new = [], [], []

        for rid in sorted(all_ids):
            in1 = rid in entries1
            in2 = rid in entries2

            if in1 and in2:
                e1, e2 = entries1[rid], entries2[rid]
                if e1['clickable'] != e2['clickable'] or e1['text'] != e2['text']:
                    matches.append((rid, 'diff', e1, e2))
                else:
                    matches.append((rid, 'same', e1, e2))
            elif in1:
                missing.append((rid, entries1[rid]))
            elif in2:
                new.append((rid, entries2[rid]))

        return matches, missing, new

    def print_comparison_report(self, matches: List, missing: List, new: List,
                                 label1: str, label2: str):
        """打印对比报告"""
        print(f"\n{'='*60}")
        print(f"  对比结果: {label1} vs {label2}")
        print(f"{'='*60}")

        print(f"\n✅ 匹配元素: {len(matches)} 个")
        for rid, status, e1, e2 in matches:
            if status == 'diff':
                print(f"  ⚠️  {rid}: 差异")
                print(f"      {label1}: clickable={e1['clickable']}, text='{e1['text']}'")
                print(f"      {label2}: clickable={e2['clickable']}, text='{e2['text']}'")
            else:
                print(f"  ✓ {rid}")

        print(f"\n❌ {label1}缺失: {len(missing)} 个")
        for rid, info in missing:
            print(f"  - {rid}: clickable={info['clickable']}, text='{info['text']}'")

        print(f"\n🆕 {label2}新增: {len(new)} 个")
        for rid, info in new:
            print(f"  + {rid}: clickable={info['clickable']}, text='{info['text']}'")

    def find_element_parent(self, xml_path: str, target_text: str) -> Dict:
        """查找元素的父元素"""
        with open(xml_path, 'r', encoding='utf-8') as f:
            content = f.read()

        print(f"\n查找 '{target_text}' 的父元素:")
        tiaojian_match = re.search(r'(<[^>]*text="[^"]*' + re.escape(target_text) + r'[^"]*"[^>]*>)', content)
        if tiaojian_match:
            elem = tiaojian_match.group(1)
            print(f"元素: {elem[:200]}")

            search_area = content[max(0, tiaojian_match.start()-500):tiaojian_match.start()]
            parent_match = re.search(r'(<android\.view\.View[^>]*resource-id="entry-[^"]*"[^>]*>)',
                                     search_area, re.DOTALL)
            if parent_match:
                print(f"\n父元素:")
                print(parent_match.group(1)[:300])

        return {}

    def get_page_id(self, xml_path: str) -> str:
        """获取页面的autodroid:page_id"""
        with open(xml_path, 'r', encoding='utf-8') as f:
            content = f.read()
        page_match = re.search(r'autodroid:page_id="([^"]*)"', content)
        return page_match.group(1) if page_match else 'N/A'

    def analyze_clickable_entries(self, xml_path: str, label: str = "") -> List[Dict]:
        """分析可点击的entry元素"""
        with open(xml_path, 'r', encoding='utf-8') as f:
            content = f.read()

        entries = []
        pattern = r'<node[^>]*resource-id="entry-[^"]*"[^>]*clickable="true"[^>]*>'
        for m in re.finditer(pattern, content, re.IGNORECASE):
            resource_match = re.search(r'resource-id="([^"]*)"', m.group(0))
            bounds_match = re.search(r'bounds="(\[[0-9,]+)"', m.group(0))
            text_match = re.search(r'text="([^"]*)"', m.group(0))
            index_match = re.search(r'index="([^"]*)"', m.group(0))

            if resource_match:
                entries.append({
                    'resource_id': resource_match.group(1),
                    'bounds': bounds_match.group(1) if bounds_match else '',
                    'text': text_match.group(1) if text_match else '',
                    'index': index_match.group(1) if index_match else '',
                })

        if label:
            print(f"\n{'='*60}")
            print(f"  {label} - 可点击Entry元素")
            print(f"{'='*60}")
            print(f"共 {len(entries)} 个元素:\n")
            for e in sorted(entries, key=lambda x: x['index']):
                print(f"  [{e['index']:>2}] {e['resource_id']:<15} text='{e['text']:<15}' bounds={e['bounds']}")

        return sorted(entries, key=lambda x: x['index'])


def compare_local_and_device():
    """比较本地XML和设备XML"""
    analyzer = XMLAnalyzer()

    local_path = 'd:/git/autodroid/autodroid-trader-executor/app/src/main/assets/apks/com.tdx.androidCCZQ/general/home.xml'
    device_path = 'd:/tmp/device_home.xml'

    local_entries = analyzer.parse_xml_entries(local_path, '本地 general/home.xml')
    device_entries = analyzer.parse_xml_entries(device_path, '设备 Home Page')

    matches, missing, new = analyzer.compare_entries(local_entries, device_entries,
                                                      '本地', '设备')
    analyzer.print_comparison_report(matches, missing, new, '本地', '设备')


def analyze_multiple_pages():
    """分析多个页面的entry元素"""
    analyzer = XMLAnalyzer()

    pages = [
        ('general/home.xml', 'general/home.xml'),
        ('netgrid-trading/home.xml', 'netgrid-trading/home.xml'),
    ]

    for relative_path, label in pages:
        full_path = f'd:/git/autodroid/autodroid-trader-executor/app/src/main/assets/apks/com.tdx.androidCCZQ/{relative_path}'
        if Path(full_path).exists():
            analyzer.analyze_clickable_entries(full_path, f'com.tdx.androidCCZQ/{label}')


def analyze_specific_element():
    """分析特定元素"""
    analyzer = XMLAnalyzer()

    print("\n" + "=" * 60)
    print("  分析 '条件单' 元素")
    print("=" * 60)

    xml_path = 'd:/git/autodroid/autodroid-trader-executor/app/src/main/assets/apks/com.tdx.androidCCZQ/general/home.xml'

    with open(xml_path, 'r', encoding='utf-8') as f:
        content = f.read()

    pattern = r'(<android\.view\.View[^>]*text="条件单"[^>]*>)'
    matches = re.findall(pattern, content)
    print(f"\n找到 {len(matches)} 个 '条件单' 相关元素")

    for i, match in enumerate(matches[:3]):
        print(f"\n[{i+1}] {match[:200]}")

    analyzer.find_element_parent(xml_path, '条件单')


if __name__ == "__main__":
    import sys

    if len(sys.argv) > 1:
        command = sys.argv[1]

        if command == 'compare':
            compare_local_and_device()
        elif command == 'analyze':
            analyze_multiple_pages()
        elif command == 'element':
            analyze_specific_element()
        else:
            print("用法: python xml_analyzer.py [compare|analyze|element]")
    else:
        print("=" * 60)
        print("  XML分析工具")
        print("=" * 60)
        print("\n可用命令:")
        print("  compare - 比较本地和设备XML")
        print("  analyze - 分析多个页面的entry元素")
        print("  element - 分析特定元素")
        print()

        compare_local_and_device()
