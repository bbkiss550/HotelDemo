@echo off
setlocal EnableExtensions

set "APP_DIR=%~dp0"
set "APP_DIR=%APP_DIR:~0,-1%"
set "JAR_NAME=hotel-system-0.0.1-SNAPSHOT.jar"
set "APP_JAR=%APP_DIR%\target\%JAR_NAME%"
set "OUT_LOG=%APP_DIR%\app-8080.log"
set "ERR_LOG=%APP_DIR%\app-8080.err.log"
set "MAVEN_ZIP=%APP_DIR%\apache-maven-3.9.9-bin.zip"
set "MAVEN_DIR=%TEMP%\codex-maven-3.9.9"
set "MAVEN_CMD=%MAVEN_DIR%\apache-maven-3.9.9\bin\mvn.cmd"
set "BUILD_DIR=%TEMP%\HotelSystemBuild"

echo [1/6] Checking Java...
where java >nul 2>nul
if errorlevel 1 (
    echo Java was not found in PATH.
    echo Please install JDK/JRE or set JAVA_HOME.
    exit /b 1
)

echo [2/6] Preparing Maven in temp path...
if not exist "%MAVEN_CMD%" (
    if not exist "%MAVEN_ZIP%" (
        echo Maven zip not found: "%MAVEN_ZIP%"
        exit /b 1
    )
    if exist "%MAVEN_DIR%" rmdir /s /q "%MAVEN_DIR%"
    mkdir "%MAVEN_DIR%" >nul 2>nul
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%MAVEN_ZIP%' -DestinationPath '%MAVEN_DIR%' -Force"
    if errorlevel 1 exit /b 1
)

echo [3/6] Copying source to temp build folder...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%BUILD_DIR%" >nul 2>nul
copy "%APP_DIR%\pom.xml" "%BUILD_DIR%\pom.xml" >nul
robocopy "%APP_DIR%\src" "%BUILD_DIR%\src" /E /NFL /NDL /NJH /NJS /NP >nul
if errorlevel 8 (
    echo Failed to copy source files.
    exit /b 1
)

echo [4/6] Running Maven clean package...
call "%MAVEN_CMD%" -f "%BUILD_DIR%\pom.xml" -DskipTests clean package
if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

echo [5/6] Copying packaged jar back to project target...
if not exist "%APP_DIR%\target" mkdir "%APP_DIR%\target" >nul 2>nul
copy /Y "%BUILD_DIR%\target\%JAR_NAME%" "%APP_JAR%" >nul
if errorlevel 1 (
    echo Failed to copy jar to "%APP_JAR%".
    exit /b 1
)

echo [6/6] Restarting port 8080...
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":8080 .*LISTENING"') do (
    echo Stopping existing PID %%P on port 8080...
    taskkill /PID %%P /F >nul 2>nul
)

cd /d "%APP_DIR%"
echo Starting HotelSystem on port 8080...
echo Logs:
echo   %OUT_LOG%
echo   %ERR_LOG%
start "HotelSystem 8080" /min cmd /c ""java" -jar "%APP_JAR%" > "%OUT_LOG%" 2> "%ERR_LOG%""

echo Done. Wait a few seconds, then open http://localhost:8080/guests
exit /b 0
