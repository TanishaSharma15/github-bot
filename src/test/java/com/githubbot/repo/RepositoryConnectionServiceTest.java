package com.githubbot.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.githubbot.auth.TokenCipher;
import com.githubbot.auth.User;
import com.githubbot.github.GitHubClient;
import com.githubbot.github.GitHubRepo;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepositoryConnectionServiceTest {

	@Test
	void storesTheWebhookSecretAndSlackUrlEncrypted() {
		GitHubClient github = mock(GitHubClient.class);
		TrackedRepositoryRepository repositories = mock(TrackedRepositoryRepository.class);
		TokenCipher cipher = new TokenCipher("test-encryption-key");
		when(github.listRepos("raw-token")).thenReturn(List.of(new GitHubRepo(7L, "octocat", "github-bot-demo")));
		when(github.createWebhook(eq("raw-token"), eq("octocat"), eq("github-bot-demo"),
				eq("http://localhost:8080/webhooks/github"), any())).thenReturn(55L);
		when(repositories.findByUserAndGithubRepoId(any(), eq(7L))).thenReturn(Optional.empty());
		when(repositories.save(any(TrackedRepository.class))).thenAnswer(invocation -> invocation.getArgument(0));
		User user = new User();
		user.setAccessToken(cipher.encrypt("raw-token"));
		RepositoryConnectionService service = new RepositoryConnectionService(github, repositories, cipher,
				"http://localhost:8080");

		TrackedRepository saved = service.connect(user, 7L, "https://hooks.slack.com/services/T/B/secret");

		ArgumentCaptor<String> secret = ArgumentCaptor.forClass(String.class);
		verify(github).createWebhook(eq("raw-token"), eq("octocat"), eq("github-bot-demo"),
				eq("http://localhost:8080/webhooks/github"), secret.capture());
		assertFalse(secret.getValue().isBlank());
		assertNotEquals(secret.getValue(), saved.getWebhookSecret());
		assertEquals(secret.getValue(), cipher.decrypt(saved.getWebhookSecret()));
		assertNotEquals("https://hooks.slack.com/services/T/B/secret", saved.getSlackWebhookUrl());
		assertEquals("https://hooks.slack.com/services/T/B/secret", cipher.decrypt(saved.getSlackWebhookUrl()));
		assertEquals(55L, saved.getWebhookId());
		assertEquals("github-bot-demo", saved.getName());
	}

}
