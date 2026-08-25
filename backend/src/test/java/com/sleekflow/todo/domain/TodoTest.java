package com.sleekflow.todo.domain;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class TodoTest {

	private static Todo todo(String name) {
		return new Todo(name, null, LocalDate.of(2026, 3, 10), TodoPriority.MEDIUM, Recurrence.none());
	}

	@Test
	void isNotBlockedWithoutDependencies() {
		assertThat(todo("solo").isBlocked()).isFalse();
	}

	@Test
	void isBlockedWhileADependencyIsUnfinished() {
		Todo dependent = todo("bake");
		dependent.addDependency(todo("buy flour"));

		assertThat(dependent.isBlocked()).isTrue();
	}

	@Test
	void isUnblockedOnceEveryDependencyIsCompleted() {
		Todo dependency = todo("buy flour");
		dependency.moveTo(TodoStatus.COMPLETED);
		Todo dependent = todo("bake");
		dependent.addDependency(dependency);

		assertThat(dependent.isBlocked()).isFalse();
	}

	@Test
	void ignoresDeletedDependencies() {
		// The work a deleted dependency represented is gone, so it cannot block.
		Todo dependency = todo("buy flour");
		dependency.softDelete();
		Todo dependent = todo("bake");
		dependent.addDependency(dependency);

		assertThat(dependent.isBlocked()).isFalse();
	}

	@Test
	void staysBlockedByAnArchivedDependency() {
		// Archived is not completed: the user must either finish it or drop the
		// link, rather than have the gate silently open.
		Todo dependency = todo("buy flour");
		dependency.moveTo(TodoStatus.ARCHIVED);
		Todo dependent = todo("bake");
		dependent.addDependency(dependency);

		assertThat(dependent.isBlocked()).isTrue();
	}

	@Test
	void recordsCompletionTimeAndClearsItWhenReopened() {
		Todo todo = todo("bake");
		todo.moveTo(TodoStatus.COMPLETED);
		assertThat(todo.getCompletedAt()).isNotNull();

		todo.moveTo(TodoStatus.IN_PROGRESS);
		assertThat(todo.getCompletedAt()).isNull();
	}

	@Test
	void nextOccurrenceCopiesTheScheduleAndMovesTheDueDate() {
		Todo weekly = new Todo("water plants", "the fern", LocalDate.of(2026, 3, 10), TodoPriority.LOW,
				Recurrence.of(RecurrenceType.WEEKLY, 1));

		Todo next = weekly.nextOccurrence();

		assertThat(next.getName()).isEqualTo("water plants");
		assertThat(next.getDescription()).isEqualTo("the fern");
		assertThat(next.getDueDate()).isEqualTo(LocalDate.of(2026, 3, 17));
		assertThat(next.getStatus()).isEqualTo(TodoStatus.NOT_STARTED);
		assertThat(next.getPriority()).isEqualTo(TodoPriority.LOW);
	}

	@Test
	void nextOccurrenceStartsWithNoDependencies() {
		// Deliberate: this cycle's dependencies were satisfied, and there is no
		// general way to know which of them recur too.
		Todo weekly = new Todo("water plants", null, LocalDate.of(2026, 3, 10), TodoPriority.LOW,
				Recurrence.of(RecurrenceType.WEEKLY, 1));
		weekly.addDependency(todo("fill watering can"));

		assertThat(weekly.nextOccurrence().getDependencies()).isEmpty();
	}

	@Test
	void refusesToScheduleANextOccurrenceForAOneOff() {
		assertThatIllegalStateException().isThrownBy(() -> todo("solo").nextOccurrence());
	}

	@Test
	void softDeleteIsReversible() {
		Todo todo = todo("bake");
		todo.softDelete();
		assertThat(todo.isDeleted()).isTrue();

		todo.restore();
		assertThat(todo.isDeleted()).isFalse();
	}
}
