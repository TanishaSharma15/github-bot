package com.githubbot.summary;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GroqSummaryClientTest {

	@Test
	void readsTheSummaryFromGroq() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.groq.com/openai/v1");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GroqSummaryClient client = new GroqSummaryClient(builder.build(), "test-key", "llama-3.1-8b-instant");
		server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
				.andExpect(method(POST))
				.andExpect(header("Authorization", "Bearer test-key"))
				.andRespond(withSuccess("""
						{"choices":[{"message":{"content":"Login fails on the homepage."}}]}
						""", MediaType.APPLICATION_JSON));

		assertTrue(client.isConfigured());
		assertEquals("Login fails on the homepage.", client.summarize("Login bug", "The form rejects the password."));
		server.verify();
	}

	@Test
	void blankKeyDoesNotCallGroq() {
		GroqSummaryClient client = new GroqSummaryClient(RestClient.create(), "  ", "llama-3.1-8b-instant");

		assertFalse(client.isConfigured());
		SummaryRequestException failure = assertThrows(SummaryRequestException.class,
				() -> client.summarize("Login bug", "The form rejects the password."));
		assertEquals("Groq is not configured.", failure.getMessage());
	}

	@Test
	void httpErrorDoesNotIncludeTheKey() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.groq.com/openai/v1");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GroqSummaryClient client = new GroqSummaryClient(builder.build(), "secret-key", "llama-3.1-8b-instant");
		server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
				.andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

		SummaryRequestException failure = assertThrows(SummaryRequestException.class,
				() -> client.summarize("Login bug", "The form rejects the password."));

		assertEquals("Groq returned 401.", failure.getMessage());
		server.verify();
	}

}
