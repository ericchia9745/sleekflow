package com.sleekflow.scheduleNote.exception;

import java.util.List;

/** Raised when a TODO is moved to IN_PROGRESS while dependencies are outstanding. */
public class DependenciesNotSatisfiedException extends RuntimeException {

	private final transient List<String> outstanding;

	public DependenciesNotSatisfiedException(Long id, List<String> outstanding) {
		super("TODO %d is blocked by %d unfinished %s".formatted(id, outstanding.size(),
				(outstanding.size() == 1) ? "dependency" : "dependencies"));
		this.outstanding = List.copyOf(outstanding);
	}

	public List<String> getOutstanding() {
		return this.outstanding;
	}
}
