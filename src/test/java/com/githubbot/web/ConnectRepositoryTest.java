package com.githubbot.web;

import java.time.Instant;
import java.util.List;

import com.githubbot.auth.TokenCipher;
import com.githubbot.auth.User;
import com.githubbot.auth.UserRepository;
import com.githubbot.github.GitHubClient;
import com.githubbot.github.GitHubRepo;
import com.githubbot.repo.TrackedRepositoryRepository;
import com.githubbot.webhook.WebhookDeliveryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConnectRepositoryTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository users;

	@Autowired
	private TrackedRepositoryRepository repositories;

	@Autowired
	private WebhookDeliveryRepository deliveries;

	@Autowired
	private TokenCipher cipher;

	@MockitoBean
	private GitHubClient github;

	@BeforeEach
	void signedInUser() {
		deliveries.deleteAll();
		repositories.deleteAll();
		users.deleteAll();
		User user = new User();
		user.setGithubId(99L);
		user.setGithubLogin("octocat");
		user.setAccessToken(cipher.encrypt("raw-token"));
		user.setCreatedAt(Instant.now());
		users.save(user);
		when(github.listRepos(anyString())).thenReturn(List.of(new GitHubRepo(7L, "octocat", "github-bot-demo")));
	}

	@Test
	void connectPageRequiresLogin() throws Exception {
		mockMvc.perform(get("/repositories"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/oauth2/authorization/github"));
	}

	@Test
	void connectPageListsRepositoriesFromGitHub() throws Exception {
		mockMvc.perform(get("/repositories").with(signedIn()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("github-bot-demo")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Slack")));
	}

	@Test
	void homeLinksToTheConnectPageAfterLogin() throws Exception {
		mockMvc.perform(get("/").with(signedIn()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Connect repository")));
	}

	@Test
	void connectingARepositoryShowsItAsConnected() throws Exception {
		when(github.createWebhook(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(55L);

		mockMvc.perform(post("/repositories").with(signedIn()).with(csrf())
				.param("githubRepoId", "7")
				.param("slackWebhookUrl", "https://hooks.slack.com/services/T/B/secret"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/repositories"));

		mockMvc.perform(get("/repositories").with(signedIn()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Connected octocat/github-bot-demo")));
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor signedIn() {
		return oauth2Login().attributes(attrs -> {
			attrs.put("id", 99);
			attrs.put("login", "octocat");
		});
	}

}
