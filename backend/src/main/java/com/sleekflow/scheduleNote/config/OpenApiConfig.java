package com.sleekflow.scheduleNote.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

	@Bean
	OpenAPI todoOpenApi() {
		return new OpenAPI().info(new Info().title("SleekFlow TODO API")
			.description("Manage TODOs with recurrence schedules and task dependencies.")
			.version("v1")
			.license(new License().name("MIT")));
	}
}
