"""
数据库基类模块
定义所有数据库管理类的基类，提供统一的数据库连接和表创建方法
"""

from typing import Optional
from . import db, create_tables


class BaseDatabase:
    """数据库基类（基于peewee ORM）"""
    
    def __init__(self, db_path: Optional[str] = None):
        """
        初始化数据库基类
        
        Args:
            db_path: 数据库文件路径，如果为None则使用配置文件中的路径
        """
        # 使用统一的数据库连接
        self.db = db
        
        # 确保表已创建
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