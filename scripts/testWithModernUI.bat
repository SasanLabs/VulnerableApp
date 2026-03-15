@echo off
echo Building local VulnerableApp image...
call gradlew.bat jibDockerBuild

if %ERRORLEVEL% NEQ 0 (
    echo Gradle build failed!
    exit /b %ERRORLEVEL%
)

echo Starting VulnerableApp with Modern UI (Facade)...
docker-compose -f docker-compose.local.yml up
