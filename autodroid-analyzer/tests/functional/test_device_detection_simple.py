#!/usr/bin/env python3
"""
测试设备检测功能
"""

import sys
import os
import unittest

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from core.device.service import DeviceManager

class TestDeviceDetectionSimple(unittest.TestCase):
    """简单设备检测测试类"""
    
    def test_device_detection(self):
        """测试设备检测功能"""
        print("🚀 开始测试设备检测功能...")
        
        device_manager = DeviceManager()
        
        # 测试获取已连接设备
        print("\n📱 测试获取已连接设备:")
        devices = device_manager.get_connected_devices()
        print(f"📱 检测到 {len(devices)} 个连接的设备")
        
        if devices:
            print("✅ 发现已连接设备:")
            for device in devices:
                print(f"\n📱 设备信息:")
                print(f"   设备ID: {device.id}")
                print(f"   设备名称: {device.device_name}")
                print(f"   设备型号: {device.device_model}")
                print(f"   Android版本: {device.android_version}")
                print(f"   电池电量: {device.battery_level}%")
                print(f"   连接状态: {'已连接' if device.is_connected else '未连接'}")
                print(f"   连接类型: {device.connection_type}")
        else:
            print("⚠️ 未发现已连接设备")
        
        # 测试搜索功能
        print("\n🔍 测试搜索功能:")
        all_devices = device_manager.search_devices()
        print(f"✅ 数据库中共有 {len(all_devices)} 个设备记录")
        
        # 测试最近连接的设备
        print("\n📅 测试最近连接的设备:")
        recent_devices = device_manager.get_recently_connected_devices(days=7)
        print(f"✅ 最近7天内连接的设备: {len(recent_devices)} 个")
        
        print("\n✅ 设备检测功能测试完成！")

if __name__ == "__main__":
    unittest.main()