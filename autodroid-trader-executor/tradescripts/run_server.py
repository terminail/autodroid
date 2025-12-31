#!/usr/bin/env python3
"""
Tradescripts Server
This script starts the API server for managing tradescripts
"""

import uvicorn
from api.main import app
import yaml
import os
import logging
import socket
import sys


logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# Add core directory to path to import config module
sys.path.append(os.path.join(os.path.dirname(__file__), "core"))
from config import get_server_config


def print_startup_info(host, port):
    """Print clear startup information for the server"""
    
    # Get the actual server IP address
    try:
        # Get the actual IP address that can be accessed from the network
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.connect(("8.8.8.8", 80))
            server_ip = s.getsockname()[0]
    except (socket.error, OSError):
        # Fallback to hostname resolution
        try:
            server_ip = socket.gethostbyname(socket.gethostname())
        except (socket.gaierror, socket.error):
            server_ip = "localhost"
    
    # Determine which URL to display
    if host == "0.0.0.0":
        display_host = server_ip
    else:
        display_host = host
    
    print("\n" + "="*60)
    print("🚀 Autodroid Tradescripts Server Started")
    print("="*60)
    print(f"📡 Server: http://{display_host}:{port}")
    print(f"📚 API Endpoint: http://{display_host}:{port}/api")
    print(f"🔍 API Documentation: http://{display_host}:{port}/docs")
    print("="*60)
    print("Press Ctrl+C to stop the server")
    print("="*60 + "\n")


def main():
    config = get_server_config()
    
    # Extract values from the config dict
    host = config['host']
    port = config['port']
    log_level = config['log_level']
    reload = config['reload']
    
    # Print startup information
    print_startup_info(host, port)
    
    uvicorn.run(
        "api.main:app",
        host=host,
        port=port,
        log_level=log_level,
        reload=reload
    )


if __name__ == "__main__":
    main()