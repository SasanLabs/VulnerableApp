---
layout: default
title: VulnerableApp — Production Deployment Guide
nav_order: 5
---
# VulnerableApp — Production Deployment Guide

## Overview

This guide covers how to run VulnerableApp securely on an EC2 instance using the provided
`docker-compose.prod.yml` and startup scripts. The setup isolates backend services from the
internet and randomises credentials on every start.

---

## Prerequisites

- EC2 instance (Amazon Linux 2 or Ubuntu)
- Docker and Docker Compose installed

### Install Docker (Amazon Linux 2)

```bash
sudo yum update -y
sudo yum install -y docker
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user
```

### Install Docker Compose

```bash
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

---

## File Structure

```
VulnerableApp/
├── docker-compose.prod.yml           # Production compose — facade + base only
├── templates/                        # Nginx config templates (mounted read-only)
│   └── default.conf.template
└── scripts/
    └── productionize/
        ├── startup_script.sh         # Linux/Mac startup script
        └── startup_script.bat        # Windows startup script
```

---

## What the Startup Script Does

Each time the startup script runs it:

1. Generates a fresh random username and password for both database users
2. Writes them to a `.env` file (never committed to source control)
3. Starts Docker Compose using those credentials

Credentials are different on every run. The H2 database is in-memory so there is no data
to migrate between restarts.

---

## Running on Linux / Mac

```bash
cd scripts/productionize
chmod +x startup_script.sh
./startup_script.sh
```

---

## Running on Windows

```bat
cd scripts\productionize
startup_script.bat
```

---

## Checking Credentials

The generated credentials are written to `.env` in the `scripts/productionize/` directory:

```bash
cat scripts/productionize/.env
```

This file is regenerated on every run. Never commit it:

```bash
echo ".env" >> .gitignore
```

---

## What Runs in Production

| Container | Purpose |
|---|---|
| `VulnerableApp-base` | Spring Boot application (internal only, not exposed) |
| `VulnerableApp-facade` | Nginx reverse proxy (the only publicly exposed container) |

The JSP and PHP containers are excluded from production. All backend traffic stays on an
internal Docker network with no internet access.

---

## Network Architecture

```
Internet
    │
    ▼
[ EC2 :80 ]
    │
[ VulnerableApp-facade ]  (edge + internal networks)
    │
[ VulnerableApp-base ]    (internal network only — no internet access)
```

The base container cannot make outbound internet requests. Only the facade is reachable
from outside.

---

## Security Controls

| Control | Detail |
|---|---|
| Random credentials | New username and password generated on every start |
| Internal network | Base container has no internet access |
| Read-only volume | Nginx templates mounted as read-only |
| H2 console disabled | `public` profile disables the H2 web console |
| Active profile | Runs `public` only — `unsafe` profile is excluded |

---

## EC2 Security Group Rules

Lock down inbound access as tightly as possible:

| Port | Source | Purpose |
|---|---|---|
| 80 | Your IP only (e.g. `1.2.3.4/32`) | App access |
| 22 | Your IP only | SSH |

Avoid opening port 80 to `0.0.0.0/0` unless absolutely necessary.

---

## Stopping the App

```bash
docker compose -f docker-compose.prod.yml down
```
