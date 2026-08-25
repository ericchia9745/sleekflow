package com.sleekflow.todo.config;

import java.util.List;

/** Raised at startup when a profile requires configuration the environment did not supply. */
public class MissingConfigurationException extends RuntimeException {

	private final transient List<String> missingKeys;

	private final String profile;

	MissingConfigurationException(String profile, List<String> missingKeys) {
		super("Missing required configuration for the '%s' profile: %s".formatted(profile,
				String.join(", ", missingKeys)));
		this.profile = profile;
		this.missingKeys = List.copyOf(missingKeys);
	}

	public List<String> getMissingKeys() {
		return this.missingKeys;
	}

	public String getProfile() {
		return this.profile;
	}
}
