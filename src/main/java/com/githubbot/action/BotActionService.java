package com.githubbot.action;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.githubbot.auth.TokenCipher;
import com.githubbot.github.GitHubClient;
import com.githubbot.repo.TrackedRepository;
import com.githubbot.slack.SlackClient;
import com.githubbot.summary.SummaryClient;
import com.githubbot.webhook.WebhookDelivery;
import com.githubbot.webhook.WebhookDeliveryRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BotActionService {

	static final int MAX_ATTEMPTS = 5;

	private final WebhookDeliveryRepository deliveries;

	private final BotActionRepository actions;

	private final BotRuleRepository rules;

	private final GitHubClient github;

	private final SlackClient slack;

	private final SummaryClient summaries;

	private final TokenCipher cipher;

	private final Duration retryDelay;

	public BotActionService(WebhookDeliveryRepository deliveries, BotActionRepository actions, BotRuleRepository rules,
			GitHubClient github, SlackClient slack, SummaryClient summaries, TokenCipher cipher,
			@Value("${app.action-retry-delay:30s}") Duration retryDelay) {
		this.deliveries = deliveries;
		this.actions = actions;
		this.rules = rules;
		this.github = github;
		this.slack = slack;
		this.summaries = summaries;
		this.cipher = cipher;
		this.retryDelay = retryDelay;
	}

	public List<Long> receivedDeliveryIds() {
		return deliveries.findIdsByStatus("received");
	}

	public List<Long> dueActionIds() {
		return actions.findDueIds(Instant.now(), MAX_ATTEMPTS);
	}

	@Transactional
	public void plan(long deliveryId) {
		WebhookDelivery delivery = deliveries.findById(deliveryId).orElse(null);
		if (delivery == null || !"received".equals(delivery.getStatus())) {
			return;
		}
		IssueMatch issue = IssueMatch.opened(delivery);
		if (issue != null) {
			planIssue(delivery, issue);
			return;
		}
		PullRequestMatch pullRequest = PullRequestMatch.opened(delivery);
		if (pullRequest != null) {
			planPullRequest(delivery, pullRequest);
			return;
		}
		PushMatch push = PushMatch.from(delivery);
		if (push != null) {
			planPush(delivery);
			return;
		}
		delivery.setStatus("skipped");
	}

	private void planIssue(WebhookDelivery delivery, IssueMatch issue) {
		RuleSettings rule = settingsFor(delivery);
		if (!issue.matches(rule.keyword())) {
			delivery.setStatus("skipped");
			return;
		}
		createAction(delivery, "label");
		if (rule.slackEnabled()) {
			if (summaries.isConfigured()) {
				createAction(delivery, "summary");
			}
			createAction(delivery, "slack");
		}
		delivery.setStatus("queued");
	}

	private void planPullRequest(WebhookDelivery delivery, PullRequestMatch pullRequest) {
		RuleSettings rule = settingsFor(delivery);
		if (pullRequest.matches(rule.keyword())) {
			createAction(delivery, "label");
			if (rule.slackEnabled() && summaries.isConfigured()) {
				createAction(delivery, "summary");
			}
		}
		if (rule.slackEnabled()) {
			createAction(delivery, "slack");
		}
		else if (!pullRequest.matches(rule.keyword())) {
			delivery.setStatus("skipped");
			return;
		}
		delivery.setStatus("queued");
	}

	private void planPush(WebhookDelivery delivery) {
		if (!settingsFor(delivery).slackEnabled()) {
			delivery.setStatus("skipped");
			return;
		}
		createAction(delivery, "slack");
		delivery.setStatus("queued");
	}

	@Transactional
	public void run(long actionId) {
		BotAction action = actions.findById(actionId).orElse(null);
		if (action == null || "succeeded".equals(action.getStatus()) || action.getAttempts() >= MAX_ATTEMPTS) {
			return;
		}
		if (action.getNextAttemptAt().isAfter(Instant.now())) {
			return;
		}
		try {
			perform(action);
			action.setStatus("succeeded");
			action.setLastError(null);
		}
		catch (RuntimeException ex) {
			action.setAttempts(action.getAttempts() + 1);
			action.setStatus("failed");
			action.setLastError(trim(ex.getMessage()));
			action.setNextAttemptAt(Instant.now().plus(retryDelay));
		}
	}

	private void perform(BotAction action) {
		WebhookDelivery delivery = action.getDelivery();
		IssueMatch issue = IssueMatch.opened(delivery);
		PullRequestMatch pullRequest = PullRequestMatch.opened(delivery);
		PushMatch push = PushMatch.from(delivery);
		if (issue == null && pullRequest == null && push == null) {
			throw new IllegalStateException("This delivery is not an opened issue, pull request, or push.");
		}
		RuleSettings rule = settingsFor(delivery);
		TrackedRepository repository = delivery.getRepository();
		if ("label".equals(action.getActionType())) {
			int number = issue != null ? issue.number() : pullRequest.number();
			github.addIssueLabel(cipher.decrypt(repository.getUser().getAccessToken()), repository.getOwner(),
					repository.getName(), number, rule.label());
			return;
		}
		if ("summary".equals(action.getActionType())) {
			String title = issue != null ? issue.title() : pullRequest.title();
			String body = issue != null ? issue.body() : pullRequest.body();
			action.setDetail(summaries.summarize(title, body));
			return;
		}
		if ("slack".equals(action.getActionType())) {
			String text = slackText(repository, issue, pullRequest, push, rule, delivery);
			slack.post(cipher.decrypt(repository.getSlackWebhookUrl()), text);
		}
	}

	private String slackText(TrackedRepository repository, IssueMatch issue, PullRequestMatch pullRequest, PushMatch push,
			RuleSettings rule, WebhookDelivery delivery) {
		if (push != null) {
			return push.slackText(repository.getOwner(), repository.getName());
		}
		if (issue != null) {
			return issue.slackText(repository.getOwner(), repository.getName(), rule.label(), savedSummary(delivery));
		}
		return pullRequest.slackText(repository.getOwner(), repository.getName(),
				pullRequest.matches(rule.keyword()) ? rule.label() : null, savedSummary(delivery));
	}

	private String savedSummary(WebhookDelivery delivery) {
		return actions.findByDeliveryAndActionType(delivery, "summary")
				.filter(action -> "succeeded".equals(action.getStatus()))
				.map(BotAction::getDetail)
				.orElse(null);
	}

	private RuleSettings settingsFor(WebhookDelivery delivery) {
		return rules.findByUser(delivery.getRepository().getUser()).map(RuleSettings::from).orElseGet(RuleSettings::defaults);
	}

	private void createAction(WebhookDelivery delivery, String type) {
		if (actions.findByDeliveryAndActionType(delivery, type).isPresent()) {
			return;
		}
		BotAction action = new BotAction();
		action.setDelivery(delivery);
		action.setActionType(type);
		action.setStatus("pending");
		action.setAttempts(0);
		action.setNextAttemptAt(Instant.now());
		actions.save(action);
	}

	private static String trim(String message) {
		if (message == null || message.isBlank()) {
			return "Action failed.";
		}
		return message.length() <= 500 ? message : message.substring(0, 500);
	}

}
