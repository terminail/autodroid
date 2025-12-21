"""
服务器模块的数据库管理
包含服务器信息的数据库操作
"""

import time
from typing import List, Optional, Dict, Any
from peewee import DoesNotExist, CharField, IntegerField, TextField, DateTimeField

from ..database.base import BaseDatabase
from ..database.models import db, create_tables, BaseModel


class Server(BaseModel):
    """服务器模型"""
    id = CharField(primary_key=True)
    name = CharField()
    ip_address = CharField()
    port = IntegerField()
    platform = CharField()
    protocol = CharField(default='http')
    version = CharField(default='1.0')
    services = TextField(null=True)  # JSON格式存储
    capabilities = TextField(null=True)  # JSON格式存储
    created_at = DateTimeField()
    updated_at = DateTimeField()
    
    class Meta:
        database = db
        table_name = 'servers'


# 确保在导入时创建表
def ensure_tables():
    """确保服务器相关表已创建"""
    if not Server.table_exists():
        Server.create_table()


class ServerDatabase(BaseDatabase):
    """服务器数据库管理类"""
    
    def __init__(self):
        """初始化服务器数据库"""
        super().__init__()
        # 确保服务器表已创建
        ensure_tables()
    
    def get_server(self, server_id: str) -> Optional[Server]:
        """根据ID获取服务器"""
        try:
            return Server.get(Server.id == server_id)
        except DoesNotExist:
            return None
    
    def get_server_by_ip_port(self, ip_address: str, port: int) -> Optional[Server]:
        """根据IP和端口获取服务器"""
        try:
            return Server.get((Server.ip_address == ip_address) & (Server.port == port))
        except DoesNotExist:
            return None
    
    def get_all_servers(self) -> List[Server]:
        """获取所有服务器"""
        return list(Server.select())
    
    def create_or_update_server(self, server_info: Dict[str, Any]) -> Server:
        """创建或更新服务器信息"""
        server_id = server_info.get('id', f"{server_info.get('ip_address', 'unknown')}:{server_info.get('port', 0)}")
        ip_address = server_info.get('ip_address')
        port = server_info.get('port')
        
        if not ip_address or not port:
            raise ValueError("IP地址和端口是必需的")
        
        import json
        services_json = json.dumps(server_info.get('services', {}))
        capabilities_json = json.dumps(server_info.get('capabilities', {}))
        
        server, created = Server.get_or_create(
            id=server_id,
            defaults={
                'name': server_info.get('name', 'Autodroid Server'),
                'ip_address': ip_address,
                'port': port,
                'platform': server_info.get('platform', 'unknown'),
                'protocol': server_info.get('protocol', 'http'),
                'version': server_info.get('version', '1.0'),
                'services': services_json,
                'capabilities': capabilities_json,
                'created_at': time.time(),
                'updated_at': time.time()
            }
        )
        
        # 如果不是新创建的，更新服务器信息
        if not created:
            server.name = server_info.get('name', server.name)
            server.ip_address = ip_address
            server.port = port
            server.platform = server_info.get('platform', server.platform)
            server.protocol = server_info.get('protocol', server.protocol)
            server.version = server_info.get('version', server.version)
            server.services = services_json
            server.capabilities = capabilities_json
            server.updated_at = time.time()
            server.save()
        
        return server
    
    def delete_server(self, server_id: str) -> bool:
        """删除服务器"""
        try:
            server = Server.get(Server.id == server_id)
            server.delete_instance()
            return True
        except DoesNotExist:
            return False
    
    def get_server_count(self) -> int:
        """获取服务器总数"""
        return Server.select().count()