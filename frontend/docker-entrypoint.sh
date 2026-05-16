#!/bin/sh
set -e

# Remplacer la variable BACKEND_URL dans la config nginx
envsubst '${BACKEND_URL}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf

exec nginx -g "daemon off;"
