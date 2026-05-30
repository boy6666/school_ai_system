@echo off
echo === Starting EduAgent ===

echo [1/3] Starting MySQL...
net start MySQL80

echo [2/3] Starting Backend...
set JAVA_HOME=C:\Users\Lenovo\.jdks\openjdk-25.0.1
start "EduAgent-Backend" cmd /c "cd edu-agent-server && C:\Users\Lenovo\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn spring-boot:run"

echo [3/3] Starting Frontend...
start "EduAgent-Frontend" cmd /c "cd edu-agent-web && npm run dev"

echo === Starting complete ===
echo Backend:  http://localhost:8080/api
echo Frontend: http://localhost:5173
pause