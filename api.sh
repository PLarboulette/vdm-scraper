#!/usr/bin/env bash
echo "[docker-compose down] STOP"
docker-compose down
echo "[docker-compose down] OK"
echo "[docker-compose run api] START"
docker-compose run -p 8080:8080 api
