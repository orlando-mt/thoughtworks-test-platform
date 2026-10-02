package com.thoughtworks.problem1application.configurations;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.thoughtworks.problem1application.infrastructure.security.JwtAuthenticationFilter;
import com.thoughtworks.problem1application.infrastructure.security.TokenCreator;

@Configuration
public class JwtConfig {

	@Bean
	public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilter(TokenCreator tokenCreator) {
		FilterRegistrationBean<JwtAuthenticationFilter> registration =
				new FilterRegistrationBean<>(new JwtAuthenticationFilter(tokenCreator));
		registration.addUrlPatterns("/api/users", "/api/users/*", "/api/modules", "/api/modules/*",
				"/api/projects", "/api/projects/*");
		registration.setOrder(1);
		return registration;
	}
}