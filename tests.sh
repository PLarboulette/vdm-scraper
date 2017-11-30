#!/usr/bin/env bash
echo "[sbt:test] START"
sbt test
echo "[sbt:test] OK"
echo "-----------------------"
echo "STOP AND DELETING mongo test"
docker stop vdm-scraper-mongo-tests && docker rm vdm-scraper-mongo-tests
read -p "Press any key to continue... " -n1 -s



