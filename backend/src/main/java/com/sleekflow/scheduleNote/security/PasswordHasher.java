package com.sleekflow.scheduleNote.security;

/**
 * Hashes and verifies stored credentials.
 * <p>
 * Implementations produce a self-describing digest, {@code <algorithm>$...},
 * so more than one algorithm can coexist in the table. That is what makes an
 * upgrade possible without a migration or a forced password reset: verification
 * dispatches on the stored prefix, and {@link #needsUpgrade(String)} tells the
 * caller when to re-hash a password it has just seen in the clear.
 */
public interface PasswordHasher {

	/** The algorithm this hasher writes; used as the digest prefix. */
	String algorithm();

	String hash(String rawPassword);

	boolean matches(String rawPassword, String storedHash);

	/** Whether {@code storedHash} was produced by a weaker algorithm than the current one. */
	default boolean needsUpgrade(String storedHash) {
		return storedHash != null && !storedHash.startsWith(algorithm() + "$");
	}
}
