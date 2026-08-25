package com.sleekflow.scheduleNote.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sha256PasswordHasherTest {

	private final Sha256PasswordHasher hasher = new Sha256PasswordHasher();

	@Test
	void producesASelfDescribingDigest() {
		assertThat(this.hasher.hash("secret")).matches("^sha256\\$[A-Za-z0-9_-]+\\$[a-f0-9]{64}$");
	}

	@Test
	void acceptsTheCorrectPassword() {
		assertThat(this.hasher.matches("secret", this.hasher.hash("secret"))).isTrue();
	}

	@Test
	void rejectsTheWrongPassword() {
		String stored = this.hasher.hash("secret");
		assertThat(this.hasher.matches("Secret", stored)).isFalse();
		assertThat(this.hasher.matches("", stored)).isFalse();
	}

	@Test
	void saltsEachHashSoIdenticalPasswordsDoNotShareADigest() {
		// Without this, the table shows at a glance which accounts share a
		// password, and one cracked digest unlocks all of them.
		assertThat(this.hasher.hash("secret")).isNotEqualTo(this.hasher.hash("secret"));
		assertThat(this.hasher.matches("secret", this.hasher.hash("secret"))).isTrue();
	}

	@Test
	void treatsAMalformedOrForeignDigestAsANonMatch() {
		assertThat(this.hasher.matches("secret", "not-a-digest")).isFalse();
		assertThat(this.hasher.matches("secret", "bcrypt$salt$digest")).isFalse();
		assertThat(this.hasher.matches("secret", null)).isFalse();
	}

	@Test
	void reportsWhenADigestWasWrittenByAnotherAlgorithm() {
		// The hook that lets a stronger hasher take over with no migration:
		// anything not written by the current algorithm is re-hashed at login.
		assertThat(this.hasher.needsUpgrade(this.hasher.hash("secret"))).isFalse();
		assertThat(this.hasher.needsUpgrade("bcrypt$salt$digest")).isTrue();
	}
}
