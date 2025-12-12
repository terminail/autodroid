import jwt
import datetime
from typing import Optional
from . import Token, TokenData
from .database import AuthDatabase

class AuthService:
    """认证服务类"""
    
    def __init__(self, secret_key: str, algorithm: str = "HS256", token_expire_minutes: int = 30):
        self.secret_key = secret_key
        self.algorithm = algorithm
        self.token_expire_minutes = token_expire_minutes
        self.user_db = AuthDatabase()
    
    def verify_password(self, plain_password: str, hashed_password: str) -> bool:
        """验证密码"""
        return self.user_db._verify_password(plain_password, hashed_password)
    
    def get_password_hash(self, password: str) -> str:
        """获取密码哈希"""
        return self.user_db._hash_password(password)
    
    def create_access_token(self, data: dict, expires_delta: Optional[datetime.timedelta] = None) -> str:
        """创建访问令牌"""
        to_encode = data.copy()
        if expires_delta:
            expire = datetime.datetime.utcnow() + expires_delta
        else:
            expire = datetime.datetime.utcnow() + datetime.timedelta(minutes=self.token_expire_minutes)
        
        to_encode.update({"exp": expire})
        encoded_jwt = jwt.encode(to_encode, self.secret_key, algorithm=self.algorithm)
        return encoded_jwt
    
    def authenticate_user(self, email: str, password: str) -> Optional[Token]:
        """用户认证"""
        user = self.user_db.authenticate_user(email, password)
        if not user:
            return None
        
        # 创建访问令牌
        access_token_expires = datetime.timedelta(minutes=self.token_expire_minutes)
        token_data = {
            "sub": user["id"],
            "email": user["email"],
            "role": user["role"]
        }
        access_token = self.create_access_token(token_data, access_token_expires)
        

        
        return Token(
            access_token=access_token,
            token_type="bearer",
            expires_in=self.token_expire_minutes * 60
        )
    
    def register_user(self, email: str, name: str, password: str, role: str = "user") -> Optional[dict]:
        """用户注册"""
        try:
            # 创建用户
            user_id = self.user_db.create_user(email, name, password, role)
            if not user_id:
                return None
            
            # 获取新创建的用户信息
            user = self.user_db.get_user_by_id(user_id)
            if not user:
                return None
            
            # 创建访问令牌
            access_token_expires = datetime.timedelta(minutes=self.token_expire_minutes)
            token_data = {
                "sub": user["id"],
                "email": user["email"],
                "role": user["role"]
            }
            access_token = self.create_access_token(token_data, access_token_expires)
            

            
            return {
                "user": user,
                "token": Token(
                    access_token=access_token,
                    token_type="bearer",
                    expires_in=self.token_expire_minutes * 60
                )
            }
        except ValueError as e:
            # 重新抛出具体的错误信息
            raise e
        except Exception as e:
            # 其他异常返回None
            return None
    
    def verify_token(self, token: str) -> Optional[TokenData]:
        """验证令牌"""
        try:
            # 验证JWT令牌
            payload = jwt.decode(token, self.secret_key, algorithms=[self.algorithm])
            user_id: str = payload.get("sub")
            email: str = payload.get("email")
            role: str = payload.get("role")
            
            if user_id is None or email is None:
                return None
            
            # 验证用户是否存在
            user = self.user_db.get_user_by_id(user_id)
            if not user:
                return None
            
            return TokenData(user_id=user_id, email=email, role=role)
            
        except jwt.PyJWTError:
            return None
    
    def logout(self, token: str) -> bool:
        """用户登出"""
        # JWT 令牌无需服务器端存储，登出只需客户端删除令牌即可
        return True
    
    def get_current_user(self, token: str) -> Optional[dict]:
        """获取当前用户信息"""
        token_data = self.verify_token(token)
        if not token_data:
            return None
        
        return self.user_db.get_user_by_id(token_data.user_id)