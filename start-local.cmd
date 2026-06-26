@echo off
setlocal

set "APP_DIR=%~dp0"
set "JAR=%APP_DIR%target\hotel-system-0.0.1-SNAPSHOT.jar"
set "OUT_LOG=%APP_DIR%app-8080.log"
set "ERR_LOG=%APP_DIR%app-8080.err.log"

if not exist "%JAR%" (
    echo JAR not found: "%JAR%"
    echo Please package the project first, then run this file again.
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo Java was not found in PATH.
    echo Please install JDK/JRE or set JAVA_HOME.
    exit /b 1
)

for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":8080 .*LISTENING"') do (
    echo Port 8080 is already in use by PID %%P.
    echo Stop that process first, or run stop-8080.cmd if you want to close it.
    exit /b 1
)

cd /d "%APP_DIR%"
echo Starting HotelSystem on port 8080...
echo Logs:
echo   %OUT_LOG%
echo   %ERR_LOG%

start "HotelSystem 8080" /min cmd /c ""java" -jar "%JAR%" > "%OUT_LOG%" 2> "%ERR_LOG%""

echo Started. Wait a few seconds, then open http://localhost:8080/guests
exit /b 0
