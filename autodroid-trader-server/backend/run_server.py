#!/usr/bin/env python3
"""
Autodroid Container Server
This script starts both the API server and the frontend application
"""

import uvicorn
from api import main as api_main
from core.tradeplan.daemon import get_daemon
import asyncio
import time
import yaml
import os
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def load_config():
    """Load configuration from config.yaml"""
    config_path = os.path.join(os.path.dirname(__file__), "config.yaml")
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

def print_startup_info(host, port, frontend_mount_path):
    """Print clear startup information for both API and frontend"""
    
    # Get the actual server IP address
    import socket
    try:
        # Get the actual IP address that can be accessed from the network
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.connect(("8.8.8.8", 80))
            server_ip = s.getsockname()[0]
    except:
        # Fallback to hostname resolution
        try:
            server_ip = socket.gethostbyname(socket.gethostname())
        except:
            server_ip = "localhost"
    
    # Determine which URL to display
    if host == "0.0.0.0":
        display_host = server_ip
    else:
        display_host = host
    
    print("\n" + "="*60)
    print("🚀 Autodroid Container Server Started")
    print("="*60)
    print(f"📡 API Server: http://{display_host}:{port}")
    print(f"� API Endpoint: http://{display_host}:{port}/api")
    print(f"�📚 API Documentation: http://{display_host}:{port}/docs")
    print(f"🌐 Frontend Application: http://{display_host}:{port}{frontend_mount_path}")
    print(f"🔍 API Health Check: http://{display_host}:{port}/api/health")
    print("="*60)
    print("Press Ctrl+C to stop the server")
    print("="*60 + "\n")

async def main():
    """Run the server with proper async handling"""
    config = load_config()
    
    daemon = get_daemon()
    await daemon.start()
    logger.info("Trade plan daemon started successfully")
    
    # Get server configuration with defaults
    server_config = config.get('server', {})
    backend_config = server_config.get('backend', {})
    frontend_config = server_config.get('frontend', {})
    
    host = backend_config.get('host', '0.0.0.0')  # Bind to all interfaces by default
    port = backend_config.get('port', 8004)  # Use 8004 as default
    log_level = backend_config.get('log_level', 'info')
    reload = backend_config.get('reload', False)
    
    # Get frontend mount path from config
    frontend_mount_path = frontend_config.get('mount_path', '/app')
    
    # Print startup information
    print_startup_info(host, port, frontend_mount_path)
    
    uvicorn_config = uvicorn.Config(
        api_main.app,
        host=host,
        port=port,
        log_level=log_level,
        reload=reload
    )
    server = uvicorn.Server(uvicorn_config)
    
    await server.serve()

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n🛑 Server stopped by user")
    except Exception as e:
        print(f"❌ Server error: {e}")