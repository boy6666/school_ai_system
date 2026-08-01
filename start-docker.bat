@echo off
chcp 65001 >nul
echo ================================================
echo    EduAgent 微服务 - Docker 一键启动（deploy）
echo ================================================
echo.

cd /d "%~dp0deploy"

if not exist ".env" (
    echo [0/5] 复制 .env.example -^> .env
    copy .env.example .env
)

echo [1/5] 检查 Docker 环境...
docker --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到 Docker，请先安装 Docker Desktop
    echo 下载地址: https://www.docker.com/products/docker-desktop
    pause
    exit /b 1
)

echo [2/5] 启动基础设施（mysql / nacos / redis / rabbitmq / chroma）...
docker compose up -d mysql nacos redis rabbitmq chroma

echo [3/5] 等待 Nacos 就绪（最多 ~60s）...
set /a NACOS_WAIT=0
:wait_nacos
curl -f http://localhost:8848/nacos/ >nul 2>&1
if not errorlevel 1 goto nacos_ok
set /a NACOS_WAIT+=1
if %NACOS_WAIT%==30 (
    echo [警告] Nacos 未在预期时间内就绪，仍尝试推送配置...
    goto nacos_ok
)
timeout /t 2 /nobreak >nul
goto wait_nacos
:nacos_ok

echo [4/5] 推送各服务配置到 Nacos 配置中心...
python push-nacos-config.py
if errorlevel 1 (
    echo [警告] Nacos 配置推送失败，请确认 Nacos 已启动且 NACOS_AUTH_ENABLE=false。
)

echo [5/5] 启动全部微服务（gateway / auth / learning / resource / teacher / code / ai）...
docker compose up -d

echo.
echo ================================================
echo    服务启动完成！
echo ================================================
echo.
echo    网关(前端入口): http://localhost:8080
echo    Nacos 控制台:   http://localhost:8848/nacos/
echo    RabbitMQ 控制台: http://localhost:15672
echo    AI 服务:        http://localhost:8001
echo.
echo    可选 profile:
echo      前端:        docker compose --profile web up -d
echo      SkyWalking:  docker compose --profile skywalking up -d   (UI :8088)
echo.
echo    查看日志: docker compose logs -f
echo    停止服务: docker compose down
echo.
pause
