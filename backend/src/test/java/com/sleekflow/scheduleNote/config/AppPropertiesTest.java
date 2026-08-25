package com.sleekflow.scheduleNote.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The project is handed over and re-run on other machines, so the contract that
 * matters is: sensible defaults with nothing configured, and every value
 * overridable from the environment.
 */
class AppPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(TestConfig.class);

	@Test
	void appliesDefaultsWhenNothingIsConfigured() {
		this.contextRunner.run((context) -> {
			AppProperties properties = context.getBean(AppProperties.class);
			assertThat(properties.cors().allowedOrigins()).containsExactly("http://localhost:5173");
			assertThat(properties.pagination().defaultPageSize()).isEqualTo(25);
			assertThat(properties.pagination().maxPageSize()).isEqualTo(200);
		});
	}

	@Test
	void bindsMultipleCorsOriginsFromACommaSeparatedValue() {
		this.contextRunner.withPropertyValues("app.cors.allowed-origins=https://a.test,https://b.test")
			.run((context) -> assertThat(context.getBean(AppProperties.class).cors().allowedOrigins())
				.isEqualTo(List.of("https://a.test", "https://b.test")));
	}

	@Test
	void overridesPaginationLimits() {
		this.contextRunner.withPropertyValues("app.pagination.default-page-size=50", "app.pagination.max-page-size=500")
			.run((context) -> {
				AppProperties properties = context.getBean(AppProperties.class);
				assertThat(properties.pagination().defaultPageSize()).isEqualTo(50);
				assertThat(properties.pagination().maxPageSize()).isEqualTo(500);
			});
	}

	@EnableConfigurationProperties(AppProperties.class)
	static class TestConfig {

	}
}
