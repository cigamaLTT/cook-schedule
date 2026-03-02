#!/bin/bash

# Update source code
git pull origin main || git pull origin master

# Stop existing containers
docker-compose down

# Rebuild and start containers in detached mode
docker-compose up -d --build
