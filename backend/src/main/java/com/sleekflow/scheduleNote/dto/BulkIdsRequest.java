package com.sleekflow.scheduleNote.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/**
 * Bulk delete and restore take bare ids, no versions -- deliberately matching
 * their single-item endpoints, which do not ask for one either. Removing a TODO
 * from the list is not an edit of its contents, so there is no lost update to
 * guard against.
 */
public record BulkIdsRequest(@NotEmpty List<Long> ids) {
}
