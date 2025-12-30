#!/usr/bin/env python3
"""
测试 U2Device 类的 d 属性
"""

import sys
import os
from pathlib import Path

# 添加项目路径到sys.path
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

# 添加当前目录到sys.path
sys.path.insert(0, os.path.dirname(__file__))

from u2device import U2Device

def test_d_property():
    """测试 d 属性的类型和调用方式"""
    
    print("🔍 开始测试 U2Device 的 d 属性...")
    
    try:
        # 初始化设备
        device = U2Device()
        print("✅ 设备初始化成功")
        
        # 检查 d 属性的类型
        print(f"📊 d 属性的类型: {type(device.d)}")
        print(f"📊 d 属性的值: {device.d}")
        
        # 尝试直接调用 d 属性
        print("🔍 尝试直接调用 d 属性...")
        try:
            result = device.d("text(\"test\")")
            print(f"✅ 直接调用成功: {result}")
        except Exception as e:
            print(f"❌ 直接调用失败: {e}")
        
        # 尝试使用正确的 uiautomator2 调用方式
        print("🔍 尝试使用正确的调用方式...")
        try:
            # 正确的 uiautomator2 调用方式
            result = device.d(text="test")
            print(f"✅ 正确调用方式成功: {result}")
        except Exception as e:
            print(f"❌ 正确调用方式失败: {e}")
            
    except Exception as e:
        print(f"❌ 测试失败: {e}")

if __name__ == "__main__":
    test_d_property()