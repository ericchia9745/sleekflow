package com.sleekflow.scheduleNote.api;

import java.util.List;
import java.util.Map;

import com.sleekflow.scheduleNote.exception.AuthenticationFailedException;
import com.sleekflow.scheduleNote.exception.CircularDependencyException;
import com.sleekflow.scheduleNote.exception.DependenciesNotSatisfiedException;
import com.sleekflow.scheduleNote.exception.InvalidTodoRequestException;
import com.sleekflow.scheduleNote.exception.NotTodoOwnerException;
import com.sleekflow.scheduleNote.exception.StaleTodoException;
import com.sleekflow.scheduleNote.exception.TodoNotFoundException;
import com.sleekflow.scheduleNote.exception.UnauthenticatedException;
import com.sleekflow.scheduleNote.exception.UserNotFoundException;
import com.sleekflow.scheduleNote.exception.UsernameTakenException;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain failures onto RFC 9457 problem responses, so a client can react to
 * {@code type} rather than parsing prose.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

	private static final String PROBLEM_BASE = "https://sleekflow.example/problems/";

	@ExceptionHandler(TodoNotFoundException.class)
	ProblemDetail handleNotFound(TodoNotFoundException ex) {
		ProblemDetail problem = problem(HttpStatus.NOT_FOUND, "TODO not found", ex.getMessage(), "todo-not-found");
		problem.setProperty("todoId", ex.getId());
		return problem;
	}

	@ExceptionHandler(DependenciesNotSatisfiedException.class)
	ProblemDetail handleBlocked(DependenciesNotSatisfiedException ex) {
		ProblemDetail problem = problem(HttpStatus.CONFLICT, "Dependencies not satisfied", ex.getMessage(),
				"dependencies-not-satisfied");
		problem.setProperty("outstandingDependencies", ex.getOutstanding());
		return problem;
	}

	@ExceptionHandler(NotTodoOwnerException.class)
	ProblemDetail handleNotOwner(NotTodoOwnerException ex) {
		return problem(HttpStatus.FORBIDDEN, "Not your TODO", ex.getMessage(), "not-todo-owner");
	}

	@ExceptionHandler(CircularDependencyException.class)
	ProblemDetail handleCycle(CircularDependencyException ex) {
		return problem(HttpStatus.CONFLICT, "Circular dependency", ex.getMessage(), "circular-dependency");
	}

	/**
	 * Both the explicit version check and a lost update detected by the database
	 * mean the same thing to a client: refetch and try again.
	 */
	@ExceptionHandler(StaleTodoException.class)
	ProblemDetail handleStale(StaleTodoException ex) {
		ProblemDetail problem = problem(HttpStatus.CONFLICT, "Concurrent modification", ex.getMessage(),
				"stale-version");
		problem.setProperty("expectedVersion", ex.getExpectedVersion());
		problem.setProperty("actualVersion", ex.getActualVersion());
		return problem;
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
		return problem(HttpStatus.CONFLICT, "Concurrent modification",
				"Someone else changed this TODO while your request was in flight. Refetch it and try again.",
				"stale-version");
	}

	/**
	 * Deliberately vague: telling a caller whether the username exists turns the
	 * sign-in form into a way to enumerate accounts.
	 */
	@ExceptionHandler(AuthenticationFailedException.class)
	ProblemDetail handleAuthenticationFailed(AuthenticationFailedException ex) {
		return problem(HttpStatus.UNAUTHORIZED, "Sign-in failed", ex.getMessage(), "authentication-failed");
	}

	@ExceptionHandler(UnauthenticatedException.class)
	ProblemDetail handleUnauthenticated(UnauthenticatedException ex) {
		return problem(HttpStatus.UNAUTHORIZED, "Not signed in", ex.getMessage(), "unauthenticated");
	}

	@ExceptionHandler(UsernameTakenException.class)
	ProblemDetail handleUsernameTaken(UsernameTakenException ex) {
		return problem(HttpStatus.CONFLICT, "Username taken", ex.getMessage(), "username-taken");
	}

	@ExceptionHandler(UserNotFoundException.class)
	ProblemDetail handleUserNotFound(UserNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "User not found", ex.getMessage(), "user-not-found");
	}

	@ExceptionHandler(InvalidTodoRequestException.class)
	ProblemDetail handleInvalidRequest(InvalidTodoRequestException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage(), "invalid-request");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
		List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream().map((error) -> Map.of(
				"field", error.getField(), "message", String.valueOf(error.getDefaultMessage()))).toList();
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed",
				"One or more fields are invalid.", "validation-failed");
		problem.setProperty("errors", errors);
		return problem;
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setType(java.net.URI.create(PROBLEM_BASE + type));
		return problem;
	}
}
