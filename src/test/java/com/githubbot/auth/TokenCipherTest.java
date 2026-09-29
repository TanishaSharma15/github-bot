package com.githubbot.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TokenCipherTest {

	@Test
	void encryptsTokenSoTheStoredValueIsNotTheRawToken() {
		TokenCipher cipher = new TokenCipher("test-encryption-key");
		String raw = "gho_example_token_value";

		String stored = cipher.encrypt(raw);

		assertFalse(stored.contains(raw));
		assertEquals(raw, cipher.decrypt(stored));
	}

}
