package com.sleekflow.scheduleNote.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * Issues opaque session tokens and derives the value stored against them.
 * <p>
 * The token is random, not a signed claim: revoking it is a row update rather
 * than a blocklist, which is what the "session in the database" requirement
 * asks for. Only its SHA-256 is persisted, so the sessions table cannot be used
 * to impersonate anyone.
 */
@Component
public class TokenFactory {

	private static final int TOKEN_BYTES = 32;

	private final SecureRandom random = new SecureRandom();

	/** A fresh 256-bit token, URL-safe so it survives headers and storage intact. */
	public String issue() {
		byte[] token = new byte[TOKEN_BYTES];
		this.random.nextBytes(token);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
	}

	public String fingerprint(String token) {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is required by every JVM", ex);
		}
	}
}
