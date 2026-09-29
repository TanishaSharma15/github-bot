package com.githubbot.web;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.githubbot.action.BotAction;
import com.githubbot.action.BotActionRepository;
import com.githubbot.action.BotRule;
import com.githubbot.action.BotRuleRepository;
import com.githubbot.auth.User;
import com.githubbot.webhook.WebhookDelivery;
import com.githubbot.webhook.WebhookDeliveryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

	private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

	private static final int MAX_LENGTH = 50;

	private final BotRuleRepository rules;

	private final WebhookDeliveryRepository deliveries;

	private final BotActionRepository actions;

	public DashboardService(BotRuleRepository rules, WebhookDeliveryRepository deliveries, BotActionRepository actions) {
		this.rules = rules;
		this.deliveries = deliveries;
		this.actions = actions;
	}

	@Transactional(readOnly = true)
	public BotRule ruleFor(User user) {
		return rules.findByUser(user).orElseGet(() -> defaultRule(user));
	}

	@Transactional(readOnly = true)
	public List<EventRow> eventsFor(User user) {
		List<WebhookDelivery> deliveriesForUser = deliveries.findForUser(user);
		if (deliveriesForUser.isEmpty()) {
			return List.of();
		}
		List<Long> ids = deliveriesForUser.stream().map(WebhookDelivery::getId).toList();
		List<BotAction> stored = actions.findByDeliveryIdIn(ids);
		return deliveriesForUser.stream().map(delivery -> new EventRow(
				WHEN.format(delivery.getReceivedAt()),
				delivery.getRepository().getOwner() + "/" + delivery.getRepository().getName(),
				delivery.getEvent(),
				delivery.getStatus(),
				stored.stream()
						.filter(action -> action.getDelivery().getId().equals(delivery.getId()))
						.map(action -> action.getActionType() + " " + action.getStatus())
						.toList()))
				.toList();
	}

	@Transactional
	public void saveRule(User user, String keyword, String label, boolean slackEnabled) {
		String cleanKeyword = clean(keyword);
		String cleanLabel = clean(label);
		if (cleanKeyword == null || cleanLabel == null) {
			throw new IllegalArgumentException("Enter a keyword and a label, each up to 50 characters.");
		}
		BotRule rule = rules.findByUser(user).orElseGet(() -> {
			BotRule created = new BotRule();
			created.setUser(user);
			return created;
		});
		rule.setKeyword(cleanKeyword);
		rule.setLabel(cleanLabel);
		rule.setSlackEnabled(slackEnabled);
		rules.save(rule);
	}

	private static BotRule defaultRule(User user) {
		BotRule rule = new BotRule();
		rule.setUser(user);
		rule.setKeyword("bug");
		rule.setLabel("bug");
		rule.setSlackEnabled(true);
		return rule;
	}

	private static String clean(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
			return null;
		}
		return trimmed;
	}

	public record EventRow(String when, String repository, String event, String status, List<String> actions) {
	}

}
