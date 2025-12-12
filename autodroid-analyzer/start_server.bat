@echo off
echo 启动 Autodroid Analyzer API 服务器...
echo.

REM 切换到项目根目录
cd /d %~dp0

REM 检查Python是否可用
python --version >nul 2>&1
if errorlevel 1 (
    echo ❌ 未找到Python，请确保Python已安装并添加到PATH
    pause
    exit /b 1
)

REM 检查Node.js是否可用
node --version >nul 2>&1
if errorlevel 1 (
    echo ❌ 未找到Node.js，请确保Node.js已安装并添加到PATH
    pause
    exit /b 1
)

REM 检查npm是否可用
npm --version >nul 2>&1
if errorlevel 1 (
    echo ❌ 未找到npm，请确保npm已正确安装
    pause
    exit /b 1
)

REM 启动API服务器
echo ✅ 环境检查通过，启动API服务器...
echo 注意：前端需要单独启动，请在前端目录运行 'npm run dev'
echo.
python run_server.py

pause