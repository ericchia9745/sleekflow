package com.sleekflow.scheduleNote.domain.enums;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public enum RecurrenceType {

	/** The TODO happens once. */
	NONE(null),
	DAILY(ChronoUnit.DAYS),
	WEEKLY(ChronoUnit.WEEKS),
	MONTHLY(ChronoUnit.MONTHS),
	/** Every N days, where N is supplied by the caller. */
	CUSTOM(ChronoUnit.DAYS);

	private final ChronoUnit unit;

	RecurrenceType(ChronoUnit unit) {
		this.unit = unit;
	}

	public boolean recurs() {
		return this != NONE;
	}

	/**
	 * An explicit interval is only meaningful for {@link #CUSTOM}; the named
	 * schedules default to every one day/week/month but accept a multiplier.
	 */
	public boolean requiresInterval() {
		return this == CUSTOM;
	}

	public LocalDate advance(LocalDate from, int interval) {
		if (this.unit == null) {
			throw new IllegalStateException("%s does not recur".formatted(this));
		}
		return from.plus((long) interval, this.unit);
	}
}
