#!/usr/bin/env bash
# Source this to put the project's toolchain on PATH:  source scripts/env.sh
#
# Override any of these before sourcing if your tools live elsewhere, e.g.
#   TOOLS_HOME=/opt/homebrew/opt source scripts/env.sh
: "${TOOLS_HOME:=$HOME/.local/opt}"
: "${NODE_HOME:=$TOOLS_HOME/node}"
: "${MYSQL_HOME:=$TOOLS_HOME/mysql}"
: "${MYSQL_CONF:=$HOME/.local/etc/my.cnf}"
: "${MYSQL_VAR:=$HOME/.local/var/mysql}"
: "${MYSQL_SOCKET:=$MYSQL_VAR/mysql.sock}"
: "${JAVA_HOME:=$(/usr/libexec/java_home 2>/dev/null)}"

export TOOLS_HOME NODE_HOME MYSQL_HOME MYSQL_CONF MYSQL_VAR MYSQL_SOCKET JAVA_HOME

# Only prepend directories that actually exist, so this works unchanged on a
# machine where node/mysql came from a package manager and are already on PATH.
for dir in "$NODE_HOME/bin" "$MYSQL_HOME/bin" "$JAVA_HOME/bin"; do
  [ -d "$dir" ] && case ":$PATH:" in *":$dir:"*) ;; *) PATH="$dir:$PATH" ;; esac
done
export PATH

# Load .env if present, without clobbering variables already set in the shell.
ENV_FILE="${ENV_FILE:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.env}"
if [ -f "$ENV_FILE" ]; then
  while IFS='=' read -r key value; do
    case "$key" in ''|\#*) continue ;; esac
    [ -z "${!key:-}" ] && export "$key=$value"
  done < "$ENV_FILE"
fi

: "${DB_HOST:=127.0.0.1}"
: "${DB_PORT:=3306}"
: "${DB_NAME:=sleekflow_schedule_note}"
: "${TEST_DB_NAME:=sleekflow_schedule_note_test}"
: "${DB_USER:=todo}"
: "${DB_PASSWORD:=todo_dev_pw}"
: "${SERVER_PORT:=8080}"
export DB_HOST DB_PORT DB_NAME TEST_DB_NAME DB_USER DB_PASSWORD SERVER_PORT
