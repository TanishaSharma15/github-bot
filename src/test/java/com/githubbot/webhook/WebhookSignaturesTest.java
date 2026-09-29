package com.githubbot.webhook;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSignaturesTest {

	@Test
	void acceptsTheSignatureGitHubWouldSend() throws Exception {
		byte[] body = "{\"zen\":\"keep it logically awesome\"}".getBytes(StandardCharsets.UTF_8);

		assertTrue(WebhookSignatures.matches("hook-secret", body, sign("hook-secret", body)));
	}

	@Test
	void rejectsASignatureSignedWithADifferentSecret() throws Exception {
		byte[] body = "{\"zen\":\"keep it logically awesome\"}".getBytes(StandardCharsets.UTF_8);

		assertFalse(WebhookSignatures.matches("hook-secret", body, sign("other-secret", body)));
	}

	@Test
	void rejectsAMissingSignature() {
		assertFalse(WebhookSignatures.matches("hook-secret", new byte[0], null));
	}

	private static String sign(String secret, byte[] body) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
	}

}
