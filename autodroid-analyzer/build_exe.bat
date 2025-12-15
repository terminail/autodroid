@echo off
chcp 65001 >nul
echo === 构建 stitch_screenshots 可执行文件 ===
echo.

REM 激活 conda 环境
call conda activate liugejiao

REM 安装 PyInstaller（如果未安装）
echo 正在安装 PyInstaller...
pip install pyinstaller

REM 构建可执行文件
echo.
echo 开始构建可执行文件...
pyinstaller --onefile --console --name stitch_screenshots --clean stitch_screenshots.py

REM 检查是否构建成功
if exist "dist\stitch_screenshots.exe" (
    echo.
    echo === 构建成功 ===
    echo 可执行文件: dist\stitch_screenshots.exe
    echo.
    echo 使用方法:
    echo   stitch_screenshots.exe image1.png image2.png -o result.png
    echo   stitch_screenshots.exe images\*.png -o final_result.png
    echo.
    echo 文件已生成在 dist 目录中
) else (
    echo.
    echo === 构建失败 ===
)

pause