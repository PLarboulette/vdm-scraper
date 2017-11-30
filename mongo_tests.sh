#!/usr/bin/env bash
echo "[mongoDB:test] START"
docker run -p 27018:27017 --name vdm-scraper-mongo-tests mongo