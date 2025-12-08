from fastapi import FastAPI, HTTPException, BackgroundTasks, Depends, Header, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.security import HTTPBearer
from fastapi.responses import FileResponse, JSONResponse
from typing import List, Dict, Any, Optional
from contextlib import asynccontextmanager

# Import routers from feature-based files
from .auth import router as auth_router
from .devices import router as devices_router
from .server import router as server_router
from .apks import router as apks_router
from .workscripts import router as workscripts_router
from .mdns import MDNSService, register_mdns_from_config

import os
import asyncio
import yaml

from core.device.service import DeviceManager
from core.device.models import DeviceInfo
from core.auth.models import UserCreate, UserLogin, UserResponse, Token
from core.auth.service import AuthService

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
database_config = config.get('database', {})
auth_config = config.get('authentication', {})

# Lifespan event handler
@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifespan context manager for startup and shutdown events"""
    # Startup
    # Register mDNS service
    mdns_service = await register_mdns_from_config(config)
    app.state.mdns_service = mdns_service
    
    yield
    
    # Shutdown
    if hasattr(app.state, 'mdns_service'):
        await app.state.mdns_service.unregister_service()

# Initialize FastAPI app with lifespan
app = FastAPI(
    title="Autodroid API",
    description="RESTful API for Autodroid Android Automation System",
    version="1.0.0",
    lifespan=lifespan
)

# Add CORS middleware
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
                from fastapi.responses import RedirectResponse
                return RedirectResponse(url=frontend_mount_path, status_code=302)
            
            # Check if this is a frontend route that should be handled by SPA
            # This includes the root path and any path that starts with the frontend mount path
            if request.url.path == frontend_mount_path or request.url.path.startswith(frontend_mount_path + '/'):
                # Serve index.html for SPA routing
                return FileResponse(index_file)
            
            # Check if this is a direct path to a frontend route (without /app prefix)
            # List of known frontend routes
    frontend_routes = ['/reports', '/my', '/orders', '/auth']
    if any(request.url.path == route or request.url.path.startswith(route + '/') for route in frontend_routes):
                # Redirect to the correct path with /app prefix
                from fastapi.responses import RedirectResponse
                return RedirectResponse(url=f"{frontend_mount_path}{request.url.path}", status_code=302)
    
    # For API routes or if index.html doesn't exist, return the original 404
    return JSONResponse(
        status_code=404,
        content={"detail": "Not Found"}
    )

# Include routers from feature-based files FIRST to ensure API routes have priority
app.include_router(auth_router)
app.include_router(devices_router)
app.include_router(server_router)
app.include_router(apks_router)
app.include_router(workscripts_router)

# Add API root endpoint
@app.get("/api")
async def api_root():
    """Get API information and available endpoints"""
    return {
        "name": "Autodroid API",
        "version": "1.0.0",
        "description": "RESTful API for Autodroid Android Automation System",
        "endpoints": {
            "auth": "/api/auth",
            "devices": "/api/devices",
            "server": "/api/server",
            "apks": "/api/apks",
            "workscripts": "/api/{app_package}/workscripts",
            "config": "/api/config",
            "health": "/api/health"
        },
        "documentation": "/docs",
        "frontend": "/app"
    }

# Add favicon endpoint
@app.get("/favicon.ico")
async def favicon():
    """Serve the favicon"""
    favicon_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "favicon.ico")
    if os.path.exists(favicon_path):
        return FileResponse(favicon_path, media_type="image/x-icon")
    else:
        # Return a simple default favicon if file doesn't exist
        from fastapi.responses import Response
        return Response(content=b"", media_type="image/x-icon")

# Add configuration endpoint
@app.get("/api/config")
async def get_config():
    """Get the current server configuration"""
    return config

# Mount static files for frontend (only if frontend build exists)
frontend_build_dir = frontend_config.get('build_directory', os.path.join(os.path.dirname(os.path.dirname(__file__)), "frontend", "build"))
if os.path.exists(frontend_build_dir):
    # Mount frontend at /app path to avoid conflicts with API routes
    frontend_mount_path = frontend_config.get('mount_path', '/app')
    
    # Mount static files with html=True to support SPA routing
    app.mount(frontend_mount_path, StaticFiles(directory=frontend_build_dir, html=True), name="frontend")
    print(f"✓ Frontend mounted at {frontend_mount_path} from {frontend_build_dir}")
else:
    print("⚠ Frontend build directory not found, API routes will be available at root")

# Initialize core components
device_manager = DeviceManager()

# Initialize authentication service
auth_service = AuthService(
    secret_key=auth_config.get('secret_key', "your-secret-key-change-in-production"),
    token_expire_minutes=auth_config.get('token_expire_minutes', 60)
)

# Security scheme
security = HTTPBearer()

# Configuration
APPIUM_URL = os.getenv("APPIUM_URL", api_config.get('appium_url', "http://appium-server:4723"))
MAX_CONCURRENT_TASKS = int(os.getenv("MAX_CONCURRENT_TASKS", str(api_config.get('max_concurrent_tasks', 5))))
