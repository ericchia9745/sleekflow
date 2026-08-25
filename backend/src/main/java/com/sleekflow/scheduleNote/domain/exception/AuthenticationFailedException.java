package com.sleekflow.scheduleNote.domain.exception;

/**
 * Raised for any failed sign-in. The message is deliberately the same whether
 * the username is unknown or the password is wrong -- distinguishing them tells
 * an attacker which usernames exist.
 */
public class AuthenticationFailedException extends RuntimeException {

	public AuthenticationFailedException() {
		super("Incorrect username or password.");
	}
}
