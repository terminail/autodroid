#!/usr/bin/env python3
"""
Test script to debug device registration
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from core.device.service import DeviceManager

def test_device_registration():
    """Test device registration with just serialno"""
    device_manager = DeviceManager()
    
    # Test with just serialno (like the app sends)
    device_info = {
        'serialno': 'TDCDU17905004388'
    }
    
    print(f"Testing device registration with: {device_info}")
    
    try:
        device = device_manager.register_device(device_info)
        print(f"Registration successful!")
        print(f"Device name: {device.name}")
        print(f"Device model: {device.model}")
        print(f"Device serialno: {device.serialno}")
        return device
    except Exception as e:
        print(f"Registration failed: {e}")
        import traceback
        traceback.print_exc()
        return None

if __name__ == "__main__":
    test_device_registration()