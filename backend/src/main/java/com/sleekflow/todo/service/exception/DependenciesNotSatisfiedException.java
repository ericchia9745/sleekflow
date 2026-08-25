package com.sleekflow.todo.service.exception;

import java.util.List;

/** Raised when a TODO is moved to IN_PROGRESS while dependencies are outstanding. */
public class DependenciesNotSatisfiedException extends RuntimeException {

	private final transient List<String> outstanding;

	public DependenciesNotSatisfiedException(Long id, List<String> outstanding) {
		super("TODO %d is blocked by %d unfinished dependency/dependencies".formatted(id, outstanding.size()));
		this.outstanding = List.copyOf(outstanding);
	}

	public List<String> getOutstanding() {
		return this.outstanding;
	}
}
