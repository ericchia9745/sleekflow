package com.sleekflow.scheduleNote.dto;

import com.sleekflow.scheduleNote.entity.Todo;
import com.sleekflow.scheduleNote.domain.enums.TodoStatus;

/** Just enough of a TODO to render it as a dependency chip. */
public record TodoSummaryResponse(Long id, String name, TodoStatus status, boolean deleted) {

	public static TodoSummaryResponse from(Todo todo) {
		return new TodoSummaryResponse(todo.getId(), todo.getName(), todo.getStatus(), todo.isDeleted());
	}
}
