package com.githubbot.slack;

public interface SlackClient {

	void post(String webhookUrl, String text);

}
