package com.githubbot.repo;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import com.githubbot.auth.TokenCipher;
import com.githubbot.auth.User;
import com.githubbot.github.GitHubClient;
import com.githubbot.github.GitHubRepo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RepositoryConnectionService {

	private final GitHubClient github;

	private final TrackedRepositoryRepository repositories;

	private final TokenCipher cipher;

	private final String baseUrl;

	public RepositoryConnectionService(GitHubClient github, TrackedRepositoryRepository repositories, TokenCipher cipher,
			@Value("${app.base-url}") String baseUrl) {
		this.github = github;
		this.repositories = repositories;
		this.cipher = cipher;
		this.baseUrl = baseUrl;
	}

	public List<GitHubRepo> listRepos(User user) {
		return github.listRepos(cipher.decrypt(user.getAccessToken()));
	}

	public List<TrackedRepository> listConnected(User user) {
		return repositories.findByUserOrderByNameAsc(user);
	}

	public TrackedRepository connect(User user, long githubRepoId, String slackWebhookUrl) {
		if (slackWebhookUrl == null || !slackWebhookUrl.startsWith("https://hooks.slack.com/")) {
			throw new IllegalArgumentException("Paste a Slack incoming webhook URL from hooks.slack.com.");
		}
		String token = cipher.decrypt(user.getAccessToken());
		GitHubRepo repo = github.listRepos(token).stream()
				.filter(candidate -> candidate.id() == githubRepoId)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("That repository is not on your GitHub account."));
		TrackedRepository existing = repositories.findByUserAndGithubRepoId(user, githubRepoId).orElse(null);
		if (existing != null) {
			existing.setSlackWebhookUrl(cipher.encrypt(slackWebhookUrl));
			return repositories.save(existing);
		}
		String secret = newSecret();
		long webhookId = github.createWebhook(token, repo.owner(), repo.name(), callbackUrl(), secret);
		TrackedRepository tracked = new TrackedRepository();
		tracked.setUser(user);
		tracked.setGithubRepoId(repo.id());
		tracked.setOwner(repo.owner());
		tracked.setName(repo.name());
		tracked.setWebhookId(webhookId);
		tracked.setWebhookSecret(cipher.encrypt(secret));
		tracked.setSlackWebhookUrl(cipher.encrypt(slackWebhookUrl));
		return repositories.save(tracked);
	}

	private String callbackUrl() {
		return baseUrl.replaceAll("/$", "") + "/webhooks/github";
	}

	private static String newSecret() {
		byte[] bytes = new byte[32];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

}
