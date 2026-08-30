package com.rydr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 6 configuration: SecurityFilterChain replaces the removed WebSecurityConfigurerAdapter
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// Disable CSRF
		http.csrf(AbstractHttpConfigurer::disable);
		// All requests must be authenticated before they can be processed
		http.authorizeHttpRequests(requests -> requests.anyRequest().fullyAuthenticated());
		http.httpBasic(basic -> {
		});
		// All REST services should be set to stateless to improve efficiency and performance
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		return http.build();
	}
}
