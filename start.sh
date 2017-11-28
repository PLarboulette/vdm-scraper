#!/bin/sh
echo "[docker:publishLocal] START"
sbt docker:publishLocal
echo "[docker:publishLocal] OK"
echo "[docker-compose down] StARt"
docker-compose down
echo "[docker-compose down] OK"
echo "-----------------------"
echo "[docker-compose up] START"
docker-compose up --force-recreate
echo "[docker-compose up --force-recreate] OK"
