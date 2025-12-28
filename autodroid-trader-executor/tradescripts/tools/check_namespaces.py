#!/usr/bin/env python3
"""Check all XML files for namespace declarations and autodroid attributes."""

import os
from pathlib import Path
import xml.etree.ElementTree as ET

AUTODROID_NS = "https://autodroid.example.com"
AUTODROID_ACTION = f"{{{AUTODROID_NS}}}action"

xml_dir = r"d:\git\autodroid\autodroid-trader-executor\app\src\main\assets\apks\com.tdx.androidCCZQ\general"

print(f"Checking XML files in: {xml_dir}\n")

xml_files = sorted(Path(xml_dir).glob("*.xml"))
print(f"Found {len(xml_files)} XML files\n")

print("=" * 70)
print(f"{'File':<30} {'Namespace':<12} {'Actions':<10} {'Details'}")
print("=" * 70)

files_with_namespace = []
files_without_namespace = []
files_with_actions = []

for xml_file in xml_files:
    try:
        with open(xml_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        root = ET.fromstring(content)
        
        has_namespace = 'xmlns:autodroid="https://autodroid.example.com"' in content
        
        action_count = 0
        action_details = []
        for elem in root.iter():
            if elem.tag == "hierarchy":
                continue
            action = elem.get(AUTODROID_ACTION)
            if action:
                action_count += 1
                text = elem.get("text", "")
                action_details.append(f"'{text}': {action}")
        
        status = "✓" if has_namespace else "✗"
        if has_namespace:
            files_with_namespace.append(xml_file.name)
        else:
            files_without_namespace.append(xml_file.name)
        
        if action_count > 0:
            files_with_actions.append(xml_file.name)
        
        details = ", ".join(action_details[:2]) if action_details else "-"
        print(f"{xml_file.name:<30} {status:<12} {action_count:<10} {details}")
        
    except ET.ParseError as e:
        print(f"{xml_file.name:<30} {'?':<12} {'?':<10} Parse error: {e}")
    except Exception as e:
        print(f"{xml_file.name:<30} {'?':<12} {'?':<10} Error: {e}")

print("=" * 70)

print(f"\n📊 Summary:")
print(f"   Total XML files: {len(xml_files)}")
print(f"   Files with namespace: {len(files_with_namespace)}")
print(f"   Files without namespace: {len(files_without_namespace)}")
print(f"   Files with autodroid:action: {len(files_with_actions)}")

if files_without_namespace:
    print(f"\n⚠️ Files without namespace declaration:")
    for name in sorted(files_without_namespace):
        print(f"   - {name}")

if files_with_actions and files_without_namespace:
    print(f"\n🔴 CRITICAL: Files with actions but no namespace:")
    for name in sorted(files_without_namespace):
        if name in files_with_actions:
            print(f"   - {name}")

print("\nDone!")
