package com.sleekflow.scheduleNote.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.sleekflow.scheduleNote.dto.ChangeStatusRequest;
import com.sleekflow.scheduleNote.dto.CreateTodoRequest;
import com.sleekflow.scheduleNote.dto.RecurrenceRequest;
import com.sleekflow.scheduleNote.dto.StatusChangeResponse;
import com.sleekflow.scheduleNote.dto.TodoOwnerResponse;
import com.sleekflow.scheduleNote.dto.TodoResponse;
import com.sleekflow.scheduleNote.dto.TodoRevisionResponse;
import com.sleekflow.scheduleNote.dto.UpdateTodoRequest;
import com.sleekflow.scheduleNote.config.AppProperties;
import com.sleekflow.scheduleNote.domain.Recurrence;
import com.sleekflow.scheduleNote.domain.Todo;
import com.sleekflow.scheduleNote.domain.TodoStatus;
import com.sleekflow.scheduleNote.domain.User;
import com.sleekflow.scheduleNote.repository.TodoRepository;
import com.sleekflow.scheduleNote.repository.UserRepository;
import com.sleekflow.scheduleNote.specification.TodoSpecifications;
import com.sleekflow.scheduleNote.exception.CircularDependencyException;
import com.sleekflow.scheduleNote.exception.DependenciesNotSatisfiedException;
import com.sleekflow.scheduleNote.exception.InvalidTodoRequestException;
import com.sleekflow.scheduleNote.exception.NotTodoOwnerException;
import com.sleekflow.scheduleNote.exception.StaleTodoException;
import com.sleekflow.scheduleNote.exception.TodoNotFoundException;
import com.sleekflow.scheduleNote.security.CurrentUser;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class TodoService {

	private final TodoRepository repository;

	private final UserRepository users;

	private final AppProperties properties;

	public TodoService(TodoRepository repository, UserRepository users, AppProperties properties) {
		this.repository = repository;
		this.users = users;
		this.properties = properties;
	}

	// --- Reads ------------------------------------------------------------

	@Transactional(readOnly = true)
	public Page<TodoResponse> list(TodoQuery query, Pageable pageable) {
		Page<Todo> page = this.repository.findAll(toSpecification(query), capped(pageable));
		Set<Long> blockedIds = blockedIdsAmong(page.getContent());
		Map<Long, TodoOwnerResponse> owners = ownersOf(page.getContent());
		return page.map((todo) -> TodoResponse.of(todo, ownerOf(todo, owners), blockedIds.contains(todo.getId())));
	}

	/** The list's current fingerprint, for clients polling for changes. */
	@Transactional(readOnly = true)
	public TodoRevisionResponse revision() {
		return this.repository.loadRevision();
	}

	@Transactional(readOnly = true)
	public TodoResponse get(Long id) {
		Todo todo = this.repository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
		return TodoResponse.of(todo, ownerOf(todo), todo.isBlocked());
	}

	// --- Writes -----------------------------------------------------------

	public TodoResponse create(CreateTodoRequest request) {
		Todo todo = new Todo(currentUserId(), request.name(), request.description(), request.dueDate(),
				request.priority(), toRecurrence(request.recurrence()));
		for (Todo dependency : loadAll(request.dependencyIdsOrEmpty())) {
			todo.addDependency(dependency);
		}
		Todo saved = this.repository.save(todo);
		return respond(saved);
	}

	public TodoResponse update(Long id, UpdateTodoRequest request) {
		Todo todo = loadActive(id);
		requireCurrentVersion(todo, request.version());
		Recurrence recurrence = toRecurrence(request.recurrence());
		if (recurrence.recurs() && request.dueDate() == null) {
			throw new InvalidTodoRequestException("A recurring TODO needs a due date");
		}
		todo.setName(request.name());
		todo.setDescription(request.description());
		todo.setDueDate(request.dueDate());
		todo.setPriority(request.priority());
		todo.setRecurrence(recurrence);
		return respond(todo);
	}

	/**
	 * Moves a TODO to a new status, enforcing the dependency gate and scheduling
	 * the next occurrence when a recurring TODO is completed.
	 */
	public StatusChangeResponse changeStatus(Long id, ChangeStatusRequest request) {
		Todo todo = loadActive(id);
		requireCurrentVersion(todo, request.version());
		TodoStatus target = request.status();

		// The requirement gates IN_PROGRESS specifically: work cannot start while
		// something it depends on is unfinished.
		if (target == TodoStatus.IN_PROGRESS && todo.isBlocked()) {
			throw new DependenciesNotSatisfiedException(id, outstandingDependencyNames(todo));
		}

		boolean newlyCompleted = target == TodoStatus.COMPLETED && todo.getStatus() != TodoStatus.COMPLETED;
		todo.moveTo(target);

		// Guarded by the version check above: two clients completing the same
		// recurring TODO cannot both get past it, so only one occurrence appears.
		Todo nextOccurrence = (newlyCompleted && todo.getRecurrence().recurs())
				? this.repository.save(todo.nextOccurrence()) : null;

		this.repository.flush();
		return new StatusChangeResponse(TodoResponse.of(todo, ownerOf(todo), todo.isBlocked()),
				(nextOccurrence != null)
						? TodoResponse.of(nextOccurrence, ownerOf(nextOccurrence), nextOccurrence.isBlocked()) : null);
	}

	/**
	 * Soft delete. The row stays, so the TODO can be restored and anything that
	 * referenced it keeps a resolvable target.
	 * <p>
	 * Reserved to the owner. Everything else on a shared list is open to
	 * everyone, but taking someone's work off the list is a different act from
	 * correcting it, and it is the one that is awkward to notice.
	 */
	public void delete(Long id) {
		Todo todo = loadActive(id);
		requireOwner(todo, "deleted");
		todo.softDelete();
	}

	public TodoResponse restore(Long id) {
		Todo todo = this.repository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
		requireOwner(todo, "restored");
		todo.restore();
		return respond(todo);
	}

	// --- Dependencies -----------------------------------------------------

	public TodoResponse addDependency(Long id, Long dependsOnId) {
		Todo todo = loadActive(id);
		if (Objects.equals(id, dependsOnId)) {
			throw new CircularDependencyException(id, dependsOnId);
		}
		Todo dependency = loadActive(dependsOnId);
		requireNoCycle(id, dependsOnId);
		todo.addDependency(dependency);
		return respond(todo);
	}

	public TodoResponse removeDependency(Long id, Long dependsOnId) {
		Todo todo = loadActive(id);
		Todo dependency = this.repository.findById(dependsOnId)
			.orElseThrow(() -> new TodoNotFoundException(dependsOnId));
		todo.removeDependency(dependency);
		return respond(todo);
	}

	// --- Internals --------------------------------------------------------

	/**
	 * Maps a TODO that has just been written.
	 * <p>
	 * The flush matters: Hibernate bumps {@code @Version} when the transaction
	 * flushes, so reading it straight after mutating the entity yields the value
	 * the client already had. Returning that stale version makes the client's
	 * next write fail with a conflict that never actually happened.
	 */
	private TodoResponse respond(Todo todo) {
		this.repository.flush();
		return TodoResponse.of(todo, ownerOf(todo), todo.isBlocked());
	}

	private Todo loadActive(Long id) {
		return this.repository.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new TodoNotFoundException(id));
	}

	private static Long currentUserId() {
		return CurrentUser.get().getId();
	}

	private static void requireOwner(Todo todo, String action) {
		if (!Objects.equals(todo.getUserId(), currentUserId())) {
			throw new NotTodoOwnerException(todo.getId(), action);
		}
	}

	/**
	 * Owners for a whole page in one query, rather than one per row -- the same
	 * reasoning that keeps the blocked flag off the N+1 path.
	 */
	private Map<Long, TodoOwnerResponse> ownersOf(List<Todo> todos) {
		if (todos.isEmpty()) {
			return Map.of();
		}
		Set<Long> userIds = todos.stream().map(Todo::getUserId).collect(java.util.stream.Collectors.toSet());
		return this.users.findAllById(userIds)
			.stream()
			.collect(java.util.stream.Collectors.toMap(User::getId, TodoOwnerResponse::from));
	}

	private TodoOwnerResponse ownerOf(Todo todo) {
		return ownerOf(todo, ownersOf(List.of(todo)));
	}

	private static TodoOwnerResponse ownerOf(Todo todo, Map<Long, TodoOwnerResponse> owners) {
		TodoOwnerResponse owner = owners.get(todo.getUserId());
		return (owner != null) ? owner : TodoOwnerResponse.unknown(todo.getUserId());
	}

	private List<Todo> loadAll(Collection<Long> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		Set<Long> wanted = new LinkedHashSet<>(ids);
		Map<Long, Todo> found = this.repository.findAllById(wanted)
			.stream()
			.filter((todo) -> !todo.isDeleted())
			.collect(java.util.stream.Collectors.toMap(Todo::getId, Function.identity()));
		List<Long> missing = wanted.stream().filter((id) -> !found.containsKey(id)).toList();
		if (!missing.isEmpty()) {
			throw new TodoNotFoundException(missing.getFirst());
		}
		return wanted.stream().map(found::get).toList();
	}

	private void requireCurrentVersion(Todo todo, Long expectedVersion) {
		if (!Objects.equals(todo.getVersion(), expectedVersion)) {
			throw new StaleTodoException(todo.getId(), expectedVersion, todo.getVersion());
		}
	}

	private List<String> outstandingDependencyNames(Todo todo) {
		return todo.getDependencies()
			.stream()
			.filter((dependency) -> !dependency.isDeleted() && dependency.getStatus() != TodoStatus.COMPLETED)
			.map(Todo::getName)
			.toList();
	}

	/**
	 * Walks the graph outward from the proposed dependency. If the TODO being
	 * edited is reachable, the new edge would close a cycle.
	 */
	private void requireNoCycle(Long todoId, Long dependsOnId) {
		Set<Long> visited = new HashSet<>();
		Set<Long> frontier = Set.of(dependsOnId);
		while (!frontier.isEmpty()) {
			Set<Long> next = new HashSet<>(this.repository.findDependencyIdsOf(frontier));
			if (next.contains(todoId)) {
				throw new CircularDependencyException(todoId, dependsOnId);
			}
			next.removeAll(visited);
			visited.addAll(next);
			frontier = next;
		}
	}

	private Set<Long> blockedIdsAmong(List<Todo> todos) {
		if (todos.isEmpty()) {
			return Set.of();
		}
		return this.repository.findBlockedIdsAmong(todos.stream().map(Todo::getId).toList());
	}

	private Pageable capped(Pageable pageable) {
		int max = this.properties.pagination().maxPageSize();
		Pageable limited = (pageable.getPageSize() > max)
				? PageRequest.of(pageable.getPageNumber(), max, pageable.getSort()) : pageable;
		return PageRequest.of(limited.getPageNumber(), limited.getPageSize(), TodoSort.translate(limited.getSort()));
	}

	private static Recurrence toRecurrence(RecurrenceRequest request) {
		return (request != null) ? request.toRecurrence() : Recurrence.none();
	}

	private static Specification<Todo> toSpecification(TodoQuery query) {
		List<Specification<Todo>> specifications = new ArrayList<>();
		if (query.owner() != null) {
			specifications.add(TodoSpecifications.ownedBy(query.owner()));
		}
		if (query.deletedOnly()) {
			specifications.add(TodoSpecifications.deleted());
		}
		else if (!query.includeDeleted()) {
			specifications.add(TodoSpecifications.notDeleted());
		}
		if (query.statuses() != null && !query.statuses().isEmpty()) {
			specifications.add(TodoSpecifications.statusIn(query.statuses()));
		}
		if (query.priorities() != null && !query.priorities().isEmpty()) {
			specifications.add(TodoSpecifications.priorityIn(query.priorities()));
		}
		if (query.dueFrom() != null) {
			specifications.add(TodoSpecifications.dueOnOrAfter(query.dueFrom()));
		}
		if (query.dueTo() != null) {
			specifications.add(TodoSpecifications.dueOnOrBefore(query.dueTo()));
		}
		if (query.blocked() != null) {
			specifications.add(TodoSpecifications.blocked(query.blocked()));
		}
		if (StringUtils.hasText(query.search())) {
			specifications.add(TodoSpecifications.nameContains(query.search().trim()));
		}
		return Specification.allOf(specifications);
	}
}
