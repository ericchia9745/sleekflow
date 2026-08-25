package com.sleekflow.todo.service.exception;

/** Raised for requests that are well-formed but cannot be applied. */
public class InvalidTodoRequestException extends RuntimeException {

	public InvalidTodoRequestException(String message) {
		super(message);
	}
}
