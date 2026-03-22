#!/bin/bash
set -e

echo "Building local VulnerableApp image..."
./gradlew jibDockerBuild

echo "Starting VulnerableApp with Modern UI (Facade)..."
docker-compose -f docker-compose.local.yml up
