CREATE TABLE todos (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    name                 VARCHAR(200) NOT NULL,
    description          VARCHAR(2000) NULL,
    due_date             DATE         NULL,
    status               VARCHAR(20)  NOT NULL,
    priority             VARCHAR(10)  NOT NULL,
    recurrence_type      VARCHAR(10)  NOT NULL,
    recurrence_interval  INT          NULL,
    -- Points at the first TODO of a recurring series, so every occurrence of a
    -- chore can be traced back to its origin.
    recurrence_source_id BIGINT       NULL,
    completed_at         DATETIME(6)  NULL,
    -- Soft delete: rows are never removed, so a deletion can be undone.
    deleted_at           DATETIME(6)  NULL,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    version              BIGINT       NOT NULL DEFAULT 0,

    -- Enum names sort alphabetically, which is not the order a user expects:
    -- 'HIGH' < 'LOW' < 'MEDIUM' and 'ARCHIVED' < 'COMPLETED' < 'IN_PROGRESS'.
    -- These stored generated columns give both a meaningful sort order and
    -- something indexable, without denormalising anything the app must maintain.
    priority_rank        INT AS (CASE priority
                                        WHEN 'HIGH' THEN 3
                                        WHEN 'MEDIUM' THEN 2
                                        WHEN 'LOW' THEN 1
                                        ELSE 0 END) STORED,
    status_rank          INT AS (CASE status
                                        WHEN 'NOT_STARTED' THEN 1
                                        WHEN 'IN_PROGRESS' THEN 2
                                        WHEN 'COMPLETED' THEN 3
                                        WHEN 'ARCHIVED' THEN 4
                                        ELSE 0 END) STORED,

    PRIMARY KEY (id),
    CONSTRAINT fk_todos_recurrence_source
        FOREIGN KEY (recurrence_source_id) REFERENCES todos (id) ON DELETE SET NULL
) ENGINE = InnoDB;

-- Every list query filters out soft-deleted rows, so deleted_at leads each index.
CREATE INDEX idx_todos_active_due_date  ON todos (deleted_at, due_date, id);
CREATE INDEX idx_todos_active_status    ON todos (deleted_at, status_rank, id);
CREATE INDEX idx_todos_active_priority  ON todos (deleted_at, priority_rank, id);
CREATE INDEX idx_todos_active_name      ON todos (deleted_at, name, id);
CREATE INDEX idx_todos_recurrence_source ON todos (recurrence_source_id);

CREATE TABLE todo_dependencies (
    todo_id       BIGINT NOT NULL,
    depends_on_id BIGINT NOT NULL,
    PRIMARY KEY (todo_id, depends_on_id),
    CONSTRAINT fk_todo_dependencies_todo
        FOREIGN KEY (todo_id) REFERENCES todos (id) ON DELETE CASCADE,
    CONSTRAINT fk_todo_dependencies_depends_on
        FOREIGN KEY (depends_on_id) REFERENCES todos (id) ON DELETE CASCADE,
    CONSTRAINT chk_todo_dependencies_no_self CHECK (todo_id <> depends_on_id)
) ENGINE = InnoDB;

-- Supports "which TODOs does this one block?" as well as the blocked/unblocked filter.
CREATE INDEX idx_todo_dependencies_depends_on ON todo_dependencies (depends_on_id);
