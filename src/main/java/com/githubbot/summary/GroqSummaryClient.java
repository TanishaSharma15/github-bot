package com.githubbot.summary;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class GroqSummaryClient implements SummaryClient {

	private static final ObjectMapper JSON = new ObjectMapper();

	private static final int BODY_LIMIT = 4000;

	private final RestClient restClient;

	private final String apiKey;

	private final String model;

	@Autowired
	public GroqSummaryClient(@Value("${app.groq-api-key:}") String apiKey,
			@Value("${app.groq-model:llama-3.1-8b-instant}") String model) {
		this(RestClient.builder().baseUrl("https://api.groq.com/openai/v1").build(), apiKey, model);
	}

	GroqSummaryClient(RestClient restClient, String apiKey, String model) {
		this.restClient = restClient;
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.model = model == null || model.isBlank() ? "llama-3.1-8b-instant" : model.trim();
	}

	@Override
	public boolean isConfigured() {
		return !apiKey.isBlank();
	}

	@Override
	public String summarize(String title, String body) {
		if (!isConfigured()) {
			throw new SummaryRequestException("Groq is not configured.");
		}
		String clipped = body == null ? "" : body;
		if (clipped.length() > BODY_LIMIT) {
			clipped = clipped.substring(0, BODY_LIMIT);
		}
		Map<String, Object> request = Map.of(
				"model", model,
				"temperature", 0.2,
				"max_tokens", 120,
				"messages", List.of(
						Map.of("role", "system", "content", "Summarize this GitHub issue in one or two plain sentences."),
						Map.of("role", "user", "content", "Title: " + title + "\n\n" + clipped)));
		try {
			String raw = restClient.post()
					.uri("/chat/completions")
					.header("Authorization", "Bearer " + apiKey)
					.body(request)
					.retrieve()
					.body(String.class);
			return text(raw);
		}
		catch (RestClientResponseException ex) {
			throw new SummaryRequestException("Groq returned " + ex.getStatusCode().value() + ".");
		}
		catch (SummaryRequestException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new SummaryRequestException("Groq could not summarize the issue.");
		}
	}

	private static String text(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new SummaryRequestException("Groq returned an empty summary.");
		}
		try {
			JsonNode content = JSON.readTree(raw).path("choices").path(0).path("message").path("content");
			String summary = content.asString("").trim();
			if (summary.isEmpty()) {
				throw new SummaryRequestException("Groq returned an empty summary.");
			}
			return summary.length() <= 1000 ? summary : summary.substring(0, 1000);
		}
		catch (SummaryRequestException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new SummaryRequestException("Groq returned an empty summary.");
		}
	}

}
