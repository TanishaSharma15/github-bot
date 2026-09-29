package com.githubbot.action;

import java.util.Locale;

import com.githubbot.webhook.WebhookDelivery;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class IssueMatch {

	private static final ObjectMapper JSON = new ObjectMapper();

	private final int number;

	private final String title;

	private final String body;

	private IssueMatch(int number, String title, String body) {
		this.number = number;
		this.title = title;
		this.body = body;
	}

	static IssueMatch opened(WebhookDelivery delivery) {
		if (!"issues".equals(delivery.getEvent())) {
			return null;
		}
		try {
			JsonNode root = JSON.readTree(delivery.getPayload());
			if (!"opened".equals(root.path("action").asString())) {
				return null;
			}
			JsonNode issue = root.path("issue");
			if (!issue.hasNonNull("number")) {
				return null;
			}
			String title = issue.path("title").asString("");
			String body = issue.path("body").isNull() ? "" : issue.path("body").asString("");
			return new IssueMatch(issue.path("number").asInt(), title, body);
		}
		catch (RuntimeException ex) {
			return null;
		}
	}

	boolean matches(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return false;
		}
		return contains(title, keyword) || contains(body, keyword);
	}

	int number() {
		return number;
	}

	String slackText(String owner, String name, String label) {
		return "Labeled " + owner + "/" + name + "#" + number + " as " + label + ": " + title + "\nhttps://github.com/"
				+ owner + "/" + name + "/issues/" + number;
	}

	private static boolean contains(String value, String keyword) {
		return value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
	}

}
