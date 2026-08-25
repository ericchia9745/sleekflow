package com.sleekflow.scheduleNote.service;

import com.sleekflow.scheduleNote.ClientPasswordHash;
import com.sleekflow.scheduleNote.domain.UserSession;
import com.sleekflow.scheduleNote.dto.LoginRequest;
import com.sleekflow.scheduleNote.dto.RegisterRequest;
import com.sleekflow.scheduleNote.dto.SessionResponse;
import com.sleekflow.scheduleNote.exception.AuthenticationFailedException;
import com.sleekflow.scheduleNote.exception.UnauthenticatedException;
import com.sleekflow.scheduleNote.exception.UsernameTakenException;
import com.sleekflow.scheduleNote.repository.UserRepository;
import com.sleekflow.scheduleNote.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@Transactional
class AuthServiceIntegrationTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository users;

	@Autowired
	private UserSessionRepository sessions;

	private static final String PASSWORD = ClientPasswordHash.of("correct horse battery");

	@BeforeEach
	void clear() {
		this.sessions.deleteAllInBatch();
		this.users.deleteAllInBatch();
	}

	private SessionResponse registerEric() {
		return this.authService.register(new RegisterRequest("eric", "Eric Chia", PASSWORD));
	}

	@Test
	void registrationCreatesAUserAndSignsThemIn() {
		SessionResponse session = registerEric();

		assertThat(session.token()).isNotBlank();
		assertThat(session.user().username()).isEqualTo("eric");
		assertThat(session.user().displayName()).isEqualTo("Eric Chia");
		assertThat(session.expiresAt()).isAfter(java.time.Instant.now());
	}

	@Test
	void registrationFallsBackToTheUsernameWhenNoDisplayNameIsGiven() {
		SessionResponse session = this.authService.register(new RegisterRequest("bob", "   ", PASSWORD));
		assertThat(session.user().displayName()).isEqualTo("bob");
	}

	@Test
	void theStoredPasswordIsNotTheValueTheClientSent() {
		registerEric();
		String stored = this.users.findByUsername("eric").orElseThrow().getPasswordHash();

		// The client-side hash alone must not be enough to reconstruct the row:
		// the server salts and hashes it again before storing.
		assertThat(stored).isNotEqualTo(PASSWORD).startsWith("sha256$");
		assertThat(stored).doesNotContain(PASSWORD);
	}

	@Test
	void theStoredSessionIsNotTheToken() {
		SessionResponse session = registerEric();
		UserSession stored = this.sessions.findAll().getFirst();

		// Someone reading user_sessions must not be able to authenticate with
		// what they find there.
		assertThat(stored).isNotNull();
		assertThat(this.sessions.findByTokenHash(session.token())).isEmpty();
	}

	@Test
	void aUsernameCanOnlyBeRegisteredOnce() {
		registerEric();

		assertThatExceptionOfType(UsernameTakenException.class)
			.isThrownBy(() -> this.authService.register(new RegisterRequest("eric", "Impostor", PASSWORD)));
	}

	@Test
	void loginWithTheRightPasswordIssuesAFreshToken() {
		String firstToken = registerEric().token();

		SessionResponse second = this.authService.login(new LoginRequest("eric", PASSWORD));

		assertThat(second.token()).isNotEqualTo(firstToken);
		// The earlier session keeps working: signing in elsewhere must not
		// evict the device you are already using.
		assertThat(this.authService.authenticate(firstToken)).isNotNull();
	}

	@Test
	void loginWithTheWrongPasswordFails() {
		registerEric();

		assertThatExceptionOfType(AuthenticationFailedException.class)
			.isThrownBy(() -> this.authService.login(new LoginRequest("eric", ClientPasswordHash.of("guess"))));
	}

	@Test
	void anUnknownUsernameFailsIdenticallyToAWrongPassword() {
		registerEric();

		String unknownUser = catchMessage(() -> this.authService.login(new LoginRequest("nobody", PASSWORD)));
		String wrongPassword = catchMessage(
				() -> this.authService.login(new LoginRequest("eric", ClientPasswordHash.of("guess"))));

		// Differing messages would let an attacker enumerate valid usernames.
		assertThat(unknownUser).isEqualTo(wrongPassword);
	}

	private static String catchMessage(Runnable action) {
		try {
			action.run();
			throw new AssertionError("expected a failure");
		}
		catch (AuthenticationFailedException ex) {
			return ex.getMessage();
		}
	}

	@Test
	void authenticateResolvesTheSignedInUser() {
		SessionResponse session = registerEric();

		UserSession resolved = this.authService.authenticate(session.token());

		assertThat(resolved.getUser().getUsername()).isEqualTo("eric");
	}

	@Test
	void anUnknownTokenIsRejected() {
		registerEric();

		assertThatExceptionOfType(UnauthenticatedException.class)
			.isThrownBy(() -> this.authService.authenticate("nonsense-token"));
	}

	@Test
	void loggingOutRevokesOnlyThatSession() {
		SessionResponse first = registerEric();
		SessionResponse second = this.authService.login(new LoginRequest("eric", PASSWORD));

		this.authService.logout(first.token());

		assertThatExceptionOfType(UnauthenticatedException.class)
			.isThrownBy(() -> this.authService.authenticate(first.token()));
		assertThat(this.authService.authenticate(second.token())).isNotNull();
	}

	@Test
	void usingASessionSlidesItsExpiryForward() {
		SessionResponse session = registerEric();
		java.time.Instant issuedExpiry = session.expiresAt();

		UserSession touched = this.authService.authenticate(session.token());

		assertThat(touched.getExpiresAt()).isAfterOrEqualTo(issuedExpiry);
	}
}
