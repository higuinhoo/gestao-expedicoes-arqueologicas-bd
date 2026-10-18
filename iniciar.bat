@echo off
cd /d "%~dp0"
if not exist "app\config.properties" (
  echo Copie app\config.example.properties para app\config.properties e ajuste host/porta.
  exit /b 1
)
java -Darq.config=app/config.properties -jar dist/arqueologia-2.0.0.jar
