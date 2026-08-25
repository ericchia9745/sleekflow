#!/usr/bin/env bash
# Control the local MySQL instance:  ./scripts/db.sh {start|stop|status|shell|logs|bootstrap}
#
# Only needed for the tarball MySQL install described in the README. If you run
# MySQL through Docker or a package manager, use that tool's own commands and
# just point DB_* at it.
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/env.sh"

mysqladmin_() { "$MYSQL_HOME/bin/mysqladmin" --socket="$MYSQL_SOCKET" -u root "$@"; }

case "${1:-status}" in
  start)
    if mysqladmin_ ping >/dev/null 2>&1; then echo "MySQL already running"; exit 0; fi
    nohup "$MYSQL_HOME/bin/mysqld_safe" --defaults-file="$MYSQL_CONF" >/dev/null 2>&1 &
    for _ in $(seq 1 40); do
      mysqladmin_ ping >/dev/null 2>&1 && { echo "MySQL started on $DB_HOST:$DB_PORT"; exit 0; }
      sleep 1
    done
    echo "MySQL failed to start; see $MYSQL_VAR/mysqld.log" >&2; exit 1
    ;;
  stop)    mysqladmin_ shutdown && echo "MySQL stopped" ;;
  status)  mysqladmin_ ping 2>/dev/null || echo "MySQL is not running" ;;
  shell)   "$MYSQL_HOME/bin/mysql" --socket="$MYSQL_SOCKET" -u root "${2:-$DB_NAME}" ;;
  logs)    tail -f "$MYSQL_VAR/mysqld.log" ;;
  bootstrap)
    # Create the schemas and app user. Safe to re-run.
    "$MYSQL_HOME/bin/mysql" --socket="$MYSQL_SOCKET" -u root <<SQL
CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS \`$TEST_DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '$DB_USER'@'%' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'%';
GRANT ALL PRIVILEGES ON \`$TEST_DB_NAME\`.* TO '$DB_USER'@'%';
FLUSH PRIVILEGES;
SQL
    echo "Bootstrapped $DB_NAME and $TEST_DB_NAME for user $DB_USER"
    ;;
  *) echo "usage: $0 {start|stop|status|shell|logs|bootstrap}" >&2; exit 2 ;;
esac
