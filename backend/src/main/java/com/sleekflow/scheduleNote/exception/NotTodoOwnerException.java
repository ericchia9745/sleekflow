package com.sleekflow.scheduleNote.exception;

/**
 * Raised when someone tries to delete or restore a TODO they did not create.
 * <p>
 * The list is shared and edits are open to everyone -- this guards only the
 * destructive pair, on the reasoning that removing another person's work from
 * the list is a different kind of act from correcting it.
 */
public class NotTodoOwnerException extends RuntimeException {

	public NotTodoOwnerException(Long todoId, String action) {
		super("TODO %d belongs to someone else, so it cannot be %s by you. Ask its owner, or edit it instead."
			.formatted(todoId, action));
	}

}
