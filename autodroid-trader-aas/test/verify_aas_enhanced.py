#!/usr/bin/env python3
"""
Enhanced script to verify autodroid-trader-aas functionality by:
1. Dumping UI pages and analyzing elements
2. Clicking/inputting different types of elements
3. Verifying collected data in database tables: ui_events, element_features
"""

import os
import subprocess
import time
import json
import xml.etree.ElementTree as ET
import sys
from pathlib import Path


def run_adb_command(command):
    """Execute an ADB command and return the output"""
    try:
        result = subprocess.run(command, shell=True, capture_output=True, text=True, timeout=30)
        return result.returncode, result.stdout, result.stderr
    except subprocess.TimeoutExpired:
        return -1, "", "Command timed out"


def dump_ui_page(dump_file_path):
    """Dump the current UI page to a file"""
    # First try the standard location
    cmd1 = "adb shell uiautomator dump /sdcard/window_dump.xml"
    returncode1, stdout1, stderr1 = run_adb_command(cmd1)
    
    if returncode1 == 0:
        # Pull the dump file
        cmd_pull = f"adb pull /sdcard/window_dump.xml {dump_file_path}"
        returncode_pull, stdout_pull, stderr_pull = run_adb_command(cmd_pull)
        if returncode_pull == 0:
            print(f"UI page dumped to {dump_file_path}")
            return True
    else:
        # Try alternative location
        cmd2 = "adb shell uiautomator dump /data/local/tmp/window_dump.xml"
        returncode2, stdout2, stderr2 = run_adb_command(cmd2)
        
        if returncode2 == 0:
            cmd_pull = f"adb pull /data/local/tmp/window_dump.xml {dump_file_path}"
            returncode_pull, stdout_pull, stderr_pull = run_adb_command(cmd_pull)
            if returncode_pull == 0:
                print(f"UI page dumped to {dump_file_path}")
                return True
    
    print(f"Failed to dump UI page: {stderr1}")
    return False


def parse_ui_elements(dump_file_path):
    """Parse UI elements from the dump file"""
    try:
        tree = ET.parse(dump_file_path)
        root = tree.getroot()
        
        elements = []
        for element in root.iter():
            if element.tag == 'node':
                elem_data = {
                    'resource_id': element.get('resource-id', ''),
                    'class': element.get('class', ''),
                    'text': element.get('text', ''),
                    'content_desc': element.get('content-desc', ''),
                    'bounds': element.get('bounds', ''),
                    'clickable': element.get('clickable', 'false'),
                    'long_clickable': element.get('long-clickable', 'false'),
                    'focusable': element.get('focusable', 'false'),
                    'checkable': element.get('checkable', 'false'),
                    'enabled': element.get('enabled', 'true'),
                    'selected': element.get('selected', 'false'),
                    'package': element.get('package', ''),
                    'content_desc': element.get('content-desc', ''),
                }
                
                # Calculate center coordinates for clicking
                bounds = elem_data['bounds']
                if bounds:
                    # Parse bounds string like [x,y][x,y]
                    import re
                    coords = re.findall(r'\d+', bounds)
                    if len(coords) == 4:
                        x1, y1, x2, y2 = map(int, coords)
                        center_x = (x1 + x2) // 2
                        center_y = (y1 + y2) // 2
                        elem_data['center_x'] = center_x
                        elem_data['center_y'] = center_y
                
                elements.append(elem_data)
        
        print(f"Found {len(elements)} UI elements in the dump")
        return elements
    except Exception as e:
        print(f"Error parsing UI elements: {e}")
        return []


def filter_clickable_elements(elements):
    """Filter elements that are clickable or interactable"""
    clickable_elements = []
    
    for elem in elements:
        # Check if element is clickable, long-clickable, checkable, or focusable
        is_clickable = elem.get('clickable', 'false') == 'true'
        is_long_clickable = elem.get('long_clickable', 'false') == 'true'
        is_checkable = elem.get('checkable', 'false') == 'true'
        is_focusable = elem.get('focusable', 'false') == 'true'
        has_text = bool(elem.get('text', '').strip())
        has_resource_id = bool(elem.get('resource_id', '').strip())
        
        if (is_clickable or is_long_clickable or is_checkable or is_focusable or 
            has_text or has_resource_id) and 'center_x' in elem:
            clickable_elements.append(elem)
    
    print(f"Found {len(clickable_elements)} clickable/interactable elements")
    return clickable_elements


def is_app_installed(package_name):
    """Check if the app is installed on the device"""
    cmd = f"adb shell pm list packages | grep {package_name}"
    returncode, stdout, stderr = run_adb_command(cmd)
    return returncode == 0 and package_name in stdout


def start_app(package_name):
    """Start the specified app on the device"""
    cmd = f"adb shell monkey -p {package_name} -c android.intent.category.LAUNCHER 1"
    returncode, stdout, stderr = run_adb_command(cmd)
    
    if returncode == 0:
        print(f"Successfully launched {package_name}")
        return True
    else:
        print(f"Failed to launch {package_name}: {stderr}")
        return False


def is_aas_service_enabled():
    """Check if the AAS service is enabled in accessibility settings"""
    cmd = "adb shell settings get secure enabled_accessibility_services"
    returncode, stdout, stderr = run_adb_command(cmd)
    
    if returncode == 0 and "com.autodroid.aas/com.autodroid.aas.service.UIRecorderAccessibilityService" in stdout:
        print("AAS service is enabled")
        return True
    else:
        print("AAS service is NOT enabled. Please enable it in Accessibility settings.")
        return False


def get_table_counts():
    """Get the number of records in ui_events and element_features tables"""
    # Copy the database file locally to inspect it
    cmd = 'adb shell "run-as com.autodroid.aas cat /data/data/com.autodroid.aas/databases/ui_recorder.db" > temp_ui_recorder.db'
    returncode, stdout, stderr = run_adb_command(cmd)
    
    if returncode != 0:
        print("Could not copy database file")
        return None, None
    
    try:
        import sqlite3
        conn = sqlite3.connect('temp_ui_recorder.db')
        cursor = conn.cursor()
        
        # Count records in ui_events table
        try:
            cursor.execute("SELECT COUNT(*) FROM ui_events;")
            ui_events_count = cursor.fetchone()[0]
        except sqlite3.OperationalError:
            ui_events_count = 0
            
        # Count records in element_features table
        try:
            cursor.execute("SELECT COUNT(*) FROM element_features;")
            element_features_count = cursor.fetchone()[0]
        except sqlite3.OperationalError:
            element_features_count = 0
        
        conn.close()
        
        # Clean up temp file
        if os.path.exists('temp_ui_recorder.db'):
            os.remove('temp_ui_recorder.db')
        
        return ui_events_count, element_features_count
    except Exception as e:
        print(f"Error accessing database: {e}")
        # Clean up temp file if it exists
        if os.path.exists('temp_ui_recorder.db'):
            os.remove('temp_ui_recorder.db')
        return None, None


def interact_with_elements(clickable_elements, max_interactions=5):
    """Interact with the clickable elements to generate AAS events"""
    interactions_performed = 0
    
    for i, elem in enumerate(clickable_elements):
        if interactions_performed >= max_interactions:
            break
            
        # Get the element coordinates
        center_x = elem.get('center_x')
        center_y = elem.get('center_y')
        
        if center_x and center_y:
            print(f"Interacting with element {i+1}: {elem.get('text', 'No text')} at ({center_x}, {center_y})")
            
            # Click the element
            cmd = f"adb shell input tap {center_x} {center_y}"
            run_adb_command(cmd)
            
            # Wait a bit for the interaction to register
            time.sleep(1.5)
            
            # After some clicks, try other interactions
            if interactions_performed % 3 == 2:
                # Try text input if we find an input field
                if 'EditText' in elem.get('class', ''):
                    print("  Found input field, entering text...")
                    run_adb_command("adb shell input text 'test123'")
                    time.sleep(1)
                else:
                    # Perform a long press
                    print("  Performing long press...")
                    cmd_long_press = f"adb shell input swipe {center_x} {center_y} {center_x} {center_y} 1000"
                    run_adb_command(cmd_long_press)
                    time.sleep(1)
            
            interactions_performed += 1
        else:
            print(f"Skipping element {i+1} - no coordinates available")
    
    print(f"Performed {interactions_performed} interactions with UI elements")


def check_dump_exists(dump_name):
    """Check if a dump with the given name already exists"""
    dump_path = os.path.join("dumps", f"{dump_name}.xml")
    return os.path.exists(dump_path)


def main():
    print("Enhanced AAS verification: Dumping pages, analyzing elements, and verifying database...")
    
    # Check if AAS service is enabled
    if not is_aas_service_enabled():
        print("CRITICAL: AAS service is not enabled. Please enable it in Accessibility settings.")
        return False
    
    # Check if target app is installed
    target_package = "com.tdx.androidCCZQ"
    if not is_app_installed(target_package):
        print(f"CRITICAL: Target app {target_package} is not installed on the device.")
        return False
    
    print(f"{target_package} is installed on the device.")
    
    # Get initial database counts
    print("Getting initial database state...")
    initial_events, initial_features = get_table_counts()
    if initial_events is not None and initial_features is not None:
        print(f"Initial UI events: {initial_events}, Element features: {initial_features}")
    else:
        print("Could not get initial database counts")
        initial_events, initial_features = 0, 0
    
    # Start the app
    print(f"Starting {target_package}...")
    if not start_app(target_package):
        print(f"FAILED: Could not start {target_package}")
        return False
    
    # Wait for app to load
    time.sleep(5)
    
    # Dump the initial page
    dump_name = "initial_page"
    dump_path = os.path.join("dumps", f"{dump_name}.xml")
    
    if not check_dump_exists(dump_name):
        print("Dumping initial page...")
        if dump_ui_page(dump_path):
            print("Initial page dumped successfully")
        else:
            print("Failed to dump initial page")
            return False
    else:
        print(f"Initial page dump already exists: {dump_path}")
    
    # Parse the UI elements from the dump
    elements = parse_ui_elements(dump_path)
    if not elements:
        print("No UI elements found in the dump")
        return False
    
    # Filter clickable elements
    clickable_elements = filter_clickable_elements(elements)
    if not clickable_elements:
        print("No clickable elements found in the dump")
        # Still try some general interactions
        print("Performing general interactions...")
        for i in range(3):
            # Tap at different screen positions
            x = 300 + i * 200  # Spread across screen width
            y = 500 + i * 200  # Spread down the screen
            print(f"Tapping at ({x}, {y})")
            run_adb_command(f"adb shell input tap {x} {y}")
            time.sleep(1.5)
    else:
        # Interact with the clickable elements to generate AAS events
        print("Interacting with clickable elements to generate AAS events...")
        interact_with_elements(clickable_elements, max_interactions=8)
    
    # Wait for events to be processed
    print("Waiting for events to be processed by AAS...")
    time.sleep(5)
    
    # Dump the page again after interactions
    dump_name_after = "after_interactions"
    dump_path_after = os.path.join("dumps", f"{dump_name_after}.xml")
    
    if not check_dump_exists(dump_name_after):
        print("Dumping page after interactions...")
        if dump_ui_page(dump_path_after):
            print("Page after interactions dumped successfully")
        else:
            print("Failed to dump page after interactions")
            # Continue anyway to check database
    else:
        print(f"Page after interactions dump already exists: {dump_path_after}")
    
    # Get final database counts
    final_events, final_features = get_table_counts()
    if final_events is not None and final_features is not None:
        print(f"Final UI events: {final_events}, Element features: {final_features}")
    else:
        print("Could not get final database counts")
        final_events, final_features = 0, 0
    
    # Analyze results
    events_added = final_events - initial_events
    features_added = final_features - initial_features
    
    print(f"\n--- RESULTS ---")
    print(f"Events captured: {events_added}")
    print(f"Features extracted: {features_added}")
    
    success = True
    if events_added <= 0:
        print("WARNING: No new UI events were captured during the test.")
        success = False
    else:
        print(f"✓ Captured {events_added} new UI events")
    
    if features_added <= 0:
        print("WARNING: No new element features were extracted during the test.")
        success = False
    else:
        print(f"✓ Extracted {features_added} new element features")
    
    if success:
        print("\n✓ SUCCESS: AAS service is properly collecting element features!")
        print("  - UI pages can be dumped and analyzed")
        print("  - Different types of elements can be identified")
        print("  - Interactions generate UI events")
        print("  - Element features are extracted and stored in database")
        return True
    else:
        print("\n⚠ PARTIAL SUCCESS: Some aspects are working, but issues detected.")
        print(f"  - {events_added} events captured, {features_added} features extracted")
        return False


if __name__ == "__main__":
    success = main()
    if success:
        print("\nEnhanced verification completed successfully!")
        sys.exit(0)
    else:
        print("\nEnhanced verification completed with warnings!")
        sys.exit(1)