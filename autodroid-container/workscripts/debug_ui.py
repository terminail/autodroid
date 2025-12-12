#!/usr/bin/env python3
"""
调试UI元素捕获脚本
"""

import sys
import os

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from workscripts.device_connection import ADBDeviceController

def debug_ui_capture(device_udid):
    """调试UI捕获"""
    print(f"连接到设备: {device_udid}")
    
    # 创建设备控制器
    device = ADBDeviceController(device_udid)
    
    # 连接设备
    if not device.connect():
        print("❌ 设备连接失败")
        return
    
    print("✓ 设备连接成功")
    
    # 启动应用
    if device.start_app("com.autodroid.manager"):
        print("✓ 应用启动成功")
    else:
        print("⚠️ 应用启动可能失败，继续...")
    
    # 等待应用加载
    import time
    time.sleep(3)
    
    # 获取当前Activity
    activity = device.get_current_activity()
    print(f"当前Activity: {activity}")
    
    # 获取当前包名
    package = device.get_current_package()
    print(f"当前包名: {package}")
    
    # 尝试获取UI dump
    print("\n尝试获取UI dump...")
    try:
        # 执行UI dump命令
        result = device.adb_device.shell("uiautomator dump /sdcard/ui_dump.xml")
        print(f"UI dump结果: {result}")
        
        # 拉取文件
        import tempfile
        temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.xml', delete=False)
        temp_file.close()
        
        import subprocess
        pull_result = subprocess.run(
            device.adb_device._get_adb_prefix() + ["pull", "/sdcard/ui_dump.xml", temp_file.name],
            capture_output=True, text=True
        )
        
        if pull_result.returncode == 0:
            print(f"✓ UI dump文件拉取成功: {temp_file.name}")
            
            # 读取并分析文件内容
            import xml.etree.ElementTree as ET
            try:
                tree = ET.parse(temp_file.name)
                root = tree.getroot()
                
                print("\n找到的输入框元素:")
                for elem in root.iter():
                    resource_id = elem.get("resource-id")
                    if resource_id and ("edit" in resource_id.lower() or "input" in resource_id.lower() or "email" in resource_id.lower()):
                        text = elem.get("text", "")
                        bounds = elem.get("bounds", "")
                        print(f"  - ID: {resource_id}, Text: '{text}', Bounds: {bounds}")
                
                print("\n找到的按钮元素:")
                for elem in root.iter():
                    resource_id = elem.get("resource-id")
                    if resource_id and ("button" in resource_id.lower() or "btn" in resource_id.lower() or "login" in resource_id.lower()):
                        text = elem.get("text", "")
                        bounds = elem.get("bounds", "")
                        print(f"  - ID: {resource_id}, Text: '{text}', Bounds: {bounds}")
                        
            except Exception as e:
                print(f"解析XML失败: {e}")
                
            # 清理临时文件
            try:
                os.unlink(temp_file.name)
            except:
                pass
                
        else:
            print(f"❌ UI dump文件拉取失败: {pull_result.stderr}")
            
    except Exception as e:
        print(f"获取UI dump失败: {e}")

if __name__ == "__main__":
    device_udid = sys.argv[1] if len(sys.argv) > 1 else "emulator-5554"
    debug_ui_capture(device_udid)