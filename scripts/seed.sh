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
  ('Buy flour and yeast', 'Wholemeal, plus a fresh sachet of yeast', CURDATE() + INTERVAL 1 DAY, 'NOT_STARTED', 'HIGH', 'NONE', NOW(6), NOW(6), 0),
  ('Prove the dough',     'Two hours somewhere warm',                CURDATE() + INTERVAL 2 DAY, 'NOT_STARTED', 'MEDIUM', 'NONE', NOW(6), NOW(6), 0),
  ('Bake the bread',      'Fan oven, 220C, 35 minutes',              CURDATE() + INTERVAL 2 DAY, 'NOT_STARTED', 'MEDIUM', 'NONE', NOW(6), NOW(6), 0),
  ('Water the plants',    'Repeats weekly',                          CURDATE(),                  'NOT_STARTED', 'LOW', 'WEEKLY', NOW(6), NOW(6), 0),
  ('Pay rent',            'Repeats monthly',                         CURDATE() + INTERVAL 5 DAY, 'NOT_STARTED', 'HIGH', 'MONTHLY', NOW(6), NOW(6), 0),
  ('Deep clean kitchen',  'Every 10 days',                           CURDATE() + INTERVAL 3 DAY, 'NOT_STARTED', 'LOW', 'CUSTOM', NOW(6), NOW(6), 0),
  ('File tax return',     'Already overdue, to show the overdue styling', CURDATE() - INTERVAL 7 DAY, 'IN_PROGRESS', 'HIGH', 'NONE', NOW(6), NOW(6), 0),
  ('Cancel old gym plan', 'Finished earlier this week',              CURDATE() - INTERVAL 2 DAY, 'COMPLETED', 'MEDIUM', 'NONE', NOW(6), NOW(6), 0),
  ('Old project notes',   'Archived rather than deleted',            NULL,                       'ARCHIVED', 'LOW', 'NONE', NOW(6), NOW(6), 0);

UPDATE todos SET recurrence_interval = 1  WHERE recurrence_type IN ('WEEKLY','MONTHLY');
UPDATE todos SET recurrence_interval = 10 WHERE recurrence_type = 'CUSTOM';

INSERT INTO todo_dependencies (todo_id, depends_on_id) VALUES (2, 1), (3, 2);

-- Bulk rows for exercising paging, filtering and sorting at scale.
INSERT INTO todos (name, description, due_date, status, priority, recurrence_type, created_at, updated_at, version)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < ${BULK})
SELECT CONCAT('Seeded task ', n),
       CONCAT('Generated row ', n),
       CURDATE() + INTERVAL (n % 200) DAY,
       ELT(1 + (n % 4), 'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED'),
       ELT(1 + (n % 3), 'LOW', 'MEDIUM', 'HIGH'),
       'NONE', NOW(6), NOW(6), 0
FROM seq
WHERE ${BULK} > 0;

-- Give a tenth of the generated rows a dependency, so blocked/unblocked has substance.
INSERT IGNORE INTO todo_dependencies (todo_id, depends_on_id)
SELECT t.id, t.id - 1 FROM todos t WHERE t.id % 10 = 0 AND t.id > 10;

SELECT (SELECT COUNT(*) FROM todos) AS todos,
       (SELECT COUNT(*) FROM todo_dependencies) AS dependency_edges;
SQL

echo "Done."
