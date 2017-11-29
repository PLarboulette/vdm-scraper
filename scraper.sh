#!/usr/bin/env bash
echo "[docker:publishLocal] START"
sbt scraper/docker:publishLocal
echo "[docker:publishLocal] OK"
echo "-----------------------"
echo "[scraper] START"
docker-compose run --name vdm-scraper scraper
echo "[scraper] OK"
docker stop vdm-scraper && docker rm vdm-scraper
read -p "Press any key to continue... " -n1 -s
