#!/usr/bin/env python3
"""
测试Android模拟器网络连接的脚本
"""

import subprocess
import time

def test_network_connection():
    """测试模拟器网络连接"""
    
    print("🔍 测试Android模拟器网络连接...")
    
    # 方法1: 使用Android的HttpURLConnection进行网络测试
    print("📝 使用现有的网络测试脚本...")
    
    # 编译Java文件
    try:
        result = subprocess.run(['javac', 'd:/git/autodroid/pure-java-mdns-test/src/main/java/com/autodroid/test/NetworkTest.java'], 
                              capture_output=True, text=True)
        if result.returncode == 0:
            print("✅ 编译成功")
        else:
            print("❌ 编译失败:", result.stderr)
    except Exception as e:
        print("❌ 编译错误:", e)
    
    # 方法2: 使用adb shell直接测试网络
    print("\n🌐 使用adb测试网络连接...")
    
    # 测试telnet连接
    print("测试telnet连接...")
    result = subprocess.run(['adb', 'shell', 'echo', 'test'], capture_output=True, text=True)
    print("ADB连接状态:", "正常" if result.returncode == 0 else "异常")
    
    # 测试网络接口
    print("\n📡 检查网络接口...")
    result = subprocess.run(['adb', 'shell', 'ifconfig'], capture_output=True, text=True)
    print("网络接口信息:")
    print(result.stdout if result.stdout else "无网络接口")
    
    # 测试DNS解析
    print("\n🔍 测试DNS解析...")
    result = subprocess.run(['adb', 'shell', 'getprop', 'net.dns1'], capture_output=True, text=True)
    print("DNS服务器:", result.stdout.strip() if result.stdout else "未设置")
    
    # 测试是否可以访问外部网络
    print("\n🌍 测试外部网络访问...")
    result = subprocess.run(['adb', 'shell', 'ping', '-c', '1', '8.8.8.8'], 
                          capture_output=True, text=True, timeout=10)
    print("外部网络访问:", "正常" if result.returncode == 0 else "异常")
    
    # 测试主机访问
    print("\n🏠 测试主机访问(10.0.2.2)...")
    result = subprocess.run(['adb', 'shell', 'ping', '-c', '1', '10.0.2.2'], 
                          capture_output=True, text=True, timeout=10)
    print("主机访问:", "正常" if result.returncode == 0 else "异常")
    
    # 检查服务器状态
    print("\n🔧 检查服务器状态...")
    try:
        import requests
        response = requests.get('http://127.0.0.1:8004/api/health', timeout=5)
        print(f"本地服务器状态: {response.status_code} - {response.text}")
    except Exception as e:
        print(f"服务器检查错误: {e}")

if __name__ == "__main__":
    test_network_connection()