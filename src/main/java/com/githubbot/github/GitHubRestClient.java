package com.githubbot.github;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GitHubRestClient implements GitHubClient {

	private static final String ACCEPT = "application/vnd.github+json";

	private final RestClient restClient;

	public GitHubRestClient() {
		this(RestClient.builder().baseUrl("https://api.github.com").build());
	}

	GitHubRestClient(RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public List<GitHubRepo> listRepos(String accessToken) {
		try {
			RepoResponse[] repos = restClient.get()
					.uri(uri -> uri.path("/user/repos").queryParam("per_page", 100).queryParam("affiliation", "owner").build())
					.header("Authorization", bearer(accessToken))
					.header("Accept", ACCEPT)
					.retrieve()
					.body(RepoResponse[].class);
			if (repos == null) {
				return List.of();
			}
			return Arrays.stream(repos)
					.filter(repo -> repo.owner() != null && repo.owner().login() != null)
					.map(repo -> new GitHubRepo(repo.id(), repo.owner().login(), repo.name()))
					.toList();
		}
		catch (RestClientResponseException ex) {
			throw failure("listing repositories", ex);
		}
	}

	@Override
	public long createWebhook(String accessToken, String owner, String name, String callbackUrl, String secret) {
		Map<String, Object> body = Map.of(
				"name", "web",
				"active", true,
				"events", List.of("issues", "pull_request", "push"),
				"config", Map.of(
						"url", callbackUrl,
						"content_type", "json",
						"secret", secret));
		try {
			HookResponse created = restClient.post()
					.uri("/repos/{owner}/{repo}/hooks", owner, name)
					.header("Authorization", bearer(accessToken))
					.header("Accept", ACCEPT)
					.body(body)
					.retrieve()
					.body(HookResponse.class);
			if (created == null) {
				throw new GitHubRequestException("GitHub did not return a webhook id.");
			}
			return created.id();
		}
		catch (RestClientResponseException ex) {
			throw failure("creating the webhook", ex);
		}
	}

	private static String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}

	private static GitHubRequestException failure(String action, RestClientResponseException ex) {
		return new GitHubRequestException("GitHub returned " + ex.getStatusCode().value() + " while " + action + ".");
	}

	private record RepoResponse(long id, String name, OwnerResponse owner) {
	}

	private record OwnerResponse(String login) {
	}

	private record HookResponse(long id) {
	}

}
