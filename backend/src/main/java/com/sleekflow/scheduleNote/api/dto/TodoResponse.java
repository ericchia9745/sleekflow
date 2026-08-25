package com.sleekflow.todo.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Comparator;

import com.sleekflow.todo.domain.Todo;
import com.sleekflow.todo.domain.TodoPriority;
import com.sleekflow.todo.domain.TodoStatus;

/**
 * @param blocked whether a dependency is still outstanding. Computed per page
 * rather than per row, so it is passed in rather than derived here
 * @param version echoed back so the client can send it on the next write
 */
public record TodoResponse(Long id, String name, String description, LocalDate dueDate, TodoStatus status,
		TodoPriority priority, RecurrenceResponse recurrence, Long recurrenceSourceId,
		List<TodoSummaryResponse> dependencies, boolean blocked, Instant completedAt, Instant deletedAt,
		Instant createdAt, Instant updatedAt, Long version) {

	public static TodoResponse of(Todo todo, boolean blocked) {
		List<TodoSummaryResponse> dependencies = todo.getDependencies()
			.stream()
			.sorted(Comparator.comparing(Todo::getId))
			.map(TodoSummaryResponse::from)
			.toList();
		return new TodoResponse(todo.getId(), todo.getName(), todo.getDescription(), todo.getDueDate(),
				todo.getStatus(), todo.getPriority(), RecurrenceResponse.from(todo.getRecurrence()),
				todo.getRecurrenceSourceId(), dependencies, blocked, todo.getCompletedAt(), todo.getDeletedAt(),
				todo.getCreatedAt(), todo.getUpdatedAt(), todo.getVersion());
	}
}
