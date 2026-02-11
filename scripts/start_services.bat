@echo off
chcp 65001 >nul
REM AIdoctor 项目一键启动脚本 - Windows批处理版本
REM 这是一个快捷方式，实际功能由 start_services.py 提供

echo ========================================
echo AIdoctor 项目一键启动脚本
echo ========================================
echo.

REM 切换到项目根目录（脚本在scripts文件夹中）
cd /d "%~dp0\.."

REM 检查Python是否安装
python --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Python，请先安装Python
    pause
    exit /b 1
)

REM 运行Python启动脚本
python scripts\start_services.py

pause

