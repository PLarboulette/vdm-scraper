#!/usr/bin/env bash

echo "[mongodb:run-test] START"
docker run -p 27018:27017 mongo
echo "[mongodb:run-test] OK"
echo "-----------------------"
echo "[sbt test] START"
sbt test
echo "[sbt test] OK (or not)"
read -p "Press any key to continue... " -n1 -s
