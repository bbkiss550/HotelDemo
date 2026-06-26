@echo off
setlocal

set "FOUND="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":8080 .*LISTENING"') do (
    set "FOUND=1"
    echo Stopping PID %%P on port 8080...
    taskkill /PID %%P /F
)

if not defined FOUND (
    echo Port 8080 is not running.
)

exit /b 0
