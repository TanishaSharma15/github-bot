package com.githubbot.webhook;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.githubbot.auth.TokenCipher;
import com.githubbot.repo.TrackedRepository;
import com.githubbot.repo.TrackedRepositoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookReceiver {

	private final TrackedRepositoryRepository repositories;

	private final WebhookDeliveryRepository deliveries;

	private final TokenCipher cipher;

	public WebhookReceiver(TrackedRepositoryRepository repositories, WebhookDeliveryRepository deliveries,
			TokenCipher cipher) {
		this.repositories = repositories;
		this.deliveries = deliveries;
		this.cipher = cipher;
	}

	@Transactional
	public void accept(String event, String deliveryId, String hookIdHeader, String signature, byte[] body) {
		if (event == null || event.isBlank() || deliveryId == null || deliveryId.isBlank()) {
			throw new WebhookRejectedException();
		}
		long hookId = hookId(hookIdHeader);
		TrackedRepository repository = repositories.findFirstByWebhookId(hookId).orElseThrow(WebhookRejectedException::new);
		if (!WebhookSignatures.matches(cipher.decrypt(repository.getWebhookSecret()), body, signature)) {
			throw new WebhookRejectedException();
		}
		if (deliveries.findByDeliveryId(deliveryId).isPresent()) {
			return;
		}
		WebhookDelivery delivery = new WebhookDelivery();
		delivery.setDeliveryId(deliveryId);
		delivery.setEvent(event);
		delivery.setRepository(repository);
		delivery.setPayload(new String(body, StandardCharsets.UTF_8));
		delivery.setReceivedAt(Instant.now());
		delivery.setStatus("received");
		deliveries.saveAndFlush(delivery);
	}

	private static long hookId(String header) {
		if (header == null || header.isBlank()) {
			throw new WebhookRejectedException();
		}
		try {
			return Long.parseLong(header);
		}
		catch (NumberFormatException ex) {
			throw new WebhookRejectedException();
		}
	}

}
