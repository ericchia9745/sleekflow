-- Every TODO belongs to exactly one user; nothing before this migration
-- recorded that, so any signed-in user could read or write any other user's
-- TODOs. Backfill assigns pre-existing rows to the first user on record (this
-- is dev/demo data -- there is no multi-tenant history to reconcile); any
-- todo left unowned because the users table was itself empty is discarded, as
-- there is no user left who could ever see it again.
ALTER TABLE todos ADD COLUMN user_id BIGINT NULL AFTER id;

UPDATE todos
SET user_id = (SELECT id FROM users ORDER BY id LIMIT 1)
WHERE EXISTS (SELECT 1 FROM users);

DELETE FROM todos WHERE user_id IS NULL;

ALTER TABLE todos MODIFY COLUMN user_id BIGINT NOT NULL;
ALTER TABLE todos
    ADD CONSTRAINT fk_todos_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- Every list query now filters by owner first, so the existing deleted_at-led
-- indexes are replaced with ones led by user_id.
DROP INDEX idx_todos_active_due_date ON todos;
DROP INDEX idx_todos_active_status ON todos;
DROP INDEX idx_todos_active_priority ON todos;
DROP INDEX idx_todos_active_name ON todos;

CREATE INDEX idx_todos_owner_due_date ON todos (user_id, deleted_at, due_date, id);
CREATE INDEX idx_todos_owner_status   ON todos (user_id, deleted_at, status_rank, id);
CREATE INDEX idx_todos_owner_priority ON todos (user_id, deleted_at, priority_rank, id);
CREATE INDEX idx_todos_owner_name     ON todos (user_id, deleted_at, name, id);
