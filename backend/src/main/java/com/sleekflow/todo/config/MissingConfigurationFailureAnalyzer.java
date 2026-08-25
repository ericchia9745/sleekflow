package com.sleekflow.todo.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Turns {@link MissingConfigurationException} into an actionable startup message. */
class MissingConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<MissingConfigurationException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, MissingConfigurationException cause) {
		String description = "The '%s' profile deliberately ships no credential defaults, and these settings were not supplied:%n%s"
			.formatted(cause.getProfile(),
					cause.getMissingKeys().stream().map("    - %s"::formatted).reduce("", "%s%n%s"::formatted));
		String action = """
				Supply them as environment variables or in a .env file at the repository root, for example:

				    DB_HOST=localhost DB_NAME=sleekflow_todo DB_USER=todo DB_PASSWORD=secret \\
				      SPRING_PROFILES_ACTIVE=prod java -jar todo.jar

				See docs/CONFIGURATION.md for the full list of settings.""";
		return new FailureAnalysis(description, action, cause);
	}
}
