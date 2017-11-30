#!/usr/bin/env bash
echo "[docker-compose down] StARt"
docker-compose down
echo "[docker-compose down] OK"
echo "-----------------------"
echo "[mongoDB] START"
docker run -p 27017:27017  mongo