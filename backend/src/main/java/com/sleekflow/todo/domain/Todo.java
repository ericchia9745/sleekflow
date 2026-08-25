package com.sleekflow.todo.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;

/**
 * A single TODO item.
 * <p>
 * Rules that must always hold live here rather than in the service, so they
 * cannot be bypassed by a future caller: the dependency gate on
 * {@code IN_PROGRESS}, the soft-delete flag, and the schedule arithmetic for the
 * next occurrence of a recurring TODO.
 */
@Entity
@Table(name = "todos")
public class Todo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(length = 2000)
	private String description;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TodoStatus status = TodoStatus.NOT_STARTED;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TodoPriority priority = TodoPriority.MEDIUM;

	@Embedded
	private Recurrence recurrence = Recurrence.none();

	@Column(name = "recurrence_source_id")
	private Long recurrenceSourceId;

	/**
	 * A TODO is blocked while any of these is not yet {@code COMPLETED}. Batched
	 * so rendering a page of TODOs does not turn into one query per row.
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "todo_dependencies", joinColumns = @JoinColumn(name = "todo_id"),
			inverseJoinColumns = @JoinColumn(name = "depends_on_id"))
	@BatchSize(size = 200)
	private Set<Todo> dependencies = new LinkedHashSet<>();

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private Long version;

	/** Generated column; mapped read-only so callers can sort by it. */
	@Column(name = "priority_rank", insertable = false, updatable = false)
	private Integer priorityRank;

	/** Generated column; mapped read-only so callers can sort by it. */
	@Column(name = "status_rank", insertable = false, updatable = false)
	private Integer statusRank;

	protected Todo() {
	}

	public Todo(String name, String description, LocalDate dueDate, TodoPriority priority, Recurrence recurrence) {
		this.name = name;
		this.description = description;
		this.dueDate = dueDate;
		this.priority = (priority != null) ? priority : TodoPriority.MEDIUM;
		this.recurrence = (recurrence != null) ? recurrence : Recurrence.none();
		this.status = TodoStatus.NOT_STARTED;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	// --- Dependency rules -------------------------------------------------

	/**
	 * Whether some dependency still stands in the way. A soft-deleted dependency
	 * is ignored: the work it represented is gone, so it cannot block anything.
	 */
	public boolean isBlocked() {
		return this.dependencies.stream().anyMatch(Todo::blocksDependents);
	}

	private boolean blocksDependents() {
		return !isDeleted() && this.status != TodoStatus.COMPLETED;
	}

	public void addDependency(Todo dependency) {
		this.dependencies.add(dependency);
	}

	public void removeDependency(Todo dependency) {
		this.dependencies.remove(dependency);
	}

	// --- Lifecycle --------------------------------------------------------

	public boolean isDeleted() {
		return this.deletedAt != null;
	}

	public void softDelete() {
		this.deletedAt = Instant.now();
	}

	public void restore() {
		this.deletedAt = null;
	}

	public void moveTo(TodoStatus target) {
		this.status = target;
		this.completedAt = (target == TodoStatus.COMPLETED) ? Instant.now() : null;
	}

	/**
	 * Builds the occurrence that follows this one. Only meaningful once this
	 * TODO is completed; the caller decides when that moment is.
	 * @return the next occurrence, sharing this TODO's series identity
	 */
	public Todo nextOccurrence() {
		if (!this.recurrence.recurs()) {
			throw new IllegalStateException("TODO %d does not recur".formatted(this.id));
		}
		if (this.dueDate == null) {
			throw new IllegalStateException("A recurring TODO needs a due date to schedule the next occurrence");
		}
		Todo next = new Todo(this.name, this.description, this.recurrence.nextDueDateAfter(this.dueDate),
				this.priority, this.recurrence);
		next.recurrenceSourceId = (this.recurrenceSourceId != null) ? this.recurrenceSourceId : this.id;
		return next;
	}

	// --- Accessors --------------------------------------------------------

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDate getDueDate() {
		return this.dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public TodoStatus getStatus() {
		return this.status;
	}

	public TodoPriority getPriority() {
		return this.priority;
	}

	public void setPriority(TodoPriority priority) {
		this.priority = priority;
	}

	public Recurrence getRecurrence() {
		return this.recurrence;
	}

	public void setRecurrence(Recurrence recurrence) {
		this.recurrence = (recurrence != null) ? recurrence : Recurrence.none();
	}

	public Long getRecurrenceSourceId() {
		return this.recurrenceSourceId;
	}

	public Set<Todo> getDependencies() {
		return this.dependencies;
	}

	public Instant getCompletedAt() {
		return this.completedAt;
	}

	public Instant getDeletedAt() {
		return this.deletedAt;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

	public Long getVersion() {
		return this.version;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Todo todo)) {
			return false;
		}
		return this.id != null && this.id.equals(todo.id);
	}

	@Override
	public int hashCode() {
		return Todo.class.hashCode();
	}

	@Override
	public String toString() {
		return "Todo{id=%d, name='%s', status=%s}".formatted(this.id, this.name, this.status);
	}
}
