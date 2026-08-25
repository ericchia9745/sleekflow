package com.sleekflow.todo.domain;

public enum TodoStatus {

	NOT_STARTED, IN_PROGRESS, COMPLETED, ARCHIVED;

	public boolean isTerminal() {
		return this == COMPLETED || this == ARCHIVED;
	}
}
