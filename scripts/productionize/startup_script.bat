@echo off
REM start.bat
for /f "delims=" %%i in ('powershell -Command "[guid]::NewGuid().ToString('N')"') do set DB_ADMIN_USERNAME=%%i
for /f "delims=" %%i in ('powershell -Command "[guid]::NewGuid().ToString('N')"') do set DB_ADMIN_PASSWORD=%%i
for /f "delims=" %%i in ('powershell -Command "[guid]::NewGuid().ToString('N')"') do set DB_APP_USERNAME=%%i
for /f "delims=" %%i in ('powershell -Command "[guid]::NewGuid().ToString('N')"') do set DB_APP_PASSWORD=%%i

if "%~1"=="" (
    set NGINX_HOST=localhost
) else (
    set NGINX_HOST=%~1
)

(
echo DB_ADMIN_USERNAME=%DB_ADMIN_USERNAME%
echo DB_ADMIN_PASSWORD=%DB_ADMIN_PASSWORD%
echo DB_APP_USERNAME=%DB_APP_USERNAME%
echo DB_APP_PASSWORD=%DB_APP_PASSWORD%
echo NGINX_HOST=%NGINX_HOST%
) > .env

echo .env generated:
type .env

docker compose -f ../../docker-compose.prod.yml up -d