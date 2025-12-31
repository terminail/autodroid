import xml.etree.ElementTree as ET
import sys
sys.path.insert(0, r"D:\git\autodroid\autodroid-trader-executor\tradescripts\tools")

from page import PageMatcher

live_xml_path = r"D:\git\autodroid\autodroid-trader-executor\tradescripts\tools\dump-pages\after_home_1767059248.xml"
offline_xml_path = r"D:\git\autodroid\autodroid-trader-executor\app\src\main\assets\apks\com.tdx.androidCCZQ\netgrid-trading\tiao-jian-dan.xml"

live_root = ET.parse(live_xml_path).getroot()
offline_root = ET.parse(offline_xml_path).getroot()

pm = PageMatcher()

target_text = "d3223fb974a2add121de3c1334bd3ff0"
target_bounds = "[375,1809][702,1812]"

elem_info = {
    "resource_id": "",
    "text": target_text,
    "content_desc": "",
    "class": "android.widget.Image",
    "children": [],
    "action": "click",
    "step": None,
    "name": None,
    "value": None,
    "save_to": None,
    "desc": None,
    "id": None,
    "index": 0,
    "bounds": target_bounds,
}

print("=== Testing element matching ===")
print(f"Looking for element with text='{target_text}', bounds='{target_bounds}'")
print()

result, reason = pm._find_in_live_xml(live_root, elem_info)

if result is not None:
    print(f"✅ Found element: {reason}")
    print(f"   text: {result.get('text', '')}")
    print(f"   bounds: {result.get('bounds', '')}")
    print(f"   class: {result.get('class', '')}")
else:
    print(f"❌ Element not found!")
    print(f"   Reason: {reason}")

print()
print("=== Manual verification ===")
for elem in live_root.iter():
    if elem.tag == "hierarchy":
        continue
    text = elem.get("text", "").strip()
    bounds = elem.get("bounds", "").strip()
    if text == target_text:
        print(f"✓ Found element with text '{text}' and bounds '{bounds}'")
