package com.sleekflow.scheduleNote.service;

import java.time.LocalDate;
import java.util.List;

import com.sleekflow.scheduleNote.dto.ChangeStatusRequest;
import com.sleekflow.scheduleNote.dto.CreateTodoRequest;
import com.sleekflow.scheduleNote.dto.RecurrenceRequest;
import com.sleekflow.scheduleNote.dto.StatusChangeResponse;
import com.sleekflow.scheduleNote.dto.TodoResponse;
import com.sleekflow.scheduleNote.dto.UpdateTodoRequest;
import com.sleekflow.scheduleNote.domain.RecurrenceType;
import com.sleekflow.scheduleNote.domain.TodoPriority;
import com.sleekflow.scheduleNote.domain.TodoStatus;
import com.sleekflow.scheduleNote.domain.User;
import com.sleekflow.scheduleNote.repository.TodoRepository;
import com.sleekflow.scheduleNote.repository.UserRepository;
import com.sleekflow.scheduleNote.security.CurrentUser;
import com.sleekflow.scheduleNote.exception.CircularDependencyException;
import com.sleekflow.scheduleNote.exception.DependenciesNotSatisfiedException;
import com.sleekflow.scheduleNote.exception.StaleTodoException;
import com.sleekflow.scheduleNote.exception.TodoNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Exercises the rules against a real MySQL schema, because several of them --
 * the blocked filter, the rank-based sorts, the soft-delete predicate -- are
 * enforced in SQL and would pass a mocked test while failing in production.
 */
@SpringBootTest
@Transactional
class TodoServiceIntegrationTest {

	@Autowired
	private TodoService service;

	@Autowired
	private TodoRepository repository;

	@Autowired
	private UserRepository users;

	private User currentUser;

	@BeforeEach
	void clearDatabase() {
		this.repository.deleteAllInBatch();
		this.users.deleteAllInBatch();
		this.currentUser = this.users.saveAndFlush(new User("owner", "Owner", "sha256$x$y"));
		CurrentUser.set(this.currentUser);
	}

	@AfterEach
	void clearCurrentUser() {
		CurrentUser.clear();
	}

	private TodoResponse create(String name) {
		return this.service.create(new CreateTodoRequest(name, null, LocalDate.of(2026, 3, 10), TodoPriority.MEDIUM,
				null, List.of()));
	}

	private void complete(TodoResponse todo) {
		this.service.changeStatus(todo.id(), new ChangeStatusRequest(TodoStatus.COMPLETED, todo.version()));
	}

	@Nested
	@DisplayName("dependency gate")
	class DependencyGate {

		@Test
		void refusesToStartWorkWhileADependencyIsOutstanding() {
			TodoResponse flour = create("buy flour");
			TodoResponse bread = TodoServiceIntegrationTest.this.service.addDependency(create("bake bread").id(),
					flour.id());

			assertThatExceptionOfType(DependenciesNotSatisfiedException.class)
				.isThrownBy(() -> TodoServiceIntegrationTest.this.service.changeStatus(bread.id(),
						new ChangeStatusRequest(TodoStatus.IN_PROGRESS, bread.version())))
				.satisfies((ex) -> assertThat(ex.getOutstanding()).containsExactly("buy flour"));
		}

		@Test
		void allowsWorkToStartOnceEveryDependencyIsCompleted() {
			TodoResponse flour = create("buy flour");
			TodoResponse bread = TodoServiceIntegrationTest.this.service.addDependency(create("bake bread").id(),
					flour.id());
			complete(flour);

			StatusChangeResponse result = TodoServiceIntegrationTest.this.service.changeStatus(bread.id(),
					new ChangeStatusRequest(TodoStatus.IN_PROGRESS, bread.version()));

			assertThat(result.todo().status()).isEqualTo(TodoStatus.IN_PROGRESS);
			assertThat(result.todo().blocked()).isFalse();
		}

		@Test
		void blocksUntilTheLastDependencyIsDone() {
			TodoResponse first = create("first");
			TodoResponse second = create("second");
			TodoResponse dependent = TodoServiceIntegrationTest.this.service.addDependency(create("dependent").id(),
					first.id());
			dependent = TodoServiceIntegrationTest.this.service.addDependency(dependent.id(), second.id());
			complete(first);

			TodoResponse current = dependent;
			assertThatExceptionOfType(DependenciesNotSatisfiedException.class)
				.isThrownBy(() -> TodoServiceIntegrationTest.this.service.changeStatus(current.id(),
						new ChangeStatusRequest(TodoStatus.IN_PROGRESS, current.version())))
				.satisfies((ex) -> assertThat(ex.getOutstanding()).containsExactly("second"));
		}

		@Test
		void completingIsNotGated() {
			// The requirement gates IN_PROGRESS only: a task can turn out to be
			// unnecessary and be closed without ever being started.
			TodoResponse flour = create("buy flour");
			TodoResponse bread = TodoServiceIntegrationTest.this.service.addDependency(create("bake bread").id(),
					flour.id());

			StatusChangeResponse result = TodoServiceIntegrationTest.this.service.changeStatus(bread.id(),
					new ChangeStatusRequest(TodoStatus.COMPLETED, bread.version()));

			assertThat(result.todo().status()).isEqualTo(TodoStatus.COMPLETED);
		}

		@Test
		void aDeletedDependencyStopsBlocking() {
			TodoResponse flour = create("buy flour");
			TodoResponse bread = TodoServiceIntegrationTest.this.service.addDependency(create("bake bread").id(),
					flour.id());
			TodoServiceIntegrationTest.this.service.delete(flour.id());

			StatusChangeResponse result = TodoServiceIntegrationTest.this.service.changeStatus(bread.id(),
					new ChangeStatusRequest(TodoStatus.IN_PROGRESS, bread.version()));

			assertThat(result.todo().status()).isEqualTo(TodoStatus.IN_PROGRESS);
		}

	}

	@Nested
	@DisplayName("dependency graph")
	class DependencyGraph {

		@Test
		void rejectsADirectCycle() {
			TodoResponse a = create("a");
			TodoResponse b = create("b");
			TodoServiceIntegrationTest.this.service.addDependency(b.id(), a.id());

			assertThatExceptionOfType(CircularDependencyException.class).isThrownBy(
					() -> TodoServiceIntegrationTest.this.service.addDependency(a.id(), b.id()));
		}

		@Test
		void rejectsATransitiveCycle() {
			TodoResponse a = create("a");
			TodoResponse b = create("b");
			TodoResponse c = create("c");
			TodoServiceIntegrationTest.this.service.addDependency(b.id(), a.id());
			TodoServiceIntegrationTest.this.service.addDependency(c.id(), b.id());

			// a -> c would close the loop a -> c -> b -> a
			assertThatExceptionOfType(CircularDependencyException.class).isThrownBy(
					() -> TodoServiceIntegrationTest.this.service.addDependency(a.id(), c.id()));
		}

		@Test
		void rejectsSelfDependency() {
			TodoResponse a = create("a");

			assertThatExceptionOfType(CircularDependencyException.class).isThrownBy(
					() -> TodoServiceIntegrationTest.this.service.addDependency(a.id(), a.id()));
		}

		@Test
		void allowsADiamondWhichIsNotACycle() {
			TodoResponse root = create("root");
			TodoResponse left = TodoServiceIntegrationTest.this.service.addDependency(create("left").id(), root.id());
			TodoResponse right = TodoServiceIntegrationTest.this.service.addDependency(create("right").id(), root.id());
			TodoResponse top = TodoServiceIntegrationTest.this.service.addDependency(create("top").id(), left.id());

			TodoResponse result = TodoServiceIntegrationTest.this.service.addDependency(top.id(), right.id());

			assertThat(result.dependencies()).hasSize(2);
		}

		@Test
		void removingAnEdgeUnblocksTheDependent() {
			TodoResponse flour = create("buy flour");
			TodoResponse bread = TodoServiceIntegrationTest.this.service.addDependency(create("bake bread").id(),
					flour.id());
			assertThat(bread.blocked()).isTrue();

			TodoResponse result = TodoServiceIntegrationTest.this.service.removeDependency(bread.id(), flour.id());

			assertThat(result.blocked()).isFalse();
			assertThat(result.dependencies()).isEmpty();
		}

	}

	@Nested
	@DisplayName("recurrence")
	class Recurrence {

		private TodoResponse recurring(RecurrenceType type, Integer interval, LocalDate dueDate) {
			return TodoServiceIntegrationTest.this.service.create(new CreateTodoRequest("water plants", null, dueDate,
					TodoPriority.LOW, new RecurrenceRequest(type, interval), List.of()));
		}

		@Test
		void completingARecurringTodoSchedulesTheNextOccurrence() {
			TodoResponse weekly = recurring(RecurrenceType.WEEKLY, 1, LocalDate.of(2026, 3, 10));

			StatusChangeResponse result = TodoServiceIntegrationTest.this.service.changeStatus(weekly.id(),
					new ChangeStatusRequest(TodoStatus.COMPLETED, weekly.version()));

			assertThat(result.nextOccurrence()).isNotNull();
			assertThat(result.nextOccurrence().dueDate()).isEqualTo(LocalDate.of(2026, 3, 17));
			assertThat(result.nextOccurrence().status()).isEqualTo(TodoStatus.NOT_STARTED);
			assertThat(result.todo().status()).isEqualTo(TodoStatus.COMPLETED);
		}

		@Test
		void everyOccurrencePointsBackAtTheFirstOne() {
			TodoResponse first = recurring(RecurrenceType.DAILY, 1, LocalDate.of(2026, 3, 10));

			TodoResponse second = TodoServiceIntegrationTest.this.service
				.changeStatus(first.id(), new ChangeStatusRequest(TodoStatus.COMPLETED, first.version()))
				.nextOccurrence();
			TodoResponse third = TodoServiceIntegrationTest.this.service
				.changeStatus(second.id(), new ChangeStatusRequest(TodoStatus.COMPLETED, second.version()))
				.nextOccurrence();

			assertThat(second.recurrenceSourceId()).isEqualTo(first.id());
			assertThat(third.recurrenceSourceId()).isEqualTo(first.id());
		}

		@Test
		void completingAOneOffSchedulesNothing() {
			TodoResponse once = create("once");

			StatusChangeResponse result = TodoServiceIntegrationTest.this.service.changeStatus(once.id(),
					new ChangeStatusRequest(TodoStatus.COMPLETED, once.version()));

			assertThat(result.nextOccurrence()).isNull();
		}

		@Test
		void reCompletingAnAlreadyCompletedTodoDoesNotScheduleASecondOccurrence() {
			// Guards against a double-click producing two occurrences.
			TodoResponse weekly = recurring(RecurrenceType.WEEKLY, 1, LocalDate.of(2026, 3, 10));
			StatusChangeResponse first = TodoServiceIntegrationTest.this.service.changeStatus(weekly.id(),
					new ChangeStatusRequest(TodoStatus.COMPLETED, weekly.version()));

			StatusChangeResponse second = TodoServiceIntegrationTest.this.service.changeStatus(weekly.id(),
					new ChangeStatusRequest(TodoStatus.COMPLETED, first.todo().version()));

			assertThat(second.nextOccurrence()).isNull();
			assertThat(TodoServiceIntegrationTest.this.repository.count()).isEqualTo(2);
		}

	}

	@Nested
	@DisplayName("concurrent edits")
	class ConcurrentEdits {

		@Test
		void rejectsAWriteBasedOnAVersionSomeoneElseHasSuperseded() {
			TodoResponse todo = create("shared");
			TodoServiceIntegrationTest.this.service.update(todo.id(), new UpdateTodoRequest("renamed by first writer",
					null, todo.dueDate(), TodoPriority.HIGH, null, todo.version()));

			assertThatExceptionOfType(StaleTodoException.class)
				.isThrownBy(() -> TodoServiceIntegrationTest.this.service.update(todo.id(),
						new UpdateTodoRequest("renamed by second writer", null, todo.dueDate(), TodoPriority.LOW, null,
								todo.version())))
				.satisfies((ex) -> {
					assertThat(ex.getExpectedVersion()).isZero();
					assertThat(ex.getActualVersion()).isEqualTo(1L);
				});
		}

		@Test
		void acceptsAWriteThatCarriesTheCurrentVersion() {
			TodoResponse todo = create("shared");
			TodoResponse updated = TodoServiceIntegrationTest.this.service.update(todo.id(),
					new UpdateTodoRequest("first", null, todo.dueDate(), TodoPriority.HIGH, null, todo.version()));

			TodoResponse again = TodoServiceIntegrationTest.this.service.update(todo.id(),
					new UpdateTodoRequest("second", null, todo.dueDate(), TodoPriority.LOW, null, updated.version()));

			assertThat(again.name()).isEqualTo("second");
		}

	}

	@Nested
	@DisplayName("soft delete")
	class SoftDelete {

		@Test
		void deletedTodosLeaveTheDefaultListButAreStillStored() {
			TodoResponse todo = create("mistake");
			TodoServiceIntegrationTest.this.service.delete(todo.id());

			assertThat(TodoServiceIntegrationTest.this.service.list(TodoQuery.empty(), PageRequest.of(0, 10)))
				.isEmpty();
			assertThat(TodoServiceIntegrationTest.this.repository.findById(todo.id())).isPresent();
		}

		@Test
		void aDeletedTodoCanBeFoundInTheRecycleBinAndRestored() {
			TodoResponse todo = create("mistake");
			TodoServiceIntegrationTest.this.service.delete(todo.id());

			TodoQuery bin = new TodoQuery(null, null, null, null, null, null, false, true);
			assertThat(TodoServiceIntegrationTest.this.service.list(bin, PageRequest.of(0, 10))).hasSize(1);

			TodoServiceIntegrationTest.this.service.restore(todo.id());
			assertThat(TodoServiceIntegrationTest.this.service.list(TodoQuery.empty(), PageRequest.of(0, 10)))
				.hasSize(1);
		}

		@Test
		void writesToADeletedTodoAreRejected() {
			TodoResponse todo = create("mistake");
			TodoServiceIntegrationTest.this.service.delete(todo.id());

			assertThatExceptionOfType(TodoNotFoundException.class)
				.isThrownBy(() -> TodoServiceIntegrationTest.this.service.changeStatus(todo.id(),
						new ChangeStatusRequest(TodoStatus.IN_PROGRESS, todo.version())));
		}

	}

	@Nested
	@DisplayName("filtering and sorting")
	class FilteringAndSorting {

		@Test
		void sortsPriorityByImportanceNotAlphabetically() {
			// Alphabetically this would be HIGH, LOW, MEDIUM.
			TodoServiceIntegrationTest.this.service.create(new CreateTodoRequest("low", null, null, TodoPriority.LOW,
					null, List.of()));
			TodoServiceIntegrationTest.this.service.create(new CreateTodoRequest("high", null, null,
					TodoPriority.HIGH, null, List.of()));
			TodoServiceIntegrationTest.this.service.create(new CreateTodoRequest("medium", null, null,
					TodoPriority.MEDIUM, null, List.of()));

			List<String> names = TodoServiceIntegrationTest.this.service
				.list(TodoQuery.empty(), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "priority")))
				.map(TodoResponse::name)
				.getContent();

			assertThat(names).containsExactly("high", "medium", "low");
		}

		@Test
		void sortsStatusByLifecycleNotAlphabetically() {
			// Alphabetically this would be ARCHIVED, COMPLETED, IN_PROGRESS, NOT_STARTED.
			TodoResponse done = create("done");
			complete(done);
			create("fresh");

			List<String> names = TodoServiceIntegrationTest.this.service
				.list(TodoQuery.empty(), PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "status")))
				.map(TodoResponse::name)
				.getContent();

			assertThat(names).containsExactly("fresh", "done");
		}

		@Test
		void filtersToBlockedAndUnblockedWithoutOverlapOrGaps() {
			TodoResponse flour = create("buy flour");
			TodoServiceIntegrationTest.this.service.addDependency(create("bake bread").id(), flour.id());
			create("unrelated");

			TodoQuery blocked = new TodoQuery(null, null, null, null, true, null, false, false);
			TodoQuery unblocked = new TodoQuery(null, null, null, null, false, null, false, false);

			assertThat(TodoServiceIntegrationTest.this.service.list(blocked, PageRequest.of(0, 10))
				.map(TodoResponse::name)).containsExactly("bake bread");
			assertThat(TodoServiceIntegrationTest.this.service.list(unblocked, PageRequest.of(0, 10))
				.map(TodoResponse::name)).containsExactlyInAnyOrder("buy flour", "unrelated");
		}

		@Test
		void filtersByDueDateRange() {
			TodoServiceIntegrationTest.this.service.create(new CreateTodoRequest("early", null,
					LocalDate.of(2026, 1, 1), TodoPriority.LOW, null, List.of()));
			TodoServiceIntegrationTest.this.service.create(new CreateTodoRequest("inside", null,
					LocalDate.of(2026, 6, 1), TodoPriority.LOW, null, List.of()));
			TodoServiceIntegrationTest.this.service.create(new CreateTodoRequest("late", null,
					LocalDate.of(2026, 12, 1), TodoPriority.LOW, null, List.of()));

			TodoQuery range = new TodoQuery(null, null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 7, 1), null, null,
					false, false);

			assertThat(TodoServiceIntegrationTest.this.service.list(range, PageRequest.of(0, 10))
				.map(TodoResponse::name)).containsExactly("inside");
		}

		@Test
		void filtersByStatusAndPriorityTogether() {
			TodoResponse target = TodoServiceIntegrationTest.this.service.create(new CreateTodoRequest("wanted", null,
					null, TodoPriority.HIGH, null, List.of()));
			complete(target);
			TodoServiceIntegrationTest.this.service.create(new CreateTodoRequest("wrong priority", null, null,
					TodoPriority.LOW, null, List.of()));

			TodoQuery query = new TodoQuery(List.of(TodoStatus.COMPLETED), List.of(TodoPriority.HIGH), null, null, null,
					null, false, false);

			assertThat(TodoServiceIntegrationTest.this.service.list(query, PageRequest.of(0, 10))
				.map(TodoResponse::name)).containsExactly("wanted");
		}

		@Test
		void searchesNamesCaseInsensitively() {
			create("Buy Flour");
			create("bake bread");

			TodoQuery query = new TodoQuery(null, null, null, null, null, "flour", false, false);

			assertThat(TodoServiceIntegrationTest.this.service.list(query, PageRequest.of(0, 10))
				.map(TodoResponse::name)).containsExactly("Buy Flour");
		}

		@Test
		void capsThePageSizeSoOneRequestCannotPullTheWholeList() {
			for (int i = 0; i < 5; i++) {
				create("task " + i);
			}

			assertThat(TodoServiceIntegrationTest.this.service.list(TodoQuery.empty(), PageRequest.of(0, 10_000))
				.getSize()).isEqualTo(200);
		}

	}

	@Nested
	@DisplayName("ownership")
	class Ownership {

		private TodoResponse createAsOtherUser(String name) {
			User other = TodoServiceIntegrationTest.this.users.saveAndFlush(new User("other", "Other", "sha256$x$y"));
			CurrentUser.set(other);
			try {
				return create(name);
			}
			finally {
				CurrentUser.set(TodoServiceIntegrationTest.this.currentUser);
			}
		}

		@Test
		void listOnlyReturnsTheCallersOwnTodos() {
			create("mine");
			createAsOtherUser("theirs");

			assertThat(TodoServiceIntegrationTest.this.service.list(TodoQuery.empty(), PageRequest.of(0, 10))
				.map(TodoResponse::name)).containsExactly("mine");
		}

		@Test
		void gettingAnotherUsersTodoIs404() {
			TodoResponse theirs = createAsOtherUser("theirs");

			assertThatExceptionOfType(TodoNotFoundException.class)
				.isThrownBy(() -> TodoServiceIntegrationTest.this.service.get(theirs.id()));
		}

		@Test
		void updatingAnotherUsersTodoIs404() {
			TodoResponse theirs = createAsOtherUser("theirs");

			assertThatExceptionOfType(TodoNotFoundException.class)
				.isThrownBy(() -> TodoServiceIntegrationTest.this.service.update(theirs.id(),
						new UpdateTodoRequest("hijacked", null, theirs.dueDate(), TodoPriority.HIGH, null,
								theirs.version())));
		}

		@Test
		void deletingAnotherUsersTodoIs404() {
			TodoResponse theirs = createAsOtherUser("theirs");

			assertThatExceptionOfType(TodoNotFoundException.class)
				.isThrownBy(() -> TodoServiceIntegrationTest.this.service.delete(theirs.id()));
		}

		@Test
		void changingAnotherUsersTodoStatusIs404() {
			TodoResponse theirs = createAsOtherUser("theirs");

			assertThatExceptionOfType(TodoNotFoundException.class)
				.isThrownBy(() -> TodoServiceIntegrationTest.this.service.changeStatus(theirs.id(),
						new ChangeStatusRequest(TodoStatus.COMPLETED, theirs.version())));
		}

		@Test
		void aTodoCannotBeMadeToDependOnAnotherUsersTodo() {
			TodoResponse mine = create("mine");
			TodoResponse theirs = createAsOtherUser("theirs");

			assertThatExceptionOfType(TodoNotFoundException.class).isThrownBy(
					() -> TodoServiceIntegrationTest.this.service.addDependency(mine.id(), theirs.id()));
		}

		@Test
		void creatingATodoCannotAttachAnotherUsersDependency() {
			TodoResponse theirs = createAsOtherUser("theirs");

			assertThatExceptionOfType(TodoNotFoundException.class)
				.isThrownBy(() -> TodoServiceIntegrationTest.this.service.create(new CreateTodoRequest("mine", null,
						LocalDate.of(2026, 3, 10), TodoPriority.MEDIUM, null, List.of(theirs.id()))));
		}

	}

}
