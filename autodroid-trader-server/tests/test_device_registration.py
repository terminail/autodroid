#!/usr/bin/env python3
"""
Test script to verify device registration works with the updated schema
"""

import requests
import json
import sys

def test_device_registration():
    """Test device registration with the updated schema"""
    
    # Server URL
    server_url = "http://localhost:8004"
    
    # Device info (simulating what the app would send)
    device_info = {
        "serialno": "TDCDU17905004388",
        "udid": "TDCDU17905004388",
        "name": "Test Device",  # Using the consolidated name field
        "model": "Test Model",
        "manufacturer": "Test Manufacturer",
        "android_version": None,  # Testing that null is now accepted
        "api_level": 30,
        "platform": "Android",
        "brand": "Test Brand",
        "device": "test_device",
        "product": "test_product",
        "ip": "192.168.1.100"
    }
    
    try:
        # Register device
        response = requests.post(
            f"{server_url}/api/devices/",
            json=device_info,
            headers={"Content-Type": "application/json"}
        )
        
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.text}")
        
        if response.status_code == 200:
            data = response.json()
            if data.get("success"):
                print("✅ Device registration successful!")
                device = data.get("device", {})
                print(f"   Device ID: {device.get('id')}")
                print(f"   Device Name: {device.get('name')}")
                print(f"   Android Version: {device.get('android_version')}")
                return True
            else:
                print(f"❌ Device registration failed: {data.get('message')}")
                return False
        else:
            print(f"❌ HTTP Error: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"❌ Exception: {str(e)}")
        return False

def test_device_list():
    """Test getting the list of devices"""
    
    server_url = "http://localhost:8004"
    
    try:
        response = requests.get(f"{server_url}/api/devices/")
        
        if response.status_code == 200:
            data = response.json()
            if "devices" in data:
                devices = data.get("devices", [])
                print(f"\n✅ Found {len(devices)} devices:")
                for device in devices:
                    print(f"   - {device.get('name')} ({device.get('serialno')})")
                return True
            else:
                print(f"❌ Unexpected response format: {response.text}")
                return False
        else:
            print(f"❌ HTTP Error: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"❌ Exception: {str(e)}")
        return False

if __name__ == "__main__":
    print("Testing device registration with updated schema...")
    
    # Test device registration
    reg_success = test_device_registration()
    
    # Test device list
    list_success = test_device_list()
    
    if reg_success and list_success:
        print("\n✅ All tests passed!")
        sys.exit(0)
    else:
        print("\n❌ Some tests failed!")
        sys.exit(1)