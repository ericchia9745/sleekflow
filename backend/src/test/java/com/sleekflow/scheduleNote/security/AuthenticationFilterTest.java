package com.sleekflow.scheduleNote.security;

import com.sleekflow.scheduleNote.ClientPasswordHash;
import com.sleekflow.scheduleNote.dto.RegisterRequest;
import com.sleekflow.scheduleNote.dto.SessionResponse;
import com.sleekflow.scheduleNote.repository.UserRepository;
import com.sleekflow.scheduleNote.repository.UserSessionRepository;
import com.sleekflow.scheduleNote.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The filter decides what is reachable without signing in, so these run through
 * the real servlet chain rather than a controller slice.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationFilterTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository users;

	@Autowired
	private UserSessionRepository sessions;

	private String token;

	@BeforeEach
	void signIn() {
		this.sessions.deleteAllInBatch();
		this.users.deleteAllInBatch();
		SessionResponse session = this.authService
			.register(new RegisterRequest("eric", "Eric", ClientPasswordHash.of("correct horse battery")));
		this.token = session.token();
	}

	@Test
	void refusesAnApiRequestWithNoToken() throws Exception {
		this.mvc.perform(get("/api/todos"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("https://sleekflow.example/problems/unauthenticated"));
	}

	@Test
	void refusesAMalformedAuthorizationHeader() throws Exception {
		this.mvc.perform(get("/api/todos").header("Authorization", "Basic abc123"))
			.andExpect(status().isUnauthorized());
		this.mvc.perform(get("/api/todos").header("Authorization", "Bearer "))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void refusesAnUnknownToken() throws Exception {
		this.mvc.perform(get("/api/todos").header("Authorization", "Bearer nonsense"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void allowsAnApiRequestCarryingAValidToken() throws Exception {
		this.mvc.perform(get("/api/todos").header("Authorization", "Bearer " + this.token))
			.andExpect(status().isOk());
	}

	@Test
	void letsRegistrationAndSignInThroughWithoutAToken() throws Exception {
		// Otherwise there would be no way to obtain one.
		this.mvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"eric","password":"%s"}""".formatted(ClientPasswordHash.of("correct horse battery"))))
			.andExpect(status().isOk());
	}

	@Test
	void leavesTheDocumentationAndHealthEndpointsOpen() throws Exception {
		// These sit outside /api, so the filter never sees them.
		this.mvc.perform(get("/actuator/health")).andExpect(status().isOk());
		this.mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	@Test
	void identifiesTheSignedInUserToTheApplication() throws Exception {
		this.mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + this.token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("eric"));
	}
}
