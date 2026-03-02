#!/bin/bash

# Update source code from master
git pull origin master

# Stop existing containers
docker compose down

# Rebuild and start containers
docker compose up -d --build
