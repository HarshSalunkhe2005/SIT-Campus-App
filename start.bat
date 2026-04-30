@echo off
echo Starting SIT Campus App (Localhost Setup)...

echo Starting Spring Boot Backend (Port 8080)...
start "Backend API" cmd /k "cd campusbackend && .\mvnw spring-boot:run"

timeout /t 11 /nobreak > nul

echo Starting Frontend Server (Port 5500)...
start "Frontend UI" cmd /k "cd src\main\resources && python -m http.server 5500"

echo ===================================================
echo The application is now booting up!
echo.
echo Backend API will be available at: http://localhost:8080
echo Frontend UI will be available at: http://localhost:5500/templates/auth/login.html
echo ===================================================

echo Waiting for servers to start...
timeout /t 3 /nobreak > nul
echo Opening browser...
start http://localhost:5500/templates/auth/login.html

pause
