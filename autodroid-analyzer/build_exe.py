#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
构建 stitch_screenshots.py 为独立可执行文件
"""

import os
import sys
import subprocess
import shutil
from pathlib import Path

def install_pyinstaller():
    """安装 PyInstaller"""
    print("正在安装 PyInstaller...")
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", "pyinstaller"])
        print("PyInstaller 安装成功!")
        return True
    except subprocess.CalledProcessError as e:
        print(f"PyInstaller 安装失败: {e}")
        return False

def create_spec_file():
    """创建 PyInstaller spec 文件"""
    spec_content = '''# -*- mode: python ; coding: utf-8 -*-

block_cipher = None

a = Analysis(
    ['stitch_screenshots.py'],
    pathex=[],
    binaries=[],
    datas=[],
    hiddenimports=[],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name='stitch_screenshots',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=True,
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
    icon=None,
)
'''
    
    with open('stitch_screenshots.spec', 'w', encoding='utf-8') as f:
        f.write(spec_content)
    print("Spec 文件创建成功!")

def build_exe():
    """构建可执行文件"""
    print("开始构建可执行文件...")
    
    # 使用 PyInstaller 构建
    cmd = [
        sys.executable, "-m", "PyInstaller",
        "--onefile",  # 打包成单个文件
        "--console",  # 控制台程序
        "--name", "stitch_screenshots",
        "--add-data", ".;.",  # 包含当前目录
        "--clean",  # 清理临时文件
        "stitch_screenshots.py"
    ]
    
    try:
        subprocess.check_call(cmd)
        print("构建成功!")
        
        # 检查生成的文件
        dist_dir = Path("dist")
        if dist_dir.exists():
            exe_files = list(dist_dir.glob("*.exe"))
            if exe_files:
                print(f"生成的可执行文件: {exe_files[0]}")
                
                # 复制到当前目录
                shutil.copy(exe_files[0], ".")
                print(f"已复制到当前目录: {exe_files[0].name}")
                
                return True
        
        print("未找到生成的可执行文件")
        return False
        
    except subprocess.CalledProcessError as e:
        print(f"构建失败: {e}")
        return False

def main():
    """主函数"""
    print("=== 构建 stitch_screenshots 可执行文件 ===")
    
    # 检查 stitch_screenshots.py 是否存在
    if not os.path.exists("stitch_screenshots.py"):
        print("错误: stitch_screenshots.py 不存在")
        return
    
    # 安装 PyInstaller
    if not install_pyinstaller():
        return
    
    # 构建可执行文件
    if build_exe():
        print("\n=== 构建完成 ===")
        print("可执行文件: stitch_screenshots.exe")
        print("使用方法:")
        print("  stitch_screenshots.exe image1.png image2.png -o result.png")
        print("  stitch_screenshots.exe images/*.png -o final_result.png")
    else:
        print("\n=== 构建失败 ===")

if __name__ == "__main__":
    main()