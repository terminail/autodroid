#!/usr/bin/env python3
"""
Test script to verify the unified APK registration API
"""

import requests
import json

# Server configuration
# Use the actual server IP address (from server logs)
BASE_URL = "http://192.168.1.59:8004"
DEVICE_SERIALNO = "test-device-123"

def test_single_apk_registration():
    """Test registering a single APK using the unified list endpoint"""
    url = f"{BASE_URL}/api/devices/{DEVICE_SERIALNO}/apks"
    
    # Single APK as a list with one item
    apk_data = [{
        "apkid": "com.example.testapp",
        "package_name": "com.example.testapp",
        "app_name": "Test Application",
        "version": "1.0.0",
        "version_code": 1,
        "installed_time": 1234567890,
        "is_system": False,
        "icon_path": ""
    }]
    
    print(f"Testing single APK registration...")
    print(f"URL: {url}")
    print(f"Data: {json.dumps(apk_data, indent=2)}")
    
    try:
        response = requests.post(url, json=apk_data)
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.text}")
        
        if response.status_code == 200:
            print("✅ Single APK registration successful!")
        else:
            print("❌ Single APK registration failed")
            
    except Exception as e:
        print(f"❌ Error: {e}")

def test_multiple_apk_registration():
    """Test registering multiple APKs using the unified list endpoint"""
    url = f"{BASE_URL}/api/devices/{DEVICE_SERIALNO}/apks"
    
    # Multiple APKs as a list
    apk_data = [
        {
            "apkid": "com.example.app1",
            "package_name": "com.example.app1",
            "app_name": "App One",
            "version": "1.0.0",
            "version_code": 1,
            "installed_time": 1234567890,
            "is_system": False,
            "icon_path": ""
        },
        {
            "apkid": "com.example.app2",
            "package_name": "com.example.app2",
            "app_name": "App Two",
            "version": "2.0.0",
            "version_code": 2,
            "installed_time": 1234567891,
            "is_system": True,
            "icon_path": ""
        }
    ]
    
    print(f"\nTesting multiple APK registration...")
    print(f"URL: {url}")
    print(f"Data: {json.dumps(apk_data, indent=2)}")
    
    try:
        response = requests.post(url, json=apk_data)
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.text}")
        
        if response.status_code == 200:
            print("✅ Multiple APK registration successful!")
        else:
            print("❌ Multiple APK registration failed")
            
    except Exception as e:
        print(f"❌ Error: {e}")

def test_empty_list():
    """Test registering an empty list (edge case)"""
    url = f"{BASE_URL}/api/devices/{DEVICE_SERIALNO}/apks"
    
    # Empty list
    apk_data = []
    
    print(f"\nTesting empty list registration...")
    print(f"URL: {url}")
    print(f"Data: {json.dumps(apk_data, indent=2)}")
    
    try:
        response = requests.post(url, json=apk_data)
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.text}")
        
        if response.status_code == 200:
            print("✅ Empty list registration successful!")
        else:
            print("❌ Empty list registration failed")
            
    except Exception as e:
        print(f"❌ Error: {e}")

def test_get_device_apks():
    """Test getting all APKs for a device after registration"""
    url = f"{BASE_URL}/api/devices/{DEVICE_SERIALNO}/apks"
    
    print(f"\nTesting GET all APKs for device...")
    print(f"URL: {url}")
    
    try:
        response = requests.get(url)
        print(f"Status Code: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"Response: {json.dumps(result, indent=2)}")
            
            apk_count = len(result.get('apks', []))
            print(f"✅ Found {apk_count} APKs for device {DEVICE_SERIALNO}")
            
            # Verify the APKs we registered are present
            if apk_count > 0:
                apk_ids = [apk.get('apkid', '') for apk in result.get('apks', [])]
                print(f"APK IDs found: {apk_ids}")
                
                # Check if our test APKs are present
                expected_apks = ["com.example.testapp", "com.example.app1", "com.example.app2"]
                found_apks = [apk_id for apk_id in expected_apks if apk_id in apk_ids]
                
                if found_apks:
                    print(f"✅ Found expected APKs: {found_apks}")
                else:
                    print("⚠️  Expected APKs not found in the list")
            
        elif response.status_code == 404:
            print("⚠️  Device not found or no APKs registered")
        else:
            print(f"❌ GET APKs failed with status code: {response.status_code}")
            print(f"Response: {response.text}")
            
    except Exception as e:
        print(f"❌ Error: {e}")

def test_comprehensive_apk_workflow():
    """Test complete APK workflow: register APKs and then retrieve them"""
    print(f"\n" + "=" * 60)
    print("Testing Complete APK Workflow")
    print("=" * 60)
    
    # Step 1: Register a test device first
    device_reg_url = f"{BASE_URL}/api/devices/register"
    device_data = {
        "serialno": DEVICE_SERIALNO,
        "device_name": "Test Device",
        "model": "Pixel 6 Pro",
        "android_version": "13",
        "sdk_version": 33
    }
    
    print(f"Step 1: Registering test device...")
    try:
        response = requests.post(device_reg_url, json=device_data)
        if response.status_code == 200:
            print("✅ Device registration successful")
        else:
            print(f"⚠️  Device registration failed: {response.status_code}")
    except Exception as e:
        print(f"⚠️  Device registration error: {e}")
    
    # Step 2: Register multiple APKs
    apk_url = f"{BASE_URL}/api/devices/{DEVICE_SERIALNO}/apks"
    apk_data = [
        {
            "apkid": "com.example.workflow1",
            "package_name": "com.example.workflow1",
            "app_name": "Workflow App One",
            "version": "1.0.0",
            "version_code": 1,
            "installed_time": 1234567890,
            "is_system": False,
            "icon_path": ""
        },
        {
            "apkid": "com.example.workflow2",
            "package_name": "com.example.workflow2",
            "app_name": "Workflow App Two",
            "version": "2.0.0",
            "version_code": 2,
            "installed_time": 1234567891,
            "is_system": True,
            "icon_path": ""
        }
    ]
    
    print(f"Step 2: Registering APKs...")
    try:
        response = requests.post(apk_url, json=apk_data)
        if response.status_code == 200:
            print("✅ APK registration successful")
        else:
            print(f"❌ APK registration failed: {response.status_code}")
            print(f"Response: {response.text}")
            return
    except Exception as e:
        print(f"❌ APK registration error: {e}")
        return
    
    # Step 3: Retrieve all APKs for the device
    print(f"Step 3: Retrieving APKs for device...")
    try:
        response = requests.get(apk_url)
        if response.status_code == 200:
            result = response.json()
            apk_count = len(result.get('apks', []))
            print(f"✅ Found {apk_count} APKs for device")
            
            # Display APK details
            for i, apk in enumerate(result.get('apks', []), 1):
                print(f"  {i}. {apk.get('app_name', 'Unknown')} ({apk.get('apkid', 'Unknown')})")
        else:
            print(f"❌ Failed to retrieve APKs: {response.status_code}")
            print(f"Response: {response.text}")
    except Exception as e:
        print(f"❌ APK retrieval error: {e}")

if __name__ == "__main__":
    print("=" * 60)
    print("Testing Unified APK Registration API")
    print("=" * 60)
    
    test_single_apk_registration()
    test_multiple_apk_registration()
    test_empty_list()
    test_get_device_apks()
    test_comprehensive_apk_workflow()
    
    print("\n" + "=" * 60)
    print("Test Complete")
    print("=" * 60)