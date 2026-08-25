-- V3 made every TODO private to its owner. The requirement it was meant to
-- serve asks for the opposite: "multiple users accessing the same TODO list
-- concurrently" -- one list, several people. So ownership stays recorded, but
-- stops being a filter: user_id now says who created a row, not who may see it.
--
-- No data changes. This migration exists entirely to put the indexes back in
-- step with the queries that are actually issued.

-- The default list query is now `WHERE deleted_at IS NULL ORDER BY <sort>, id`
-- with no owner predicate, and MySQL cannot use a user_id-led index to serve
-- it -- the leading column is unconstrained, so it would fall back to scanning
-- and sorting the whole table. These four restore the covering order.
CREATE INDEX idx_todos_active_due_date ON todos (deleted_at, due_date, id);
CREATE INDEX idx_todos_active_status   ON todos (deleted_at, status_rank, id);
CREATE INDEX idx_todos_active_priority ON todos (deleted_at, priority_rank, id);
CREATE INDEX idx_todos_active_name     ON todos (deleted_at, name, id);

-- Filtering by owner is now an option rather than the rule, so one index is
-- enough: it covers "this user's live TODOs" in the default due-date order.
-- The other three owner-led indexes earn nothing against that lighter use and
-- would only slow writes down.
DROP INDEX idx_todos_owner_status ON todos;
DROP INDEX idx_todos_owner_priority ON todos;
DROP INDEX idx_todos_owner_name ON todos;
