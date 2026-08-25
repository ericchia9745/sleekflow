-- Placeholder so Flyway has a baseline; real schema lands with the domain model.
CREATE TABLE IF NOT EXISTS schema_smoke_test (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
