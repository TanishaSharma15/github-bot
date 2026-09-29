package com.githubbot.web;

import com.githubbot.action.BotRule;
import com.githubbot.auth.User;
import com.githubbot.auth.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DashboardController {

	private final UserRepository users;

	private final DashboardService dashboard;

	public DashboardController(UserRepository users, DashboardService dashboard) {
		this.users = users;
		this.dashboard = dashboard;
	}

	@GetMapping("/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		User user = currentUser(authentication);
		model.addAttribute("login", HomeController.loginOf(authentication));
		if (user == null) {
			model.addAttribute("rule", guestRule());
			model.addAttribute("events", java.util.List.of());
			return "dashboard";
		}
		model.addAttribute("rule", dashboard.ruleFor(user));
		model.addAttribute("events", dashboard.eventsFor(user));
		return "dashboard";
	}

	@PostMapping("/dashboard")
	public String save(Authentication authentication, @RequestParam String keyword, @RequestParam String label,
			@RequestParam(required = false) String slackEnabled, RedirectAttributes redirect, Model model) {
		User user = currentUser(authentication);
		if (user == null) {
			throw new IllegalStateException("Sign in with GitHub again.");
		}
		boolean slackOn = "true".equals(slackEnabled);
		try {
			dashboard.saveRule(user, keyword, label, slackOn);
			redirect.addFlashAttribute("notice", "Saved the rule.");
			return "redirect:/dashboard";
		}
		catch (IllegalArgumentException ex) {
			model.addAttribute("login", user.getGithubLogin());
			model.addAttribute("rule", draft(user, keyword, label, slackOn));
			model.addAttribute("events", dashboard.eventsFor(user));
			model.addAttribute("error", ex.getMessage());
			return "dashboard";
		}
	}

	private User currentUser(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User principal)) {
			return null;
		}
		Object id = principal.getAttribute("id");
		if (id == null) {
			return null;
		}
		long githubId = id instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(id));
		return users.findByGithubId(githubId).orElse(null);
	}

	private static BotRule guestRule() {
		BotRule rule = new BotRule();
		rule.setKeyword("bug");
		rule.setLabel("bug");
		rule.setSlackEnabled(true);
		return rule;
	}

	private static BotRule draft(User user, String keyword, String label, boolean slackEnabled) {
		BotRule rule = new BotRule();
		rule.setUser(user);
		rule.setKeyword(keyword == null ? "" : keyword);
		rule.setLabel(label == null ? "" : label);
		rule.setSlackEnabled(slackEnabled);
		return rule;
	}

}
