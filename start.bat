@echo off
REM Usage:  start.bat        run against your real configuration (.env)
REM         start.bat dev    run with an in-memory database, emails printed to the console, demo accounts

set PROFILE_ARG=
if /I "%1"=="dev" (
    set PROFILE_ARG=-Dspring-boot.run.profiles=dev
    echo Starting SIT Campus App in DEV mode ^(in-memory database, demo accounts, emails printed to the backend window^)...
) else (
    if not exist ".env" (
        echo No .env file found. Copy .env.example to .env and fill it in, or run:  start.bat dev
        pause
        exit /b 1
    )
    echo Starting SIT Campus App...
)

echo Starting Spring Boot Backend (Port 8080)...
start "Backend API" cmd /k "cd campusbackend && .\mvnw spring-boot:run %PROFILE_ARG%"

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
