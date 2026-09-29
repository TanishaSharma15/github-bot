package com.githubbot.github;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class GitHubRestClientTest {

	@Test
	void createsAWebhookForIssuesPullRequestsAndPushes() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.github.com");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GitHubRestClient github = new GitHubRestClient(builder.build());
		server.expect(requestTo("https://api.github.com/repos/octocat/github-bot-demo/hooks"))
				.andExpect(method(POST))
				.andExpect(header("Authorization", "Bearer raw-token"))
				.andExpect(content().json("""
						{
						  "name": "web",
						  "active": true,
						  "events": ["issues", "pull_request", "push"],
						  "config": {
						    "url": "http://localhost:8080/webhooks/github",
						    "content_type": "json",
						    "secret": "test-secret"
						  }
						}
						"""))
				.andRespond(withSuccess("{\"id\":55}", MediaType.APPLICATION_JSON));

		long webhookId = github.createWebhook("raw-token", "octocat", "github-bot-demo",
				"http://localhost:8080/webhooks/github", "test-secret");

		assertEquals(55L, webhookId);
		server.verify();
	}

	@Test
	void addsABugLabelWhenTheLabelAlreadyExists() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.github.com");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GitHubRestClient github = new GitHubRestClient(builder.build());
		server.expect(requestTo("https://api.github.com/repos/octocat/github-bot-demo/labels"))
				.andExpect(method(POST))
				.andExpect(header("Authorization", "Bearer raw-token"))
				.andExpect(content().json("{\"name\":\"bug\",\"color\":\"d73a4a\"}"))
				.andRespond(withStatus(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY));
		server.expect(requestTo("https://api.github.com/repos/octocat/github-bot-demo/issues/7/labels"))
				.andExpect(method(POST))
				.andExpect(content().json("{\"labels\":[\"bug\"]}"))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		github.addIssueLabel("raw-token", "octocat", "github-bot-demo", 7, "bug");

		server.verify();
	}

}
