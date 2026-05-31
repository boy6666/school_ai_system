@echo off
echo === Starting EduAgent ===

echo [1/4] Starting MySQL...
net start MySQL80

echo [2/4] Starting AI Engine (FastAPI :8002)...
start "EduAgent-AI" cmd /c "cd edu-agent-ai && python -m uvicorn api:app --host 0.0.0.0 --port 8002"

echo [3/4] Starting Backend...
set JAVA_HOME=C:\Users\Lenovo\.jdks\openjdk-25.0.1
start "EduAgent-Backend" cmd /c "cd edu-agent-server && C:\Users\Lenovo\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn spring-boot:run"

echo [4/4] Starting Frontend...
start "EduAgent-Frontend" cmd /c "cd edu-agent-web && npm run dev"

echo === Starting complete ===
echo AI Engine: http://localhost:8002/health
echo Backend:   http://localhost:8088/api
echo Frontend:  http://localhost:5173
pause