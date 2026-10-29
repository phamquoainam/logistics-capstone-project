@echo off
setlocal
if /I "%~1"=="init" goto init
if /I "%~1"=="gmail" goto gmail
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-local.ps1" %*
exit /b %errorlevel%
:init
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0initialize-local.ps1"
exit /b %errorlevel%
:gmail
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0set-gmail-secret.ps1"
exit /b %errorlevel%
