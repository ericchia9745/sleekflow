package com.sleekflow.scheduleNote.exception;

public class UsernameTakenException extends RuntimeException {

	public UsernameTakenException(String username) {
		super("The username '%s' is already registered.".formatted(username));
	}
}
