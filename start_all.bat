@echo off
chcp 65001 >nul
title wmDB 一键启动控制台
echo ============================================================
echo          wmDB (完美数据库) V2.0 商业版 一键启动脚本
echo ============================================================
echo.
set BASE_DIR=%~dp0
set MVN_CMD=
where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 set MVN_CMD=mvn
if not defined MVN_CMD if exist C:\meng\software\apache-maven-3.9.11\bin\mvn.cmd set MVN_CMD=C:\meng\software\apache-maven-3.9.11\bin\mvn.cmd
if not defined MVN_CMD if defined MAVEN_HOME if exist %MAVEN_HOME%\bin\mvn.cmd set MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd
if not defined MVN_CMD (
    echo [错误] 未检测到 Maven 环境！请配置 Maven 环境变量。
    pause
    exit /b 1
)
set NPM_CMD=
where npm >nul 2>nul
if %ERRORLEVEL% EQU 0 set NPM_CMD=npm
if not defined NPM_CMD if exist "C:\Program Files\nodejs\npm.cmd" set NPM_CMD="C:\Program Files\nodejs\npm.cmd"
if not defined NPM_CMD (
    echo [错误] 未检测到 Node.js/NPM 环境！
    pause
    exit /b 1
)
echo [环境就绪] Maven: %MVN_CMD%
echo [环境就绪] NPM:   %NPM_CMD%
echo.
echo [1/2] 正在启动后端服务 (Spring Boot 端口 8080)...
start "wmDB-Backend-8080" cmd /k "chcp 65001 >nul && cd /d %BASE_DIR%backend && echo 后端服务启动中... && %MVN_CMD% spring-boot:run"
echo [2/2] 正在启动前端开发服务 (Vue 3 端口 5173)...
start "wmDB-Frontend-5173" cmd /k "chcp 65001 >nul && cd /d %BASE_DIR%frontend && echo 前端服务启动中... && %NPM_CMD% run dev"
echo.
echo ============================================================
echo [启动成功] 前后端服务已在独立窗口中并行启动！
echo.
echo   * 前端访问地址:    http://localhost:5173
echo   * Swagger 接口文档: http://localhost:8080/swagger-ui/index.html
echo.
echo   * 默认测试账号:    testadmin1 / 123456
echo   * 手机测试登录:    13800000001 / 123456
echo ============================================================
echo.
echo 提示：关闭对应弹出的后台控制台窗口即可停止服务。
echo 正在等待 5 秒后自动打开浏览器...
timeout /t 5 >nul
start http://localhost:5173