package com.sleekflow.todo.api.dto;

import com.sleekflow.todo.domain.Todo;
import com.sleekflow.todo.domain.TodoStatus;

/** Just enough of a TODO to render it as a dependency chip. */
public record TodoSummaryResponse(Long id, String name, TodoStatus status, boolean deleted) {

	public static TodoSummaryResponse from(Todo todo) {
		return new TodoSummaryResponse(todo.getId(), todo.getName(), todo.getStatus(), todo.isDeleted());
	}
}
