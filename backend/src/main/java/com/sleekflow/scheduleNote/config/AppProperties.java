package com.sleekflow.scheduleNote.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application settings bound from the {@code app.*} configuration namespace.
 * Every value has a default so the service starts with no environment set up.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(@DefaultValue Cors cors, @DefaultValue Pagination pagination,
		@DefaultValue Auth auth) {

	public record Cors(@DefaultValue("http://localhost:5173") List<String> allowedOrigins) {
	}

	public record Pagination(@DefaultValue("25") int defaultPageSize, @DefaultValue("200") int maxPageSize) {
	}

	/**
	 * @param sessionTtl how long a session stays valid after its last use
	 * @param maxSessionsPerUser oldest sessions are revoked past this, so a
	 * user cannot accumulate live tokens indefinitely
	 */
	public record Auth(@DefaultValue("12h") Duration sessionTtl,
			@DefaultValue("10") int maxSessionsPerUser) {
	}
}
