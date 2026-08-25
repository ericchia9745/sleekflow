package com.sleekflow.todo.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * How often a TODO repeats. Immutable: changing a schedule replaces the value
 * rather than mutating it.
 */
@Embeddable
public class Recurrence {

	private static final int DEFAULT_INTERVAL = 1;

	@Enumerated(EnumType.STRING)
	@Column(name = "recurrence_type", nullable = false, length = 10)
	private RecurrenceType type = RecurrenceType.NONE;

	@Column(name = "recurrence_interval")
	private Integer interval;

	protected Recurrence() {
	}

	private Recurrence(RecurrenceType type, Integer interval) {
		this.type = type;
		this.interval = interval;
	}

	public static Recurrence none() {
		return new Recurrence(RecurrenceType.NONE, null);
	}

	public static Recurrence of(RecurrenceType type, Integer interval) {
		if (type == null || type == RecurrenceType.NONE) {
			return none();
		}
		return new Recurrence(type, (interval != null) ? interval : DEFAULT_INTERVAL);
	}

	public RecurrenceType getType() {
		return this.type;
	}

	public Integer getInterval() {
		return this.interval;
	}

	public boolean recurs() {
		return this.type.recurs();
	}

	/** The due date of the occurrence that follows {@code currentDueDate}. */
	public LocalDate nextDueDateAfter(LocalDate currentDueDate) {
		return this.type.advance(currentDueDate, (this.interval != null) ? this.interval : DEFAULT_INTERVAL);
	}
}
