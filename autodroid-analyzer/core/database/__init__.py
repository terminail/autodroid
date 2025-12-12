"""
统一数据库管理模块（基于peewee ORM）
所有核心模块（apk、analysis、device、screenshot、useroperation）共享同一个数据库
"""

import os
from typing import Optional
from .models import db, create_tables

class DatabaseManager:
    """统一数据库管理器（基于peewee ORM）"""
    
    def __init__(self, db_path: Optional[str] = None):
        """
        初始化数据库管理器
        
        Args:
            db_path: 数据库文件路径，如果为None则使用配置文件中的路径
        """
        # 确保数据库目录存在（仅对文件路径有效，跳过内存数据库和空路径）
        if db_path and db_path != ':memory:' and os.path.dirname(db_path):
            os.makedirs(os.path.dirname(db_path), exist_ok=True)
        
        # 初始化peewee数据库连接
        self.db = db
        
        # 创建所有表
        create_tables()
    
    def get_connection(self):
        """获取数据库连接（返回peewee数据库实例）"""
        return self.db
    
    def close(self):
        """关闭数据库连接"""
        if self.db:
            self.db.close()
    
    def __del__(self):
        """析构函数，确保连接关闭"""
        self.close()

# 全局数据库管理器实例
_db_manager: Optional[DatabaseManager] = None

def get_database_manager(db_path: Optional[str] = None) -> DatabaseManager:
    """获取全局数据库管理器实例"""
    global _db_manager
    if _db_manager is None:
        _db_manager = DatabaseManager(db_path)
    return _db_manager

# 导出模块级别的连接获取函数
def get_db_connection():
    """获取数据库连接（快捷方式，返回peewee数据库实例）"""
    return get_database_manager().get_connection()

__all__ = [
    'db', 'create_tables', 'DatabaseManager', 'get_database_manager', 'get_db_connection'
]