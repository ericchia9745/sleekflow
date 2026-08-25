package com.sleekflow.scheduleNote;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Reproduces what the browser does before a password leaves it: SHA-256, hex
 * encoded. Tests send this, exactly as the real client would.
 */
public final class ClientPasswordHash {

	private ClientPasswordHash() {
	}

	public static String of(String password) {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(sha256.digest(password.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
