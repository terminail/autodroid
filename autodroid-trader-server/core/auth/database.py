import time
from typing import Optional, Dict, Any
from datetime import datetime
import hashlib
import secrets
import re
import uuid
from peewee import DoesNotExist

from ..database.base import BaseDatabase
from ..database.models import User


class AuthDatabase(BaseDatabase):
    """认证数据库管理类（使用peewee ORM）"""
    
    def __init__(self):
        """初始化认证数据库"""
        super().__init__()
    
    def _hash_password(self, password: str) -> str:
        """密码哈希（使用SHA256 + salt）"""
        salt = secrets.token_hex(16)
        password_hash = hashlib.sha256((password + salt).encode()).hexdigest()
        return f"{salt}${password_hash}"
    
    def _verify_password(self, plain_password: str, hashed_password: str) -> bool:
        """验证密码"""
        try:
            salt, stored_hash = hashed_password.split('$')
            computed_hash = hashlib.sha256((plain_password + salt).encode()).hexdigest()
            return computed_hash == stored_hash
        except (ValueError, AttributeError):
            return False
    
    def _generate_user_id(self, email: str) -> str:
        """生成基于邮箱的简化用户ID"""
        # 使用邮箱前缀作为用户ID的基础
        email_prefix = email.split('@')[0]
        # 移除特殊字符，只保留字母数字
        clean_id = re.sub(r'[^a-zA-Z0-9]', '', email_prefix)
        # 如果太短，添加随机后缀
        if len(clean_id) < 3:
            clean_id += str(uuid.uuid4())[:8]
        return clean_id.lower()
    
    def create_user(self, email: str, name: str, password: str, role: str = "user") -> Optional[str]:
        """创建新用户"""
        try:
            # 检查邮箱是否已存在
            if User.select().where(User.email == email).exists():
                raise ValueError("邮箱已存在")
            
            # 生成基于邮箱的简化用户ID
            user_id = self._generate_user_id(email)
            
            # 确保用户ID唯一，如果冲突则添加数字后缀
            original_id = user_id
            counter = 1
            while User.select().where(User.id == user_id).exists():
                user_id = f"{original_id}{counter}"
                counter += 1
            
            # 密码哈希
            password_hash = self._hash_password(password)
            
            # 创建用户
            user = User.create(
                id=user_id,
                email=email,
                name=name,
                password_hash=password_hash,
                role=role
            )
            
            return user.id
            
        except Exception:
            return None
    
    def authenticate_user(self, email: str, password: str) -> Optional[Dict[str, Any]]:
        """用户认证"""
        try:
            # 获取用户信息
            user = User.get(User.email == email)
            
            # 验证密码
            if not self._verify_password(password, user.password_hash):
                return None
            
            # 更新最后登录时间
            user.last_login = datetime.now()
            user.save()
            
            # 返回用户信息
            return {
                "id": user.id,
                "email": user.email,
                "name": user.name,
                "role": user.role,
                "last_login": user.last_login,
                "created_at": user.created_at
            }
            
        except DoesNotExist:
            return None
        except Exception:
            return None
    
    def get_user_by_id(self, user_id: str) -> Optional[Dict[str, Any]]:
        """根据ID获取用户信息"""
        try:
            user = User.get(User.id == user_id)
            return {
                "id": user.id,
                "email": user.email,
                "name": user.name,
                "role": user.role,
                "last_login": user.last_login,
                "created_at": user.created_at
            }
        except DoesNotExist:
            return None
        except Exception:
            return None
    
    def get_user_id_by_email(self, email: str) -> Optional[str]:
        """根据邮箱获取用户ID"""
        try:
            user = User.get(User.email == email)
            return user.id
        except DoesNotExist:
            return None
        except Exception:
            return None
    
    def get_user_by_email(self, email: str) -> Optional[Dict[str, Any]]:
        """根据邮箱获取用户信息"""
        try:
            user = User.get(User.email == email)
            return {
                "id": user.id,
                "email": user.email,
                "name": user.name,
                "role": user.role,
                "last_login": user.last_login,
                "created_at": user.created_at
            }
        except DoesNotExist:
            return None
        except Exception:
            return None
    
