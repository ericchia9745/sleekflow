package com.sleekflow.scheduleNote.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.sleekflow.scheduleNote.config.AppProperties;
import com.sleekflow.scheduleNote.entity.User;
import com.sleekflow.scheduleNote.entity.UserSession;
import com.sleekflow.scheduleNote.dto.AuthenticatedUserResponse;
import com.sleekflow.scheduleNote.dto.ChangePasswordRequest;
import com.sleekflow.scheduleNote.dto.LoginRequest;
import com.sleekflow.scheduleNote.dto.RegisterRequest;
import com.sleekflow.scheduleNote.dto.SessionResponse;
import com.sleekflow.scheduleNote.domain.exception.AuthenticationFailedException;
import com.sleekflow.scheduleNote.domain.exception.UnauthenticatedException;
import com.sleekflow.scheduleNote.domain.exception.UserNotFoundException;
import com.sleekflow.scheduleNote.domain.exception.UsernameTakenException;
import com.sleekflow.scheduleNote.repository.UserRepository;
import com.sleekflow.scheduleNote.repository.UserSessionRepository;
import com.sleekflow.scheduleNote.security.PasswordHasher;
import com.sleekflow.scheduleNote.security.TokenFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

	private final UserRepository users;

	private final UserSessionRepository sessions;

	private final PasswordHasher passwordHasher;

	private final TokenFactory tokenFactory;

	private final AppProperties properties;

	public AuthService(UserRepository users, UserSessionRepository sessions, PasswordHasher passwordHasher,
			TokenFactory tokenFactory, AppProperties properties) {
		this.users = users;
		this.sessions = sessions;
		this.passwordHasher = passwordHasher;
		this.tokenFactory = tokenFactory;
		this.properties = properties;
	}

	public SessionResponse register(RegisterRequest request) {
		String username = request.username().trim();
		if (this.users.existsByUsername(username)) {
			throw new UsernameTakenException(username);
		}
		User user = new User(username, request.displayNameOrUsername(), this.passwordHasher.hash(request.password()));
		try {
			this.users.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException ex) {
			// Two registrations for the same name can pass the check above
			// concurrently; the unique constraint is what actually decides.
			throw new UsernameTakenException(username);
		}
		return openSession(user);
	}

	public SessionResponse login(LoginRequest request) {
		User user = this.users.findByUsername(request.username().trim())
			.orElseThrow(AuthenticationFailedException::new);
		if (!this.passwordHasher.matches(request.password(), user.getPasswordHash())) {
			throw new AuthenticationFailedException();
		}
		// The only moment the password is available in a comparable form, so it
		// is the only moment an old digest can be upgraded silently.
		if (this.passwordHasher.needsUpgrade(user.getPasswordHash())) {
			user.setPasswordHash(this.passwordHasher.hash(request.password()));
		}
		return openSession(user);
	}

	/**
	 * Replaces a user's password given only their username.
	 * <p>
	 * Deliberately asks for nothing else -- no current password, no emailed
	 * link. That is a real trade-off: anyone who knows a username can take over
	 * the account. Acceptable here because there is nowhere else a reset link
	 * could go (no email on file); revisit before this app has anything worth
	 * protecting.
	 */
	public void changePassword(ChangePasswordRequest request) {
		User user = this.users.findByUsername(request.username().trim())
			.orElseThrow(() -> new UserNotFoundException(request.username()));
		user.setPasswordHash(this.passwordHasher.hash(request.newPassword()));
	}

	/**
	 * Resolves a bearer token to its session, sliding the expiry forward.
	 * @throws UnauthenticatedException if the token is unknown, expired or revoked
	 */
	public UserSession authenticate(String token) {
		Instant now = Instant.now();
		UserSession session = this.sessions.findByTokenHash(this.tokenFactory.fingerprint(token))
			.orElseThrow(() -> new UnauthenticatedException("This session is not valid. Please sign in again."));
		if (!session.isUsableAt(now)) {
			throw new UnauthenticatedException("This session has expired. Please sign in again.");
		}
		session.touch(now, now.plus(this.properties.auth().sessionTtl()));
		return session;
	}

	public void logout(String token) {
		this.sessions.findByTokenHash(this.tokenFactory.fingerprint(token)).ifPresent(UserSession::revoke);
	}

	@Transactional(readOnly = true)
	public AuthenticatedUserResponse describe(User user) {
		return AuthenticatedUserResponse.from(user);
	}

	/** Removes sessions that expired long enough ago to be of no interest. */
	public int purgeExpiredSessions() {
		return this.sessions.deleteExpiredBefore(Instant.now());
	}

	private SessionResponse openSession(User user) {
		revokeOldestBeyondLimit(user);
		String token = this.tokenFactory.issue();
		Instant expiresAt = Instant.now().plus(this.properties.auth().sessionTtl());
		UserSession session = this.sessions
			.save(new UserSession(this.tokenFactory.fingerprint(token), user, expiresAt));
		return new SessionResponse(token, session.getExpiresAt(), AuthenticatedUserResponse.from(user));
	}

	private void revokeOldestBeyondLimit(User user) {
		int limit = this.properties.auth().maxSessionsPerUser();
		List<UserSession> live = this.sessions.findByUserAndRevokedAtIsNull(user)
			.stream()
			.filter((session) -> session.isUsableAt(Instant.now()))
			.sorted(Comparator.comparing(UserSession::getLastSeenAt))
			.toList();
		int excess = live.size() - (limit - 1);
		for (int i = 0; i < excess && i < live.size(); i++) {
			live.get(i).revoke();
		}
	}
}
