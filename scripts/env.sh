#!/usr/bin/env bash
# Source this to put the project's local toolchain on PATH:  source scripts/env.sh
export TOOLS_HOME="$HOME/.local/opt"
export JAVA_HOME="$(/usr/libexec/java_home)"
export PATH="$TOOLS_HOME/node/bin:$TOOLS_HOME/mysql/bin:$JAVA_HOME/bin:$PATH"
export MYSQL_HOME="$TOOLS_HOME/mysql"
export MYSQL_CONF="$HOME/.local/etc/my.cnf"
export MYSQL_SOCKET="$HOME/.local/var/mysql/mysql.sock"
