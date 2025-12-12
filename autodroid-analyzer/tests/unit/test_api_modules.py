#!/usr/bin/env python3
"""
Autodroid Analyzer API 模块单元测试
测试 API 模块的导入和基本功能
"""

import sys
import os

# 添加项目根目录到Python路径
project_root = os.path.join(os.path.dirname(__file__), '..', '..')
sys.path.insert(0, project_root)


def test_api_module_imports():
    """测试 API 模块导入"""
    print("🔍 测试 API 模块导入...")
    
    try:
        # 测试 API 主模块导入
        from api.main import app
        print("✅ API 主模块导入成功")
        
        # 测试路由模块导入
        from api.analysis import router as analysis_router
        print("✅ Analysis 路由模块导入成功")
        
        from api.apks import router as apks_router
        print("✅ APKs 路由模块导入成功")
        
        from api.devices import router as devices_router
        print("✅ Devices 路由模块导入成功")
        
        from api.server import router as server_router
        print("✅ Server 路由模块导入成功")
        
        # 测试模型导入
        from api.models import (
            AnalysisResult, AnalysisRequest, ScreenshotInfo, 
            PageElementInfo, UserOperationInfo, ServerInfo, HealthCheck
        )
        print("✅ API 模型导入成功")
        
        # 验证 FastAPI 应用
        assert hasattr(app, 'router'), "应用应该有路由器"
        assert hasattr(app, 'openapi'), "应用应该有 OpenAPI 文档"
        
        print("✅ FastAPI 应用验证成功")
        
        return True
        
    except Exception as e:
        print(f"❌ API 模块导入失败: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_api_routes_definition():
    """测试 API 路由定义"""
    print("\n🔍 测试 API 路由定义...")
    
    try:
        from api.main import app
        
        # 获取所有路由
        routes = app.routes
        
        # 检查关键路由是否存在
        route_paths = [route.path for route in routes if hasattr(route, 'path')]
        
        expected_routes = [
            '/',
            '/api',
            '/api/server/info',
            '/api/server/health',
            '/api/server/config',
            '/api/server/stats',
            '/api/analysis/screenshots',
            '/api/analysis/operations',
            '/api/analysis/apks/{apk_id}/operations',
            '/api/analysis/apks/{apk_id}/screenshots',
            '/api/analysis/screenshots/{screenshot_id}/elements',
            '/api/apks',
            '/api/devices',
            '/docs',
            '/openapi.json'
        ]
        
        # 检查每个预期路由是否存在
        missing_routes = []
        for expected_route in expected_routes:
            if expected_route not in route_paths:
                missing_routes.append(expected_route)
        
        if missing_routes:
            print(f"⚠️  缺少的路由: {missing_routes}")
            print(f"当前定义的路由: {route_paths}")
        else:
            print("✅ 所有预期路由都已定义")
        
        # 至少应该有基本的路由
        assert '/' in route_paths, "应该定义根路由"
        assert '/api' in route_paths, "应该定义 API 根路由"
        assert '/docs' in route_paths, "应该定义文档路由"
        
        print(f"✅ 路由定义测试通过，共找到 {len(route_paths)} 个路由")
        
        return True
        
    except Exception as e:
        print(f"❌ API 路由定义测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_api_models():
    """测试 API 数据模型"""
    print("\n🔍 测试 API 数据模型...")
    
    try:
        from api.models import (
            AnalysisResult, AnalysisRequest, ScreenshotInfo, 
            PageElementInfo, UserOperationInfo, ServerInfo, HealthCheck
        )
        
        # 测试模型实例化
        server_info = ServerInfo(
            name="Test Server",
            hostname="localhost",
            ipAddress="127.0.0.1",
            platform="test",
            apiEndpoint="/api",
            services={"api": "running"},
            capabilities={}
        )
        
        health_check = HealthCheck(
            status="healthy",
            timestamp=1234567890,
            services={"api": "healthy"}
        )
        
        screenshot_info = ScreenshotInfo(
            id="test-id",
            apk_id="com.example.app",
            timestamp=1234567890,
            file_path="/path/to/screenshot.png",
            page_title="Test Page",
            analysis_status="completed",
            created_at=1234567890
        )
        
        # 验证模型属性
        assert server_info.name == "Test Server"
        assert health_check.status == "healthy"
        assert screenshot_info.apk_id == "com.example.app"
        
        print("✅ API 数据模型测试通过")
        
        return True
        
    except Exception as e:
        print(f"❌ API 数据模型测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_api_configuration():
    """测试 API 配置"""
    print("\n🔍 测试 API 配置...")
    
    try:
        from api.main import app
        
        # 验证应用配置
        assert app.title == "Autodroid Analyzer API"
        assert app.version == "1.0.0"
        
        # 验证 CORS 配置
        cors_middleware = None
        for middleware in app.user_middleware:
            if 'CORSMiddleware' in str(middleware.cls):
                cors_middleware = middleware
                break
        
        assert cors_middleware is not None, "应该配置 CORS 中间件"
        
        print("✅ API 配置测试通过")
        
        return True
        
    except Exception as e:
        print(f"❌ API 配置测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False


def main():
    """主测试函数"""
    print("🚀 开始 Autodroid Analyzer API 模块单元测试")
    print("=" * 60)
    
    tests = [
        ("API 模块导入", test_api_module_imports),
        ("API 路由定义", test_api_routes_definition),
        ("API 数据模型", test_api_models),
        ("API 配置", test_api_configuration)
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
        print("🎉 所有 API 模块单元测试通过!")
        return 0
    else:
        print("⚠️  部分测试失败，请检查 API 模块实现")
        return 1


if __name__ == "__main__":
    sys.exit(main())