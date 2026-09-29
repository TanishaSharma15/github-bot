package com.githubbot.auth;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenCipher {

	private static final int IV_LENGTH = 12;

	private static final int TAG_LENGTH_BITS = 128;

	private final byte[] key;

	public TokenCipher(@Value("${app.encryption-key}") String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("Set APP_ENCRYPTION_KEY");
		}
		try {
			this.key = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not prepare the encryption key", ex);
		}
	}

	public String encrypt(String raw) {
		try {
			byte[] iv = new byte[IV_LENGTH];
			new SecureRandom().nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			byte[] encrypted = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));
			ByteBuffer packed = ByteBuffer.allocate(iv.length + encrypted.length);
			packed.put(iv);
			packed.put(encrypted);
			return Base64.getEncoder().encodeToString(packed.array());
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not encrypt the GitHub token", ex);
		}
	}

	public String decrypt(String stored) {
		try {
			byte[] packed = Base64.getDecoder().decode(stored);
			ByteBuffer buffer = ByteBuffer.wrap(packed);
			byte[] iv = new byte[IV_LENGTH];
			buffer.get(iv);
			byte[] encrypted = new byte[buffer.remaining()];
			buffer.get(encrypted);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not decrypt the GitHub token", ex);
		}
	}

}
