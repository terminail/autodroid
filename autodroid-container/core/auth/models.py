from pydantic import BaseModel, EmailStr
from typing import Optional
from datetime import datetime

class UserCreate(BaseModel):
    """用户注册请求模型"""
    email: EmailStr
    password: str
    name: Optional[str] = None

class UserLogin(BaseModel):
    """用户登录请求模型"""
    email: EmailStr
    password: str

class UserResponse(BaseModel):
    """用户响应模型"""
    id: str
    email: EmailStr
    name: str
    role: str
    last_login: Optional[datetime] = None
    created_at: datetime

class Token(BaseModel):
    """认证令牌响应模型"""
    access_token: str
    token_type: str
    expires_in: int

class TokenData(BaseModel):
    """令牌数据模型"""
    user_id: str
    email: str
    role: str