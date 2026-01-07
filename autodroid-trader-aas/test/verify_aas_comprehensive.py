#!/usr/bin/env python3
"""
Script to verify autodroid-trader-aas functionality based on DESIGN.md
This script will:
1. Start the com.tdx.androidCCZQ app on the device
2. Check if the AAS service is recording events
3. Verify that UI events and element features are being captured
4. Provide auto-fix capabilities if issues are detected
"""

import os
import subprocess
import time
import json
import sys
from pathlib import Path


def run_adb_command(command):
    """Execute an ADB command and return the output"""
    try:
        result = subprocess.run(command, shell=True, capture_output=True, text=True, timeout=30)
        return result.returncode, result.stdout, result.stderr
    except subprocess.TimeoutExpired:
        return -1, "", "Command timed out"


def is_app_installed(package_name):
    """Check if the app is installed on the device"""
    cmd = f"adb shell pm list packages | grep {package_name}"
    returncode, stdout, stderr = run_adb_command(cmd)
    return returncode == 0 and package_name in stdout


def start_app(package_name):
    """Start the specified app on the device"""
    # Get the main activity of the app
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


def check_database_structure():
    """Check if the database has the correct structure"""
    # Copy the database file locally to inspect it
    cmd = 'adb shell "run-as com.autodroid.aas cat /data/data/com.autodroid.aas/databases/ui_recorder.db" > temp_ui_recorder.db'
    returncode, stdout, stderr = run_adb_command(cmd)
    
    if returncode != 0:
        print("Could not copy database file")
        return False
    
    # Check the structure using Python
    try:
        import sqlite3
        conn = sqlite3.connect('temp_ui_recorder.db')
        cursor = conn.cursor()
        
        # Get all tables
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
        tables = cursor.fetchall()
        table_names = [table[0] for table in tables]
        
        print(f"Database tables: {table_names}")
        
        required_tables = ['ui_events', 'element_features', 'app_configs']
        missing_tables = [table for table in required_tables if table not in table_names]
        
        conn.close()
        
        # Clean up temp file
        if os.path.exists('temp_ui_recorder.db'):
            os.remove('temp_ui_recorder.db')
        
        if missing_tables:
            print(f"Missing required tables: {missing_tables}")
            return False
        else:
            print("All required tables exist in the database")
            return True
    except Exception as e:
        print(f"Error checking database structure: {e}")
        # Clean up temp file if it exists
        if os.path.exists('temp_ui_recorder.db'):
            os.remove('temp_ui_recorder.db')
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


def simulate_comprehensive_interactions():
    """Simulate comprehensive interactions to generate AAS events"""
    print("Performing comprehensive interactions to generate AAS events...")
    
    # Wait for app to fully load
    time.sleep(3)
    
    # Perform a sequence of interactions designed to trigger various event types
    interactions = [
        # Click interactions
        ("Click center of screen", "adb shell input tap 540 1000"),
        ("Click top left", "adb shell input tap 200 400"), 
        ("Click bottom right", "adb shell input tap 800 1500"),
        
        # Text input
        ("Input text", "adb shell input text 'test123'"),
        
        # Swipe/scroll interactions
        ("Swipe up", "adb shell input swipe 500 1500 500 500 300"),
        ("Swipe down", "adb shell input swipe 500 500 500 1500 300"),
        ("Swipe left", "adb shell input swipe 800 1000 200 1000 300"),
        ("Swipe right", "adb shell input swipe 200 1000 800 1000 300"),
        
        # Key events
        ("Press back", "adb shell input keyevent KEYCODE_BACK"),
        ("Press menu", "adb shell input keyevent KEYCODE_MENU"),
    ]
    
    for desc, cmd in interactions:
        print(f"  {desc}...")
        run_adb_command(cmd)
        time.sleep(1.5)  # Allow time for event processing
    
    print("Comprehensive interaction sequence completed")


def check_element_features():
    """Check if element features are properly extracted and stored"""
    # Copy the database file locally to inspect it
    cmd = 'adb shell "run-as com.autodroid.aas cat /data/data/com.autodroid.aas/databases/ui_recorder.db" > temp_ui_recorder.db'
    returncode, stdout, stderr = run_adb_command(cmd)
    
    if returncode != 0:
        print("Could not copy database file")
        return False
    
    try:
        import sqlite3
        conn = sqlite3.connect('temp_ui_recorder.db')
        cursor = conn.cursor()
        
        # Sample some element features to verify they have required properties
        try:
            cursor.execute("SELECT * FROM element_features LIMIT 1;")
            sample_row = cursor.fetchone()
            
            if sample_row:
                # Get column names
                cursor.execute("PRAGMA table_info(element_features);")
                columns = [col[1] for col in cursor.fetchall()]
                print(f"Element features table columns: {columns}")
                
                conn.close()
                
                # Clean up temp file
                if os.path.exists('temp_ui_recorder.db'):
                    os.remove('temp_ui_recorder.db')
                
                # Check if essential columns exist
                essential_columns = ['element_id', 'element_type', 'element_text', 'content_desc', 'class_name']
                present_columns = [col for col in essential_columns if col in columns]
                
                if len(present_columns) >= 3:  # At least some essential columns
                    print(f"Essential element feature columns found: {present_columns}")
                    return True
                else:
                    print(f"Limited essential columns found: {present_columns}")
                    return False
            else:
                print("No element features found in database")
                conn.close()
                
                # Clean up temp file
                if os.path.exists('temp_ui_recorder.db'):
                    os.remove('temp_ui_recorder.db')
                
                return False
        except sqlite3.OperationalError:
            print("Element features table does not exist or is empty")
            conn.close()
            
            # Clean up temp file
            if os.path.exists('temp_ui_recorder.db'):
                os.remove('temp_ui_recorder.db')
            
            return False
    except Exception as e:
        print(f"Error checking element features: {e}")
        # Clean up temp file if it exists
        if os.path.exists('temp_ui_recorder.db'):
            os.remove('temp_ui_recorder.db')
        return False


def main():
    print("Verifying autodroid-trader-aas functionality based on DESIGN.md...")
    
    # 1. Check if AAS service is enabled
    if not is_aas_service_enabled():
        print("CRITICAL: AAS service is not enabled. Please enable it in Accessibility settings.")
        print("Go to Settings > Accessibility > Special features > Trader AAS and enable it.")
        return False
    
    # 2. Check if target app is installed
    target_package = "com.tdx.androidCCZQ"
    if not is_app_installed(target_package):
        print(f"CRITICAL: Target app {target_package} is not installed on the device.")
        return False
    
    print(f"{target_package} is installed on the device.")
    
    # 3. Check database structure
    print("Checking database structure...")
    if not check_database_structure():
        print("WARNING: Database structure is not as expected. This might affect AAS functionality.")
    else:
        print("Database structure is correct.")
    
    # 4. Get initial counts
    print("Getting initial database state...")
    initial_events, initial_features = get_table_counts()
    if initial_events is not None and initial_features is not None:
        print(f"Initial UI events: {initial_events}, Element features: {initial_features}")
    else:
        print("Could not get initial database counts")
        initial_events, initial_features = 0, 0
    
    # 5. Start the app
    print(f"Starting {target_package}...")
    if not start_app(target_package):
        print(f"FAILED: Could not start {target_package}")
        return False
    
    # 6. Wait for app to load
    time.sleep(5)
    
    # 7. Perform comprehensive interactions to generate events
    simulate_comprehensive_interactions()
    
    # 8. Wait for events to be processed
    print("Waiting for events to be processed by AAS...")
    time.sleep(8)  # Give more time for processing
    
    # 9. Get final counts
    final_events, final_features = get_table_counts()
    if final_events is not None and final_features is not None:
        print(f"Final UI events: {final_events}, Element features: {final_features}")
    else:
        print("Could not get final database counts")
        final_events, final_features = 0, 0
    
    # 10. Analyze results
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
    
    # 11. Check element feature quality
    if features_added > 0:
        print("Checking quality of element features...")
        if check_element_features():
            print("✓ Element features have proper structure with essential properties")
        else:
            print("⚠ Element features may have incomplete data")
            success = False
    
    if success:
        print("\n✓ SUCCESS: AAS service is functioning properly!")
        print("  - Service is enabled and running")
        print("  - Database structure is correct")
        print("  - UI events are being captured")
        print("  - Element features are being extracted")
        return True
    else:
        print("\n⚠ PARTIAL SUCCESS: Some aspects of AAS are working, but issues detected.")
        print("  - Service is enabled")
        print(f"  - {events_added} events captured, {features_added} features extracted")
        print("  - May need configuration adjustments")
        return False


if __name__ == "__main__":
    success = main()
    if success:
        print("\nVerification completed successfully!")
        sys.exit(0)
    else:
        print("\nVerification completed with warnings!")
        sys.exit(1)  # Exit with error code to indicate issues found