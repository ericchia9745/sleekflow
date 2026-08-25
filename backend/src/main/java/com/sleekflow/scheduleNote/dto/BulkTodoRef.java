package com.sleekflow.scheduleNote.dto;

import jakarta.validation.constraints.NotNull;

/**
 * One TODO in a bulk request, with the version the client last saw.
 * <p>
 * The version travels per item rather than per request because that is the
 * only way a batch can tell "someone else changed this one row" from "the
 * whole batch is stale". A single request-level version would have to fail
 * everything on one conflict.
 */
public record BulkTodoRef(@NotNull Long id, @NotNull Long version) {
}
