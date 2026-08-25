package com.sleekflow.scheduleNote.dto;

import java.util.List;

/**
 * The outcome of a bulk operation, item by item.
 * <p>
 * A batch is best-effort: a row that is blocked, or that someone else has since
 * edited, is an expected outcome for that row rather than a failure of the
 * request. So this returns 200 with both lists populated, instead of a single
 * problem response that would have to throw away the work that did succeed.
 *
 * @param createdOccurrences occurrences scheduled because recurring TODOs were
 * completed in this batch
 */
public record BulkResultResponse(int requested, List<Long> succeeded, List<BulkFailureResponse> failed,
		List<TodoResponse> createdOccurrences) {
}
