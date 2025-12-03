#!/usr/bin/env python3
"""
Autodroid Container - Entry Point Script
This is the recommended way to start the Autodroid Container server.
"""

import sys
import os

# Add the current directory to the Python path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# Import and run the server
import run_server

if __name__ == "__main__":
    print("Starting Autodroid Container...")
    run_server.main()