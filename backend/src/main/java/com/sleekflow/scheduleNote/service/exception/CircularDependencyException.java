package com.sleekflow.todo.service.exception;

/** Raised when an edge would make a TODO depend on itself, directly or indirectly. */
public class CircularDependencyException extends RuntimeException {

	public CircularDependencyException(Long todoId, Long dependsOnId) {
		super("TODO %d cannot depend on %d: that would create a dependency cycle".formatted(todoId, dependsOnId));
	}
}
