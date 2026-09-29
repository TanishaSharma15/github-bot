package com.githubbot.web;

import java.time.Instant;

import com.githubbot.action.BotAction;
import com.githubbot.action.BotActionRepository;
import com.githubbot.action.BotRule;
import com.githubbot.action.BotRuleRepository;
import com.githubbot.auth.TokenCipher;
import com.githubbot.auth.User;
import com.githubbot.auth.UserRepository;
import com.githubbot.repo.TrackedRepository;
import com.githubbot.repo.TrackedRepositoryRepository;
import com.githubbot.webhook.WebhookDelivery;
import com.githubbot.webhook.WebhookDeliveryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository users;

	@Autowired
	private TrackedRepositoryRepository repositories;

	@Autowired
	private WebhookDeliveryRepository deliveries;

	@Autowired
	private BotActionRepository actions;

	@Autowired
	private BotRuleRepository rules;

	@Autowired
	private TokenCipher cipher;

	@BeforeEach
	void signedInUserWithDelivery() {
		actions.deleteAll();
		deliveries.deleteAll();
		repositories.deleteAll();
		rules.deleteAll();
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
		repository.setWebhookSecret(cipher.encrypt("hook-secret"));
		repository.setSlackWebhookUrl(cipher.encrypt("https://hooks.slack.com/services/T/B/secret"));
		repositories.save(repository);
		WebhookDelivery delivery = new WebhookDelivery();
		delivery.setDeliveryId("delivery-1");
		delivery.setEvent("issues");
		delivery.setRepository(repository);
		delivery.setPayload("{\"action\":\"opened\"}");
		delivery.setReceivedAt(Instant.parse("2026-09-29T14:30:00Z"));
		delivery.setStatus("queued");
		deliveries.save(delivery);
		saveAction(delivery, "label", "succeeded");
		saveAction(delivery, "slack", "succeeded");
	}

	@Test
	void showsTheDeliveryAndItsActions() throws Exception {
		mockMvc.perform(get("/dashboard").with(signedIn()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("octocat/github-bot-demo")))
				.andExpect(content().string(containsString("issues")))
				.andExpect(content().string(containsString("queued")))
				.andExpect(content().string(containsString("label succeeded")))
				.andExpect(content().string(containsString("slack succeeded")))
				.andExpect(content().string(containsString("value=\"bug\"")));
	}

	@Test
	void savingARuleStoresTheKeywordLabelAndSlackSetting() throws Exception {
		mockMvc.perform(post("/dashboard").with(signedIn()).with(csrf())
				.param("keyword", "note")
				.param("label", "question"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dashboard"));

		BotRule saved = rules.findByUser(users.findByGithubId(99L).orElseThrow()).orElseThrow();
		assertEquals("note", saved.getKeyword());
		assertEquals("question", saved.getLabel());
		assertFalse(saved.isSlackEnabled());

		mockMvc.perform(get("/dashboard").with(signedIn()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("value=\"note\"")))
				.andExpect(content().string(containsString("value=\"question\"")));
	}

	private void saveAction(WebhookDelivery delivery, String type, String status) {
		BotAction action = new BotAction();
		action.setDelivery(delivery);
		action.setActionType(type);
		action.setStatus(status);
		action.setAttempts(1);
		action.setNextAttemptAt(Instant.parse("2026-09-29T14:30:00Z"));
		actions.save(action);
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor signedIn() {
		return oauth2Login().attributes(attrs -> {
			attrs.put("id", 99);
			attrs.put("login", "octocat");
		});
	}

}
