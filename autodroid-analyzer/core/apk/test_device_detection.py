#!/usr/bin/env python3
"""
测试APK加固检测工具的设备检测功能
"""

import sys
import os

# 添加当前目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from apk_packer_detector import APKPackerDetector

def test_device_detection():
    """测试设备检测功能"""
    detector = APKPackerDetector()
    
    # 测试一些常见的应用包名
    test_packages = [
        "com.android.chrome",      # Chrome浏览器
        "com.tencent.mm",          # 微信（可能被加固）
        "com.eg.android.AlipayGphone",  # 支付宝（可能被加固）
        "com.taobao.taobao",        # 淘宝（可能被加固）
    ]
    
    print("APK加固检测工具 - 设备检测测试")
    print("=" * 60)
    
    # 检查ADB是否可用
    try:
        import subprocess
        result = subprocess.run(["adb", "devices"], capture_output=True, text=True, timeout=10)
        if result.returncode != 0:
            print("❌ ADB不可用，请确保ADB已安装并配置")
            return
        
        # 检查设备连接
        devices = []
        for line in result.stdout.strip().split('\n')[1:]:
            if line.strip() and '\t' in line:
                device_id, status = line.strip().split('\t')
                if status == 'device':
                    devices.append(device_id)
        
        if not devices:
            print("❌ 未找到连接的Android设备")
            print("请确保设备已连接并启用USB调试模式")
            return
        
        print(f"✅ 找到 {len(devices)} 个设备: {', '.join(devices)}")
        
        # 测试第一个设备
        device_id = devices[0] if len(devices) == 1 else None
        
        print(f"\n将在{'设备 ' + device_id if device_id else '默认设备'}上进行测试...")
        
        for package_name in test_packages:
            print(f"\n测试应用: {package_name}")
            print("-" * 40)
            
            result = detector.detect_packer_from_device(package_name, device_id)
            
            if "error" in result:
                print(f"❌ 检测失败: {result['error']}")
            else:
                if result["is_packed"]:
                    print(f"🔴 检测到加固: {result['packer_type']} (置信度: {result['confidence']:.2%})")
                else:
                    print("🟢 未检测到加固")
                
                # 显示基本信息
                print(f"文件总数: {result['total_files']}")
                print(f"DEX文件: {result['dex_files']}")
                print(f"原生库: {result['native_libs']}")
        
        print("\n" + "=" * 60)
        print("测试完成！")
        
    except Exception as e:
        print(f"❌ 测试过程中出现错误: {str(e)}")

def test_local_apk():
    """测试本地APK文件检测"""
    detector = APKPackerDetector()
    
    # 检查是否有测试APK文件
    test_apk_path = "test.apk"
    if not os.path.exists(test_apk_path):
        print(f"⚠️ 未找到测试APK文件: {test_apk_path}")
        print("请将测试APK文件放置在当前目录下")
        return
    
    print(f"\n测试本地APK文件: {test_apk_path}")
    print("-" * 40)
    
    result = detector.detect_packer(test_apk_path)
    
    if "error" in result:
        print(f"❌ 检测失败: {result['error']}")
    else:
        if result["is_packed"]:
            print(f"🔴 检测到加固: {result['packer_type']} (置信度: {result['confidence']:.2%})")
        else:
            print("🟢 未检测到加固")

if __name__ == "__main__":
    print("选择测试模式:")
    print("1. 设备检测测试")
    print("2. 本地APK文件测试")
    
    choice = input("请输入选择 (1 或 2): ").strip()
    
    if choice == "1":
        test_device_detection()
    elif choice == "2":
        test_local_apk()
    else:
        print("无效选择")