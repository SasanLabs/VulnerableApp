@echo off
REM start.bat
for /f "delims=" %%i in ('powershell -Command "[guid]::NewGuid().ToString('N')"') do set DB_ADMIN_USERNAME=%%i
for /f "delims=" %%i in ('powershell -Command "[guid]::NewGuid().ToString('N')"') do set DB_ADMIN_PASSWORD=%%i
for /f "delims=" %%i in ('powershell -Command "[guid]::NewGuid().ToString('N')"') do set DB_APP_PASSWORD=%%i

if "%~1"=="" (
    set NGINX_HOST=localhost
) else (
    set NGINX_HOST=%~1
)

if "%SMTP_USERNAME%"=="" set SMTP_USERNAME=
if "%SMTP_PASSWORD%"=="" set SMTP_PASSWORD=
if "%APP_BASE_URL%"=="" set APP_BASE_URL=http://localhost

(
echo DB_ADMIN_USERNAME=%DB_ADMIN_USERNAME%
echo DB_ADMIN_PASSWORD=%DB_ADMIN_PASSWORD%
echo DB_APP_PASSWORD=%DB_APP_PASSWORD%
echo NGINX_HOST=%NGINX_HOST%
echo SMTP_USERNAME=%SMTP_USERNAME%
echo SMTP_PASSWORD=%SMTP_PASSWORD%
echo APP_BASE_URL=%APP_BASE_URL%
) > .env

echo .env generated:
type .env

docker compose -f ../../docker-compose.prod.yml up -d