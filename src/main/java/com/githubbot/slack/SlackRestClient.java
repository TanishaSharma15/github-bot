package com.githubbot.slack;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class SlackRestClient implements SlackClient {

	private final RestClient restClient;

	public SlackRestClient() {
		this(RestClient.create());
	}

	SlackRestClient(RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public void post(String webhookUrl, String text) {
		try {
			restClient.post().uri(webhookUrl).body(Map.of("text", text)).retrieve().toBodilessEntity();
		}
		catch (RestClientResponseException ex) {
			throw new SlackRequestException("Slack returned " + ex.getStatusCode().value() + ".");
		}
	}

}
