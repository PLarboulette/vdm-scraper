#!/usr/bin/env bash

echo "[mongodb:run-test] START"
docker run -d --name mongo-test -p 27018:27017 mongo
echo "[mongodb:run-test] OK"
echo "-----------------------"
echo "[sbt test] START"
sbt test
echo "[sbt test] OK (or not)"
echo "-----------------------"
echo "[mongodb:stop-test] START"
docker stop mongo-test && docker rm mongo-test
echo "[mongodb:stop-test] OK"
read -p "Press any key to continue... " -n1 -s
