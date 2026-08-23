@echo off
chcp 65001 >nul
title wmDB 一键启动控制台
echo ============================================================
echo          wmDB (完美数据库) V2.0 商业版 一键启动脚本
echo ============================================================
echo.
set BASE_DIR=%~dp0

:: 1. 检测与配置 JAVA_HOME
if not defined JAVA_HOME if exist "C:\Program Files\Java\jdk-17" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-17"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

:: 2. 检测 Maven 环境
set MVN_CMD=
where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 set MVN_CMD=mvn
if not defined MVN_CMD if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2025.1.7.1\plugins\maven\lib\maven3\bin\mvn.cmd" set MVN_CMD="C:\Program Files\JetBrains\IntelliJ IDEA 2025.1.7.1\plugins\maven\lib\maven3\bin\mvn.cmd"
if not defined MVN_CMD if exist "C:\meng\software\apache-maven-3.9.11\bin\mvn.cmd" set MVN_CMD="C:\meng\software\apache-maven-3.9.11\bin\mvn.cmd"
if not defined MVN_CMD if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set MVN_CMD="%MAVEN_HOME%\bin\mvn.cmd"
if not defined MVN_CMD (
    echo [错误] 未检测到 Maven 环境！请配置 Maven 环境变量。
    pause
    exit /b 1
)

:: 3. 检测 Node.js / NPM 环境
set NPM_CMD=
where npm >nul 2>nul
if %ERRORLEVEL% EQU 0 set NPM_CMD=npm
if not defined NPM_CMD if exist "C:\Program Files\nodejs\npm.cmd" set NPM_CMD="C:\Program Files\nodejs\npm.cmd"
if not defined NPM_CMD (
    echo [错误] 未检测到 Node.js/NPM 环境！
    pause
    exit /b 1
)

echo [环境就绪] Java:  %JAVA_HOME%
echo [环境就绪] Maven: %MVN_CMD%
echo [环境就绪] NPM:   %NPM_CMD%
echo.

:: 4. 步骤 1/2：启动后端服务
echo [1/2] 正在启动后端服务 (Spring Boot 端口 8080)...
start "wmDB-Backend-8080" cmd /k "chcp 65001 >nul && cd /d %BASE_DIR%backend && echo 后端服务启动与编译中... && %MVN_CMD% spring-boot:run"

echo 正在侦听等待后端服务启动就绪 (检测 8080 端口与服务心跳)...
powershell -NoProfile -Command ^
  "$maxRetries = 60; $count = 0;" ^
  "Write-Host -NoNewline '后端服务启动中 ';" ^
  "while ($count -lt $maxRetries) {" ^
  "  try {" ^
  "    $tcp = New-Object System.Net.Sockets.TcpClient;" ^
  "    $tcp.Connect('127.0.0.1', 8080);" ^
  "    if ($tcp.Connected) {" ^
  "      $tcp.Close();" ^
  "      Write-Host '';" ^
  "      Write-Host '------------------------------------------------------------' -ForegroundColor Green;" ^
  "      Write-Host '[后端启动成功] Spring Boot 8080 端口已就绪，服务正常运行！' -ForegroundColor Green;" ^
  "      Write-Host '------------------------------------------------------------' -ForegroundColor Green;" ^
  "      exit 0;" ^
  "    }" ^
  "  } catch {}" ^
  "  Write-Host -NoNewline '.';" ^
  "  Start-Sleep -Seconds 2;" ^
  "  $count++;" ^
  "};" ^
  "Write-Host '';" ^
  "Write-Host '[提示] 等待超时，可能正在首次编译或下载依赖，将尝试继续启动前端。' -ForegroundColor Yellow;" ^
  "exit 1;"

echo.
:: 5. 步骤 2/2：启动前端开发服务
echo [2/2] 正在启动前端开发服务 (Vue 3 端口 5173)...
start "wmDB-Frontend-5173" cmd /k "chcp 65001 >nul && cd /d %BASE_DIR%frontend && echo 前端服务启动中... && %NPM_CMD% run dev"

echo.
echo ============================================================
echo [启动完成] wmDB 完美数据库前后端服务均已成功启动！
echo.
echo   * 系统登录入口:    http://localhost:5173
echo   * Swagger 接口文档: http://localhost:8080/swagger-ui/index.html
echo.
echo   * 默认测试账号:    testadmin1 / 123456
echo   * 手机快捷登录:    13800000001 / 123456
echo ============================================================
echo.
echo 提示：关闭弹出的后台控制台窗口即可停止对应服务。
echo 正在为您自动打开浏览器...
timeout /t 3 >nul
start http://localhost:5173