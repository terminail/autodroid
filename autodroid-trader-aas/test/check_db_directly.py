#!/usr/bin/env python3
"""
Script to check the AAS database directly on the device using ADB commands
"""
import subprocess
import tempfile
import os

def run_adb_command(command):
    """Execute an ADB command and return the output"""
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    return result.returncode, result.stdout, result.stderr

def check_database_tables():
    """Check what tables exist and their row counts"""
    print("Checking database tables...")
    
    # Create a temporary file to store the database dump
    with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.sql') as temp_file:
        temp_filename = temp_file.name

    try:
        # Get the database schema and data
        cmd = f'adb shell "run-as com.autodroid.aas sqlite3 /data/data/com.autodroid.aas/databases/ui_recorder.db .dump" > {temp_filename}'
        returncode, stdout, stderr = run_adb_command(cmd)
        
        if returncode == 0:
            # Read the dumped data
            with open(temp_filename, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
                
            print("Database dump retrieved successfully!")
            
            # Extract table names
            import re
            table_names = re.findall(r'CREATE TABLE `?(\w+)`?', content)
            print(f"Tables found: {table_names}")
            
            # Look for data in ui_events table
            ui_events_data = re.findall(r'INSERT INTO ui_events.*?;', content, re.DOTALL)
            print(f"UI Events count: {len(ui_events_data)}")
            
            # Look for data in element_features table  
            element_features_data = re.findall(r'INSERT INTO element_features.*?;', content, re.DOTALL)
            print(f"Element Features count: {len(element_features_data)}")
            
            # Show sample of UI events if any exist
            if ui_events_data:
                print("\nSample UI Events:")
                for i, event in enumerate(ui_events_data[:3]):  # Show first 3 events
                    print(f"  {i+1}: {event[:200]}...")
                    
            # Show sample of element features
            if element_features_data:
                print("\nSample Element Features:")
                for i, feature in enumerate(element_features_data[:3]):  # Show first 3 features
                    print(f"  {i+1}: {feature[:200]}...")
                    
            return len(ui_events_data), len(element_features_data)
        else:
            print(f"Error executing command: {stderr}")
            # Try alternative approach without run-as
            print("Trying alternative approach...")
            
            # Try to check if database file exists
            cmd = 'adb shell "test -f /data/data/com.autodroid.aas/databases/ui_recorder.db && echo EXISTS || echo NOT_EXISTS"'
            returncode, stdout, stderr = run_adb_command(cmd)
            if "EXISTS" in stdout:
                print("Database file exists on device")
            else:
                print("Database file does not exist or is not accessible")
                
            return 0, 0
            
    except Exception as e:
        print(f"Error checking database: {e}")
        return 0, 0
    finally:
        # Clean up temp file
        if os.path.exists(temp_filename):
            os.unlink(temp_filename)

if __name__ == "__main__":
    print("Direct database check for AAS...")
    ui_events_count, element_features_count = check_database_tables()
    print(f"\nSummary: {ui_events_count} UI events, {element_features_count} element features")