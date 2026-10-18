#!/bin/sh
set -eu
cd "$(dirname "$0")"
if [ ! -f app/config.properties ]; then
  printf 'Copie app/config.example.properties para app/config.properties e ajuste host/porta antes de executar.\n' >&2
  exit 1
fi
exec java -Darq.config=app/config.properties -jar dist/arqueologia-2.0.0.jar
