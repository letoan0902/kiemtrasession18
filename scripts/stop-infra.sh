#!/usr/bin/env bash
set -u

GOC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$GOC" || exit 1

echo "== docker compose down =="
docker compose down
echo "Đã dừng hạ tầng Docker."
