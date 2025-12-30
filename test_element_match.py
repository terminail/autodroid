import xml.etree.ElementTree as ET

live_xml_path = r"D:\git\autodroid\autodroid-trader-executor\tradescripts\tools\dump-pages\after_home_1767059248.xml"
offline_xml_path = r"D:\git\autodroid\autodroid-trader-executor\app\src\main\assets\apks\com.tdx.androidCCZQ\netgrid-trading\tiao-jian-dan.xml"

live_root = ET.parse(live_xml_path).getroot()
offline_root = ET.parse(offline_xml_path).getroot()

target_text = "d3223fb974a2add121de3c1334bd3ff0"
target_bounds = "[375,1809][702,1812]"

print("=== LIVE XML ===")
for elem in live_root.iter():
    if elem.tag == "hierarchy":
        continue
    text = elem.get("text", "").strip()
    bounds = elem.get("bounds", "").strip()
    if text == target_text:
        print(f"Found element with text '{text}':")
        print(f"  bounds: {bounds}")
        print(f"  resource-id: {elem.get('resource-id', '')}")
        print(f"  class: {elem.get('class', '')}")
        print(f"  visible-to-user: {elem.get('visible-to-user', 'not set')}")
        print(f"  All attributes: {elem.attrib}")
        print()

print("=== OFFLINE XML ===")
for elem in offline_root.iter():
    if elem.tag == "hierarchy":
        continue
    text = elem.get("text", "").strip()
    bounds = elem.get("bounds", "").strip()
    if text == target_text:
        print(f"Found element with text '{text}':")
        print(f"  bounds: {bounds}")
        print(f"  resource-id: {elem.get('resource-id', '')}")
        print(f"  class: {elem.get('class', '')}")
        print(f"  All attributes: {elem.attrib}")
        print()

print("=== CHECKING BOUNDS MATCH ===")
live_bounds_match = False
offline_bounds_match = False

for elem in live_root.iter():
    if elem.tag == "hierarchy":
        continue
    bounds = elem.get("bounds", "").strip()
    if bounds == target_bounds:
        print(f"LIVE: Found element with bounds {target_bounds}")
        print(f"  text: {elem.get('text', '')}")
        live_bounds_match = True

for elem in offline_root.iter():
    if elem.tag == "hierarchy":
        continue
    bounds = elem.get("bounds", "").strip()
    if bounds == target_bounds:
        print(f"OFFLINE: Found element with bounds {target_bounds}")
        print(f"  text: {elem.get('text', '')}")
        offline_bounds_match = True

print(f"\nLive bounds match: {live_bounds_match}")
print(f"Offline bounds match: {offline_bounds_match}")
