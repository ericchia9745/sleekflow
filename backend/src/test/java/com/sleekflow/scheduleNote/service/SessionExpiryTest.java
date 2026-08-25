package com.sleekflow.scheduleNote.service;

import com.sleekflow.scheduleNote.ClientPasswordHash;
import com.sleekflow.scheduleNote.dto.RegisterRequest;
import com.sleekflow.scheduleNote.dto.SessionResponse;
import com.sleekflow.scheduleNote.exception.UnauthenticatedException;
import com.sleekflow.scheduleNote.repository.UserRepository;
import com.sleekflow.scheduleNote.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * A zero TTL makes every issued session already expired the moment it is used,
 * which tests the expiry branch without sleeping or racing a short timer.
 */
@SpringBootTest(properties = "app.auth.session-ttl=0ms")
@Transactional
class SessionExpiryTest {

	private static final String PASSWORD = ClientPasswordHash.of("correct horse battery");

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository users;

	@Autowired
	private UserSessionRepository sessions;

	@BeforeEach
	void clear() {
		this.sessions.deleteAllInBatch();
		this.users.deleteAllInBatch();
	}

	@Test
	@DisplayName("an expired session is refused rather than silently renewed")
	void expiredSessionIsRefused() {
		SessionResponse session = this.authService.register(new RegisterRequest("eric", null, PASSWORD));

		assertThatExceptionOfType(UnauthenticatedException.class)
			.isThrownBy(() -> this.authService.authenticate(session.token()))
			.withMessageContaining("expired");
	}

	@Test
	@DisplayName("expired sessions can be swept out of the table")
	void expiredSessionsArePurgeable() {
		this.authService.register(new RegisterRequest("eric", null, PASSWORD));
		assertThat(this.sessions.count()).isEqualTo(1);

		assertThat(this.authService.purgeExpiredSessions()).isEqualTo(1);
		assertThat(this.sessions.count()).isZero();
	}
}
