package com.sleekflow.todo.config;

import java.util.List;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * Fails startup with a readable message when a profile that has no credential
 * defaults is missing them.
 * <p>
 * Without this check the unresolved {@code ${DB_HOST}} placeholder is passed
 * through to the JDBC driver verbatim, and the first symptom is an
 * {@code UnknownHostException} several seconds into startup -- which says
 * nothing about the actual problem.
 * <p>
 * Runs last so that {@code .env} and profile-specific files have been loaded.
 */
public class RequiredConfigurationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final String GUARDED_PROFILE = "prod";

	private static final List<String> REQUIRED_KEYS = List.of("DB_HOST", "DB_NAME", "DB_USER", "DB_PASSWORD");

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (!environment.matchesProfiles(GUARDED_PROFILE)) {
			return;
		}
		List<String> missing = REQUIRED_KEYS.stream()
			.filter((key) -> !StringUtils.hasText(environment.getProperty(key)))
			.toList();
		if (!missing.isEmpty()) {
			throw new MissingConfigurationException(GUARDED_PROFILE, missing);
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
}
