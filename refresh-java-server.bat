@echo off
echo 强制刷新 Java 语言服务器配置...

REM 删除 Java 语言服务器的缓存文件
if exist "%USERPROFILE%\.vscode\extensions\redhat.java-*" (
    echo 删除 VSCode Java 扩展缓存...
    rmdir /s /q "%USERPROFILE%\.vscode\extensions\redhat.java-*" 2>nul
)

if exist "%USERPROFILE%\.vscode\server\*" (
    echo 删除 VSCode 服务器缓存...
    rmdir /s /q "%USERPROFILE%\.vscode\server\*" 2>nul
)

REM 删除项目特定的缓存
if exist "d:\git\autodroid\.classpath" (
    echo 删除 .classpath 文件...
    del "d:\git\autodroid\.classpath"
)

if exist "d:\git\autodroid\.settings" (
    echo 删除 .settings 目录...
    rmdir /s /q "d:\git\autodroid\.settings"
)

echo.
echo 刷新完成！请重新启动 VSCode 并等待 Java 语言服务器重新初始化。
echo.
pause