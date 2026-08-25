package com.sleekflow.scheduleNote.exception;

public class UserNotFoundException extends RuntimeException {

	public UserNotFoundException(String username) {
		super("No account with username '%s'.".formatted(username));
	}
}
