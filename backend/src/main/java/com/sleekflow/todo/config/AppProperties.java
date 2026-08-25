package com.sleekflow.todo.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application settings bound from the {@code app.*} configuration namespace.
 * Every value has a default so the service starts with no environment set up.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(@DefaultValue Cors cors, @DefaultValue Pagination pagination) {

	public record Cors(@DefaultValue("http://localhost:5173") List<String> allowedOrigins) {
	}

	public record Pagination(@DefaultValue("25") int defaultPageSize, @DefaultValue("200") int maxPageSize) {
	}
}
