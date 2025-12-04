@echo off
echo ========================================
echo Tharidia Things - Windows Setup Tester
echo ========================================
echo.

echo Testing FFmpeg...
where ffmpeg >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] FFmpeg found in PATH
    ffmpeg -version | findstr "ffmpeg version"
) else (
    echo [ERROR] FFmpeg NOT found!
    echo Please install FFmpeg: https://www.gyan.dev/ffmpeg/builds/
)
echo.

echo Testing FFplay...
where ffplay >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] FFplay found in PATH
    ffplay -version | findstr "ffplay version"
) else (
    echo [ERROR] FFplay NOT found!
    echo FFplay comes with FFmpeg - install FFmpeg first
)
echo.

echo Testing yt-dlp...
where yt-dlp >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] yt-dlp found in PATH
    yt-dlp --version
) else (
    echo [ERROR] yt-dlp NOT found!
    echo Download from: https://github.com/yt-dlp/yt-dlp/releases/latest
)
echo.

echo Testing streamlink...
where streamlink >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] streamlink found in PATH
    streamlink --version
) else (
    echo [ERROR] streamlink NOT found!
    echo Download from: https://streamlink.github.io/install.html#windows
)
echo.

echo ========================================
echo Test complete!
echo.
echo If any tools are missing, see WINDOWS_SETUP.md
echo for installation instructions.
echo ========================================
pause
