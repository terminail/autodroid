#!/usr/bin/env python3
"""
测试设备API是否返回正确的应用列表
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from core.device.service import DeviceManager

def main():
    # 创建设备管理器
    device_manager = DeviceManager()
    
    # 获取所有设备
    devices = device_manager.get_all_devices()
    
    print(f"找到 {len(devices)} 个设备")
    
    for device in devices:
        print(f"\n设备序列号: {device.serialno}")
        print(f"设备名称: {device.name}")
        print(f"应用数量: {len(device.apps) if device.apps else 0}")
        
        if device.apps:
            print("已安装的应用:")
            for app in device.apps:
                print(f"  - {app.get('app_name', 'Unknown')} ({app.get('package_name', 'Unknown')})")
        else:
            print("没有已安装的应用")

if __name__ == "__main__":
    main()