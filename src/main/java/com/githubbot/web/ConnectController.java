package com.githubbot.web;

import com.githubbot.auth.User;
import com.githubbot.auth.UserRepository;
import com.githubbot.github.GitHubRequestException;
import com.githubbot.repo.RepositoryConnectionService;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ConnectController {

	private final UserRepository users;

	private final RepositoryConnectionService connections;

	public ConnectController(UserRepository users, RepositoryConnectionService connections) {
		this.users = users;
		this.connections = connections;
	}

	@GetMapping("/repositories")
	public String form(Authentication authentication, Model model) {
		User user = currentUser(authentication);
		model.addAttribute("login", user.getGithubLogin());
		model.addAttribute("connected", connections.listConnected(user));
		try {
			model.addAttribute("repos", connections.listRepos(user));
		}
		catch (GitHubRequestException ex) {
			model.addAttribute("repos", java.util.List.of());
			model.addAttribute("error", ex.getMessage());
		}
		return "connect";
	}

	@PostMapping("/repositories")
	public String connect(Authentication authentication, @RequestParam long githubRepoId,
			@RequestParam String slackWebhookUrl, RedirectAttributes redirect, Model model) {
		User user = currentUser(authentication);
		try {
			var saved = connections.connect(user, githubRepoId, slackWebhookUrl.trim());
			redirect.addFlashAttribute("notice", "Connected " + saved.getOwner() + "/" + saved.getName());
			return "redirect:/repositories";
		}
		catch (IllegalArgumentException | GitHubRequestException ex) {
			model.addAttribute("login", user.getGithubLogin());
			model.addAttribute("connected", connections.listConnected(user));
			model.addAttribute("repos", safeRepos(user));
			model.addAttribute("error", ex.getMessage());
			return "connect";
		}
	}

	private java.util.List<com.githubbot.github.GitHubRepo> safeRepos(User user) {
		try {
			return connections.listRepos(user);
		}
		catch (GitHubRequestException ex) {
			return java.util.List.of();
		}
	}

	private User currentUser(Authentication authentication) {
		OAuth2User principal = (OAuth2User) authentication.getPrincipal();
		Object id = principal.getAttribute("id");
		long githubId = id instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(id));
		return users.findByGithubId(githubId)
				.orElseThrow(() -> new IllegalStateException("Sign in with GitHub again."));
	}

}
