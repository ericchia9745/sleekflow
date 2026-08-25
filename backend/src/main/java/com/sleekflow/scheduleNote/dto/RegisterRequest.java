package com.sleekflow.scheduleNote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param password the SHA-256 of the user's password, hex-encoded by the
 * browser so the plaintext never leaves it. The server salts and hashes this
 * again before storing it.
 */
public record RegisterRequest(
		@NotBlank @Size(min = 3, max = 60)
		@Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "may contain only letters, digits, dot, underscore or hyphen")
		String username,

		@Size(max = 100) String displayName,

		@NotBlank @Pattern(regexp = "^[a-f0-9]{64}$",
				message = "must be a hex-encoded SHA-256 digest; hash the password in the client")
		String password) {

	public String displayNameOrUsername() {
		return (this.displayName != null && !this.displayName.isBlank()) ? this.displayName.trim() : this.username;
	}
}
