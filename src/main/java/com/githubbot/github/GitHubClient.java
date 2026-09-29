package com.githubbot.github;

import java.util.List;

public interface GitHubClient {

	List<GitHubRepo> listRepos(String accessToken);

	long createWebhook(String accessToken, String owner, String name, String callbackUrl, String secret);

}
