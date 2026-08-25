package com.sleekflow.scheduleNote.dto;

import java.time.Instant;

/**
 * @param token the bearer token; returned exactly once, at sign-in, and never
 * recoverable from the server afterwards
 */
public record SessionResponse(String token, Instant expiresAt, AuthenticatedUserResponse user) {
}
