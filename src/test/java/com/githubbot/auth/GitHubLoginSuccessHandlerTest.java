package com.githubbot.auth;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class GitHubLoginSuccessHandlerTest {

	@Autowired
	private GitHubLoginSuccessHandler handler;

	@Autowired
	private OAuth2AuthorizedClientRepository authorizedClients;

	@Autowired
	private ClientRegistrationRepository registrations;

	@Autowired
	private UserRepository users;

	@Test
	void savesTheUserWithAnEncryptedTokenAfterGitHubLogin() throws Exception {
		DefaultOAuth2User principal = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
				Map.of("id", 12345, "login", "TanishaSharma15"), "id");
		OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(principal, principal.getAuthorities(),
				"github");
		OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "ghu_example_token",
				Instant.now(), Instant.now().plusSeconds(3600));
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		authorizedClients.saveAuthorizedClient(
				new OAuth2AuthorizedClient(registrations.findByRegistrationId("github"), authentication.getName(),
						accessToken),
				authentication, request, response);

		handler.onAuthenticationSuccess(request, response, authentication);

		User saved = users.findByGithubId(12345L).orElseThrow();
		assertEquals("TanishaSharma15", saved.getGithubLogin());
		assertFalse(saved.getAccessToken().contains("ghu_example_token"));
		assertTrue(response.getRedirectedUrl().endsWith("/"));
	}

}
