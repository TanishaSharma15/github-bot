package com.githubbot.auth;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class GitHubLoginSuccessHandler implements AuthenticationSuccessHandler {

	private final OAuth2AuthorizedClientService clients;

	private final UserAccountService accounts;

	public GitHubLoginSuccessHandler(OAuth2AuthorizedClientService clients, UserAccountService accounts) {
		this.clients = clients;
		this.accounts = accounts;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) authentication;
		OAuth2AuthorizedClient client = clients.loadAuthorizedClient(oauth.getAuthorizedClientRegistrationId(),
				oauth.getName());
		OAuth2User principal = oauth.getPrincipal();
		Number githubId = principal.getAttribute("id");
		String login = principal.getAttribute("login");
		accounts.upsert(githubId.longValue(), login, client.getAccessToken().getTokenValue());
		response.sendRedirect("/");
	}

}
