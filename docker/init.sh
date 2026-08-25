#!/bin/bash
# Runs once on first container start, after MYSQL_DATABASE/MYSQL_USER exist.
# Creates the separate schema the integration tests use, honouring the same
# overrides as the rest of the project.
set -euo pipefail

TEST_DB="${TEST_DB_NAME:-sleekflow_schedule_note_test}"

mysql --protocol=socket -u root -p"$MYSQL_ROOT_PASSWORD" <<SQL
CREATE DATABASE IF NOT EXISTS \`${TEST_DB}\`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON \`${TEST_DB}\`.* TO '${MYSQL_USER}'@'%';
FLUSH PRIVILEGES;
SQL

echo "init: created ${TEST_DB} and granted it to ${MYSQL_USER}"
