#!/bin/bash

function get_free_port() {
  local start=$1
  local end=$2
  for ((i=start; i<=end; i++)); do
    nc -z localhost $i > /dev/null 2>&1
    if [ $? -ne 0 ]; then
      echo $i
      return
    fi
  done
  echo 0
}

COMPOSE_FILE="./docker-compose.yml"

# Stop containers
docker-compose -f $COMPOSE_FILE down

# Scan ports and select free ports
MYSQL_PORT=$(get_free_port 3306 3316)
HTTP_PORT=$(get_free_port 8080 8099)
PMA_PORT=$(get_free_port 9090 9099)

# Set environment variables
export DAMP_MYSQL_PORT=$MYSQL_PORT
export DAMP_HTTP_PORT=$HTTP_PORT
export DAMP_PMA_PORT=$PMA_PORT
export DAMP_HOME_DIRECTORY="./"

# Start containers
docker-compose -f $COMPOSE_FILE up -d

# Show ports and host
echo "|====================================|"
echo "|          BTS SIO : DAMP            |"
echo "|====================================|"
echo "| MySQL : http://localhost:$MYSQL_PORT      |"
echo "|====================================|"
echo "| Serveur : http://localhost:$HTTP_PORT    |"
echo "|====================================|"
echo "| PhyMyAdmin : http://localhost:$PMA_PORT |"
echo "|====================================|"

# Wait for user input
read -p "Appuyer sur une touche pour quitter"

# Stop containers
docker-compose -f $COMPOSE_FILE down
