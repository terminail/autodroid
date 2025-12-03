@echo off
echo Starting Autodroid Container Server...
echo.

REM Activate conda environment
call conda activate liugejiao

REM Change to the correct directory
cd /d %~dp0

REM Start the server
python run_server.py

pause