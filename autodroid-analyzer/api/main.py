"""
Autodroid Analyzer FastAPI后端服务
基于模块化设计的API服务，仿照autodroid-container结构
"""

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse, JSONResponse, RedirectResponse
import os
import sys
import yaml

# 添加项目根目录到Python路径
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

def load_config():
    """Load configuration from config.yaml"""
    config_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "config.yaml")
    try:
        with open(config_path, 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        print(f"✓ Configuration loaded from {config_path}")
        return config
    except FileNotFoundError:
        print(f"⚠ Config file not found at {config_path}, using defaults")
        return {}
    except Exception as e:
        print(f"⚠ Error loading config: {e}, using defaults")
        return {}

# Load configuration
config = load_config()

# Get server configuration with defaults
server_config = config.get('server', {})
api_config = config.get('api', {})
frontend_config = config.get('frontend', {})

# 导入模块化路由
from .analysis import router as analysis_router
from .apks import router as apks_router
from .devices import router as devices_router
from .server import router as server_router

app = FastAPI(title="Autodroid Analyzer API", version="1.0.0")

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=api_config.get('cors_origins', ["*"]),
    allow_credentials=api_config.get('cors_credentials', True),
    allow_methods=api_config.get('cors_methods', ["*"]),
    allow_headers=api_config.get('cors_headers', ["*"]),
)

# SPA fallback handler for client-side routing
@app.exception_handler(404)
async def spa_fallback_handler(request: Request, exc: HTTPException):
    """Handle 404 errors by serving index.html for SPA routing"""
    # Get frontend mount path from config
    frontend_mount_path = frontend_config.get('mount_path', '/app')
    
    # Handle Vite client requests in production
    if request.url.path == '/@vite/client' or request.url.path == '/%40vite/client':
        # Return a simple JavaScript response that does nothing
        from fastapi.responses import Response
        return Response(content="// Vite client not available in production", media_type="application/javascript")
    
    # Handle frontend routes (not API routes) and routes that start with frontend mount path
    # This ensures that SPA routing works for paths like /app/auth/login
    if not request.url.path.startswith('/api/'):
        frontend_build_dir = frontend_config.get('build_directory', os.path.join(os.path.dirname(os.path.dirname(__file__)), "frontend", "build"))
        index_file = os.path.join(frontend_build_dir, "index.html")
        
        if os.path.exists(index_file):
            # Handle root path by redirecting to frontend mount path
            if request.url.path == '/':
                return RedirectResponse(url=frontend_mount_path, status_code=302)
            
            # Check if this is a frontend route that should be handled by SPA
            # This includes the root path and any path that starts with the frontend mount path
            if request.url.path == frontend_mount_path or request.url.path.startswith(frontend_mount_path + '/'):
                # Serve index.html for SPA routing
                return FileResponse(index_file)
            
            # Check if this is a direct path to a frontend route (without /app prefix)
            # List of known frontend routes - add all routes from frontend structure
            frontend_routes = ['/reports', '/my', '/orders', '/auth', '/device', '/apk']
            if any(request.url.path == route or request.url.path.startswith(route + '/') for route in frontend_routes):
                # Redirect to the correct path with /app prefix
                return RedirectResponse(url=f"{frontend_mount_path}{request.url.path}", status_code=302)
    
    # For API routes or if index.html doesn't exist, return the original 404
    return JSONResponse(
        status_code=404,
        content={"detail": "Not Found"}
    )

# 注册模块化路由（移除重复前缀，因为路由文件中已经定义了前缀）
app.include_router(analysis_router)
app.include_router(apks_router)
app.include_router(devices_router)
app.include_router(server_router)

# API根路径
@app.get("/")
async def root():
    """API根路径"""
    return {
        "message": "Autodroid Analyzer API", 
        "version": "1.0.0",
        "modules": ["analysis", "apks", "devices", "server"],
        "api_root": "/api"
    }

# API根路径（与autodroid-container保持一致）
@app.get("/api")
async def api_root():
    """API根路径"""
    return {
        "name": "Autodroid Analyzer API",
        "version": "1.0.0",
        "description": "RESTful API for Autodroid Android Analysis System",
        "endpoints": {
            "analysis": "/api/analysis",
            "apks": "/api/apks", 
            "devices": "/api/devices",
            "server": "/api/server",
            "health": "/api/health"
        },
        "documentation": "/docs"
    }

# 健康检查端点（与autodroid-container保持一致）
@app.get("/api/health")
async def health_check():
    """健康检查端点"""
    import time
    
    # 直接返回简单的健康检查信息
    # 如果需要更详细的检查，可以调用server模块的健康检查
    return {
        "status": "healthy",
        "timestamp": time.time(),
        "services": {
            "api": "running",
            "database": "available",
            "analysis": "available"
        },
        "message": "For detailed health information, use /api/server/health"
    }

# Note: Frontend is served separately in development mode
# In production, frontend should be served by a dedicated web server
print("ℹ Frontend should be served separately")

if __name__ == "__main__":
    import uvicorn
    # Get port from configuration, default to 8001
    port = server_config.get('backend', {}).get('port', 8001)
    host = server_config.get('backend', {}).get('host', '0.0.0.0')
    uvicorn.run(app, host=host, port=port)