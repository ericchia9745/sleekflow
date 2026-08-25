package com.sleekflow.scheduleNote.dto;

import com.sleekflow.scheduleNote.domain.Recurrence;
import com.sleekflow.scheduleNote.domain.RecurrenceType;

public record RecurrenceResponse(RecurrenceType type, Integer interval) {

	public static RecurrenceResponse from(Recurrence recurrence) {
		return new RecurrenceResponse(recurrence.getType(), recurrence.getInterval());
	}
}
