package com.githubbot.webhook;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {

	private final WebhookReceiver receiver;

	public WebhookController(WebhookReceiver receiver) {
		this.receiver = receiver;
	}

	@PostMapping("/webhooks/github")
	public ResponseEntity<Void> receive(HttpServletRequest request) throws IOException {
		try {
			receiver.accept(request.getHeader("X-GitHub-Event"), request.getHeader("X-GitHub-Delivery"),
					request.getHeader("X-GitHub-Hook-ID"), request.getHeader("X-Hub-Signature-256"),
					request.getInputStream().readAllBytes());
			return ResponseEntity.ok().build();
		}
		catch (WebhookRejectedException ex) {
			return ResponseEntity.status(401).build();
		}
		catch (DataIntegrityViolationException ex) {
			return ResponseEntity.ok().build();
		}
	}

}
