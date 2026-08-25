package com.sleekflow.scheduleNote.dto;

import java.time.Instant;

/**
 * A cheap fingerprint of the whole TODO list, for clients to poll.
 * <p>
 * Any write moves {@code lastModifiedAt}, because every row stamps
 * {@code updated_at} on insert and update, and deletion is a soft delete -- so
 * a change of any kind, including a deletion or a restore, shows up here.
 * {@code total} guards the one case a timestamp alone could miss: two writes
 * landing inside the same microsecond.
 *
 * @param lastModifiedAt newest {@code updated_at} across all TODOs, null when empty
 * @param total row count including soft-deleted rows
 */
public record TodoRevisionResponse(Instant lastModifiedAt, long total) {
}
