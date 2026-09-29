package com.githubbot.auth;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountServiceTest {

	@Test
	void storesCiphertextRatherThanTheGitHubToken() {
		UserRepository users = mock(UserRepository.class);
		when(users.findByGithubId(42L)).thenReturn(Optional.empty());
		when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		UserAccountService service = new UserAccountService(users, new TokenCipher("test-encryption-key"));

		User saved = service.upsert(42L, "octocat", "gho_example_token_value");

		assertEquals(42L, saved.getGithubId());
		assertEquals("octocat", saved.getGithubLogin());
		assertFalse(saved.getAccessToken().contains("gho_example_token_value"));
		verify(users).save(saved);
	}

}
