package com.sleekflow.scheduleNote.security;

import java.io.IOException;
import java.util.List;

import com.sleekflow.scheduleNote.entity.UserSession;
import com.sleekflow.scheduleNote.domain.exception.UnauthenticatedException;
import com.sleekflow.scheduleNote.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Resolves the bearer token on every API request and rejects the ones that
 * cannot be resolved.
 * <p>
 * Sign-in, registration, password changes and the API documentation are open;
 * everything else under {@code /api} requires a session. Preflight requests
 * pass through untouched, since a browser never attaches credentials to them.
 */
@Component
public class AuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private static final List<String> OPEN_PATHS = List.of("/api/auth/register", "/api/auth/login",
			"/api/auth/change-password");

	private final AuthService authService;

	private final ObjectMapper objectMapper;

	public AuthenticationFilter(AuthService authService, ObjectMapper objectMapper) {
		this.authService = authService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			return true;
		}
		String path = request.getRequestURI();
		return !path.startsWith("/api/") || OPEN_PATHS.contains(path);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String token = bearerToken(request);
		if (token == null) {
			reject(response, request, "This request needs a session. Sign in and send the token as "
					+ "'Authorization: Bearer <token>'.");
			return;
		}
		try {
			UserSession session = this.authService.authenticate(token);
			CurrentUser.set(session.getUser());
			request.setAttribute(SessionAttributes.TOKEN, token);
			chain.doFilter(request, response);
		}
		catch (UnauthenticatedException ex) {
			reject(response, request, ex.getMessage());
		}
		finally {
			CurrentUser.clear();
		}
	}

	private static String bearerToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BEARER)) {
			return null;
		}
		String token = header.substring(BEARER.length()).trim();
		return token.isEmpty() ? null : token;
	}

	/**
	 * Written here rather than rethrown, because a filter runs outside the
	 * dispatcher and the controller advice would never see the exception.
	 */
	private void reject(HttpServletResponse response, HttpServletRequest request, String detail) throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(org.springframework.http.HttpStatus.UNAUTHORIZED,
				detail);
		problem.setTitle("Not signed in");
		problem.setType(java.net.URI.create("https://sleekflow.example/problems/unauthenticated"));
		problem.setInstance(java.net.URI.create(request.getRequestURI()));

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		this.objectMapper.writeValue(response.getOutputStream(), problem);
	}
}
