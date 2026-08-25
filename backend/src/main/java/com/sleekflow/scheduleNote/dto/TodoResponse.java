package com.sleekflow.scheduleNote.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Comparator;

import com.sleekflow.scheduleNote.entity.Todo;
import com.sleekflow.scheduleNote.domain.enums.TodoPriority;
import com.sleekflow.scheduleNote.domain.enums.TodoStatus;

/**
 * @param owner who created this TODO. The list is shared, so this is
 * attribution rather than a permission -- but only the owner may delete or
 * restore it. Resolved per page, for the same reason as {@code blocked}
 * @param blocked whether a dependency is still outstanding. Computed per page
 * rather than per row, so it is passed in rather than derived here
 * @param version echoed back so the client can send it on the next write
 */
public record TodoResponse(Long id, String name, String description, LocalDate dueDate, TodoStatus status,
		TodoPriority priority, RecurrenceResponse recurrence, Long recurrenceSourceId,
		List<TodoSummaryResponse> dependencies, TodoOwnerResponse owner, boolean blocked, Instant completedAt,
		Instant deletedAt, Instant createdAt, Instant updatedAt, Long version) {

	public static TodoResponse of(Todo todo, TodoOwnerResponse owner, boolean blocked) {
		List<TodoSummaryResponse> dependencies = todo.getDependencies()
			.stream()
			.sorted(Comparator.comparing(Todo::getId))
			.map(TodoSummaryResponse::from)
			.toList();
		return new TodoResponse(todo.getId(), todo.getName(), todo.getDescription(), todo.getDueDate(),
				todo.getStatus(), todo.getPriority(), RecurrenceResponse.from(todo.getRecurrence()),
				todo.getRecurrenceSourceId(), dependencies, owner, blocked, todo.getCompletedAt(),
				todo.getDeletedAt(), todo.getCreatedAt(), todo.getUpdatedAt(), todo.getVersion());
	}
}
