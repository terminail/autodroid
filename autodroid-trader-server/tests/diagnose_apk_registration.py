#!/usr/bin/env python3
"""
Detailed diagnosis of APK registration issue
"""

import requests
import json

# Server configuration
BASE_URL = "http://192.168.1.59:8004"
DEVICE_UDID = "c96ebeecb0a69a20"

def diagnose_apk_registration():
    """Diagnose APK registration issue in detail"""
    
    print("=" * 80)
    print("DETAILED DIAGNOSIS: APK Registration Issue")
    print("=" * 80)
    
    # Step 1: Check current device state
    print(f"\n1. CURRENT DEVICE STATE")
    print("-" * 40)
    
    try:
        response = requests.get(f"{BASE_URL}/api/devices/{DEVICE_UDID}")
        if response.status_code == 200:
            device_info = response.json()
            print(f"Device: {device_info.get('device_name', 'Unknown')}")
            print(f"APK Count: {len(device_info.get('apks', {}))}")
            
            if device_info.get('apks'):
                print("Current APKs:")
                for apk_id, apk_info in device_info.get('apks', {}).items():
                    print(f"  - {apk_info.get('app_name', 'Unknown')} ({apk_id})")
            else:
                print("No APKs currently registered")
        else:
            print(f"Failed to get device: {response.status_code}")
    except Exception as e:
        print(f"Error: {e}")
    
    # Step 2: Test different APK registration scenarios
    print(f"\n2. APK REGISTRATION TESTS")
    print("-" * 40)
    
    # Test 1: Single APK registration
    print("\nTest 1: Single APK Registration")
    single_apk = [{
        "apkid": "com.example.single.test",
        "package_name": "com.example.single.test",
        "app_name": "Single Test App",
        "version": "1.0.0",
        "version_code": 1,
        "installed_time": 1234567890,
        "is_system": False,
        "icon_path": ""
    }]
    
    try:
        response = requests.post(f"{BASE_URL}/api/devices/{DEVICE_UDID}/apks", 
                                json=single_apk)
        print(f"Status: {response.status_code}")
        result = response.json()
        print(f"Response: {json.dumps(result, indent=2)}")
    except Exception as e:
        print(f"Error: {e}")
    
    # Test 2: Multiple APK registration (simulating real app scan)
    print("\nTest 2: Multiple APK Registration (Realistic Scenario)")
    multiple_apks = [
        {
            "apkid": "com.android.chrome",
            "package_name": "com.android.chrome",
            "app_name": "Chrome",
            "version": "120.0.6099.43",
            "version_code": 609904300,
            "installed_time": 1700000000,
            "is_system": False,
            "icon_path": "/data/app/~~abc123==/com.android.chrome-base.apk"
        },
        {
            "apkid": "com.google.android.youtube",
            "package_name": "com.google.android.youtube",
            "app_name": "YouTube",
            "version": "18.49.37",
            "version_code": 184937000,
            "installed_time": 1700000001,
            "is_system": False,
            "icon_path": "/data/app/~~def456==/com.google.android.youtube-base.apk"
        }
    ]
    
    try:
        response = requests.post(f"{BASE_URL}/api/devices/{DEVICE_UDID}/apks", 
                                json=multiple_apks)
        print(f"Status: {response.status_code}")
        result = response.json()
        print(f"Response: {json.dumps(result, indent=2)}")
    except Exception as e:
        print(f"Error: {e}")
    
    # Step 3: Verify registration results
    print(f"\n3. VERIFICATION")
    print("-" * 40)
    
    # Check APK list endpoint
    print("\nAPK List Endpoint:")
    try:
        response = requests.get(f"{BASE_URL}/api/devices/{DEVICE_UDID}/apks")
        if response.status_code == 200:
            result = response.json()
            apk_count = len(result.get('apks', []))
            print(f"APK Count: {apk_count}")
            
            if apk_count > 0:
                print("APK Details:")
                for apk in result.get('apks', []):
                    print(f"  - {apk.get('app_name', 'Unknown')} ({apk.get('apkid', 'Unknown')})")
        else:
            print(f"Failed: {response.status_code}")
    except Exception as e:
        print(f"Error: {e}")
    
    # Check device details endpoint
    print("\nDevice Details Endpoint:")
    try:
        response = requests.get(f"{BASE_URL}/api/devices/{DEVICE_UDID}")
        if response.status_code == 200:
            device_info = response.json()
            apk_count = len(device_info.get('apks', {}))
            print(f"APK Count: {apk_count}")
            
            if device_info.get('apks'):
                print("APK Details:")
                for apk_id, apk_info in device_info.get('apks', {}).items():
                    print(f"  - {apk_info.get('app_name', 'Unknown')} ({apk_id})")
        else:
            print(f"Failed: {response.status_code}")
    except Exception as e:
        print(f"Error: {e}")
    
    # Step 4: Check server-side data persistence
    print(f"\n4. DATA PERSISTENCE CHECK")
    print("-" * 40)
    
    # Check if data persists after multiple queries
    print("\nMultiple queries to verify persistence:")
    for i in range(3):
        try:
            response = requests.get(f"{BASE_URL}/api/devices/{DEVICE_UDID}/apks")
            if response.status_code == 200:
                result = response.json()
                apk_count = len(result.get('apks', []))
                print(f"Query {i+1}: {apk_count} APKs")
            else:
                print(f"Query {i+1}: Failed")
        except Exception as e:
            print(f"Query {i+1}: Error - {e}")
    
    print("\n" + "=" * 80)
    print("DIAGNOSIS COMPLETE")
    print("=" * 80)

if __name__ == "__main__":
    diagnose_apk_registration()