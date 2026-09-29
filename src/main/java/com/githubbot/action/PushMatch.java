package com.githubbot.action;

import com.githubbot.webhook.WebhookDelivery;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class PushMatch {

	private static final ObjectMapper JSON = new ObjectMapper();

	private final String branch;

	private final String author;

	private final int commits;

	private final String message;

	private final String compareUrl;

	private PushMatch(String branch, String author, int commits, String message, String compareUrl) {
		this.branch = branch;
		this.author = author;
		this.commits = commits;
		this.message = message;
		this.compareUrl = compareUrl;
	}

	static PushMatch from(WebhookDelivery delivery) {
		if (!"push".equals(delivery.getEvent())) {
			return null;
		}
		try {
			JsonNode root = JSON.readTree(delivery.getPayload());
			if (!root.hasNonNull("ref")) {
				return null;
			}
			String author = root.path("pusher").path("name").asString("");
			if (author.isBlank()) {
				author = root.path("sender").path("login").asString("");
			}
			JsonNode commits = root.path("commits");
			int commitCount = commits.isArray() ? commits.size() : 0;
			String message = root.path("head_commit").path("message").asString("");
			if (message.isBlank() && commitCount > 0) {
				message = commits.get(commitCount - 1).path("message").asString("");
			}
			return new PushMatch(branch(root.path("ref").asString("")), author, commitCount, firstLine(message),
					root.path("compare").asString(""));
		}
		catch (RuntimeException ex) {
			return null;
		}
	}

	String slackText(String owner, String name) {
		String count = commits == 1 ? "1 commit" : commits + " commits";
		String text = "Pushed " + count + " to " + owner + "/" + name + " on " + branch;
		if (!author.isBlank()) {
			text = text + " by " + author;
		}
		if (!message.isBlank()) {
			text = text + ": " + message;
		}
		if (!compareUrl.isBlank()) {
			text = text + "\n" + compareUrl;
		}
		return text;
	}

	private static String branch(String ref) {
		if (ref.startsWith("refs/heads/")) {
			return ref.substring("refs/heads/".length());
		}
		if (ref.startsWith("refs/tags/")) {
			return ref.substring("refs/tags/".length());
		}
		return ref;
	}

	private static String firstLine(String message) {
		int lineBreak = message.indexOf('\n');
		return lineBreak < 0 ? message : message.substring(0, lineBreak);
	}

}
