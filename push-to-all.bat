@echo off
REM Script to push to both GitHub and Gitee repositories

echo Pushing to GitHub...
git push github main

if %errorlevel% equ 0 (
    echo Successfully pushed to GitHub!
) else (
    echo Failed to push to GitHub
    exit /b 1
)

echo Attempting to push to Gitee...
git push origin main

if %errorlevel% equ 0 (
    echo Successfully pushed to Gitee!
) else (
    echo Failed to push to Gitee ^(this is expected due to current issues^)
)

echo Push process completed!