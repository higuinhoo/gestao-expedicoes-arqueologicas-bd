@echo off
cd /d "%~dp0"
docker compose -f compose.yaml stop
if errorlevel 1 pause
