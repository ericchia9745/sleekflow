package com.sleekflow.todo.service.exception;

/** Raised when a client writes using a version another writer has already superseded. */
public class StaleTodoException extends RuntimeException {

	private final Long expectedVersion;

	private final Long actualVersion;

	public StaleTodoException(Long id, Long expectedVersion, Long actualVersion) {
		super("TODO %d has moved on: you sent version %d but the current version is %d".formatted(id, expectedVersion,
				actualVersion));
		this.expectedVersion = expectedVersion;
		this.actualVersion = actualVersion;
	}

	public Long getExpectedVersion() {
		return this.expectedVersion;
	}

	public Long getActualVersion() {
		return this.actualVersion;
	}
}
