@echo off
echo ================================================
echo Payment Gateway - Quick Start Script
echo ================================================
echo.

REM Check if Docker is installed
docker --version >nul 2>&1
if errorlevel 1 (
    echo Error: Docker is not installed. Please install Docker first.
    exit /b 1
)

REM Check if Docker Compose is installed
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo Error: Docker Compose is not installed. Please install Docker Compose first.
    exit /b 1
)

echo Docker and Docker Compose are installed
echo.

REM Stop any existing containers
echo Stopping existing containers...
docker-compose down

echo.
echo Building and starting services...
echo This may take 2-3 minutes on first run...
echo.

REM Start services
docker-compose up -d --build

echo.
echo Waiting for services to initialize (60 seconds)...
timeout /t 60 /nobreak >nul

echo.
echo Checking service status...
docker-compose ps

echo.
echo ================================================
echo Services are ready!
echo ================================================
echo.
echo Access Points:
echo   - API Server:       http://localhost:8000
echo   - Dashboard:        http://localhost:3000
echo   - Checkout Widget:  http://localhost:3001
echo   - SDK Demo:         Open demo/index.html in browser
echo.
echo Test Credentials:
echo   - API Key:          key_test_abc123
echo   - API Secret:       secret_test_xyz789
echo   - Webhook Secret:   whsec_test_abc123
echo.
echo View Logs:
echo   docker-compose logs -f api
echo   docker-compose logs -f worker
echo.
echo Stop Services:
echo   docker-compose down
echo.
echo ================================================
