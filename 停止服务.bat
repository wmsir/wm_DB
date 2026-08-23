@echo off
chcp 65001 >nul
title wmDB 服务停止工具
echo ============================================================
echo          wmDB (完美数据库) 服务一键停止脚本
echo ============================================================
echo.
echo 正在停止 8080 端口 (后端 Spring Boot 服务)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080" ^| findstr "LISTENING"') do (
    taskkill /f /pid %%a >nul 2>nul
    echo   已终止 PID: %%a
)
echo 正在停止 5173 端口 (前端 Vite 开发服务)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":5173" ^| findstr "LISTENING"') do (
    taskkill /f /pid %%a >nul 2>nul
    echo   已终止 PID: %%a
)
echo.
echo ============================================================
echo wmDB 相关前后端服务已停止！
echo ============================================================
pause