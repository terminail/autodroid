#!/usr/bin/env python3
"""
测试脚本：检查设备上是否安装了特定应用
例如：检查 com.tdx.androidCCZQ 是否已安装
"""

import sys
import os

# 添加 workscripts 目录到 Python 路径
sys.path.append(os.path.join(os.path.dirname(__file__), 'workscripts'))

from adb_device import ADBDevice, list_devices

def check_app_installed(package_name: str = "com.tdx.androidCCZQ"):
    """检查指定设备上是否安装了特定应用"""
    
    # 列出所有连接的设备
    devices = list_devices()
    
    if not devices:
        print("❌ 没有找到连接的Android设备")
        return
    
    print(f"找到 {len(devices)} 个设备:")
    for i, device_id in enumerate(devices):
        print(f"  {i+1}. {device_id}")
    
    # 如果只有一个设备，直接使用它
    if len(devices) == 1:
        device_id = devices[0]
    else:
        # 多个设备，让用户选择
        try:
            choice = int(input("请选择设备编号: ")) - 1
            if choice < 0 or choice >= len(devices):
                print("❌ 无效的设备编号")
                return
            device_id = devices[choice]
        except ValueError:
            print("❌ 无效的输入")
            return
    
    print(f"\n正在检查设备 {device_id} 上是否安装了应用 {package_name}...")
    
    try:
        # 连接设备
        adb_device = ADBDevice(device_id)
        
        if not adb_device.is_connected():
            print(f"❌ 无法连接到设备 {device_id}")
            return
        
        # 获取设备信息
        device_info = adb_device.get_device_info()
        print(f"✅ 已连接到设备: {device_info.get('model', 'Unknown')} (Android {device_info.get('android_version', 'Unknown')})")
        
        # 检查应用是否安装
        is_installed = adb_device.is_app_installed(package_name)
        
        if is_installed:
            print(f"✅ 应用 {package_name} 已安装在设备 {device_id} 上")
        else:
            print(f"❌ 应用 {package_name} 未安装在设备 {device_id} 上")
            
        return is_installed
        
    except Exception as e:
        print(f"❌ 检查过程中出错: {str(e)}")
        return None

if __name__ == "__main__":
    # 如果命令行参数提供了包名，使用它；否则使用默认的 com.tdx.androidCCZQ
    package_name = sys.argv[1] if len(sys.argv) > 1 else "com.tdx.androidCCZQ"
    check_app_installed(package_name)