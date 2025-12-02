#!/usr/bin/env python3
"""
Simple server runner script for Autodroid API
"""

import uvicorn
import api.main
import asyncio
import time

async def main():
    """Run the server with proper async handling"""
    config = uvicorn.Config(
        api.main.app,
        host="127.0.0.1",
        port=8003,
        log_level="info"
    )
    server = uvicorn.Server(config)
    
    print("Starting Autodroid server...")
    await server.serve()

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nServer stopped by user")
    except Exception as e:
        print(f"Server error: {e}")