#!/usr/bin/env python3
"""
本地模型部署依赖安装脚本
专门为容器化部署优化
"""

import os
import sys
import subprocess
import platform

def check_python_version():
    """检查Python版本"""
    version = sys.version_info
    if version.major < 3 or (version.major == 3 and version.minor < 10):
        print("❌ Python版本过低，需要Python 3.10+")
        return False
    print(f"✓ Python版本: {version.major}.{version.minor}.{version.micro}")
    return True

def install_torch():
    """安装PyTorch（根据平台选择最优版本）"""
    system = platform.system().lower()
    
    if system == "windows":
        # Windows平台使用CPU版本（更稳定）
        cmd = "pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu"
    else:
        # Linux平台使用CUDA版本（如果可用）
        cmd = "pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121"
    
    print("🔧 安装PyTorch...")
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if result.returncode == 0:
        print("✓ PyTorch安装成功")
        return True
    else:
        print("❌ PyTorch安装失败，尝试备用方案...")
        # 备用方案：使用pip默认源
        cmd = "pip install torch torchvision torchaudio"
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        if result.returncode == 0:
            print("✓ PyTorch安装成功（备用方案）")
            return True
        else:
            print(f"❌ PyTorch安装失败: {result.stderr}")
            return False

def install_other_dependencies():
    """安装其他依赖"""
    dependencies = [
        "transformers>=4.35.0",
        "flask>=2.3.0", 
        "pyyaml>=6.0.0",
        "Pillow>=12.0.0",
        "openai>=2.9.0",
        "accelerate>=0.23.0"
    ]
    
    print("🔧 安装其他依赖...")
    for dep in dependencies:
        cmd = f"pip install {dep}"
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        if result.returncode == 0:
            print(f"✓ {dep} 安装成功")
        else:
            print(f"❌ {dep} 安装失败: {result.stderr}")
            return False
    return True

def verify_installation():
    """验证安装结果"""
    print("🔍 验证安装...")
    
    try:
        import torch
        print(f"✓ PyTorch版本: {torch.__version__}")
        print(f"  CUDA可用: {torch.cuda.is_available()}")
        
        import transformers
        print(f"✓ Transformers版本: {transformers.__version__}")
        
        import flask
        print(f"✓ Flask版本: {flask.__version__}")
        
        import openai
        print(f"✓ OpenAI版本: {openai.__version__}")
        
        return True
    except ImportError as e:
        print(f"❌ 导入失败: {e}")
        return False

def main():
    """主函数"""
    print("🚀 开始安装本地模型部署依赖...")
    
    # 检查Python版本
    if not check_python_version():
        return False
    
    # 安装依赖
    if not install_torch():
        return False
    
    if not install_other_dependencies():
        return False
    
    # 验证安装
    if not verify_installation():
        return False
    
    print("\n🎉 所有依赖安装完成！")
    print("\n📋 下一步操作:")
    print("1. 启动模型服务: python model_server.py")
    print("2. 测试服务: curl http://localhost:8000/health")
    print("3. 使用PhoneAgent: python -c 'from phone_agent import PhoneAgent; print(\"PhoneAgent可用\")'")
    
    return True

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)