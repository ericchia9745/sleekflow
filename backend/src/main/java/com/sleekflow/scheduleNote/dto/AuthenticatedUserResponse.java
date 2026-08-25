package com.sleekflow.scheduleNote.dto;

import java.time.Instant;

import com.sleekflow.scheduleNote.domain.User;

public record AuthenticatedUserResponse(Long id, String username, String displayName, Instant createdAt) {

	public static AuthenticatedUserResponse from(User user) {
		return new AuthenticatedUserResponse(user.getId(), user.getUsername(), user.getDisplayName(),
				user.getCreatedAt());
	}
}
