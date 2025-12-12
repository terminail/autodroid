#!/usr/bin/env python3
"""
无线调试功能测试脚本
用于测试设备管理器中的无线调试功能
"""

import sys
import os
import subprocess

# 添加项目根目录到Python路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from core.device import DeviceManager

def test_wireless_debugging():
    """测试无线调试功能"""
    print("📱 无线调试功能测试")
    print("=" * 50)
    
    # 获取当前连接的设备
    result = subprocess.run(["adb", "devices"], capture_output=True, text=True)
    
    if "device" not in result.stdout:
        print("❌ 未检测到已连接的设备")
        print("请先通过USB连接Android设备并开启USB调试模式")
        return
    
    # 解析设备列表
    devices = []
    for line in result.stdout.split('\n'):
        if '\tdevice' in line:
            device_id = line.split('\t')[0]
            devices.append(device_id)
    
    if not devices:
        print("❌ 未找到可用的设备")
        return
    
    print(f"📱 检测到 {len(devices)} 个设备:")
    for i, device_id in enumerate(devices, 1):
        print(f"   {i}. {device_id}")
    
    # 选择第一个设备进行测试
    device_id = devices[0]
    print(f"\n🔧 选择设备: {device_id}")
    
    # 创建设备管理器实例
    try:
        device_manager = DeviceManager(device_id)
        
        if not device_manager.is_device_connected():
            print("❌ 设备连接失败")
            return
        
        print("✅ 设备连接成功")
        
        # 获取设备信息
        device_info = device_manager.get_device_info()
        print(f"📊 设备信息:")
        print(f"   • 型号: {device_info.get('model', '未知')}")
        print(f"   • Android版本: {device_info.get('android_version', '未知')}")
        print(f"   • 分辨率: {device_info.get('resolution', '未知')}")
        
        # 测试无线调试功能
        print("\n🔧 开始测试无线调试功能...")
        
        # 使用默认端口5555
        result = device_manager.enable_wireless_debugging(port=5555)
        
        if result["success"]:
            print("\n✅ 无线调试功能测试成功!")
            print(f"📱 无线地址: {result['wireless_address']}")
            
            # 测试无线连接状态检查
            print("\n🔍 测试无线连接状态检查...")
            is_connected = device_manager.is_wireless_connected(
                result['wireless_ip'], 
                result['wireless_port']
            )
            
            if is_connected:
                print("✅ 无线连接状态检查正常")
            else:
                print("⚠️ 无线连接状态检查异常")
            
            # 测试断开连接
            print("\n🔌 测试断开无线连接...")
            disconnect_result = device_manager.disconnect_wireless(
                result['wireless_ip'], 
                result['wireless_port']
            )
            
            if disconnect_result:
                print("✅ 断开无线连接成功")
            else:
                print("❌ 断开无线连接失败")
                
        else:
            print(f"❌ 无线调试功能测试失败: {result.get('error', '未知错误')}")
        
    except Exception as e:
        print(f"❌ 测试过程中出现异常: {e}")
        import traceback
        traceback.print_exc()

def main():
    """主函数"""
    print("🚀 AutoDroid 无线调试功能测试")
    print("=" * 50)
    
    # 检查adb是否可用
    try:
        result = subprocess.run(["adb", "version"], capture_output=True, text=True)
        if result.returncode != 0:
            print("❌ ADB未安装或不可用")
            print("请确保Android SDK已安装且adb命令在PATH中")
            return
        
        print("✅ ADB工具可用")
        print(f"📋 ADB版本信息:\n{result.stdout}")
        
    except Exception as e:
        print(f"❌ 检查ADB失败: {e}")
        return
    
    # 运行测试
    test_wireless_debugging()
    
    print("\n" + "=" * 50)
    print("🎉 测试完成!")

if __name__ == "__main__":
    main()