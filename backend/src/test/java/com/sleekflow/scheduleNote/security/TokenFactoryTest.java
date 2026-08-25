package com.sleekflow.scheduleNote.security;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenFactoryTest {

	private final TokenFactory factory = new TokenFactory();

	@Test
	void issuesUrlSafeTokensOfUsefulLength() {
		assertThat(this.factory.issue()).matches("^[A-Za-z0-9_-]{43}$"); // 32 bytes, base64url
	}

	@Test
	void neverRepeatsAToken() {
		Set<String> issued = new HashSet<>();
		for (int i = 0; i < 5_000; i++) {
			issued.add(this.factory.issue());
		}
		assertThat(issued).hasSize(5_000);
	}

	@Test
	void fingerprintIsStableForTheSameToken() {
		String token = this.factory.issue();
		assertThat(this.factory.fingerprint(token)).isEqualTo(this.factory.fingerprint(token));
	}

	@Test
	void fingerprintDoesNotRevealTheToken() {
		// What is stored must not be usable as a credential.
		String token = this.factory.issue();
		assertThat(this.factory.fingerprint(token)).isNotEqualTo(token).matches("^[a-f0-9]{64}$");
	}

	@Test
	void differentTokensFingerprintDifferently() {
		assertThat(this.factory.fingerprint(this.factory.issue()))
			.isNotEqualTo(this.factory.fingerprint(this.factory.issue()));
	}
}
