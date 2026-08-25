package com.sleekflow.scheduleNote.domain.enums;

public enum TodoStatus {

	NOT_STARTED, IN_PROGRESS, COMPLETED, ARCHIVED;

	public boolean isTerminal() {
		return this == COMPLETED || this == ARCHIVED;
	}
}
