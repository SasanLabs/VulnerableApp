#!/bin/bash
# start.sh
 
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOGFILE"
}

# --- 1. Install Docker and Git if missing --------------------------------
if ! command -v docker &>/dev/null; then
    log "Docker not found. Installing..."
    sudo dnf update -y
    sudo dnf install -y docker
    sudo systemctl enable docker
    sudo systemctl start docker
    sudo usermod -aG docker "$(whoami)"
    log "Docker installed."
else
    log "Docker already installed."
fi
 
if ! command -v git &>/dev/null; then
    log "Git not found. Installing..."
    sudo dnf install -y git
    log "Git installed."
else
    log "Git already installed."
fi
 
# Make sure docker daemon is running (in case instance just rebooted)
if ! sudo systemctl is-active --quiet docker; then
    log "Starting docker service..."
    sudo systemctl start docker
fi
 
# --- 1b. Clean slate: stop & remove ALL containers and images ------------
log "Cleaning Docker environment for a fresh start..."
 
RUNNING_CONTAINERS=$(sudo docker ps -aq)
if [ -n "$RUNNING_CONTAINERS" ]; then
    log "Stopping all containers..."
    sudo docker stop $RUNNING_CONTAINERS || true
    log "Removing all containers..."
    sudo docker rm -f $RUNNING_CONTAINERS || true
else
    log "No containers to remove."
fi
 
ALL_IMAGES=$(sudo docker images -aq)
if [ -n "$ALL_IMAGES" ]; then
    log "Removing all images..."
    sudo docker rmi -f $ALL_IMAGES || true
else
    log "No images to remove."
fi
 
# Also clear volumes, networks, and build cache for a truly clean slate
log "Pruning unused volumes, networks, and build cache..."
sudo docker system prune -af --volumes || true
 
log "Docker environment cleaned."

# --- 2. Generate .env with random credentials and start containers ----------- 

generate_password() {
  openssl rand -base64 32 | tr -d '=+/' | cut -c1-32
}

generate_username() {
  openssl rand -base64 12 | tr -d '=+/' | cut -c1-12
}

cat > .env <<EOF
DB_ADMIN_USERNAME=$(generate_username)
DB_ADMIN_PASSWORD=$(generate_password)
DB_APP_PASSWORD=$(generate_password)
NGINX_HOST=${1:-localhost}
EOF

echo ".env generated with random credentials"

if ! docker compose version &>/dev/null; then
    log "Docker Compose not found. Installing..."
    sudo mkdir -p /usr/local/lib/docker/cli-plugins
    COMPOSE_VERSION="v2.29.7"
    sudo curl -SL "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-x86_64" \
        -o /usr/local/lib/docker/cli-plugins/docker-compose
    sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
    log "Docker Compose installed."
else
    log "Docker Compose already installed."
fi

sudo docker compose --env-file .env -f ../../docker-compose.prod.yml up -d