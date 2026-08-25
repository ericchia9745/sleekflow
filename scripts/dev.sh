#!/usr/bin/env bash
# Start database + backend + frontend for local development.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/env.sh"

"$ROOT/scripts/db.sh" start

( cd "$ROOT/backend"  && ./mvnw -q spring-boot:run ) &
BACKEND_PID=$!
( cd "$ROOT/frontend" && npm run dev ) &
FRONTEND_PID=$!

trap 'kill $BACKEND_PID $FRONTEND_PID 2>/dev/null' INT TERM
echo "backend  -> http://localhost:8080  (swagger: /swagger-ui.html)"
echo "frontend -> http://localhost:5173"
wait
