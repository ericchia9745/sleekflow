package com.sleekflow.scheduleNote.dto;

/**
 * One item a bulk operation could not apply.
 * <p>
 * {@code type} is the same slug the equivalent single-item failure carries in
 * its RFC 9457 {@code type} URI -- {@code stale-version},
 * {@code dependencies-not-satisfied}, {@code not-todo-owner},
 * {@code todo-not-found} -- so a client can branch on a stable code rather than
 * on prose.
 */
public record BulkFailureResponse(Long id, String type, String detail) {
}
