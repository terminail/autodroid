#!/usr/bin/env python3
"""
API integration tests - combines API response and unified APK API tests
"""

import requests
import json
import pytest

BASE_URL = "http://localhost:8004"
DEVICE_SERIALNO = "test-device-123"


class TestDeviceApiResponse:
    """测试设备API响应"""

    def test_device_registration_response(self):
        """测试设备注册的API响应"""
        url = f"{BASE_URL}/api/devices/"

        device_data = {
            'serialno': 'TDCDU17905004388'
        }

        print(f"Testing API with: {device_data}")

        response = requests.post(url, json=device_data)
        print(f"Response status: {response.status_code}")

        if response.status_code == 200:
            response_data = response.json()
            print(f"Response body: {json.dumps(response_data, indent=2)}")

            if response_data.get('success') and response_data.get('device'):
                device = response_data['device']
                print(f"Device name in response: {device.get('name')}")
                print(f"Device model in response: {device.get('model')}")
                print(f"Device serialno in response: {device.get('serialno')}")
            else:
                print("No device info in response or registration failed")
        else:
            print(f"API call failed with status: {response.status_code}")


class TestUnifiedApkApi:
    """测试统一APK注册API"""

    def test_single_apk_registration(self):
        """测试注册单个APK"""
        url = f"{BASE_URL}/api/devices/{DEVICE_SERIALNO}/apks"

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

        try:
            response = requests.post(url, json=apk_data)
            print(f"Status Code: {response.status_code}")

            if response.status_code == 200:
                print("Single APK registration successful!")
            else:
                print(f"Single APK registration failed: {response.text}")

        except Exception as e:
            print(f"Error: {e}")

    def test_multiple_apk_registration(self):
        """测试注册多个APK"""
        url = f"{BASE_URL}/api/devices/{DEVICE_SERIALNO}/apks"

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

        try:
            response = requests.post(url, json=apk_data)
            print(f"Status Code: {response.status_code}")

            if response.status_code == 200:
                print("Multiple APK registration successful!")
            else:
                print(f"Multiple APK registration failed: {response.text}")

        except Exception as e:
            print(f"Error: {e}")

    def test_empty_apk_list(self):
        """测试注册空列表"""
        url = f"{BASE_URL}/api/devices/{DEVICE_SERIALNO}/apks"
        apk_data = []

        print(f"\nTesting empty list registration...")

        try:
            response = requests.post(url, json=apk_data)
            print(f"Status Code: {response.status_code}")

            if response.status_code == 200:
                print("Empty list registration successful!")
            else:
                print(f"Empty list registration failed")

        except Exception as e:
            print(f"Error: {e}")

    def test_get_device_apks(self):
        """测试获取设备的所有APK"""
        url = f"{BASE_URL}/api/devices/{DEVICE_SERIALNO}/apks"

        print(f"\nTesting GET all APKs for device...")

        try:
            response = requests.get(url)
            print(f"Status Code: {response.status_code}")

            if response.status_code == 200:
                result = response.json()
                apk_count = len(result.get('apks', []))
                print(f"Found {apk_count} APKs for device {DEVICE_SERIALNO}")

                if apk_count > 0:
                    apk_ids = [apk.get('apkid', '') for apk in result.get('apks', [])]
                    print(f"APK IDs found: {apk_ids}")
            elif response.status_code == 404:
                print("Device not found or no APKs registered")
            else:
                print(f"GET APKs failed with status code: {response.status_code}")

        except Exception as e:
            print(f"Error: {e}")

    def test_comprehensive_apk_workflow(self):
        """测试完整的APK工作流程"""
        print(f"\n" + "=" * 60)
        print("Testing Complete APK Workflow")
        print("=" * 60)

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
                print("Device registration successful")
            else:
                print(f"Device registration failed: {response.status_code}")
        except Exception as e:
            print(f"Device registration error: {e}")

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
                print("APK registration successful")
            else:
                print(f"APK registration failed: {response.status_code}")
                return
        except Exception as e:
            print(f"APK registration error: {e}")
            return

        print(f"Step 3: Retrieving APKs for device...")
        try:
            response = requests.get(apk_url)
            if response.status_code == 200:
                result = response.json()
                apk_count = len(result.get('apks', []))
                print(f"Found {apk_count} APKs for device")

                for i, apk in enumerate(result.get('apks', []), 1):
                    print(f"  {i}. {apk.get('app_name', 'Unknown')} ({apk.get('apkid', 'Unknown')})")
            else:
                print(f"Failed to retrieve APKs: {response.status_code}")
        except Exception as e:
            print(f"APK retrieval error: {e}")


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
