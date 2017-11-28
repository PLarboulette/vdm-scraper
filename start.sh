#!/bin/sh
echo "[docker-compose down] STOP"
docker-compose down
echo "[docker-compose down] OK"
echo "-----------------------"
echo "[docker-compose up] START"
docker-compose up --force-recreate
echo "[docker-compose up --force-recreate] OK"
