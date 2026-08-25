package com.sleekflow.todo.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sleekflow.todo.domain.Recurrence;
import com.sleekflow.todo.domain.RecurrenceType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * @param type how often the TODO repeats
 * @param interval multiplier for the unit implied by {@code type}; required for
 * {@code CUSTOM} (every N days), optional elsewhere and defaulted to 1
 */
public record RecurrenceRequest(@NotNull RecurrenceType type, @Positive @Max(365) Integer interval) {

	@JsonIgnore
	@AssertTrue(message = "A CUSTOM schedule requires an explicit interval in days")
	public boolean isIntervalPresentWhenRequired() {
		return this.type == null || !this.type.requiresInterval() || this.interval != null;
	}

	public Recurrence toRecurrence() {
		return Recurrence.of(this.type, this.interval);
	}
}
