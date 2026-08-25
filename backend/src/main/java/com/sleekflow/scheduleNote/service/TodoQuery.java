package com.sleekflow.scheduleNote.service;

import java.time.LocalDate;
import java.util.List;

import com.sleekflow.scheduleNote.domain.TodoPriority;
import com.sleekflow.scheduleNote.domain.TodoStatus;

/**
 * Filters for the TODO list. Every field is optional; a null means "do not
 * constrain on this".
 *
 * @param blocked {@code true} for TODOs waiting on a dependency, {@code false}
 * for ones clear to start
 * @param includeDeleted include soft-deleted TODOs alongside live ones
 * @param deletedOnly show only soft-deleted TODOs -- the recycle bin view
 */
public record TodoQuery(List<TodoStatus> statuses, List<TodoPriority> priorities, LocalDate dueFrom, LocalDate dueTo,
		Boolean blocked, String search, boolean includeDeleted, boolean deletedOnly) {

	public static TodoQuery empty() {
		return new TodoQuery(null, null, null, null, null, null, false, false);
	}
}
