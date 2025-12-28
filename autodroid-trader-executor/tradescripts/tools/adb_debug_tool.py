#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ADB调试工具箱 - 统一的ADB页面分析和元素定位工具
合并了以下脚本的功能:
- adb_automation.py
- debug_adb_only.py
- analyze_bounds.py
- analyze_dump.py
- analyze_element.py
- analyze_entries.py
- find_element.py
- find_element_v2.py
- full_analysis.py
"""

import re
import time
import subprocess
import sys
from typing import Optional, Tuple, List, Dict
from pathlib import Path


class ADBDebugTool:
    """统一的ADB调试工具类"""

    DEVICE_ID = "TDCDU17905004388"

    def __init__(self, device_id: str = None):
        self.device_id = device_id or self.DEVICE_ID

    def run_adb(self, command: str, timeout: int = 30) -> Tuple[int, str, str]:
        """执行ADB命令"""
        full_cmd = f"adb -s {self.device_id} {command}"
        try:
            result = subprocess.run(
                full_cmd, shell=True, capture_output=True, text=True,
                encoding='utf-8', errors='ignore', timeout=timeout
            )
            return result.returncode, result.stdout or "", result.stderr or ""
        except subprocess.TimeoutExpired:
            return -1, "", "Command timed out"
        except Exception as e:
            return -1, "", str(e)

    def dump_page(self) -> str:
        """获取当前页面XML"""
        self.run_adb("shell uiautomator dump")
        code, stdout, stderr = self.run_adb("shell cat /sdcard/window_dump.xml")
        return stdout.strip() if code == 0 and stdout else ""

    def get_device_info(self) -> Dict:
        """获取设备信息"""
        info = {}
        code, stdout, _ = self.run_adb("devices")
        if code == 0:
            lines = stdout.strip().split('\n')
            devices = [l for l in lines[1:] if l.strip() and 'device' in l.split()[1:]]
            info['devices'] = devices

        code, stdout, _ = self.run_adb("shell getprop ro.build.version.release")
        info['android_version'] = stdout.strip()

        return info

    def analyze_bounds(self, xml_content: str, search_text: str) -> List[Dict]:
        """分析特定文本元素的边界"""
        results = []
        pattern = rf'<node[^>]*text="{re.escape(search_text)}"[^>]*>'
        for m in re.finditer(pattern, xml_content):
            bounds_match = re.search(r'bounds="(\[[0-9,]+)"', m.group(0))
            if bounds_match:
                results.append({
                    'text': search_text,
                    'bounds': bounds_match.group(1),
                    'full_element': m.group(0)[:200]
                })
        return results

    def find_element_with_priority(self, xml_content: str, resource_id: str = None,
                                    text: str = None, content_desc: str = None) -> List[Dict]:
        """按优先级查找元素"""
        results = []

        if resource_id:
            pattern = rf'<node[^>]*resource-id="{re.escape(resource_id)}"[^>]*>'
            for m in re.finditer(pattern, xml_content):
                results.append(self._parse_element(m.group(0), 'resource-id'))

        if text and not results:
            pattern = rf'<node[^>]*text="[^"]*{re.escape(text)}[^"]*"[^>]*>'
            for m in re.finditer(pattern, xml_content):
                results.append(self._parse_element(m.group(0), 'text'))

        if content_desc and not results:
            pattern = rf'<node[^>]*content-desc="[^"]*{re.escape(content_desc)}[^"]*"[^>]*>'
            for m in re.finditer(pattern, xml_content):
                results.append(self._parse_element(m.group(0), 'content-desc'))

        return results

    def _parse_element(self, node_str: str, match_type: str) -> Dict:
        """解析单个节点元素"""
        bounds_match = re.search(r'bounds="(\[[0-9,]+)"', node_str)
        resource_match = re.search(r'resource-id="([^"]*)"', node_str)
        text_match = re.search(r'text="([^"]*)"', node_str)
        index_match = re.search(r'index="([^"]*)"', node_str)
        class_match = re.search(r'class="([^"]*)"', node_str)
        clickable_match = re.search(r'clickable="([^"]*)"', node_str)

        bounds = bounds_match.group(1) if bounds_match else ''
        if bounds:
            coords = re.findall(r'\[(\d+),(\d+)\]', bounds)
            if len(coords) >= 2:
                center = ((int(coords[0][0]) + int(coords[1][0])) // 2,
                         (int(coords[0][1]) + int(coords[1][1])) // 2)
            else:
                center = (0, 0)
        else:
            center = (0, 0)

        return {
            'match_type': match_type,
            'resource_id': resource_match.group(1) if resource_match else '',
            'text': text_match.group(1) if text_match else '',
            'index': index_match.group(1) if index_match else '',
            'class': class_match.group(1) if class_match else '',
            'clickable': clickable_match.group(1) if clickable_match else '',
            'bounds': bounds,
            'center': center,
            'full_element': node_str[:200]
        }

    def find_clickable_parent(self, xml_content: str, target_text: str) -> Optional[Dict]:
        """查找可点击的父元素"""
        pattern = rf'<node[^>]*text="[^"]*{re.escape(target_text)}[^"]*"[^>]*>'
        for m in re.finditer(pattern, xml_content):
            search_start = m.start()
            for _ in range(20):
                prev_start = xml_content.rfind('<node', 0, search_start)
                if prev_start == -1:
                    break
                node_end = xml_content.find('>', prev_start)
                if node_end == -1:
                    break
                node_str = xml_content[prev_start:node_end+1]
                if 'clickable="true"' in node_str:
                    return self._parse_element(node_str, 'clickable_parent')
                search_start = prev_start
        return None

    def list_clickable_entries(self, xml_content: str) -> List[Dict]:
        """列出所有可点击的entry-*元素"""
        results = []
        pattern = r'<node[^>]*resource-id="entry-[^"]*"[^>]*clickable="true"[^>]*>'
        for m in re.finditer(pattern, xml_content, re.IGNORECASE):
            elem = self._parse_element(m.group(0), 'clickable_entry')
            results.append(elem)
        return sorted(results, key=lambda x: x['index'])

    def tap_by_resource_id(self, resource_id: str, index: int = 0) -> bool:
        """根据resource-id点击元素"""
        xml_content = self.dump_page()
        if not xml_content:
            return False

        pattern = rf'<node[^>]*resource-id="{re.escape(resource_id)}"[^>]*bounds="(\[[0-9,]+)"[^>]*>'
        matches = list(re.finditer(pattern, xml_content))

        if index < len(matches):
            bounds_match = re.search(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', matches[index].group(1))
            if bounds_match:
                x, y = (int(bounds_match.group(1)) + int(bounds_match.group(3))) // 2, \
                       (int(bounds_match.group(2)) + int(bounds_match.group(4))) // 2
                code, _, _ = self.run_adb(f"shell input tap {x} {y}")
                return code == 0
        return False

    def tap_by_bounds(self, bounds: str) -> bool:
        """根据边界坐标点击"""
        coords = re.findall(r'\[(\d+),(\d+)\]', bounds)
        if len(coords) >= 2:
            x = (int(coords[0][0]) + int(coords[1][0])) // 2
            y = (int(coords[0][1]) + int(coords[1][1])) // 2
            code, _, _ = self.run_adb(f"shell input tap {x} {y}")
            return code == 0
        return False

    def print_element_report(self, xml_content: str):
        """打印完整的元素分析报告"""
        print("\n" + "=" * 70)
        print("  页面元素分析报告")
        print("=" * 70)

        print(f"\n📊 XML长度: {len(xml_content)} 字符")

        entries = self.list_clickable_entries(xml_content)
        print(f"\n📋 可点击的Entry元素 ({len(entries)} 个):")
        for e in entries:
            print(f"   [{e['index']:>2}] {e['resource_id']:<15} text='{e['text']:<15}' bounds={e['bounds']}")

        print("\n🔍 查找 '条件单' 元素:")
        tiaojian = self.analyze_bounds(xml_content, "条件单")
        if tiaojian:
            for t in tiaojian:
                print(f"   找到: bounds={t['bounds']}")
                parent = self.find_clickable_parent(xml_content, "条件单")
                if parent:
                    print(f"   父元素: resource-id='{parent['resource_id']}' bounds={parent['bounds']}")
        else:
            print("   未找到")

        print("\n📍 页面中所有文本元素:")
        all_texts = re.findall(r'text="([^"]*)"[^>]*resource-id="([^"]*)"', xml_content)
        seen = set()
        for text, rid in all_texts:
            if text and text not in seen and len(text) > 1:
                print(f"   '{text}' -> {rid}")
                seen.add(text)
                if len(seen) >= 15:
                    break


def main():
    tool = ADBDebugTool()

    print("=" * 70)
    print("  ADB调试工具箱 - 统一调试脚本")
    print("=" * 70)

    device_info = tool.get_device_info()
    print(f"\n设备信息: {device_info}")

    xml_content = tool.dump_page()
    if not xml_content:
        print("❌ 无法获取页面XML")
        sys.exit(1)

    tool.print_element_report(xml_content)

    print("\n" + "=" * 70)
    print("  测试: 点击 'entry-xzsg' 元素")
    print("=" * 70)

    entries = tool.list_clickable_entries(xml_content)
    target = next((e for e in entries if 'xzsg' in e['resource_id']), None)

    if target:
        print(f"找到目标: {target['resource_id']} center={target['center']}")
        print("等待2秒后点击...")
        time.sleep(2)
        if tool.tap_by_resource_id(target['resource_id']):
            print("✅ 点击成功")
            time.sleep(3)
            new_xml = tool.dump_page()
            if new_xml and new_xml != xml_content:
                print("✅ 页面已变化")
            else:
                print("❌ 页面无变化")
        else:
            print("❌ 点击失败")
    else:
        print("❌ 未找到 entry-xzsg 元素")

    print("\n" + "=" * 70)
    print("  调试完成")
    print("=" * 70)


if __name__ == "__main__":
    main()
