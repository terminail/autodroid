"""
服务器模块的业务逻辑层
处理服务器信息管理和二维码生成
"""

import socket
import json
import qrcode
import io
import base64
from typing import Dict, Any, Optional
from datetime import datetime, timedelta
from pydantic import BaseModel

from .database import ServerDatabase
from .models import ServerInfoResponse, QRCodeResponse


class ServerManager:
    """服务器管理器，处理当前服务器实例的信息和二维码生成"""
    
    def __init__(self):
        """初始化服务器管理器"""
        self.db = ServerDatabase()
        self._server_info = None
    
    def get_server_info(self, force_refresh: bool = False) -> ServerInfoResponse:
        """获取当前服务器信息"""
        if self._server_info is None or force_refresh:
            # 获取主机名和IP地址
            hostname = socket.gethostname()
            ip_address = socket.gethostbyname(hostname)
            
            # 获取服务器配置
            import os
            import yaml
            config_path = os.path.join(os.path.dirname(__file__), "..", "..", "config.yaml")
            
            port = 8003  # 默认端口
            use_https = False
            platform = "unknown"
            
            if os.path.exists(config_path):
                with open(config_path, 'r') as f:
                    config = yaml.safe_load(f)
                    server_config = config.get('server', {})
                    server_backend_config = server_config.get('backend', {})
                    port = server_backend_config.get('port', 8003)
                    use_https = server_backend_config.get('use_https', False)
            
            # 获取平台信息
            import platform as platform_module
            platform = platform_module.system()
            
            # 创建服务器信息
            self._server_info = ServerInfoResponse(
                ip=ip_address,
                port=port,
                name="Autodroid Server",
                platform=platform,
                services={
                    "api": f"{'https' if use_https else 'http'}://{ip_address}:{port}/api",
                    "mdns": "Autodroid._http._tcp.local"
                },
                capabilities={
                    "device_management": True,
                    "apk_management": True,
                    "workscript_execution": True,
                    "qr_code_generation": True
                }
            )
            
            # 保存到数据库
            self._save_server_info_to_db()
        
        return self._server_info
    
    def _save_server_info_to_db(self):
        """将服务器信息保存到数据库"""
        if self._server_info:
            server_data = {
                "id": "current_server",
                "name": self._server_info.name,
                "ip_address": self._server_info.ip,
                "port": self._server_info.port,
                "platform": self._server_info.platform,
                "services": self._server_info.services,
                "capabilities": self._server_info.capabilities
            }
            self.db.create_or_update_server(server_data)
    
    def generate_qr_code(self, expiry_hours: int = 24) -> QRCodeResponse:
        """生成包含服务器连接信息的二维码"""
        try:
            # 获取服务器信息
            server_info = self.get_server_info()
            
            # 创建简化的服务器信息用于二维码（只包含必要字段）
            qr_server_info = ServerInfoResponse(
                ip=server_info.ip,
                port=server_info.port,
                name=server_info.name,
                platform=server_info.platform
            )
            
            # 转换为JSON字符串
            qr_json = json.dumps(qr_server_info.dict(), separators=(',', ':'))
            
            # 生成二维码
            qr = qrcode.QRCode(
                version=1,
                error_correction=qrcode.constants.ERROR_CORRECT_L,
                box_size=10,
                border=4,
            )
            qr.add_data(qr_json)
            qr.make(fit=True)
            
            # 创建图像
            img = qr.make_image(fill_color="black", back_color="white")
            
            # 转换为Base64
            buffer = io.BytesIO()
            img.save(buffer, format="PNG")
            img_str = base64.b64encode(buffer.getvalue()).decode()
            
            # 生成过期时间
            expiry_time = datetime.now() + timedelta(hours=expiry_hours)
            
            # 返回响应
            return QRCodeResponse(
                success=True,
                message="二维码生成成功",
                qr_code_data=qr_json,
                qr_code_image=f"data:image/png;base64,{img_str}",
                expires_at=expiry_time
            )
            
        except Exception as e:
            return QRCodeResponse(
                success=False,
                message=f"生成二维码失败: {str(e)}"
            )
    
    def get_server_status(self) -> Dict[str, Any]:
        """获取服务器状态信息"""
        server_info = self.get_server_info()
        
        # 获取数据库统计信息
        from ..device.service import DeviceManager
        from ..apk.service import ApkManager
        
        device_manager = DeviceManager()
        apk_manager = ApkManager()
        
        device_count = device_manager.get_device_count()
        online_device_count = device_manager.get_online_device_count()
        apk_count = apk_manager.get_apk_count()
        
        return {
            "server": server_info.dict(),
            "statistics": {
                "device_count": device_count,
                "online_device_count": online_device_count,
                "apk_count": apk_count
            },
            "status": "running"
        }


if __name__ == "__main__":
    """Main entry point for server manager service"""
    import logging
    import time
    
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)
    
    server_manager = ServerManager()
    logger.info("Server Manager Service Started")
    
    # Keep the service running
    try:
        while True:
            time.sleep(60)
    except KeyboardInterrupt:
        logger.info("Server Manager Service Stopped")