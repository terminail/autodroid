#!/usr/bin/env python3
"""
Autodroid Analyzer API 端点测试
测试所有主要 API 端点的功能和响应
"""

import sys
import os
import pytest
import requests
import time
from typing import Dict, Any

# 添加项目根目录到Python路径
project_root = os.path.join(os.path.dirname(__file__), '..', '..')
sys.path.insert(0, project_root)

# API 服务器配置
API_BASE_URL = "http://localhost:8001"


class TestAPIEndpoints:
    """API 端点测试类"""
    
    def setup_method(self):
        """测试方法设置"""
        self.session = requests.Session()
        
    def teardown_method(self):
        """测试方法清理"""
        self.session.close()
    
    def test_api_root_endpoint(self):
        """测试 API 根端点"""
        response = self.session.get(f"{API_BASE_URL}/")
        
        assert response.status_code == 200
        data = response.json()
        
        assert "message" in data
        assert "version" in data
        assert "modules" in data
        assert "api_root" in data
        assert data["api_root"] == "/api"
        
        print(f"✅ API 根端点测试通过: {data}")
    
    def test_api_info_endpoint(self):
        """测试 API 信息端点"""
        response = self.session.get(f"{API_BASE_URL}/api")
        
        assert response.status_code == 200
        data = response.json()
        
        assert "name" in data
        assert "version" in data
        assert "description" in data
        assert "endpoints" in data
        assert "documentation" in data
        assert "frontend" in data
        
        # 验证端点结构
        endpoints = data["endpoints"]
        assert "analysis" in endpoints
        assert "apks" in endpoints
        assert "devices" in endpoints
        assert "server" in endpoints
        assert "health" in endpoints
        
        print(f"✅ API 信息端点测试通过: {data}")
    
    def test_server_info_endpoint(self):
        """测试服务器信息端点"""
        response = self.session.get(f"{API_BASE_URL}/api/server/info")
        
        assert response.status_code == 200
        data = response.json()
        
        assert "name" in data
        assert "hostname" in data
        assert "ipAddress" in data
        assert "platform" in data
        assert "apiEndpoint" in data
        assert "services" in data
        assert "capabilities" in data
        
        # 验证服务状态
        services = data["services"]
        assert "api" in services
        assert "database" in services
        assert "analysis" in services
        
        print(f"✅ 服务器信息端点测试通过: {data}")
    
    def test_server_health_endpoint(self):
        """测试服务器健康检查端点"""
        response = self.session.get(f"{API_BASE_URL}/api/server/health")
        
        assert response.status_code == 200
        data = response.json()
        
        assert "status" in data
        assert "timestamp" in data
        assert "services" in data
        
        # 验证健康状态
        assert data["status"] in ["healthy", "degraded", "unhealthy"]
        
        print(f"✅ 服务器健康检查端点测试通过: {data}")
    
    def test_server_config_endpoint(self):
        """测试服务器配置端点"""
        response = self.session.get(f"{API_BASE_URL}/api/server/config")
        
        assert response.status_code == 200
        data = response.json()
        
        # 配置端点可能返回错误信息或配置数据
        if "error" not in data:
            assert "server" in data
            assert "database" in data
            assert "analysis" in data
            assert "logging" in data
        
        print(f"✅ 服务器配置端点测试通过: {data}")
    
    def test_server_stats_endpoint(self):
        """测试服务器统计信息端点"""
        response = self.session.get(f"{API_BASE_URL}/api/server/stats")
        
        assert response.status_code == 200
        data = response.json()
        
        # 统计端点可能返回错误信息或统计数据
        if "error" not in data:
            assert "devices" in data
            assert "apks" in data
            assert "operations" in data
            assert "screenshots" in data
            assert "timestamp" in data
        
        print(f"✅ 服务器统计信息端点测试通过: {data}")
    
    def test_analysis_screenshots_endpoint(self):
        """测试分析截屏列表端点"""
        response = self.session.get(f"{API_BASE_URL}/api/analysis/screenshots")
        
        assert response.status_code == 200
        data = response.json()
        
        # 应该返回截屏列表（可能为空）
        assert isinstance(data, list)
        
        print(f"✅ 分析截屏列表端点测试通过: 返回 {len(data)} 个截屏")
    
    def test_analysis_operations_endpoint(self):
        """测试分析操作记录端点"""
        response = self.session.get(f"{API_BASE_URL}/api/analysis/operations")
        
        assert response.status_code == 200
        data = response.json()
        
        # 应该返回操作记录列表（可能为空）
        assert isinstance(data, list)
        
        print(f"✅ 分析操作记录端点测试通过: 返回 {len(data)} 个操作记录")
    
    def test_apks_list_endpoint(self):
        """测试 APK 列表端点"""
        response = self.session.get(f"{API_BASE_URL}/api/apks")
        
        assert response.status_code == 200
        data = response.json()
        
        # 应该返回 APK 列表（可能为空）
        assert isinstance(data, list)
        
        print(f"✅ APK 列表端点测试通过: 返回 {len(data)} 个 APK")
    
    def test_devices_list_endpoint(self):
        """测试设备列表端点"""
        response = self.session.get(f"{API_BASE_URL}/api/devices")
        
        assert response.status_code == 200
        data = response.json()
        
        # 应该返回设备列表（可能为空）
        assert isinstance(data, list)
        
        print(f"✅ 设备列表端点测试通过: 返回 {len(data)} 个设备")
    
    def test_api_documentation_endpoint(self):
        """测试 API 文档端点"""
        response = self.session.get(f"{API_BASE_URL}/docs")
        
        # 文档端点应该返回 HTML 页面
        assert response.status_code == 200
        assert "text/html" in response.headers["content-type"]
        
        print("✅ API 文档端点测试通过")
    
    def test_api_swagger_endpoint(self):
        """测试 Swagger JSON 端点"""
        response = self.session.get(f"{API_BASE_URL}/openapi.json")
        
        # Swagger JSON 端点应该返回 JSON 数据
        assert response.status_code == 200
        data = response.json()
        
        assert "openapi" in data
        assert "info" in data
        assert "paths" in data
        
        print("✅ Swagger JSON 端点测试通过")


def test_api_response_time():
    """测试 API 响应时间"""
    session = requests.Session()
    
    endpoints_to_test = [
        "/",
        "/api", 
        "/api/server/info",
        "/api/server/health",
        "/api/analysis/screenshots",
        "/api/analysis/operations"
    ]
    
    max_response_time = 5.0  # 最大响应时间（秒）
    
    for endpoint in endpoints_to_test:
        start_time = time.time()
        response = session.get(f"{API_BASE_URL}{endpoint}")
        end_time = time.time()
        
        response_time = end_time - start_time
        
        assert response.status_code == 200
        assert response_time < max_response_time, f"端点 {endpoint} 响应时间过长: {response_time:.2f}s"
        
        print(f"✅ 端点 {endpoint} 响应时间: {response_time:.2f}s")
    
    session.close()


def test_api_error_handling():
    """测试 API 错误处理"""
    session = requests.Session()
    
    # 测试不存在的端点
    response = session.get(f"{API_BASE_URL}/api/nonexistent")
    assert response.status_code == 404
    
    # 测试无效的 HTTP 方法
    response = session.post(f"{API_BASE_URL}/api/server/info")
    assert response.status_code == 405  # Method Not Allowed
    
    # 测试无效的参数
    response = session.get(f"{API_BASE_URL}/api/analysis/screenshots?invalid_param=test")
    # 应该返回 200 或 422（参数验证错误）
    assert response.status_code in [200, 422]
    
    print("✅ API 错误处理测试通过")
    session.close()


def main():
    """主测试函数"""
    print("🚀 开始 Autodroid Analyzer API 端点测试")
    print("=" * 60)
    
    # 检查 API 服务器是否运行
    try:
        response = requests.get(f"{API_BASE_URL}/", timeout=5)
        if response.status_code != 200:
            print("❌ API 服务器未运行，请先启动服务器")
            return 1
    except requests.exceptions.ConnectionError:
        print("❌ 无法连接到 API 服务器，请先启动服务器")
        return 1
    
    # 运行测试
    test_cases = [
        ("API 根端点", TestAPIEndpoints().test_api_root_endpoint),
        ("API 信息端点", TestAPIEndpoints().test_api_info_endpoint),
        ("服务器信息端点", TestAPIEndpoints().test_server_info_endpoint),
        ("健康检查端点", TestAPIEndpoints().test_server_health_endpoint),
        ("配置端点", TestAPIEndpoints().test_server_config_endpoint),
        ("统计端点", TestAPIEndpoints().test_server_stats_endpoint),
        ("截屏列表端点", TestAPIEndpoints().test_analysis_screenshots_endpoint),
        ("操作记录端点", TestAPIEndpoints().test_analysis_operations_endpoint),
        ("APK 列表端点", TestAPIEndpoints().test_apks_list_endpoint),
        ("设备列表端点", TestAPIEndpoints().test_devices_list_endpoint),
        ("API 文档端点", TestAPIEndpoints().test_api_documentation_endpoint),
        ("Swagger 端点", TestAPIEndpoints().test_api_swagger_endpoint),
        ("API 响应时间", test_api_response_time),
        ("API 错误处理", test_api_error_handling)
    ]
    
    passed = 0
    total = len(test_cases)
    
    for test_name, test_func in test_cases:
        try:
            test_func()
            passed += 1
            print(f"✅ {test_name} - 通过")
        except Exception as e:
            print(f"❌ {test_name} - 失败: {e}")
    
    print("\n" + "=" * 60)
    print(f"📊 测试结果: {passed}/{total} 通过")
    
    if passed == total:
        print("🎉 所有 API 测试通过!")
        return 0
    else:
        print("⚠️  部分测试失败，请检查 API 服务器状态")
        return 1


if __name__ == "__main__":
    sys.exit(main())