package com.sleekflow.scheduleNote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @param newPassword the SHA-256 of the new password, hex-encoded by the
 * browser the same way as registration and login. The server salts and hashes
 * this again before storing it.
 */
public record ChangePasswordRequest(
		@NotBlank String username,

		@NotBlank @Pattern(regexp = "^[a-f0-9]{64}$",
				message = "must be a hex-encoded SHA-256 digest; hash the password in the client")
		String newPassword) {
}
