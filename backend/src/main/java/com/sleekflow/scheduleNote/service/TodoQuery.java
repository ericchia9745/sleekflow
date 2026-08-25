package com.sleekflow.scheduleNote.service;

import java.time.LocalDate;
import java.util.List;

import com.sleekflow.scheduleNote.domain.enums.TodoPriority;
import com.sleekflow.scheduleNote.domain.enums.TodoStatus;

/**
 * Filters for the TODO list. Every field is optional; a null means "do not
 * constrain on this".
 *
 * @param owner restrict to the TODOs created by one user. The list itself is
 * shared, so this is a convenience filter -- "show me mine" -- not a boundary
 * @param blocked {@code true} for TODOs waiting on a dependency, {@code false}
 * for ones clear to start
 * @param includeDeleted include soft-deleted TODOs alongside live ones
 * @param deletedOnly show only soft-deleted TODOs -- the recycle bin view
 */
public record TodoQuery(List<TodoStatus> statuses, List<TodoPriority> priorities, LocalDate dueFrom, LocalDate dueTo,
		Long owner, Boolean blocked, String search, boolean includeDeleted, boolean deletedOnly) {

	public static TodoQuery empty() {
		return new TodoQuery(null, null, null, null, null, null, null, false, false);
	}
}
