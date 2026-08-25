package com.sleekflow.scheduleNote.security;

import com.sleekflow.scheduleNote.entity.User;

/**
 * The authenticated user for the request in flight.
 * <p>
 * A thread-local rather than a Spring Security context because this service
 * does not use Spring Security; the filter sets it and clears it in a finally
 * block, so nothing leaks onto a pooled thread.
 */
public final class CurrentUser {

	private static final ThreadLocal<User> HOLDER = new ThreadLocal<>();

	private CurrentUser() {
	}

	public static void set(User user) {
		HOLDER.set(user);
	}

	public static User get() {
		return HOLDER.get();
	}

	public static void clear() {
		HOLDER.remove();
	}
}
