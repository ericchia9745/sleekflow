package com.sleekflow.scheduleNote.config;

import com.sleekflow.scheduleNote.security.AuthenticationFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers the token filter for {@code /api/*} only.
 * <p>
 * Without an explicit registration Boot would apply the filter to every
 * request, including the Swagger UI and actuator. Ordering it after the CORS
 * handling matters: a rejected preflight would otherwise surface in the browser
 * as an opaque CORS failure rather than a 401.
 */
@Configuration
class SecurityConfig {

	@Bean
	FilterRegistrationBean<AuthenticationFilter> authenticationFilterRegistration(AuthenticationFilter filter) {
		FilterRegistrationBean<AuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
		registration.addUrlPatterns("/api/*");
		registration.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
		return registration;
	}
}
