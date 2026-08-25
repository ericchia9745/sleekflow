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

# Every TODO now belongs to a user, so seeding needs at least one account to
# own the demo set. This reproduces Sha256PasswordHasher's own format
# (sha256$<salt>$<digest>, digest = sha256(salt + "$" + password)) so the
# account can actually be logged into afterwards; the salt is fixed rather
# than random purely so re-running this script is idempotent.
DEMO_USERNAME="demo"
DEMO_PASSWORD="demo12345"
DEMO_SALT="seed0000demo0000"
DEMO_DIGEST=$(printf '%s$%s' "$DEMO_SALT" "$DEMO_PASSWORD" | openssl dgst -sha256 -r | awk '{print $1}')
DEMO_HASH="sha256\$${DEMO_SALT}\$${DEMO_DIGEST}"

# A second account with a couple of todos of its own, so the per-user
# isolation is visible in the seeded data rather than just in tests.
OTHER_USERNAME="demo2"
OTHER_PASSWORD="demo12345"
OTHER_SALT="seed0001demo0001"
OTHER_DIGEST=$(printf '%s$%s' "$OTHER_SALT" "$OTHER_PASSWORD" | openssl dgst -sha256 -r | awk '{print $1}')
OTHER_HASH="sha256\$${OTHER_SALT}\$${OTHER_DIGEST}"

echo "Seeding ${DB_NAME} on ${DB_HOST}:${DB_PORT} (${BULK} generated rows)…"
echo "Demo login: ${DEMO_USERNAME} / ${DEMO_PASSWORD} (second account: ${OTHER_USERNAME} / ${OTHER_PASSWORD})"

"$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" <<SQL
SET SESSION cte_max_recursion_depth = 100000;

DELETE FROM todo_dependencies;
DELETE FROM todos;
ALTER TABLE todos AUTO_INCREMENT = 1;

INSERT INTO users (username, display_name, password_hash, created_at, updated_at, version)
VALUES ('${DEMO_USERNAME}', 'Demo', '${DEMO_HASH}', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
       ('${OTHER_USERNAME}', 'Demo Two', '${OTHER_HASH}', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), updated_at = UTC_TIMESTAMP(6);

SET @demo_user_id  = (SELECT id FROM users WHERE username = '${DEMO_USERNAME}');
SET @other_user_id = (SELECT id FROM users WHERE username = '${OTHER_USERNAME}');

-- A dependency chain: shop -> prep -> bake. Only the first is free to start.
INSERT INTO todos (user_id, name, description, due_date, status, priority, recurrence_type, created_at, updated_at, version)
VALUES
  (@demo_user_id, 'Buy flour and yeast', 'Wholemeal, plus a fresh sachet of yeast', UTC_DATE() + INTERVAL 1 DAY, 'NOT_STARTED', 'HIGH', 'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  (@demo_user_id, 'Prove the dough',     'Two hours somewhere warm',                UTC_DATE() + INTERVAL 2 DAY, 'NOT_STARTED', 'MEDIUM', 'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  (@demo_user_id, 'Bake the bread',      'Fan oven, 220C, 35 minutes',              UTC_DATE() + INTERVAL 2 DAY, 'NOT_STARTED', 'MEDIUM', 'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  (@demo_user_id, 'Water the plants',    'Repeats weekly',                          UTC_DATE(),                  'NOT_STARTED', 'LOW', 'WEEKLY', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  (@demo_user_id, 'Pay rent',            'Repeats monthly',                         UTC_DATE() + INTERVAL 5 DAY, 'NOT_STARTED', 'HIGH', 'MONTHLY', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  (@demo_user_id, 'Deep clean kitchen',  'Every 10 days',                           UTC_DATE() + INTERVAL 3 DAY, 'NOT_STARTED', 'LOW', 'CUSTOM', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  (@demo_user_id, 'File tax return',     'Already overdue, to show the overdue styling', UTC_DATE() - INTERVAL 7 DAY, 'IN_PROGRESS', 'HIGH', 'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  (@demo_user_id, 'Cancel old gym plan', 'Finished earlier this week',              UTC_DATE() - INTERVAL 2 DAY, 'COMPLETED', 'MEDIUM', 'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  (@demo_user_id, 'Old project notes',   'Archived rather than deleted',            NULL,                       'ARCHIVED', 'LOW', 'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  (@other_user_id, 'Renew passport',     'Belongs to demo2 -- proves todos are not shared across accounts', UTC_DATE() + INTERVAL 30 DAY, 'NOT_STARTED', 'MEDIUM', 'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
  (@other_user_id, 'Book dentist',       'Also demo2''s',                           UTC_DATE() + INTERVAL 14 DAY, 'NOT_STARTED', 'LOW', 'NONE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0);

UPDATE todos SET recurrence_interval = 1  WHERE recurrence_type IN ('WEEKLY','MONTHLY');
UPDATE todos SET recurrence_interval = 10 WHERE recurrence_type = 'CUSTOM';

-- Demo's rows land first (ids 1-9), so these are the same edges as before:
-- prove depends on buy, bake depends on prove.
INSERT INTO todo_dependencies (todo_id, depends_on_id) VALUES (2, 1), (3, 2);

-- Bulk rows for exercising paging, filtering and sorting at scale. All owned
-- by the demo account, since they exist to stress the list endpoint.
INSERT INTO todos (user_id, name, description, due_date, status, priority, recurrence_type, created_at, updated_at, version)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < ${BULK})
SELECT @demo_user_id,
       CONCAT('Seeded task ', n),
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
