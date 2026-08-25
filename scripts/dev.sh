#!/usr/bin/env bash
# Start backend + frontend for local development (assumes MySQL is reachable).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/env.sh"

# Start the bundled MySQL only if we manage it and it is not already up.
if [ -x "$MYSQL_HOME/bin/mysqld_safe" ]; then
  "$ROOT/scripts/db.sh" start
fi

( cd "$ROOT/backend"  && ./mvnw -q spring-boot:run ) &
BACKEND_PID=$!
( cd "$ROOT/frontend" && npm run dev ) &
FRONTEND_PID=$!

trap 'kill $BACKEND_PID $FRONTEND_PID 2>/dev/null' INT TERM
echo "backend  -> http://localhost:${SERVER_PORT}  (swagger: /swagger-ui.html)"
echo "frontend -> http://localhost:${VITE_DEV_PORT:-5173}"
wait
