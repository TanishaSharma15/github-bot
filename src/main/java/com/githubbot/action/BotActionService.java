package com.githubbot.action;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.githubbot.auth.TokenCipher;
import com.githubbot.github.GitHubClient;
import com.githubbot.repo.TrackedRepository;
import com.githubbot.slack.SlackClient;
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

	private final GitHubClient github;

	private final SlackClient slack;

	private final TokenCipher cipher;

	private final Duration retryDelay;

	public BotActionService(WebhookDeliveryRepository deliveries, BotActionRepository actions, GitHubClient github,
			SlackClient slack, TokenCipher cipher, @Value("${app.action-retry-delay:30s}") Duration retryDelay) {
		this.deliveries = deliveries;
		this.actions = actions;
		this.github = github;
		this.slack = slack;
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
		IssueMatch match = IssueMatch.from(delivery);
		if (match == null) {
			delivery.setStatus("skipped");
			return;
		}
		createAction(delivery, "label");
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
		IssueMatch match = IssueMatch.from(delivery);
		if (match == null) {
			throw new IllegalStateException("This delivery is not an opened bug issue.");
		}
		TrackedRepository repository = delivery.getRepository();
		if ("label".equals(action.getActionType())) {
			github.addIssueLabel(cipher.decrypt(repository.getUser().getAccessToken()), repository.getOwner(),
					repository.getName(), match.number(), "bug");
			return;
		}
		if ("slack".equals(action.getActionType())) {
			slack.post(cipher.decrypt(repository.getSlackWebhookUrl()),
					match.slackText(repository.getOwner(), repository.getName()));
		}
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
