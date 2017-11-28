#!/usr/bin/env bash
echo "[docker:publishLocal] START"
sbt api/docker:publishLocal
echo "[docker:publishLocal] OK"
echo "-----------------------"
echo "[docker-compose down] STOP"
docker-compose down
echo "[docker-compose down] OK"
echo "-----------------------"
echo "[docker-compose run api] START"
docker-compose run -p 8080:8080 api
