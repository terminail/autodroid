#!/usr/bin/env python3
import xml.etree.ElementTree as ET
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'autodroid-trader-executor', 'tradescripts', 'tools'))

from page import PageMatcher

def test_element_matching():
    live_xml_path = r'd:\git\autodroid\autodroid-trader-executor\tradescripts\tools\dump-pages\after_home_1767060699.xml'
    offline_xml_path = r'd:\git\autodroid\autodroid-trader-executor\app\src\main\assets\apks\com.tdx.androidCCZQ\netgrid-trading\tiao-jian-dan.xml'
    
    print(f"Loading live XML from: {live_xml_path}")
    print(f"Loading offline XML from: {offline_xml_path}")
    
    live_tree = ET.parse(live_xml_path)
    live_root = live_tree.getroot()
    
    offline_tree = ET.parse(offline_xml_path)
    offline_root = offline_tree.getroot()
    
    page_matcher = PageMatcher()
    
    print("\nSearching for element with text='d3223fb974a2add121de3c1334bd3ff0'...")
    
    for elem in offline_root.iter():
        if elem.tag == "hierarchy":
            continue
        text = elem.get("text", "").strip()
        if text == "d3223fb974a2add121de3c1334bd3ff0":
            bounds = elem.get("bounds", "").strip()
            print(f"Found element in offline XML:")
            print(f"  Text: {text}")
            print(f"  Bounds: {bounds}")
            print(f"  Resource-id: {elem.get('resource-id', '')}")
            print(f"  Class: {elem.get('class', '')}")
            
            offline_elem = {
                "text": text,
                "bounds": bounds,
                "resource_id": elem.get("resource-id", ""),
                "class": elem.get("class", "")
            }
            
            found_elem, method = page_matcher._find_in_live_xml(live_root, offline_elem)
            
            if found_elem is not None:
                print(f"\n✓ Element found in live XML using method: {method}")
                print(f"  Found text: {found_elem.get('text', '')}")
                print(f"  Found bounds: {found_elem.get('bounds', '')}")
                return True
            else:
                print(f"\n✗ Element NOT found in live XML")
                
                print("\nDebugging: Checking live XML for matching bounds...")
                for live_elem in live_root.iter():
                    if live_elem.tag == "hierarchy":
                        continue
                    live_bounds = live_elem.get("bounds", "").strip()
                    live_text = live_elem.get("text", "").strip()
                    if live_bounds == bounds:
                        print(f"  Found element with matching bounds:")
                        print(f"    Bounds: {live_bounds}")
                        print(f"    Text: {live_text}")
                        print(f"    Resource-id: {live_elem.get('resource-id', '')}")
                return False
    
    print("\n✗ Element not found in offline XML")
    return False

if __name__ == "__main__":
    success = test_element_matching()
    sys.exit(0 if success else 1)
