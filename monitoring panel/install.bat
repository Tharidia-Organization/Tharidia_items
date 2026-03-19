@echo off
cd /d "%~dp0"
echo ================================================
echo   Tharidia God Eye Monitor -- Installer
echo ================================================
echo.

python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python not found!
    echo Please install Python 3.10+ from https://www.python.org/downloads/
    echo Make sure to check "Add Python to PATH" during installation.
    echo.
    pause
    exit /b 1
)

echo Installing Python dependencies...
echo.
python -m pip install -r requirements.txt

if errorlevel 1 (
    echo.
    echo ERROR: Failed to install dependencies.
    echo Try running as administrator or check your internet connection.
    pause
    exit /b 1
)

echo.
echo ================================================
echo   Installation complete!
echo   Run start.bat to launch the monitor.
echo ================================================
pause
