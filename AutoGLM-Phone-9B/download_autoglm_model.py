#!/usr/bin/env python3
"""
AutoGLM-Phone-9B 模型下载脚本
支持从 Hugging Face 和 ModelScope 下载模型
"""

import os
import sys
import subprocess
from pathlib import Path

def install_dependencies():
    """安装必要的依赖"""
    print("正在安装依赖...")
    dependencies = [
        "huggingface_hub",
        "modelscope"
    ]
    
    for dep in dependencies:
        try:
            subprocess.check_call([sys.executable, "-m", "pip", "install", dep])
            print(f"✓ 已安装 {dep}")
        except subprocess.CalledProcessError:
            print(f"✗ 安装 {dep} 失败")
            return False
    return True

def download_from_huggingface():
    """从 Hugging Face 下载模型"""
    print("\n=== 从 Hugging Face 下载 AutoGLM-Phone-9B ===")
    
    try:
        from huggingface_hub import snapshot_download
        
        model_id = "zai-org/AutoGLM-Phone-9B"
        local_dir = "./autoglm-phone-9b"
        
        print(f"模型ID: {model_id}")
        print(f"下载目录: {local_dir}")
        
        # 创建下载目录
        Path(local_dir).mkdir(parents=True, exist_ok=True)
        
        # 下载模型
        snapshot_download(
            repo_id=model_id,
            local_dir=local_dir,
            local_dir_use_symlinks=False,
            resume_download=True
        )
        
        print("✓ Hugging Face 下载完成！")
        return True
        
    except Exception as e:
        print(f"✗ Hugging Face 下载失败: {e}")
        return False

def download_from_modelscope():
    """从 ModelScope 下载模型"""
    print("\n=== 从 ModelScope 下载 AutoGLM-Phone-9B ===")
    
    try:
        from modelscope import snapshot_download
        
        model_id = "ZhipuAI/AutoGLM-Phone-9B"
        local_dir = "./autoglm-phone-9b"
        
        print(f"模型ID: {model_id}")
        print(f"下载目录: {local_dir}")
        
        # 下载模型
        snapshot_download(
            model_id=model_id,
            local_dir=local_dir,
            cache_dir="./cache"
        )
        
        print("✓ ModelScope 下载完成！")
        return True
        
    except Exception as e:
        print(f"✗ ModelScope 下载失败: {e}")
        return False

def verify_model_files():
    """验证模型文件是否完整"""
    print("\n=== 验证模型文件 ===")
    
    model_dir = Path("./autoglm-phone-9b")
    
    if not model_dir.exists():
        print("✗ 模型目录不存在")
        return False
    
    # 检查关键文件
    required_files = [
        "config.json",
        "pytorch_model.bin",
        "tokenizer.json"
    ]
    
    missing_files = []
    for file in required_files:
        file_path = model_dir / file
        if not file_path.exists():
            missing_files.append(file)
        else:
            size = file_path.stat().st_size / (1024 * 1024)  # MB
            print(f"✓ {file}: {size:.1f} MB")
    
    if missing_files:
        print(f"✗ 缺失文件: {missing_files}")
        return False
    
    print("✓ 模型文件验证通过！")
    return True

def create_model_server_config():
    """创建模型服务配置文件"""
    print("\n=== 创建模型服务配置 ===")
    
    config_content = """
# AutoGLM-Phone-9B 模型服务配置
model_name: "autoglm-phone-9b"
model_path: "./autoglm-phone-9b"
host: "localhost"
port: 8000
max_tokens: 3000
temperature: 0.1

# 推理配置
batch_size: 1
dtype: "float16"
device: "cuda"  # 或 "cpu"

# 日志配置
log_level: "INFO"
"""
    
    config_path = "model_server_config.yaml"
    with open(config_path, 'w', encoding='utf-8') as f:
        f.write(config_content)
    
    print(f"✓ 配置文件已创建: {config_path}")
    return config_path

def main():
    """主函数"""
    print("AutoGLM-Phone-9B 模型下载工具")
    print("=" * 40)
    
    # 安装依赖
    if not install_dependencies():
        print("依赖安装失败，请手动安装后重试")
        return False
    
    # 检查是否已有模型
    if Path("./autoglm-phone-9b").exists():
        print("发现已有模型目录，进行验证...")
        if verify_model_files():
            print("模型已存在且完整，跳过下载")
            create_model_server_config()
            return True
        else:
            print("模型不完整，重新下载...")
    
    # 尝试从 Hugging Face 下载
    success = download_from_huggingface()
    
    # 如果失败，尝试从 ModelScope 下载
    if not success:
        print("Hugging Face 下载失败，尝试 ModelScope...")
        success = download_from_modelscope()
    
    if success:
        # 验证下载的模型
        if verify_model_files():
            create_model_server_config()
            print("\n🎉 模型下载和配置完成！")
            print("\n下一步:")
            print("1. 启动模型服务: python model_server.py")
            print("2. 运行工作脚本: python autoglm_workscript.py <device_id>")
            return True
        else:
            print("\n✗ 模型验证失败")
            return False
    else:
        print("\n✗ 所有下载方式都失败了")
        print("\n建议:")
        print("1. 检查网络连接")
        print("2. 手动下载模型到 ./autoglm-phone-9b 目录")
        print("3. 使用代理或VPN访问 Hugging Face")
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)