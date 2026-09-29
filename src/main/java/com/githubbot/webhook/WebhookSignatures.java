package com.githubbot.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class WebhookSignatures {

	private WebhookSignatures() {
	}

	static boolean matches(String secret, byte[] body, String header) {
		if (secret == null || secret.isBlank() || header == null || !header.startsWith("sha256=")) {
			return false;
		}
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
			return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8));
		}
		catch (Exception ex) {
			return false;
		}
	}

}
