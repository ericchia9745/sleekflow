package com.sleekflow.scheduleNote.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import com.sleekflow.scheduleNote.dto.AddDependencyRequest;
import com.sleekflow.scheduleNote.dto.BulkIdsRequest;
import com.sleekflow.scheduleNote.dto.BulkResultResponse;
import com.sleekflow.scheduleNote.dto.BulkStatusRequest;
import com.sleekflow.scheduleNote.dto.ChangeStatusRequest;
import com.sleekflow.scheduleNote.dto.CreateTodoRequest;
import com.sleekflow.scheduleNote.dto.StatusChangeResponse;
import com.sleekflow.scheduleNote.dto.TodoResponse;
import com.sleekflow.scheduleNote.dto.TodoRevisionResponse;
import com.sleekflow.scheduleNote.dto.UpdateTodoRequest;
import com.sleekflow.scheduleNote.domain.enums.TodoPriority;
import com.sleekflow.scheduleNote.domain.enums.TodoStatus;
import com.sleekflow.scheduleNote.service.TodoQuery;
import com.sleekflow.scheduleNote.service.TodoService;
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
					The list is shared: every signed-in user sees every TODO, and each one
					carries the owner who created it. Filter by status, priority, due-date
					range, dependency state, name, and owner.
					Sort with `sort=<key>,<asc|desc>` using dueDate, priority, status, name,
					createdAt, or updatedAt -- priority and status sort in their natural
					order rather than alphabetically. Results are always paged.""")
	public Page<TodoResponse> list(@RequestParam(required = false) List<TodoStatus> status,
			@RequestParam(required = false) List<TodoPriority> priority,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
			@Parameter(description = "Restrict to the TODOs created by one user, by user id")
			@RequestParam(required = false) Long owner,
			@Parameter(description = "true for TODOs waiting on a dependency, false for ones clear to start")
			@RequestParam(required = false) Boolean blocked,
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "false") boolean includeDeleted,
			@Parameter(description = "Show only soft-deleted TODOs")
			@RequestParam(defaultValue = "false") boolean deletedOnly,
			@PageableDefault(size = 25) Pageable pageable) {
		TodoQuery query = new TodoQuery(status, priority, dueFrom, dueTo, owner, blocked, search, includeDeleted,
				deletedOnly);
		return this.service.list(query, pageable);
	}

	@GetMapping("/revision")
	@Operation(summary = "Fingerprint of the shared list, for change polling",
			description = """
					Returns the newest update timestamp and the row count. Clients poll this
					and refetch only when it differs from what they hold, which keeps an
					idle tab from pulling the list on a timer.""")
	public TodoRevisionResponse revision() {
		return this.service.revision();
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
			description = """
					The TODO is hidden from the default list but retained and can be restored.
					Reserved to the TODO's owner: deleting someone else's returns 403.""")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		this.service.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/restore")
	@Operation(summary = "Restore a soft-deleted TODO",
			description = "Reserved to the TODO's owner, like deleting.")
	public TodoResponse restore(@PathVariable Long id) {
		return this.service.restore(id);
	}

	@PostMapping("/bulk/status")
	@Operation(summary = "Change the status of many TODOs at once",
			description = """
					Best-effort rather than all-or-nothing: every item carries the version
					the client last saw, and an item that is blocked or has been edited by
					someone else is reported in `failed` while the rest still apply. The
					dependency gate is evaluated once for the whole batch, so the outcome
					does not depend on the order items appear in.
					Recurring TODOs completed here schedule their next occurrences, which
					are returned in `createdOccurrences`. Capped at PAGE_SIZE_MAX items.""")
	public BulkResultResponse changeStatusInBulk(@Valid @RequestBody BulkStatusRequest request) {
		return this.service.changeStatusInBulk(request);
	}

	@PostMapping("/bulk/delete")
	@Operation(summary = "Soft-delete many TODOs at once",
			description = "TODOs the caller does not own are reported in `failed` and left alone.")
	public BulkResultResponse deleteInBulk(@Valid @RequestBody BulkIdsRequest request) {
		return this.service.deleteInBulk(request);
	}

	@PostMapping("/bulk/restore")
	@Operation(summary = "Restore many soft-deleted TODOs at once",
			description = "TODOs the caller does not own are reported in `failed` and left alone.")
	public BulkResultResponse restoreInBulk(@Valid @RequestBody BulkIdsRequest request) {
		return this.service.restoreInBulk(request);
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
