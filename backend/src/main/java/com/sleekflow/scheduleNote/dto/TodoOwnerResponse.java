package com.sleekflow.scheduleNote.dto;

import com.sleekflow.scheduleNote.domain.User;

/**
 * Who created a TODO, on a shared list.
 * <p>
 * Attribution rather than access control: everyone sees every TODO, and this
 * says whose it is. Resolved once per page rather than per row -- {@code Todo}
 * holds a plain {@code user_id} column, not a relation, so there is nothing to
 * lazily traverse and nothing to N+1.
 */
public record TodoOwnerResponse(Long id, String username, String displayName) {

	public static TodoOwnerResponse from(User user) {
		return new TodoOwnerResponse(user.getId(), user.getUsername(), user.getDisplayName());
	}

	/** Stands in for an owner whose account has since been removed. */
	public static TodoOwnerResponse unknown(Long userId) {
		return new TodoOwnerResponse(userId, null, "Unknown");
	}
}
