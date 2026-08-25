package com.sleekflow.scheduleNote.domain.exception;

/** Raised when a request carries no session, or one that has expired or been revoked. */
public class UnauthenticatedException extends RuntimeException {

	public UnauthenticatedException(String message) {
		super(message);
	}
}
