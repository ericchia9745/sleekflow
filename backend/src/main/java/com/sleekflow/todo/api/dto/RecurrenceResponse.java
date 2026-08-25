package com.sleekflow.todo.api.dto;

import com.sleekflow.todo.domain.Recurrence;
import com.sleekflow.todo.domain.RecurrenceType;

public record RecurrenceResponse(RecurrenceType type, Integer interval) {

	public static RecurrenceResponse from(Recurrence recurrence) {
		return new RecurrenceResponse(recurrence.getType(), recurrence.getInterval());
	}
}
