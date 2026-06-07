@echo off
chcp 65001 >nul
echo ============================================
echo         EduAgent 一键启动脚本
echo ============================================
echo.

echo [1/3] Starting AI Engine (port 8000)...
start "AI-Engine" cmd /c "cd /d E:\college_information\edu-agent\edu-agent-ai && uvicorn api:app --port 8000"
echo   AI Engine starting in new window...

echo [2/3] Starting Backend (port 8080)...
start "Backend" cmd /c "cd /d E:\college_information\edu-agent\edu-agent-server && mvn spring-boot:run"
echo   Backend starting in new window...

echo [3/3] Starting Frontend (port 5173)...
start "Frontend" cmd /c "cd /d E:\college_information\edu-agent\edu-agent-web && npm run dev"
echo   Frontend starting in new window...

echo.
echo ============================================
echo   All services starting...
echo   Make sure MySQL is running!
echo.
echo   Frontend : http://localhost:5173
echo   Backend  : http://localhost:8080
echo   AI Engine: http://localhost:8000/health
echo ============================================
pause
