package com.sleekflow.scheduleNote.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * Salted SHA-256, in the format {@code sha256$<salt>$<digest>}.
 * <p>
 * <strong>This is a deliberate starting point, not a recommendation.</strong>
 * SHA-256 is built to be fast, which is precisely the wrong property for a
 * password digest: commodity hardware can test billions of candidates per
 * second, so an attacker holding the table recovers weak passwords quickly. The
 * per-user salt here defeats rainbow tables and stops identical passwords
 * sharing a digest, but it does nothing about brute force -- only a
 * deliberately slow algorithm does that.
 * <p>
 * The upgrade is one class: add an Argon2 or bcrypt implementation, make it the
 * primary hasher, and existing users are re-hashed on their next login by
 * {@code AuthService}. No migration, no password reset.
 */
@Component
public class Sha256PasswordHasher implements PasswordHasher {

	private static final String ALGORITHM = "sha256";

	private static final int SALT_BYTES = 16;

	private final SecureRandom random = new SecureRandom();

	@Override
	public String algorithm() {
		return ALGORITHM;
	}

	@Override
	public String hash(String rawPassword) {
		byte[] salt = new byte[SALT_BYTES];
		this.random.nextBytes(salt);
		String encodedSalt = Base64.getUrlEncoder().withoutPadding().encodeToString(salt);
		return "%s$%s$%s".formatted(ALGORITHM, encodedSalt, digest(rawPassword, encodedSalt));
	}

	@Override
	public boolean matches(String rawPassword, String storedHash) {
		if (rawPassword == null || storedHash == null) {
			return false;
		}
		String[] parts = storedHash.split("\\$");
		if (parts.length != 3 || !ALGORITHM.equals(parts[0])) {
			return false;
		}
		return constantTimeEquals(digest(rawPassword, parts[1]), parts[2]);
	}

	private static String digest(String rawPassword, String encodedSalt) {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			sha256.update(encodedSalt.getBytes(StandardCharsets.UTF_8));
			sha256.update((byte) '$');
			sha256.update(rawPassword.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(sha256.digest());
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is required by every JVM", ex);
		}
	}

	/**
	 * Compares without leaking, through timing, how much of the digest matched.
	 */
	private static boolean constantTimeEquals(String left, String right) {
		return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8),
				right.getBytes(StandardCharsets.UTF_8));
	}
}
