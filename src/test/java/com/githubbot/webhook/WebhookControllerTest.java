package com.githubbot.webhook;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.githubbot.auth.TokenCipher;
import com.githubbot.auth.User;
import com.githubbot.auth.UserRepository;
import com.githubbot.repo.TrackedRepository;
import com.githubbot.repo.TrackedRepositoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebhookControllerTest {

	private static final String SECRET = "hook-secret";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository users;

	@Autowired
	private TrackedRepositoryRepository repositories;

	@Autowired
	private WebhookDeliveryRepository deliveries;

	@Autowired
	private TokenCipher cipher;

	@BeforeEach
	void connectedRepository() {
		deliveries.deleteAll();
		repositories.deleteAll();
		users.deleteAll();
		User user = new User();
		user.setGithubId(99L);
		user.setGithubLogin("octocat");
		user.setAccessToken(cipher.encrypt("raw-token"));
		user.setCreatedAt(Instant.now());
		users.save(user);
		TrackedRepository repository = new TrackedRepository();
		repository.setUser(user);
		repository.setGithubRepoId(7L);
		repository.setOwner("octocat");
		repository.setName("github-bot-demo");
		repository.setWebhookId(55L);
		repository.setWebhookSecret(cipher.encrypt(SECRET));
		repository.setSlackWebhookUrl(cipher.encrypt("https://hooks.slack.com/services/T/B/secret"));
		repositories.save(repository);
	}

	@Test
	void validDeliveryIsStoredOnce() throws Exception {
		byte[] body = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

		mockMvc.perform(delivery("issues", "delivery-1", body)).andExpect(status().isOk());
		mockMvc.perform(delivery("issues", "delivery-1", body)).andExpect(status().isOk());

		assertEquals(1, deliveries.countByDeliveryId("delivery-1"));
		WebhookDelivery stored = deliveries.findByDeliveryId("delivery-1").orElseThrow();
		assertEquals("issues", stored.getEvent());
		assertEquals("received", stored.getStatus());
		assertEquals("{\"action\":\"opened\"}", stored.getPayload());
	}

	@Test
	void pingIsStored() throws Exception {
		byte[] body = "{\"zen\":\"keep it logically awesome\"}".getBytes(StandardCharsets.UTF_8);

		mockMvc.perform(delivery("ping", "delivery-ping", body)).andExpect(status().isOk());

		assertEquals("ping", deliveries.findByDeliveryId("delivery-ping").orElseThrow().getEvent());
	}

	@Test
	void badSignatureIsRejectedAndStoresNothing() throws Exception {
		byte[] body = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

		mockMvc.perform(post("/webhooks/github").contentType(MediaType.APPLICATION_JSON).content(body)
				.header("X-GitHub-Event", "issues")
				.header("X-GitHub-Delivery", "delivery-bad")
				.header("X-GitHub-Hook-ID", "55")
				.header("X-Hub-Signature-256", "sha256=deadbeef"))
				.andExpect(status().isUnauthorized());

		assertEquals(0, deliveries.count());
	}

	@Test
	void missingSignatureIsRejected() throws Exception {
		mockMvc.perform(post("/webhooks/github").contentType(MediaType.APPLICATION_JSON).content("{}")
				.header("X-GitHub-Event", "issues")
				.header("X-GitHub-Delivery", "delivery-missing")
				.header("X-GitHub-Hook-ID", "55"))
				.andExpect(status().isUnauthorized());

		assertEquals(0, deliveries.count());
	}

	@Test
	void unknownHookIsRejected() throws Exception {
		byte[] body = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

		mockMvc.perform(post("/webhooks/github").contentType(MediaType.APPLICATION_JSON).content(body)
				.header("X-GitHub-Event", "issues")
				.header("X-GitHub-Delivery", "delivery-unknown")
				.header("X-GitHub-Hook-ID", "999")
				.header("X-Hub-Signature-256", sign(body)))
				.andExpect(status().isUnauthorized());

		assertEquals(0, deliveries.count());
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder delivery(String event,
			String deliveryId, byte[] body) throws Exception {
		return post("/webhooks/github").contentType(MediaType.APPLICATION_JSON).content(body)
				.header("X-GitHub-Event", event)
				.header("X-GitHub-Delivery", deliveryId)
				.header("X-GitHub-Hook-ID", "55")
				.header("X-Hub-Signature-256", sign(body));
	}

	private static String sign(byte[] body) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
	}

}
