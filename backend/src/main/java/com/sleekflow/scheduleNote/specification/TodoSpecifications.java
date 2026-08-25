package com.sleekflow.scheduleNote.specification;

import java.time.LocalDate;
import java.util.Collection;

import com.sleekflow.scheduleNote.domain.Todo;
import com.sleekflow.scheduleNote.domain.TodoPriority;
import com.sleekflow.scheduleNote.domain.TodoStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

/** Composable filters for the TODO list endpoint. */
public final class TodoSpecifications {

	private TodoSpecifications() {
	}

	public static Specification<Todo> notDeleted() {
		return (root, query, builder) -> builder.isNull(root.get("deletedAt"));
	}

	public static Specification<Todo> deleted() {
		return (root, query, builder) -> builder.isNotNull(root.get("deletedAt"));
	}

	public static Specification<Todo> statusIn(Collection<TodoStatus> statuses) {
		return (root, query, builder) -> root.get("status").in(statuses);
	}

	public static Specification<Todo> priorityIn(Collection<TodoPriority> priorities) {
		return (root, query, builder) -> root.get("priority").in(priorities);
	}

	public static Specification<Todo> dueOnOrAfter(LocalDate date) {
		return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("dueDate"), date);
	}

	public static Specification<Todo> dueOnOrBefore(LocalDate date) {
		return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("dueDate"), date);
	}

	public static Specification<Todo> hasNoDueDate() {
		return (root, query, builder) -> builder.isNull(root.get("dueDate"));
	}

	public static Specification<Todo> nameContains(String term) {
		return (root, query, builder) -> builder.like(builder.lower(root.get("name")),
				"%" + term.toLowerCase() + "%");
	}

	/**
	 * Blocked means at least one dependency is still outstanding. Expressed as an
	 * EXISTS subquery so the database does the work and the result stays usable
	 * for both paging and counting.
	 */
	public static Specification<Todo> blocked(boolean blocked) {
		return (root, query, builder) -> {
			Subquery<Integer> outstanding = query.subquery(Integer.class);
			Root<Todo> dependent = outstanding.from(Todo.class);
			Join<Todo, Todo> dependency = dependent.join("dependencies");
			outstanding.select(builder.literal(1))
				.where(builder.equal(dependent.get("id"), root.get("id")),
						builder.isNull(dependency.get("deletedAt")),
						builder.notEqual(dependency.get("status"), TodoStatus.COMPLETED));
			return blocked ? builder.exists(outstanding) : builder.not(builder.exists(outstanding));
		};
	}
}
