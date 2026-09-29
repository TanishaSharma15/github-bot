package com.githubbot.auth;

import java.time.Instant;

import org.springframework.stereotype.Service;

@Service
public class UserAccountService {

	private final UserRepository users;

	private final TokenCipher cipher;

	public UserAccountService(UserRepository users, TokenCipher cipher) {
		this.users = users;
		this.cipher = cipher;
	}

	public User upsert(long githubId, String login, String rawToken) {
		User user = users.findByGithubId(githubId).orElseGet(() -> {
			User created = new User();
			created.setGithubId(githubId);
			created.setCreatedAt(Instant.now());
			return created;
		});
		user.setGithubLogin(login);
		user.setAccessToken(cipher.encrypt(rawToken));
		return users.save(user);
	}

}
