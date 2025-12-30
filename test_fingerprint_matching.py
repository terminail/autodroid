import xml.etree.ElementTree as ET
import sys
sys.path.insert(0, r"D:\git\autodroid\autodroid-trader-executor\tradescripts\tools")

from page import PageMatcher, build_page_fingerprint

# Use the latest XML dump
live_xml_path = r"D:\git\autodroid\autodroid-trader-executor\tradescripts\tools\dump-pages\tiao-jian-dan_step1_before_1767061185.xml"
offline_xml_path = r"D:\git\autodroid\autodroid-trader-executor\app\src\main\assets\apks\com.tdx.androidCCZQ\netgrid-trading\tiao-jian-dan.xml"

live_root = ET.parse(live_xml_path).getroot()
offline_root = ET.parse(offline_xml_path).getroot()

pm = PageMatcher()

# Build the page fingerprint from offline XML (this is what the app does)
with open(offline_xml_path, 'r', encoding='utf-8') as f:
    offline_xml_content = f.read()

page_fingerprint = build_page_fingerprint(offline_root, offline_xml_content)

print("=== Page fingerprint action elements ===")
for i, elem in enumerate(page_fingerprint.action_elements):
    print(f"[{i}] text='{elem.get('text', '')}', bounds='{elem.get('bounds', '')}', action='{elem.get('action', '')}'")

print("\n=== Testing element matching with fingerprint ===")
target_elem_info = None
for elem in page_fingerprint.action_elements:
    if elem.get('text') == 'd3223fb974a2add121de3c1334bd3ff0':
        target_elem_info = elem
        break

if target_elem_info:
    print(f"Found target element in fingerprint:")
    print(f"  text: {target_elem_info.get('text', '')}")
    print(f"  bounds: {target_elem_info.get('bounds', '')}")
    print(f"  resource_id: {target_elem_info.get('resource_id', '')}")
    print(f"  action: {target_elem_info.get('action', '')}")
    print()

    result, reason = pm._find_in_live_xml(live_root, target_elem_info, page_fingerprint)

    if result is not None:
        print(f"✅ Found element: {reason}")
        print(f"   text: {result.get('text', '')}")
        print(f"   bounds: {result.get('bounds', '')}")
        print(f"   class: {result.get('class', '')}")
    else:
        print(f"❌ Element not found!")
        print(f"   Reason: {reason}")
else:
    print("❌ Target element not found in fingerprint")