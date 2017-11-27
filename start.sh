#!/bin/sh
echo "[docker-compose stop] STOP"
docker-compose stop
echo "[docker-compose stop] OK"
echo "-----------------------"
echo "[docker-compose up] START"
docker-compose up --force-recreate
echo "[docker-compose up --force-recreate] OK"
