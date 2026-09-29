package com.githubbot.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, GitHubLoginSuccessHandler successHandler)
			throws Exception {
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/health", "/error", "/css/**", "/oauth2/**", "/login/oauth2/**", "/webhooks/github")
				.permitAll()
				.anyRequest().authenticated())
			.csrf(csrf -> csrf.ignoringRequestMatchers("/webhooks/github"))
			.oauth2Login(oauth -> oauth.successHandler(successHandler))
			.logout(logout -> logout.logoutSuccessUrl("/").permitAll());
		return http.build();
	}

}
