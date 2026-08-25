#!/usr/bin/env bash
# Control the local MySQL instance:  ./scripts/db.sh {start|stop|status|shell|logs}
set -euo pipefail
MYSQL_HOME="$HOME/.local/opt/mysql"
MYSQL_CONF="$HOME/.local/etc/my.cnf"
MYSQL_VAR="$HOME/.local/var/mysql"
SOCKET="$MYSQL_VAR/mysql.sock"

case "${1:-status}" in
  start)
    if "$MYSQL_HOME/bin/mysqladmin" --socket="$SOCKET" -u root ping >/dev/null 2>&1; then
      echo "MySQL already running"; exit 0
    fi
    nohup "$MYSQL_HOME/bin/mysqld_safe" --defaults-file="$MYSQL_CONF" >/dev/null 2>&1 &
    for _ in $(seq 1 40); do
      "$MYSQL_HOME/bin/mysqladmin" --socket="$SOCKET" -u root ping >/dev/null 2>&1 && { echo "MySQL started on 127.0.0.1:3306"; exit 0; }
      sleep 1
    done
    echo "MySQL failed to start; see $MYSQL_VAR/mysqld.log" >&2; exit 1
    ;;
  stop)
    "$MYSQL_HOME/bin/mysqladmin" --socket="$SOCKET" -u root shutdown && echo "MySQL stopped"
    ;;
  status)
    "$MYSQL_HOME/bin/mysqladmin" --socket="$SOCKET" -u root ping 2>/dev/null || echo "MySQL is not running"
    ;;
  shell)
    "$MYSQL_HOME/bin/mysql" --socket="$SOCKET" -u root "${2:-sleekflow_todo}"
    ;;
  logs)
    tail -f "$MYSQL_VAR/mysqld.log"
    ;;
  *)
    echo "usage: $0 {start|stop|status|shell|logs}" >&2; exit 2
    ;;
esac
