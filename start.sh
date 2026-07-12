#!/usr/bin/env bash

echo "=== Сборка Maven ==="
mvn clean package

echo "=== Сборка Docker Compose ==="
docker compose build --no-cache

echo "=== Запуск Docker Compose ==="
docker compose up -d
docker compose logs -f