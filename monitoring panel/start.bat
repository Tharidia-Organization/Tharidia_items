@echo off
cd /d "%~dp0"
title Tharidia God Eye Monitor

echo ================================================
echo   Tharidia God Eye Monitor
echo ================================================
echo.

REM Check Python
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python not found!
    echo Run install.bat first, or install Python from https://www.python.org/downloads/
    echo.
    pause
    exit /b 1
)

REM Check if dash is installed
python -c "import dash" >nul 2>&1
if errorlevel 1 (
    echo Dependencies not installed. Installing now...
    echo.
    python -m pip install -r requirements.txt
    if errorlevel 1 (
        echo ERROR: Failed to install dependencies. Run install.bat manually.
        pause
        exit /b 1
    )
    echo.
)

echo Starting server...
echo The browser will open automatically.
echo Press Ctrl+C in this window to stop the server.
echo.

python app.py

echo.
echo Server stopped.
pause
