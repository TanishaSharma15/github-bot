package com.githubbot.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PageAccessTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthStaysPublic() throws Exception {
		mockMvc.perform(get("/health"))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"status\":\"ok\"}"));
	}

	@Test
	void homeShowsSignInWhenLoggedOut() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Sign in with GitHub")));
	}

	@Test
	void dashboardRedirectsWhenLoggedOut() throws Exception {
		mockMvc.perform(get("/dashboard"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/oauth2/authorization/github"));
	}

	@Test
	void dashboardShowsUsernameWhenLoggedIn() throws Exception {
		mockMvc.perform(get("/dashboard").with(oauth2Login().attributes(attrs -> attrs.put("login", "octocat"))))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("octocat")));
	}

}
