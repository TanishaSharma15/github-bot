package com.githubbot.action;

import java.util.Locale;

import com.githubbot.webhook.WebhookDelivery;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class PullRequestMatch {

	private static final ObjectMapper JSON = new ObjectMapper();

	private final int number;

	private final String title;

	private final String body;

	private final String author;

	private PullRequestMatch(int number, String title, String body, String author) {
		this.number = number;
		this.title = title;
		this.body = body;
		this.author = author;
	}

	static PullRequestMatch opened(WebhookDelivery delivery) {
		if (!"pull_request".equals(delivery.getEvent())) {
			return null;
		}
		try {
			JsonNode root = JSON.readTree(delivery.getPayload());
			if (!"opened".equals(root.path("action").asString())) {
				return null;
			}
			JsonNode pullRequest = root.path("pull_request");
			if (!pullRequest.hasNonNull("number")) {
				return null;
			}
			String title = pullRequest.path("title").asString("");
			String body = pullRequest.path("body").isNull() ? "" : pullRequest.path("body").asString("");
			String author = pullRequest.path("user").path("login").asString("");
			return new PullRequestMatch(pullRequest.path("number").asInt(), title, body, author);
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

	String title() {
		return title;
	}

	String body() {
		return body;
	}

	String slackText(String owner, String name, String label, String summary) {
		String heading;
		if (label == null || label.isBlank()) {
			heading = "Opened pull request " + owner + "/" + name + "#" + number + ": " + title;
		}
		else {
			heading = "Labeled " + owner + "/" + name + "#" + number + " as " + label + ": " + title;
		}
		if (!author.isBlank()) {
			heading = heading + " by " + author;
		}
		String text = heading + "\nhttps://github.com/" + owner + "/" + name + "/pull/" + number;
		if (summary != null && !summary.isBlank()) {
			text = text + "\n" + summary.trim();
		}
		return text;
	}

	private static boolean contains(String value, String keyword) {
		return value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
	}

}
