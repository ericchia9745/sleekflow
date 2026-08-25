package com.sleekflow.scheduleNote.exception;

public class TodoNotFoundException extends RuntimeException {

	private final Long id;

	public TodoNotFoundException(Long id) {
		super("No TODO with id %d".formatted(id));
		this.id = id;
	}

	public Long getId() {
		return this.id;
	}
}
