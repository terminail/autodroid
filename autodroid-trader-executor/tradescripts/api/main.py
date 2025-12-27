from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime
from enum import Enum
import os
import uuid
from pathlib import Path
import asyncio
import logging
import sys
from core.tradescript import TradeScriptResponse, TradeScriptListResponse, scan_apks_directory

# Load configuration from shared config module
from core.config import get_server_config, load_config
from core.daemon import get_daemon

config_data = get_server_config()
log_config = load_config().get('logging', {})

logs_dir = Path(__file__).parent.parent / "logs"
logs_dir.mkdir(exist_ok=True)

log_level = getattr(logging, log_config.get('level', 'info').upper(), logging.INFO)
log_file = logs_dir / log_config.get('file_path', 'autodroid-trader-executor.log').split('/')[-1]

logging.basicConfig(
    level=log_level,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(log_file, encoding='utf-8'),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger(__name__)



app = FastAPI(
    title="Tradescript API",
    version="1.0.0",
    debug=True,  # Always True for development server
    docs_url=config_data.get('docs_url', '/docs'),
    redoc_url=config_data.get('redoc_url', '/redoc')
)


@app.on_event("startup")
async def startup_event():
    """应用启动时启动 Daemon"""
    server_url = config_data.get('trader_server_api_endpoint', 'http://localhost:8008/api')
    poll_interval = config_data.get('poll_interval', 5)
    daemon = get_daemon(server_url)
    await daemon.start(poll_interval)
    logger.info(f"交易计划 Daemon 已启动，轮询 Server: {server_url}")


@app.on_event("shutdown")
async def shutdown_event():
    """应用停止时停止 Daemon"""
    daemon = get_daemon()
    await daemon.stop()
    logger.info("交易计划 Daemon 已停止")


@app.get(f"{config_data.get('api_base', '/api')}/tradescripts", response_model=TradeScriptListResponse)
async def get_tradescripts():
    """获取交易脚本信息列表"""
    tradescripts = scan_apks_directory()
    return TradeScriptListResponse(tradescripts=tradescripts, total=len(tradescripts))


@app.get("/")
async def root():
    """Root endpoint for health check"""
    return {"message": "Tradescript API is running", "version": "1.0.0"}