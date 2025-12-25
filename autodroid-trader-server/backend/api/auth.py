"""
Authentication API endpoints for Autodroid system.
Handles user registration, login, logout, and JWT token management.
"""

from fastapi import APIRouter, HTTPException, Depends, Query
from fastapi.security import HTTPBearer
from typing import Optional

from core.auth.models import UserCreate, UserLogin, UserResponse, Token
from core.auth.service import AuthService

# Initialize router
router = APIRouter(prefix="/api/auth", tags=["authentication"])

# Initialize authentication service
auth_service = AuthService(
    secret_key="your-secret-key-change-in-production",
    token_expire_minutes=60
)

# Security scheme
security = HTTPBearer()

@router.post("/register", response_model=UserResponse)
async def register_user(user_data: UserCreate):
    """Register a new user"""
    try:
        print(f"DEBUG: Received registration request - email: {user_data.email}, name: {user_data.name}")
        
        # 如果前端只提供了email和password，自动生成name
        if not user_data.name:
            user_data.name = user_data.email.split('@')[0]
            print(f"DEBUG: Auto-generated name: {user_data.name}")
        
        auth_response = auth_service.register_user(
            email=user_data.email,
            name=user_data.name,
            password=user_data.password
        )
        if auth_response:
            print("DEBUG: Registration successful")
            return auth_response["user"]
        else:
            print("DEBUG: Registration failed - auth_response is None")
            raise HTTPException(status_code=400, detail="Registration failed")
    except ValueError as e:
        print(f"DEBUG: ValueError in registration: {e}")
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        print(f"DEBUG: Unexpected error in registration: {e}")
        raise HTTPException(status_code=400, detail="Registration failed")

@router.post("/login", response_model=Token)
async def login_user(user_data: UserLogin):
    """Login user and return access token"""
    try:
        token = auth_service.authenticate_user(user_data.email, user_data.password)
        if not token:
            raise HTTPException(status_code=401, detail="Invalid email or password")
        return token
    except ValueError as e:
        raise HTTPException(status_code=401, detail=str(e))

@router.get("/me", response_model=UserResponse)
async def get_current_user(authorization: str = Depends(security)):
    """Get current user information"""
    try:
        user = auth_service.get_current_user(authorization.credentials)
        return user
    except ValueError as e:
        raise HTTPException(status_code=401, detail=str(e))

@router.post("/logout")
async def logout_user(
    authorization: Optional[str] = Depends(security),
    token: Optional[str] = Query(None, description="Token for logout (alternative to Authorization header)")
):
    """Logout user (JWT token - client-side deletion)"""
    try:
        # 优先使用Authorization header中的token，如果没有则使用查询参数中的token
        auth_token = authorization.credentials if authorization else token
        
        if not auth_token:
            raise HTTPException(status_code=401, detail="No token provided")
            
        auth_service.logout(auth_token)
        return {"message": "Logged out successfully"}
    except ValueError as e:
        raise HTTPException(status_code=401, detail=str(e))