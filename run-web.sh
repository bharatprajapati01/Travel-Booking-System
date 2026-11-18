#!/usr/bin/env bash
# Starts the web frontend at http://localhost:8080 (pass a port to change it)
cd "$(dirname "$0")"
mkdir -p bin
javac -cp "lib/*" -d bin src/travel/core/*.java && java -cp "bin:lib/*" travel.core.Main web "${1:-8080}"
