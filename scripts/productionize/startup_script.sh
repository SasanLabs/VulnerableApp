#!/bin/bash
# start.sh

generate_password() {
  openssl rand -base64 32 | tr -d '=+/' | cut -c1-32
}

generate_username() {
  openssl rand -base64 12 | tr -d '=+/' | cut -c1-12
}

cat > .env <<EOF
DB_ADMIN_USERNAME=$(generate_username)
DB_ADMIN_PASSWORD=$(generate_password)
DB_APP_USERNAME=$(generate_username)
DB_APP_PASSWORD=$(generate_password)
NGINX_HOST=${1:-localhost}
EOF

echo ".env generated with random credentials"
docker compose -f ../../docker-compose.prod.yml up -d