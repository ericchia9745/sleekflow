#!/usr/bin/env bash
# Load demo data:  ./scripts/seed.sh [bulk-row-count]
#
# Wipes the TODO tables, inserts a small hand-built set that exercises every
# feature, then optionally adds N generated rows so the list can be exercised at
# scale. Default is 12000; pass 0 for just the demo set.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/env.sh"

BULK="${1:-12000}"
MYSQL_BIN="${MYSQL_HOME}/bin/mysql"
if [ ! -x "$MYSQL_BIN" ]; then
  MYSQL_BIN="$(command -v mysql)" || { echo "mysql client not found" >&2; exit 1; }
fi

echo "Seeding ${DB_NAME} on ${DB_HOST}:${DB_PORT} (${BULK} generated rows)…"

"$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" <<SQL
SET SESSION cte_max_recursion_depth = 100000;

DELETE FROM todo_dependencies;
DELETE FROM todos;
ALTER TABLE todos AUTO_INCREMENT = 1;

-- A dependency chain: shop -> prep -> bake. Only the first is free to start.
INSERT INTO todos (name, description, due_date, status, priority, recurrence_type, created_at, updated_at, version)
VALUES
  ('Buy flour and yeast', 'Wholemeal, plus a fresh sachet of yeast', UTC_DATE() + INTERVAL 1 DAY, 'NOT_STARTED', 'HIGH', 'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  ('Prove the dough',     'Two hours somewhere warm',                UTC_DATE() + INTERVAL 2 DAY, 'NOT_STARTED', 'MEDIUM', 'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  ('Bake the bread',      'Fan oven, 220C, 35 minutes',              UTC_DATE() + INTERVAL 2 DAY, 'NOT_STARTED', 'MEDIUM', 'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  ('Water the plants',    'Repeats weekly',                          UTC_DATE(),                  'NOT_STARTED', 'LOW', 'WEEKLY', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  ('Pay rent',            'Repeats monthly',                         UTC_DATE() + INTERVAL 5 DAY, 'NOT_STARTED', 'HIGH', 'MONTHLY', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  ('Deep clean kitchen',  'Every 10 days',                           UTC_DATE() + INTERVAL 3 DAY, 'NOT_STARTED', 'LOW', 'CUSTOM', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  ('File tax return',     'Already overdue, to show the overdue styling', UTC_DATE() - INTERVAL 7 DAY, 'IN_PROGRESS', 'HIGH', 'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  ('Cancel old gym plan', 'Finished earlier this week',              UTC_DATE() - INTERVAL 2 DAY, 'COMPLETED', 'MEDIUM', 'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  ('Old project notes',   'Archived rather than deleted',            NULL,                       'ARCHIVED', 'LOW', 'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0);

UPDATE todos SET recurrence_interval = 1  WHERE recurrence_type IN ('WEEKLY','MONTHLY');
UPDATE todos SET recurrence_interval = 10 WHERE recurrence_type = 'CUSTOM';

INSERT INTO todo_dependencies (todo_id, depends_on_id) VALUES (2, 1), (3, 2);

-- Bulk rows for exercising paging, filtering and sorting at scale.
INSERT INTO todos (name, description, due_date, status, priority, recurrence_type, created_at, updated_at, version)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < ${BULK})
SELECT CONCAT('Seeded task ', n),
       CONCAT('Generated row ', n),
       UTC_DATE() + INTERVAL (n % 200) DAY,
       ELT(1 + (n % 4), 'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED'),
       ELT(1 + (n % 3), 'LOW', 'MEDIUM', 'HIGH'),
       'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0
FROM seq
WHERE ${BULK} > 0;

-- Give a tenth of the generated rows a dependency, so blocked/unblocked has substance.
INSERT IGNORE INTO todo_dependencies (todo_id, depends_on_id)
SELECT t.id, t.id - 1 FROM todos t WHERE t.id % 10 = 0 AND t.id > 10;

SELECT (SELECT COUNT(*) FROM todos) AS todos,
       (SELECT COUNT(*) FROM todo_dependencies) AS dependency_edges;
SQL

echo "Done."
