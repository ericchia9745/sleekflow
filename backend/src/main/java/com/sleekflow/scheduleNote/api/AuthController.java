package com.sleekflow.scheduleNote.api;

import com.sleekflow.scheduleNote.dto.AuthenticatedUserResponse;
import com.sleekflow.scheduleNote.dto.ChangePasswordRequest;
import com.sleekflow.scheduleNote.dto.LoginRequest;
import com.sleekflow.scheduleNote.dto.RegisterRequest;
import com.sleekflow.scheduleNote.dto.SessionResponse;
import com.sleekflow.scheduleNote.security.CurrentUser;
import com.sleekflow.scheduleNote.security.SessionAttributes;
import com.sleekflow.scheduleNote.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, sign-in, and session management")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register and sign in",
			description = """
					`password` must be the hex-encoded SHA-256 of the user's password: the
					client hashes it so the plaintext never crosses the wire. The server
					salts and hashes that value again before storing it.
					Returns a bearer token, which is shown only here.""")
	public SessionResponse register(@Valid @RequestBody RegisterRequest request) {
		return this.authService.register(request);
	}

	@PostMapping("/login")
	@Operation(summary = "Sign in",
			description = "Takes the same hex-encoded SHA-256 as registration. Returns a bearer token.")
	public SessionResponse login(@Valid @RequestBody LoginRequest request) {
		return this.authService.login(request);
	}

	@PostMapping("/change-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Change a password by username",
			description = """
					No re-authentication and no email confirmation: if the username exists,
					its password is replaced outright. `newPassword` must be the hex-encoded
					SHA-256 of the new password, hashed the same way as registration and
					login.""")
	public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		this.authService.changePassword(request);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Sign out",
			description = "Revokes the session behind the supplied token. Other sessions are unaffected.")
	public void logout(HttpServletRequest request) {
		this.authService.logout((String) request.getAttribute(SessionAttributes.TOKEN));
	}

	@GetMapping("/me")
	@Operation(summary = "The signed-in user",
			description = "Used by the client on start-up to decide whether a stored token is still good.")
	public AuthenticatedUserResponse me() {
		return this.authService.describe(CurrentUser.get());
	}
}
