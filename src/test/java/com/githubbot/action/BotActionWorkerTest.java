package com.githubbot.action;

import java.time.Instant;

import com.githubbot.auth.TokenCipher;
import com.githubbot.auth.User;
import com.githubbot.auth.UserRepository;
import com.githubbot.github.GitHubClient;
import com.githubbot.repo.TrackedRepository;
import com.githubbot.repo.TrackedRepositoryRepository;
import com.githubbot.slack.SlackClient;
import com.githubbot.slack.SlackRequestException;
import com.githubbot.summary.SummaryClient;
import com.githubbot.summary.SummaryRequestException;
import com.githubbot.webhook.WebhookDelivery;
import com.githubbot.webhook.WebhookDeliveryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class BotActionWorkerTest {

	private static final String SLACK_URL = "https://hooks.slack.com/services/T/B/secret";

	@Autowired
	private BotActionWorker worker;

	@Autowired
	private UserRepository users;

	@Autowired
	private TrackedRepositoryRepository repositories;

	@Autowired
	private WebhookDeliveryRepository deliveries;

	@Autowired
	private BotActionRepository actions;

	@Autowired
	private BotRuleRepository rules;

	@Autowired
	private TokenCipher cipher;

	@MockitoBean
	private GitHubClient github;

	@MockitoBean
	private SlackClient slack;

	@MockitoBean
	private SummaryClient summaries;

	@BeforeEach
	void connectedRepository() {
		actions.deleteAll();
		deliveries.deleteAll();
		repositories.deleteAll();
		rules.deleteAll();
		users.deleteAll();
		User user = new User();
		user.setGithubId(99L);
		user.setGithubLogin("octocat");
		user.setAccessToken(cipher.encrypt("raw-token"));
		user.setCreatedAt(Instant.now());
		users.save(user);
		TrackedRepository repository = new TrackedRepository();
		repository.setUser(user);
		repository.setGithubRepoId(7L);
		repository.setOwner("octocat");
		repository.setName("github-bot-demo");
		repository.setWebhookId(55L);
		repository.setWebhookSecret(cipher.encrypt("hook-secret"));
		repository.setSlackWebhookUrl(cipher.encrypt(SLACK_URL));
		repositories.save(repository);
	}

	@Test
	void matchingIssueLabelsOnceAndNotifiesSlackOnce() {
		saveDelivery("issues", "delivery-bug", issue("opened", 7, "Test bug webhook", ""));

		worker.process();
		worker.process();

		verify(github, times(1)).addIssueLabel("raw-token", "octocat", "github-bot-demo", 7, "bug");
		verify(slack, times(1)).post(eq(SLACK_URL), contains("Test bug webhook"));
		assertEquals("succeeded", actionStatus("label"));
		assertEquals("succeeded", actionStatus("slack"));
	}

	@Test
	void slackFailureRetriesSlackWithoutLabelingAgain() {
		saveDelivery("issues", "delivery-retry", issue("opened", 7, "Test bug webhook", ""));
		doThrow(new SlackRequestException("Slack returned 500.")).doNothing().when(slack).post(anyString(), anyString());

		worker.process();
		worker.process();

		verify(github, times(1)).addIssueLabel("raw-token", "octocat", "github-bot-demo", 7, "bug");
		verify(slack, times(2)).post(eq(SLACK_URL), anyString());
		assertEquals("succeeded", actionStatus("label"));
		assertEquals("succeeded", actionStatus("slack"));
	}

	@Test
	void issueWithoutBugIsSkipped() {
		saveDelivery("issues", "delivery-note", issue("opened", 8, "Just a note", "Nothing to do"));

		worker.process();

		verify(github, never()).addIssueLabel(anyString(), anyString(), anyString(), anyInt(), anyString());
		verify(slack, never()).post(anyString(), anyString());
		assertEquals("skipped", deliveries.findByDeliveryId("delivery-note").orElseThrow().getStatus());
		assertEquals(0, actions.count());
	}

	@Test
	void savedRuleLabelsOnlyMatchingIssues() {
		saveRule("note", "question", true);
		saveDelivery("issues", "delivery-note", issue("opened", 8, "Just a note", ""));
		saveDelivery("issues", "delivery-bug", issue("opened", 7, "Test bug webhook", ""));

		worker.process();

		verify(github, times(1)).addIssueLabel("raw-token", "octocat", "github-bot-demo", 8, "question");
		verify(github, never()).addIssueLabel(anyString(), anyString(), anyString(), eq(7), anyString());
		verify(slack, times(1)).post(eq(SLACK_URL), contains("as question"));
		assertEquals("skipped", deliveries.findByDeliveryId("delivery-bug").orElseThrow().getStatus());
	}

	@Test
	void slackOffStillLabelsAndDoesNotNotify() {
		when(summaries.isConfigured()).thenReturn(true);
		saveRule("bug", "bug", false);
		saveDelivery("issues", "delivery-bug", issue("opened", 7, "Test bug webhook", ""));

		worker.process();

		verify(github, times(1)).addIssueLabel("raw-token", "octocat", "github-bot-demo", 7, "bug");
		verify(slack, never()).post(anyString(), anyString());
		verify(summaries, never()).summarize(anyString(), anyString());
		assertEquals(1, actions.count());
		assertEquals("succeeded", actionStatus("label"));
	}

	@Test
	void summaryIsIncludedInTheSlackMessage() {
		when(summaries.isConfigured()).thenReturn(true);
		when(summaries.summarize(eq("Login bug on the homepage"), anyString()))
				.thenReturn("The homepage login fails.");
		saveDelivery("issues", "delivery-summary",
				issue("opened", 7, "Login bug on the homepage", "The form rejects the password."));

		worker.process();

		verify(github, times(1)).addIssueLabel("raw-token", "octocat", "github-bot-demo", 7, "bug");
		verify(summaries).summarize(eq("Login bug on the homepage"), contains("password"));
		verify(slack).post(eq(SLACK_URL), contains("The homepage login fails."));
		assertEquals("succeeded", actionStatus("summary"));
		assertEquals("succeeded", actionStatus("label"));
		assertEquals("succeeded", actionStatus("slack"));
	}

	@Test
	void summaryFailureStillLabelsAndNotifies() {
		when(summaries.isConfigured()).thenReturn(true);
		when(summaries.summarize(anyString(), anyString()))
				.thenThrow(new SummaryRequestException("Groq returned 500."));
		saveDelivery("issues", "delivery-summary-fail", issue("opened", 7, "Test bug webhook", ""));

		worker.process();

		verify(github, times(1)).addIssueLabel("raw-token", "octocat", "github-bot-demo", 7, "bug");
		verify(slack).post(eq(SLACK_URL), contains("Test bug webhook"));
		assertEquals("failed", actionStatus("summary"));
		assertEquals("succeeded", actionStatus("label"));
		assertEquals("succeeded", actionStatus("slack"));
	}

	@Test
	void missingGroqKeySkipsTheSummary() {
		when(summaries.isConfigured()).thenReturn(false);
		saveDelivery("issues", "delivery-no-key", issue("opened", 7, "Test bug webhook", ""));

		worker.process();

		verify(summaries, never()).summarize(anyString(), anyString());
		verify(github, times(1)).addIssueLabel("raw-token", "octocat", "github-bot-demo", 7, "bug");
		verify(slack, times(1)).post(eq(SLACK_URL), contains("Test bug webhook"));
		assertEquals(2, actions.count());
	}

	@Test
	void pingCreatesNoActions() {
		saveDelivery("ping", "delivery-ping", "{\"zen\":\"keep it logically awesome\"}");

		worker.process();

		verify(github, never()).addIssueLabel(anyString(), anyString(), anyString(), anyInt(), anyString());
		verify(slack, never()).post(anyString(), anyString());
		assertEquals(0, actions.count());
		assertEquals("skipped", deliveries.findByDeliveryId("delivery-ping").orElseThrow().getStatus());
	}

	private void saveRule(String keyword, String label, boolean slackEnabled) {
		BotRule rule = new BotRule();
		rule.setUser(users.findAll().getFirst());
		rule.setKeyword(keyword);
		rule.setLabel(label);
		rule.setSlackEnabled(slackEnabled);
		rules.save(rule);
	}

	private void saveDelivery(String event, String deliveryId, String payload) {
		WebhookDelivery delivery = new WebhookDelivery();
		delivery.setDeliveryId(deliveryId);
		delivery.setEvent(event);
		delivery.setRepository(repositories.findAll().getFirst());
		delivery.setPayload(payload);
		delivery.setReceivedAt(Instant.now());
		delivery.setStatus("received");
		deliveries.save(delivery);
	}

	private String actionStatus(String type) {
		return actions.findAll().stream()
				.filter(action -> type.equals(action.getActionType()))
				.findFirst()
				.orElseThrow()
				.getStatus();
	}

	private static String issue(String action, int number, String title, String body) {
		return """
				{"action":"%s","issue":{"number":%d,"title":"%s","body":"%s"}}
				""".formatted(action, number, title, body);
	}

}
