import sqlite3
import os
from typing import Optional, Dict
from datetime import datetime
import hashlib
import uuid
import secrets

class UserDatabase:
    """用户数据库管理类"""
    
    def __init__(self, db_path: str = "autodroid.db"):
        self.db_path = db_path
        self._init_db()
    
    def _init_db(self):
        """初始化数据库表"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        # 创建用户表
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                email TEXT UNIQUE NOT NULL,
                name TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                role TEXT DEFAULT 'user',
                last_login DATETIME,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        
        # 创建会话表
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS sessions (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                token TEXT NOT NULL,
                expires_at DATETIME NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users (id)
            )
        ''')
        
        conn.commit()
        conn.close()
    
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
        import re
        clean_id = re.sub(r'[^a-zA-Z0-9]', '', email_prefix)
        # 如果太短，添加随机后缀
        if len(clean_id) < 3:
            clean_id += str(uuid.uuid4())[:8]
        return clean_id.lower()
    
    def create_user(self, email: str, name: str, password: str, role: str = "user") -> Optional[str]:
        """创建新用户"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            # 检查邮箱是否已存在
            cursor.execute("SELECT id FROM users WHERE email = ?", (email,))
            existing_user = cursor.fetchone()
            if existing_user:
                raise ValueError("邮箱已存在")
            
            # 生成基于邮箱的简化用户ID
            user_id = self._generate_user_id(email)
            
            # 确保用户ID唯一，如果冲突则添加数字后缀
            original_id = user_id
            counter = 1
            while True:
                cursor.execute("SELECT id FROM users WHERE id = ?", (user_id,))
                if not cursor.fetchone():
                    break
                user_id = f"{original_id}{counter}"
                counter += 1
            
            # 密码哈希
            password_hash = self._hash_password(password)
            
            # 插入用户数据（email 同时作为 username）
            cursor.execute('''
                INSERT INTO users (id, email, name, password_hash, role)
                VALUES (?, ?, ?, ?, ?)
            ''', (user_id, email, name, password_hash, role))
            
            conn.commit()
            conn.close()
            return user_id
            
        except sqlite3.Error:
            return None
    
    def authenticate_user(self, email: str, password: str) -> Optional[dict]:
        """用户认证"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            # 获取用户信息
            cursor.execute('''
                SELECT id, email, name, password_hash, role, last_login, created_at
                FROM users WHERE email = ?
            ''', (email,))
            
            user_data = cursor.fetchone()
            if not user_data:
                return None
            
            # 验证密码
            if not self._verify_password(password, user_data[3]):
                return None
            
            # 更新最后登录时间
            cursor.execute("UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?", (user_data[0],))
            conn.commit()
            conn.close()
            
            # 返回用户信息
            return {
                "id": user_data[0],
                "email": user_data[1],
                "name": user_data[2],
                "role": user_data[4],
                "last_login": user_data[5],
                "created_at": user_data[6]
            }
            
        except sqlite3.Error:
            return None
    
    def get_user_by_id(self, user_id: str) -> Optional[dict]:
        """根据ID获取用户信息"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
                SELECT id, email, name, role, last_login, created_at
                FROM users WHERE id = ?
            ''', (user_id,))
            
            user_data = cursor.fetchone()
            conn.close()
            
            if user_data:
                return {
                    "id": user_data[0],
                    "email": user_data[1],
                    "name": user_data[2],
                    "role": user_data[3],
                    "last_login": user_data[4],
                    "created_at": user_data[5]
                }
            return None
            
        except sqlite3.Error:
            return None
    
    def get_user_id_by_email(self, email: str) -> Optional[str]:
        """根据邮箱获取用户ID"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            cursor.execute("SELECT id FROM users WHERE email = ?", (email,))
            
            user_data = cursor.fetchone()
            conn.close()
            
            if user_data:
                return user_data[0]
            return None
            
        except sqlite3.Error:
            return None
    
    def get_user_by_email(self, email: str) -> Optional[dict]:
        """根据邮箱获取用户信息"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
                SELECT id, email, name, role, last_login, created_at
                FROM users WHERE email = ?
            ''', (email,))
            
            user_data = cursor.fetchone()
            conn.close()
            
            if user_data:
                return {
                    "id": user_data[0],
                    "email": user_data[1],
                    "name": user_data[2],
                    "role": user_data[3],
                    "last_login": user_data[4],
                    "created_at": user_data[5]
                }
            return None
            
        except sqlite3.Error:
            return None
    
    def create_session(self, user_id: str, token: str, expires_at: datetime) -> bool:
        """创建用户会话"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            session_id = str(uuid.uuid4())
            cursor.execute('''
                INSERT INTO sessions (id, user_id, token, expires_at)
                VALUES (?, ?, ?, ?)
            ''', (session_id, user_id, token, expires_at.isoformat()))
            
            conn.commit()
            conn.close()
            return True
            
        except sqlite3.Error:
            return False
    
    def validate_session(self, token: str) -> Optional[dict]:
        """验证会话令牌"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
                SELECT s.user_id, s.expires_at, u.email, u.role
                FROM sessions s
                JOIN users u ON s.user_id = u.id
                WHERE s.token = ? AND s.expires_at > CURRENT_TIMESTAMP
            ''', (token,))
            
            session_data = cursor.fetchone()
            conn.close()
            
            if session_data:
                return {
                    "user_id": session_data[0],
                    "email": session_data[2],
                    "role": session_data[3],
                    "expires_at": session_data[1]
                }
            return None
            
        except sqlite3.Error:
            return None
    
    def delete_session(self, token: str) -> bool:
        """删除会话"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            cursor.execute("DELETE FROM sessions WHERE token = ?", (token,))
            conn.commit()
            conn.close()
            return True
            
        except sqlite3.Error:
            return False