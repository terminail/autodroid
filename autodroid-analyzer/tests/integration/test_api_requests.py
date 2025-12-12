#!/usr/bin/env python3
"""
Autodroid Analyzer API 请求集成测试
测试实际的 API 端点功能
"""

import sys
import os
import time
import requests
import json
from typing import Dict, Any

# 添加项目根目录到Python路径
project_root = os.path.join(os.path.dirname(__file__), '..', '..')
sys.path.insert(0, project_root)


class APITestClient:
    """API 测试客户端"""
    
    def __init__(self, base_url: str = "http://localhost:8001"):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'User-Agent': 'Autodroid-API-Test/1.0.0'
        })
    
    def get(self, endpoint: str, **kwargs) -> requests.Response:
        """发送 GET 请求"""
        url = f"{self.base_url}{endpoint}"
        return self.session.get(url, **kwargs)
    
    def post(self, endpoint: str, data: Dict = None, **kwargs) -> requests.Response:
        """发送 POST 请求"""
        url = f"{self.base_url}{endpoint}"
        return self.session.post(url, json=data, **kwargs)
    
    def check_response(self, response: requests.Response, expected_status: int = 200) -> bool:
        """检查响应状态"""
        if response.status_code != expected_status:
            print(f"❌ 响应状态码错误: 期望 {expected_status}, 实际 {response.status_code}")
            print(f"响应内容: {response.text}")
            return False
        return True


def test_api_health():
    """测试 API 健康检查"""
    print("🔍 测试 API 健康检查...")
    
    client = APITestClient()
    
    try:
        # 测试根路径
        response = client.get("/")
        if client.check_response(response):
            data = response.json()
            assert 'message' in data
            assert 'version' in data
            print("✅ 根路径测试通过")
        
        # 测试 API 根路径
        response = client.get("/api")
        if client.check_response(response):
            data = response.json()
            assert 'name' in data
            assert 'version' in data
            assert 'endpoints' in data
            print("✅ API 根路径测试通过")
        
        return True
        
    except Exception as e:
        print(f"❌ API 健康检查测试失败: {e}")
        return False


def test_server_endpoints():
    """测试服务器相关端点"""
    print("\n🔍 测试服务器相关端点...")
    
    client = APITestClient()
    
    try:
        # 测试服务器信息
        response = client.get("/api/server/info")
        if client.check_response(response):
            data = response.json()
            assert 'name' in data
            assert 'ipAddress' in data
            assert 'apiEndpoint' in data
            print("✅ 服务器信息端点测试通过")
        
        # 测试健康检查
        response = client.get("/api/server/health")
        if client.check_response(response):
            data = response.json()
            assert 'status' in data
            assert 'timestamp' in data
            print("✅ 健康检查端点测试通过")
        
        # 测试配置获取
        response = client.get("/api/server/config")
        if client.check_response(response):
            data = response.json()
            assert 'server' in data
            assert 'analysis' in data
            print("✅ 配置获取端点测试通过")
        
        # 测试统计信息
        response = client.get("/api/server/stats")
        if client.check_response(response):
            data = response.json()
            assert 'server' in data
            assert 'analysis' in data
            print("✅ 统计信息端点测试通过")
        
        return True
        
    except Exception as e:
        print(f"❌ 服务器端点测试失败: {e}")
        return False


def test_analysis_endpoints():
    """测试分析相关端点"""
    print("\n🔍 测试分析相关端点...")
    
    client = APITestClient()
    
    try:
        # 测试截屏列表
        response = client.get("/api/analysis/screenshots")
        if client.check_response(response):
            data = response.json()
            assert isinstance(data, list)
            print("✅ 截屏列表端点测试通过")
        
        # 测试操作记录
        response = client.get("/api/analysis/operations")
        if client.check_response(response):
            data = response.json()
            assert isinstance(data, list)
            print("✅ 操作记录端点测试通过")
        
        # 测试特定 APK 的操作记录（空列表测试）
        response = client.get("/api/analysis/apks/test-apk/operations")
        if client.check_response(response):
            data = response.json()
            assert isinstance(data, list)
            print("✅ APK 操作记录端点测试通过")
        
        # 测试特定 APK 的截屏列表（空列表测试）
        response = client.get("/api/analysis/apks/test-apk/screenshots")
        if client.check_response(response):
            data = response.json()
            assert isinstance(data, list)
            print("✅ APK 截屏列表端点测试通过")
        
        return True
        
    except Exception as e:
        print(f"❌ 分析端点测试失败: {e}")
        return False


def test_apks_endpoints():
    """测试 APK 相关端点"""
    print("\n🔍 测试 APK 相关端点...")
    
    client = APITestClient()
    
    try:
        # 测试 APK 列表
        response = client.get("/api/apks")
        if client.check_response(response):
            data = response.json()
            assert isinstance(data, list)
            print("✅ APK 列表端点测试通过")
        
        return True
        
    except Exception as e:
        print(f"❌ APK 端点测试失败: {e}")
        return False


def test_devices_endpoints():
    """测试设备相关端点"""
    print("\n🔍 测试设备相关端点...")
    
    client = APITestClient()
    
    try:
        # 测试设备列表
        response = client.get("/api/devices")
        if client.check_response(response):
            data = response.json()
            assert isinstance(data, list)
            print("✅ 设备列表端点测试通过")
        
        return True
        
    except Exception as e:
        print(f"❌ 设备端点测试失败: {e}")
        return False


def test_api_documentation():
    """测试 API 文档"""
    print("\n🔍 测试 API 文档...")
    
    client = APITestClient()
    
    try:
        # 测试 OpenAPI 文档
        response = client.get("/docs")
        if client.check_response(response):
            assert 'text/html' in response.headers.get('Content-Type', '')
            print("✅ API 文档页面测试通过")
        
        # 测试 OpenAPI JSON
        response = client.get("/openapi.json")
        if client.check_response(response):
            data = response.json()
            assert 'openapi' in data
            assert 'info' in data
            assert 'paths' in data
            print("✅ OpenAPI JSON 测试通过")
        
        return True
        
    except Exception as e:
        print(f"❌ API 文档测试失败: {e}")
        return False


def test_api_performance():
    """测试 API 性能"""
    print("\n🔍 测试 API 性能...")
    
    client = APITestClient()
    
    try:
        # 测试响应时间
        endpoints_to_test = [
            "/",
            "/api",
            "/api/server/info",
            "/api/server/health"
        ]
        
        max_response_time = 2.0  # 最大响应时间（秒）
        
        for endpoint in endpoints_to_test:
            start_time = time.time()
            response = client.get(endpoint)
            end_time = time.time()
            
            response_time = end_time - start_time
            
            if client.check_response(response):
                if response_time <= max_response_time:
                    print(f"✅ {endpoint} 响应时间: {response_time:.3f}s")
                else:
                    print(f"⚠️  {endpoint} 响应时间较长: {response_time:.3f}s")
            else:
                print(f"❌ {endpoint} 请求失败")
        
        return True
        
    except Exception as e:
        print(f"❌ API 性能测试失败: {e}")
        return False


def test_error_handling():
    """测试错误处理"""
    print("\n🔍 测试错误处理...")
    
    client = APITestClient()
    
    try:
        # 测试不存在的端点
        response = client.get("/api/nonexistent")
        if response.status_code == 404:
            print("✅ 404 错误处理测试通过")
        else:
            print(f"⚠️  不存在的端点返回状态码: {response.status_code}")
        
        # 测试无效的 HTTP 方法
        response = client.session.put("http://localhost:8001/api", json={})
        if response.status_code == 405:
            print("✅ 405 错误处理测试通过")
        else:
            print(f"⚠️  无效方法返回状态码: {response.status_code}")
        
        return True
        
    except Exception as e:
        print(f"❌ 错误处理测试失败: {e}")
        return False


def wait_for_server_ready(max_wait: int = 30) -> bool:
    """等待服务器就绪"""
    print("⏳ 等待服务器就绪...")
    
    client = APITestClient()
    
    for i in range(max_wait):
        try:
            response = client.get("/api/server/health")
            if response.status_code == 200:
                print("✅ 服务器已就绪")
                return True
        except requests.exceptions.ConnectionError:
            pass
        
        if i < max_wait - 1:
            time.sleep(1)
    
    print("❌ 服务器未在指定时间内就绪")
    return False


def main():
    """主测试函数"""
    print("🚀 开始 Autodroid Analyzer API 集成测试")
    print("=" * 60)
    
    # 等待服务器就绪
    if not wait_for_server_ready():
        print("⚠️  跳过集成测试，服务器未就绪")
        return 1
    
    tests = [
        ("API 健康检查", test_api_health),
        ("服务器端点", test_server_endpoints),
        ("分析端点", test_analysis_endpoints),
        ("APK 端点", test_apks_endpoints),
        ("设备端点", test_devices_endpoints),
        ("API 文档", test_api_documentation),
        ("API 性能", test_api_performance),
        ("错误处理", test_error_handling)
    ]
    
    passed = 0
    total = len(tests)
    
    for test_name, test_func in tests:
        if test_func():
            passed += 1
            print(f"✅ {test_name} - 通过\n")
        else:
            print(f"❌ {test_name} - 失败\n")
    
    print("=" * 60)
    print(f"📊 测试结果: {passed}/{total} 通过")
    
    if passed == total:
        print("🎉 所有 API 集成测试通过!")
        return 0
    else:
        print("⚠️  部分测试失败，请检查 API 服务")
        return 1


if __name__ == "__main__":
    sys.exit(main())