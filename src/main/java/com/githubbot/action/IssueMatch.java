package com.githubbot.action;

import java.util.Locale;

import com.githubbot.webhook.WebhookDelivery;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class IssueMatch {

	private static final ObjectMapper JSON = new ObjectMapper();

	private final int number;

	private final String title;

	private IssueMatch(int number, String title) {
		this.number = number;
		this.title = title;
	}

	static IssueMatch from(WebhookDelivery delivery) {
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
			if (!containsBug(title) && !containsBug(body)) {
				return null;
			}
			return new IssueMatch(issue.path("number").asInt(), title);
		}
		catch (RuntimeException ex) {
			return null;
		}
	}

	int number() {
		return number;
	}

	String slackText(String owner, String name) {
		return "Labeled " + owner + "/" + name + "#" + number + " as bug: " + title + "\nhttps://github.com/" + owner
				+ "/" + name + "/issues/" + number;
	}

	private static boolean containsBug(String value) {
		return value.toLowerCase(Locale.ROOT).contains("bug");
	}

}
