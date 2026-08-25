package com.sleekflow.scheduleNote.config;

import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RequiredConfigurationEnvironmentPostProcessorTest {

	private final RequiredConfigurationEnvironmentPostProcessor postProcessor = new RequiredConfigurationEnvironmentPostProcessor();

	@Test
	void ignoresProfilesOtherThanProd() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("test");

		assertThatCode(() -> this.postProcessor.postProcessEnvironment(environment, null)).doesNotThrowAnyException();
	}

	@Test
	void reportsEveryMissingKeyAtOnce() {
		MockEnvironment environment = prodEnvironment();
		environment.setProperty("DB_HOST", "db.internal");

		assertThatExceptionOfType(MissingConfigurationException.class)
			.isThrownBy(() -> this.postProcessor.postProcessEnvironment(environment, null))
			.satisfies((ex) -> assertThat(ex.getMissingKeys()).containsExactly("DB_NAME", "DB_USER", "DB_PASSWORD"));
	}

	@Test
	void treatsABlankValueAsMissing() {
		MockEnvironment environment = prodEnvironment();
		environment.setProperty("DB_HOST", "db.internal");
		environment.setProperty("DB_NAME", "todo");
		environment.setProperty("DB_USER", "   ");
		environment.setProperty("DB_PASSWORD", "secret");

		assertThatExceptionOfType(MissingConfigurationException.class)
			.isThrownBy(() -> this.postProcessor.postProcessEnvironment(environment, null))
			.satisfies((ex) -> assertThat(ex.getMissingKeys()).containsExactly("DB_USER"));
	}

	@Test
	void passesWhenEveryRequiredKeyIsPresent() {
		MockEnvironment environment = prodEnvironment();
		environment.setProperty("DB_HOST", "db.internal");
		environment.setProperty("DB_NAME", "todo");
		environment.setProperty("DB_USER", "app");
		environment.setProperty("DB_PASSWORD", "secret");

		assertThatCode(() -> this.postProcessor.postProcessEnvironment(environment, null)).doesNotThrowAnyException();
	}

	private MockEnvironment prodEnvironment() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod");
		return environment;
	}
}
