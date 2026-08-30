package com.rydr.eureka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 6 configuration: SecurityFilterChain replaces the removed WebSecurityConfigurerAdapter
 *
 * @author oi
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		/*
		 * By default, applications with SpringSecurity dependency require a CSRF token for every request.
		 * Eureka clients do not add one during registration, so the /eureka/** path must be excluded
		 */
		http.csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"));
		// Enable authentication with HttpBasic support
		http.authorizeHttpRequests(requests -> requests.anyRequest().authenticated());
		http.httpBasic(basic -> {
		});
		return http.build();
	}
}
