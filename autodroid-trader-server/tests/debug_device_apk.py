#!/usr/bin/env python3
"""
Debug script to diagnose APK registration issues for specific device
"""

import requests
import json
import sys

# Server configuration
BASE_URL = "http://192.168.1.59:8004"
DEVICE_UDID = "c96ebeecb0a69a20"

def debug_device_apk_issue():
    """Debug APK registration issue for specific device"""
    
    print("=" * 70)
    print("DEBUG: Device APK Registration Issue")
    print("=" * 70)
    
    # Step 1: Check device registration
    print(f"\n1. Checking device registration for UDID: {DEVICE_UDID}")
    try:
        response = requests.get(f"{BASE_URL}/api/devices/{DEVICE_UDID}")
        if response.status_code == 200:
            device_info = response.json()
            print(f"✅ Device found: {device_info.get('device_name', 'Unknown')}")
            print(f"   APK count: {len(device_info.get('apks', {}))}")
        else:
            print(f"❌ Device not found: {response.status_code}")
            return
    except Exception as e:
        print(f"❌ Error checking device: {e}")
        return
    
    # Step 2: Check all registered devices
    print(f"\n2. Checking all registered devices")
    try:
        response = requests.get(f"{BASE_URL}/api/devices/")
        if response.status_code == 200:
            devices = response.json()
            print(f"Total devices registered: {len(devices)}")
            for device in devices:
                udid = device.get('udid', 'Unknown')
                apk_count = len(device.get('apks', {}))
                print(f"   - {udid}: {apk_count} APKs")
        else:
            print(f"❌ Failed to get devices: {response.status_code}")
    except Exception as e:
        print(f"❌ Error getting devices: {e}")
    
    # Step 3: Test APK registration for this device
    print(f"\n3. Testing APK registration for device {DEVICE_UDID}")
    
    # Test APK data
    test_apk = [{
        "apkid": "com.example.debug.test",
        "package_name": "com.example.debug.test",
        "app_name": "Debug Test App",
        "version": "1.0.0",
        "version_code": 1,
        "installed_time": 1234567890,
        "is_system": False,
        "icon_path": ""
    }]
    
    try:
        response = requests.post(f"{BASE_URL}/api/devices/{DEVICE_UDID}/apks", json=test_apk)
        print(f"Status Code: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"✅ APK registration response:")
            print(f"   Message: {result.get('message', 'No message')}")
            print(f"   Added APKs: {len(result.get('added_apks', []))}")
            print(f"   Errors: {len(result.get('errors', []))}")
            
            if result.get('errors'):
                print("   Error details:")
                for error in result.get('errors', []):
                    print(f"     - {error.get('error', 'Unknown error')}")
        else:
            print(f"❌ APK registration failed: {response.text}")
    except Exception as e:
        print(f"❌ APK registration error: {e}")
    
    # Step 4: Verify APK was added
    print(f"\n4. Verifying APK was added to device")
    try:
        response = requests.get(f"{BASE_URL}/api/devices/{DEVICE_UDID}/apks")
        if response.status_code == 200:
            result = response.json()
            apk_count = len(result.get('apks', []))
            print(f"✅ Found {apk_count} APKs for device")
            
            if apk_count > 0:
                for apk in result.get('apks', []):
                    print(f"   - {apk.get('app_name', 'Unknown')} ({apk.get('apkid', 'Unknown')})")
            else:
                print("   No APKs found")
        else:
            print(f"❌ Failed to get APKs: {response.status_code}")
    except Exception as e:
        print(f"❌ Error getting APKs: {e}")
    
    # Step 5: Check device details again
    print(f"\n5. Final device status check")
    try:
        response = requests.get(f"{BASE_URL}/api/devices/{DEVICE_UDID}")
        if response.status_code == 200:
            device_info = response.json()
            apk_count = len(device_info.get('apks', {}))
            print(f"✅ Final APK count: {apk_count}")
            
            if apk_count > 0:
                print("   APK details:")
                for apk_id, apk_info in device_info.get('apks', {}).items():
                    print(f"     - {apk_info.get('app_name', 'Unknown')} ({apk_id})")
        else:
            print(f"❌ Failed to get device: {response.status_code}")
    except Exception as e:
        print(f"❌ Error getting device: {e}")
    
    print("\n" + "=" * 70)
    print("DEBUG Complete")
    print("=" * 70)

if __name__ == "__main__":
    debug_device_apk_issue()