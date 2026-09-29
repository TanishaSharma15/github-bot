package com.githubbot.webhook;

public class WebhookRejectedException extends RuntimeException {

	public WebhookRejectedException() {
		super("Rejected GitHub webhook");
	}

}
