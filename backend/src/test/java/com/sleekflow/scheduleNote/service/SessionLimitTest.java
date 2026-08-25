package com.sleekflow.scheduleNote.service;

import java.time.Instant;

import com.sleekflow.scheduleNote.ClientPasswordHash;
import com.sleekflow.scheduleNote.dto.LoginRequest;
import com.sleekflow.scheduleNote.dto.RegisterRequest;
import com.sleekflow.scheduleNote.repository.UserRepository;
import com.sleekflow.scheduleNote.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** A live TTL, with a deliberately small cap so the eviction path is reachable. */
@SpringBootTest(properties = { "app.auth.session-ttl=12h", "app.auth.max-sessions-per-user=2" })
@Transactional
class SessionLimitTest {

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
	@DisplayName("a user cannot accumulate live tokens beyond the cap")
	void signingInPastTheCapRevokesTheOldest() {
		this.authService.register(new RegisterRequest("eric", null, PASSWORD));
		this.authService.login(new LoginRequest("eric", PASSWORD));
		this.authService.login(new LoginRequest("eric", PASSWORD));
		this.sessions.flush();

		long live = this.sessions.findAll().stream().filter((s) -> s.isUsableAt(Instant.now())).count();

		assertThat(this.sessions.count()).isEqualTo(3);
		assertThat(live).isLessThanOrEqualTo(2);
	}

	@Test
	@DisplayName("staying under the cap leaves every session usable")
	void sessionsUnderTheCapAllRemainValid() {
		this.authService.register(new RegisterRequest("eric", null, PASSWORD));
		this.authService.login(new LoginRequest("eric", PASSWORD));
		this.sessions.flush();

		long live = this.sessions.findAll().stream().filter((s) -> s.isUsableAt(Instant.now())).count();

		assertThat(live).isEqualTo(2);
	}
}
