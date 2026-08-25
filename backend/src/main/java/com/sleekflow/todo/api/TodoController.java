package com.sleekflow.todo.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import com.sleekflow.todo.api.dto.AddDependencyRequest;
import com.sleekflow.todo.api.dto.ChangeStatusRequest;
import com.sleekflow.todo.api.dto.CreateTodoRequest;
import com.sleekflow.todo.api.dto.StatusChangeResponse;
import com.sleekflow.todo.api.dto.TodoResponse;
import com.sleekflow.todo.api.dto.UpdateTodoRequest;
import com.sleekflow.todo.domain.TodoPriority;
import com.sleekflow.todo.domain.TodoStatus;
import com.sleekflow.todo.service.TodoQuery;
import com.sleekflow.todo.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/todos")
@Tag(name = "TODOs", description = "Create, organise, and track TODO items")
public class TodoController {

	private final TodoService service;

	public TodoController(TodoService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "List TODOs",
			description = """
					Filter by status, priority, due-date range, dependency state, and name.
					Sort with `sort=<key>,<asc|desc>` using dueDate, priority, status, name,
					createdAt, or updatedAt -- priority and status sort in their natural
					order rather than alphabetically. Results are always paged.""")
	public Page<TodoResponse> list(@RequestParam(required = false) List<TodoStatus> status,
			@RequestParam(required = false) List<TodoPriority> priority,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
			@Parameter(description = "true for TODOs waiting on a dependency, false for ones clear to start")
			@RequestParam(required = false) Boolean blocked,
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "false") boolean includeDeleted,
			@Parameter(description = "Show only soft-deleted TODOs")
			@RequestParam(defaultValue = "false") boolean deletedOnly,
			@PageableDefault(size = 25) Pageable pageable) {
		TodoQuery query = new TodoQuery(status, priority, dueFrom, dueTo, blocked, search, includeDeleted, deletedOnly);
		return this.service.list(query, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Fetch a single TODO, including soft-deleted ones")
	public TodoResponse get(@PathVariable Long id) {
		return this.service.get(id);
	}

	@PostMapping
	@Operation(summary = "Create a TODO")
	public ResponseEntity<TodoResponse> create(@Valid @RequestBody CreateTodoRequest request) {
		TodoResponse created = this.service.create(request);
		return ResponseEntity.created(URI.create("/api/todos/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a TODO",
			description = "Requires the version last seen by the client; a mismatch returns 409.")
	public TodoResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTodoRequest request) {
		return this.service.update(id, request);
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Change a TODO's status",
			description = """
					Moving to IN_PROGRESS fails with 409 while any dependency is unfinished.
					Completing a recurring TODO also schedules the next occurrence, which is
					returned alongside the updated TODO.""")
	public StatusChangeResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
		return this.service.changeStatus(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Soft-delete a TODO",
			description = "The TODO is hidden from the default list but retained and can be restored.")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		this.service.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/restore")
	@Operation(summary = "Restore a soft-deleted TODO")
	public TodoResponse restore(@PathVariable Long id) {
		return this.service.restore(id);
	}

	@PostMapping("/{id}/dependencies")
	@Operation(summary = "Make this TODO depend on another",
			description = "Rejected with 409 if the edge would create a dependency cycle.")
	public TodoResponse addDependency(@PathVariable Long id, @Valid @RequestBody AddDependencyRequest request) {
		return this.service.addDependency(id, request.dependsOnId());
	}

	@DeleteMapping("/{id}/dependencies/{dependsOnId}")
	@Operation(summary = "Remove a dependency edge")
	public TodoResponse removeDependency(@PathVariable Long id, @PathVariable Long dependsOnId) {
		return this.service.removeDependency(id, dependsOnId);
	}
}
