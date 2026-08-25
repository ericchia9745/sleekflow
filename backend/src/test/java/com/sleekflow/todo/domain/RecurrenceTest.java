package com.sleekflow.todo.domain;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class RecurrenceTest {

	@ParameterizedTest(name = "{0} every {1} from {2} -> {3}")
	@CsvSource({ "DAILY, 1, 2026-03-10, 2026-03-11", "DAILY, 3, 2026-03-10, 2026-03-13",
			"WEEKLY, 1, 2026-03-10, 2026-03-17", "WEEKLY, 2, 2026-03-10, 2026-03-24",
			"MONTHLY, 1, 2026-03-10, 2026-04-10", "CUSTOM, 10, 2026-03-10, 2026-03-20" })
	void advancesTheDueDateByTheScheduledAmount(RecurrenceType type, int interval, LocalDate from, LocalDate expected) {
		assertThat(Recurrence.of(type, interval).nextDueDateAfter(from)).isEqualTo(expected);
	}

	@Test
	void clampsToTheEndOfAShorterMonth() {
		// 31 January has no counterpart in February; java.time clamps rather than
		// rolling into March, which is the behaviour a user expects from "monthly".
		assertThat(Recurrence.of(RecurrenceType.MONTHLY, 1).nextDueDateAfter(LocalDate.of(2026, 1, 31)))
			.isEqualTo(LocalDate.of(2026, 2, 28));
	}

	@Test
	void handlesALeapDay() {
		assertThat(Recurrence.of(RecurrenceType.MONTHLY, 12).nextDueDateAfter(LocalDate.of(2028, 2, 29)))
			.isEqualTo(LocalDate.of(2029, 2, 28));
	}

	@Test
	void defaultsTheIntervalToOneForNamedSchedules() {
		assertThat(Recurrence.of(RecurrenceType.WEEKLY, null).getInterval()).isEqualTo(1);
	}

	@Test
	void treatsANullTypeAsNotRecurring() {
		assertThat(Recurrence.of(null, 5).recurs()).isFalse();
	}

	@Test
	void refusesToScheduleWhatDoesNotRecur() {
		assertThatIllegalStateException()
			.isThrownBy(() -> Recurrence.none().nextDueDateAfter(LocalDate.of(2026, 3, 10)));
	}
}
