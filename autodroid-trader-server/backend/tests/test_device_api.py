#!/usr/bin/env python3
"""
Device API tests - combines device registration and device apps tests
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from core.device.service import DeviceManager
import pytest


class TestDeviceRegistration:
    """测试设备注册功能"""

    def test_device_registration_with_serialno_only(self):
        """测试仅使用序列号注册设备"""
        device_manager = DeviceManager()

        device_info = {
            'serialno': 'TDCDU17905004388'
        }

        print(f"Testing device registration with: {device_info}")

        device = device_manager.register_device(device_info)
        assert device is not None
        assert device.serialno == device_info['serialno']
        print(f"Registration successful!")
        print(f"Device name: {device.name}")
        print(f"Device model: {device.model}")
        return device

    def test_device_registration_with_full_info(self):
        """测试使用完整信息注册设备"""
        device_manager = DeviceManager()

        device_info = {
            'serialno': 'TEST-DEVICE-001',
            'name': 'Test Device',
            'model': 'Pixel 6 Pro',
            'android_version': '13',
            'sdk_version': 33
        }

        device = device_manager.register_device(device_info)
        assert device is not None
        assert device.serialno == device_info['serialno']
        return device


class TestDeviceApps:
    """测试设备应用列表功能"""

    def test_get_all_devices(self):
        """测试获取所有设备"""
        device_manager = DeviceManager()
        devices = device_manager.get_all_devices()

        print(f"Found {len(devices)} devices")

        for device in devices:
            print(f"\nDevice serialno: {device.serialno}")
            print(f"Device name: {device.name}")
            print(f"Apps count: {len(device.apps) if device.apps else 0}")

            if device.apps:
                print("Installed apps:")
                for app in device.apps:
                    print(f"  - {app.get('app_name', 'Unknown')} ({app.get('package_name', 'Unknown')})")
            else:
                print("No installed apps")

        return devices


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
