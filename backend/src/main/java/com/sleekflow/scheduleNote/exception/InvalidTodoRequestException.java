package com.sleekflow.scheduleNote.exception;

/** Raised for requests that are well-formed but cannot be applied. */
public class InvalidTodoRequestException extends RuntimeException {

	public InvalidTodoRequestException(String message) {
		super(message);
	}
}
